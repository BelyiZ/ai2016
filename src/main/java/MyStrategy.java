import model.*;

import java.util.*;

public final class MyStrategy implements Strategy {

    private static final double LOW_HP_FACTOR = 0.30D;
    private static final int SAFE_DISTANCE = 400;
    private static final int SAFE_MINION_ATTACK_DISTANCE = 150;
    private static final int ADDL_STEP_TICK = 50;

    private Random random;
    private Wizard self;
    private World world;
    private Game game;
    private Move move;
    private MovesHelper movesHelper = MovesHelper.getInstance();
    private AttackHelper attackHelper = AttackHelper.getInstance();


    private int addlStepCount = 0;
    private double strafeSpeed;
    private boolean lastStrafeSpeed = false;


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
        if (world.getTickIndex() < 300) {
            return;
        }
        initializeTick(self, world, game, move);
        movesHelper.initializeTick(self, world, game);
        attackHelper.initializeTick(self, world, game);
        initializeStrategy(game);

        if (safe()) {
            runBack();
//            addlStep();
            return;
        }
        if (atack()) {
            addlStep();
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
        movesHelper.initParams();
    }


    /**
     * Простейший способ перемещения волшебника.
     */
    private void goTo(Point2D point, boolean useReverse) {
        double angle = self.getAngleTo(point.getX(), point.getY());

        if (useReverse && Math.abs(angle) > Math.PI / 2) {
            if (angle > 0)
                angle = Math.PI - angle;
            else
                angle = -1 * (Math.PI + angle);
            move.setTurn(angle * -1);
        } else {
            move.setTurn(angle);
        }

        if (StrictMath.abs(angle) < game.getStaffSector() / 4.0D) {
            move.setSpeed(game.getWizardForwardSpeed());
            if (useReverse && Math.abs(self.getAngleTo(point.getX(), point.getY())) > Math.PI / 2) {
                move.setSpeed(-game.getWizardBackwardSpeed());
            }
        }
    }

    private void runBack() {
        Point2D prevWaypoint = movesHelper.getPreviousWaypoint();
        LivingUnit nearestTarget = attackHelper.getNearestTarget();
        if (nearestTarget != null) {
            double angleToNearestTarget = self.getAngleTo(nearestTarget.getX(), nearestTarget.getY());
            double distanceToNearestTarget = self.getDistanceTo(nearestTarget);

            if (Math.abs(angleToNearestTarget) <= Math.abs(game.getStaffSector() / 2.0)) {
                move.setAction(ActionType.MAGIC_MISSILE);
                move.setCastAngle(angleToNearestTarget);
                move.setMinCastDistance(distanceToNearestTarget - nearestTarget.getRadius() + game.getMagicMissileRadius());
            }
        }
        goTo(prevWaypoint, true);
    }

    /**
     * Если осталось мало жизненной энергии, отступаем к предыдущей ключевой точке на линии.
     */
    private boolean safe() {
        Double distancetoEnemyBase = attackHelper.getDistanceToEnemyBase();
        if (distancetoEnemyBase != null && distancetoEnemyBase < self.getCastRange()) {
            return false;
        }
        if (self.getLife() < self.getMaxLife() * LOW_HP_FACTOR) {
            return true;
        }

        int count = 0;
        for (LivingUnit livingUnit : world.getMinions()) {
            if (livingUnit.getFaction() == Faction.NEUTRAL || livingUnit.getFaction() == self.getFaction()) {
                continue;
            }
            double distance = self.getDistanceTo(livingUnit);
            if (distance < SAFE_MINION_ATTACK_DISTANCE) {
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
        LivingUnit nearestTarget = attackHelper.getNearestTarget();

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
            } else {
                goTo(new Point2D(nearestTarget.getX(), nearestTarget.getY()), false);
            }
        }
        return false;
    }

    /**
     * Постоянно двигаемся из-стороны в сторону, чтобы по нам было сложнее попасть.
     *
     */
    private void addlStep() {
        if (addlStepCount == 0) {
            strafeSpeed = lastStrafeSpeed ? game.getWizardStrafeSpeed() : -game.getWizardStrafeSpeed();
            addlStepCount++;
        } else if (addlStepCount == ADDL_STEP_TICK) {
            addlStepCount = 0;
            lastStrafeSpeed = !lastStrafeSpeed;
        } else {
            addlStepCount++;
        }
        move.setStrafeSpeed(strafeSpeed);
    }

    /**
     * Если нет других действий, просто продвигаемся вперёд.
     */
    private void moving() {
        Point2D nextWaypoint = movesHelper.getNextWaypoint();
        goTo(nextWaypoint, false);
        int count = StrategyHelper.countUnitByPath(Arrays.asList(world.getTrees()), self, nextWaypoint);
        if (count != 0) {
            LivingUnit nearestTarget = attackHelper.getNearestTarget(world.getTrees());
            if (nearestTarget != null) {
                double distance = self.getDistanceTo(nearestTarget);
                move.setAction(ActionType.MAGIC_MISSILE);
                move.setCastAngle(self.getAngleTo(nearestTarget));
                move.setMinCastDistance(distance - nearestTarget.getRadius() + game.getMagicMissileRadius());
                addlStep();
            }
        }
    }
}