import model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

class MovesHelper {

    private static final double WAYPOINT_RADIUS = 100.0D;
    private static final int TILE_SIZE = 400;
    private static final int INDENT_LENGTH = 200;

    private static MovesHelper helper;

    static synchronized MovesHelper getInstance() {
        if (helper == null) {
            helper = new MovesHelper();
        }
        return helper;
    }


    private Wizard self;
    private World world;
    private Game game;
    private Move move;
    private Point2D[] waypoints;

    private Random random;


    /**
     * Сохраняем все входные данные в полях класса для упрощения доступа к ним.
     */
    void initializeTick(Wizard self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;
        if (random == null) {
            random = new Random(game.getRandomSeed());
        }
    }

    void initialize() {
        double mapSize = game.getMapSize();
        LineType lineType = selectMoveLine();

        if (LineType.MIDDLE.equals(lineType)) {
            waypoints = new Point2D[] {
                    new Point2D(100.0D, mapSize - 100.0D),
                    random.nextBoolean()
                            ? new Point2D(600.0D, mapSize - 200.0D)
                            : new Point2D(200.0D, mapSize - 600.0D),
                    new Point2D(800.0D, mapSize - 800.0D),
                    new Point2D(mapSize - 600.0D, 600.0D)
            };
        } else if (LineType.TOP.equals(lineType)) {
            waypoints = new Point2D[] {
                    new Point2D(100.0D, mapSize - 100.0D),
                    new Point2D(100.0D, mapSize - 400.0D),
                    new Point2D(200.0D, mapSize - 800.0D),
                    new Point2D(200.0D, mapSize * 0.75D),
                    new Point2D(200.0D, mapSize * 0.5D),
                    new Point2D(200.0D, mapSize * 0.25D),
                    new Point2D(200.0D, 200.0D),
                    new Point2D(mapSize * 0.25D, 200.0D),
                    new Point2D(mapSize * 0.5D, 200.0D),
                    new Point2D(mapSize * 0.75D, 200.0D),
                    new Point2D(mapSize - 200.0D, 200.0D)
            };
        } else {
            waypoints = new Point2D[]{
                    new Point2D(100.0D, mapSize - 100.0D),
                    new Point2D(400.0D, mapSize - 100.0D),
                    new Point2D(800.0D, mapSize - 200.0D),
                    new Point2D(mapSize * 0.25D, mapSize - 200.0D),
                    new Point2D(mapSize * 0.5D, mapSize - 200.0D),
                    new Point2D(mapSize * 0.75D, mapSize - 200.0D),
                    new Point2D(mapSize - 200.0D, mapSize - 200.0D),
                    new Point2D(mapSize - 200.0D, mapSize * 0.75D),
                    new Point2D(mapSize - 200.0D, mapSize * 0.5D),
                    new Point2D(mapSize - 200.0D, mapSize * 0.25D),
                    new Point2D(mapSize - 200.0D, 200.0D)
            };
        }


    }


    private LineType selectMoveLine() {
        switch ((int) self.getId()) {
            case 1:
            case 2:
            case 6:
            case 7:
                return LineType.TOP;
            case 3:
            case 8:
                return LineType.MIDDLE;
            case 4:
            case 5:
            case 9:
            case 10:
                return LineType.BOTTOM;
            default:
                return LineType.MIDDLE;
        }
    }


