import model.*;

import java.util.*;

public final class MyStrategy implements Strategy {

    private static final double LOW_HP_FACTOR = 0.30D;
    private static final int SAFE_DISTANCE = 400;
    private static final int SAFE_MINION_ATACK_DISTANCE = 150;

    private Random random;
    private Wizard self;
    private World world;
    private Game game;
    private Move move;
    private MovesHelper movesHelper = MovesHelper.getInstance();
    private AtackHelper atackHelper = AtackHelper.getInstance();


    /**
     * Основной метод стратегии, осуществляющий управление волшебником.
     * Вызывается каждый тик для каждого волшебника.
     *
     * @param self  Волшебник, которым данный метод будет осуществлять управление.
     * @param world Текущее состояние мира.
     * @param game  Различные игровые константы.
     * @param move  Результатом работы метода является изменение полей данного объекта.
     */
    @Override
    public void move(Wizard self, World world, Game game, Move move) {
        initializeTick(self, world, game, move);
        movesHelper.initializeTick(self, world, game, move);
        atackHelper.initializeTick(self, world, game, move);
        initializeStrategy(game);

        // Постоянно двигаемся из-стороны в сторону, чтобы по нам было сложнее попасть.
        // Считаете, что сможете придумать более эффективный алгоритм уклонения? Попробуйте! ;)
        move.setStrafeSpeed(random.nextBoolean() ? game.getWizardStrafeSpeed() : -game.getWizardStrafeSpeed());

        if (safe()) {
            goTo(movesHelper.getPreviousWaypoint());
            return;
        }
        if (atack()) {
            return;
        }
        moving();
    }

    /**
     * Сохраняем все входные данные в полях класса для упрощения доступа к ним.
     */
    private void initializeTick(Wizard self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;
    }

    /**
     * Инциализируем стратегию.
     * <p>
     * Для этих целей обычно можно использовать конструктор, однако в данном случае мы хотим инициализировать генератор
     * случайных чисел значением, полученным от симулятора игры.
     */
    private void initializeStrategy(Game game) {
        if (random == null) {
            random = new Random(game.getRandomSeed());
        }
        movesHelper.initialize();
    }


    /**
     * Простейший способ перемещения волшебника.
     */
    private void goTo(Point2D point) {
        double angle = self.getAngleTo(point.getX(), point.getY());

        move.setTurn(angle);

        if (StrictMath.abs(angle) < game.getStaffSector() / 4.0D) {
            move.setSpeed(game.getWizardForwardSpeed());
        }
    }

    /**
     * Если осталось мало жизненной энергии, отступаем к предыдущей ключевой точке на линии.
     */
    private boolean safe() {
        if (self.getLife() < self.getMaxLife() * LOW_HP_FACTOR) {
            return true;
        }

        int count = 0;
        for (LivingUnit livingUnit : world.getMinions()) {
            if (livingUnit.getFaction() == Faction.NEUTRAL || livingUnit.getFaction() == self.getFaction()) {
                continue;
            }
            double distance = self.getDistanceTo(livingUnit);
            if (distance < SAFE_MINION_ATACK_DISTANCE) {
                count += 3;
            } else if (distance < SAFE_DISTANCE) {
                count++;
            }
        }
        return count >= 3;
    }

    /**
     * Атакуем противника, если он виден.
     */
    private boolean atack() {
        LivingUnit nearestTarget = atackHelper.getNearestTarget();

        // Если видим противника ...
        if (nearestTarget != null) {
            double distance = self.getDistanceTo(nearestTarget);

            // ... и он в пределах досягаемости наших заклинаний, ...
            if (distance <= self.getCastRange()) {
                double angle = self.getAngleTo(nearestTarget);

                // ... то поворачиваемся к цели.
                move.setTurn(angle);

                // Если цель перед нами, ...
                if (StrictMath.abs(angle) < game.getStaffSector() / 2.0D) {
                    // ... то атакуем.
                    move.setAction(ActionType.MAGIC_MISSILE);
                    move.setCastAngle(angle);
                    move.setMinCastDistance(distance - nearestTarget.getRadius() + game.getMagicMissileRadius());
                }

                return true;
            }
        }
        return false;
    }

    /**
     * Если нет других действий, просто продвигаемся вперёд.
     */
    private void moving() {
        goTo(movesHelper.getNextWaypoint());
    }
}