    /**
     * Данный метод предполагает, что все ключевые точки на линии упорядочены по уменьшению дистанции до последней
     * ключевой точки. Перебирая их по порядку, находим первую попавшуюся точку, которая находится ближе к последней
     * точке на линии, чем волшебник. Это и будет следующей ключевой точкой.
     * <p>
     * Дополнительно проверяем, не находится ли волшебник достаточно близко к какой-либо из ключевых точек. Если это
     * так, то мы сразу возвращаем следующую ключевую точку.
     */
    Point2D getNextWaypoint() {
        int lastWaypointIndex = waypoints.length - 1;
        Point2D lastWaypoint = waypoints[lastWaypointIndex];

        Point2D result = null;
        for (int waypointIndex = 0; waypointIndex < lastWaypointIndex; ++waypointIndex) {
            Point2D waypoint = waypoints[waypointIndex];

            if (waypoint.getDistanceTo(self) <= WAYPOINT_RADIUS) {
                result = waypoints[waypointIndex + 1];
                break;
            }

            if (lastWaypoint.getDistanceTo(waypoint) < lastWaypoint.getDistanceTo(self)) {
                result = waypoint;
                break;
            }
        }
        if (result == null) {
            result = lastWaypoint;
        }

        List<LivingUnit> targets = new ArrayList<>();
        targets.addAll(Arrays.asList(world.getBuildings()));
        targets.addAll(Arrays.asList(world.getMinions()));
        targets.addAll(Arrays.asList(world.getTrees()));

        if (StrategyHelper.countUnitByPath(targets, self, result) != 0) {
            Point2D left = new Point2D(result.getX() - INDENT_LENGTH, result.getY());
            Point2D right = new Point2D(result.getX() + INDENT_LENGTH, result.getY());
            Point2D top = new Point2D(result.getX(), result.getY() - INDENT_LENGTH);
            Point2D bottom = new Point2D(result.getX(), result.getY() + INDENT_LENGTH);

            if (StrategyHelper.countUnitByPath(targets, self, left) == 0) {
                result = left;
            } else if (StrategyHelper.countUnitByPath(targets, self, right) != 0) {
                result = right;
            } else if (StrategyHelper.countUnitByPath(targets, self, top) != 0) {
                result = top;
            } else if (StrategyHelper.countUnitByPath(targets, self, bottom) != 0) {
                result = bottom;
            }
        }

        return result;
    }

    /**
     * Действие данного метода абсолютно идентично действию метода {@code getNextWaypoint}, если перевернуть массив
     * {@code waypoints}.
     */
    Point2D getPreviousWaypoint() {
        Point2D firstWaypoint = waypoints[0];

        Point2D result = null;
        for (int waypointIndex = waypoints.length - 1; waypointIndex > 0; --waypointIndex) {
            Point2D waypoint = waypoints[waypointIndex];

            if (waypoint.getDistanceTo(self) <= WAYPOINT_RADIUS) {
                result = waypoints[waypointIndex - 1];
                break;
            }

            if (firstWaypoint.getDistanceTo(waypoint) < firstWaypoint.getDistanceTo(self)) {
                result = waypoint;
                break;
            }
        }
        if (result == null) {
            result = firstWaypoint;
        }

        List<LivingUnit> targets = new ArrayList<>();
        targets.addAll(Arrays.asList(world.getBuildings()));
        targets.addAll(Arrays.asList(world.getMinions()));
        targets.addAll(Arrays.asList(world.getTrees()));

        if (StrategyHelper.countUnitByPath(targets, self, result) != 0) {
            Point2D left = new Point2D(result.getX() - INDENT_LENGTH < 0 ? 0 : result.getX() - INDENT_LENGTH, result.getY());
            Point2D right = new Point2D(result.getX() + INDENT_LENGTH > world.getWidth() ? world.getWidth() : result.getX() + INDENT_LENGTH, result.getY());
            Point2D top = new Point2D(result.getX(), result.getY() - INDENT_LENGTH < 0 ? 0 : result.getY() - INDENT_LENGTH);
            Point2D bottom = new Point2D(result.getX(), result.getY() + INDENT_LENGTH > world.getHeight() ? world.getHeight() : result.getY() - INDENT_LENGTH);

            if (StrategyHelper.countUnitByPath(targets, self, left) == 0) {
                result = left;
            } else if (StrategyHelper.countUnitByPath(targets, self, right) != 0) {
                result = right;
            } else if (StrategyHelper.countUnitByPath(targets, self, top) != 0) {
                result = top;
            } else if (StrategyHelper.countUnitByPath(targets, self, bottom) != 0) {
                result = bottom;
            }
        }

        return result;
    }
}
