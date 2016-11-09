import model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

class MovesHelper {

    private static final double WAYPOINT_RADIUS = 150.0D;
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
    private Point2D[] waypoints;

    private Random random;

    private Point2D nextWaypoint;
    private Point2D prevWaypoint;


    /**
     * Сохраняем все входные данные в полях класса для упрощения доступа к ним.
     */
    void initializeTick(Wizard self, World world, Game game) {
        this.self = self;
        this.world = world;
        this.game = game;
        if (random == null) {
            random = new Random(game.getRandomSeed());
        }
    }

    void initialize() {
        if (waypoints != null)
            return;

        LaneType lineType = selectMoveLine();

        if (LaneType.MIDDLE.equals(lineType)) {
            waypoints = new Point2D[] {
                    new Point2D(100.0D, 3900.0D),
                    new Point2D(100.0D, 3300.0D),
                    new Point2D(800.0D, 3200.0D),
                    new Point2D(1200.0D, 2800.0D),
                    new Point2D(1600.0D, 2400.0D),
                    new Point2D(2000.0D, 2000.0D),
                    new Point2D(2400.0D, 1600.0D),
                    new Point2D(2800.0D, 1200.0D),
                    new Point2D(3200.0D, 800.0D),
                    new Point2D(3600.0D, 400.0D)
            };
        } else if (LaneType.TOP.equals(lineType)) {
            waypoints = new Point2D[] {
                    new Point2D(100.0D, 3900.0D),
                    new Point2D(100.0D, 3500.0D),
                    new Point2D(200.0D, 3200.0D),
                    new Point2D(200.0D, 2800.0D),
                    new Point2D(200.0D, 2400.0D),
                    new Point2D(200.0D, 2000.0D),
                    new Point2D(200.0D, 1600.0D),
                    new Point2D(200.0D, 1200.0D),
                    new Point2D(200.0D, 800.0D),

                    new Point2D(500D, 500D),

                    new Point2D(800D, 200.0D),
                    new Point2D(1200D, 200.0D),
                    new Point2D(1600D, 200.0D),
                    new Point2D(2000D, 200.0D),
                    new Point2D(2400D, 200.0D),
                    new Point2D(2800D, 200.0D),
                    new Point2D(3200D, 200.0D),
                    new Point2D(3600D, 200.0D),
                    new Point2D(3800.0D, 200.0D)
            };
        } else {
            waypoints = new Point2D[]{
                    new Point2D(100.0D, 3900.0D),
                    new Point2D(500.0D, 3900.0D),
                    new Point2D(800.0D, 3800.0D),
                    new Point2D(1200.0D, 3800.0D),
                    new Point2D(1600.0D, 3800.0D),
                    new Point2D(2000.0D, 3800.0D),
                    new Point2D(2400.0D, 3800.0D),
                    new Point2D(2800.0D, 3800.0D),
                    new Point2D(3200.0D, 3800.0D),

                    new Point2D(3500D, 3500D),

                    new Point2D(3800D, 3200.0D),
                    new Point2D(3800D, 2800.0D),
                    new Point2D(3800D, 2400.0D),
                    new Point2D(3800D, 2000.0D),
                    new Point2D(3800D, 1600.0D),
                    new Point2D(3800D, 1200.0D),
                    new Point2D(3800D, 800.0D),
                    new Point2D(3900D, 500.0D),
                    new Point2D(3900.0D, 100.0D)
            };
        }
    }

    void initParams() {
        Point2D nearPoint1 = null;
        Point2D nearPoint2 = null;
        for (Point2D waypoint : waypoints) {
            if (waypoint.getDistanceTo(self) < WAYPOINT_RADIUS) {
                continue;
            }
            if (nearPoint1 == null) {
                nearPoint1 = waypoint;
                continue;
            }

            if (waypoint.getDistanceTo(self) < nearPoint1.getDistanceTo(self)) {
                nearPoint2 = nearPoint1;
                nearPoint1 = waypoint;
            } else if (nearPoint2 == null || waypoint.getDistanceTo(self) < nearPoint2.getDistanceTo(self)) {
                nearPoint2 = waypoint;
            }
        }

        if (waypoints[0].getDistanceTo(nearPoint1) < waypoints[0].getDistanceTo(nearPoint2)) {
            prevWaypoint = nearPoint1;
            nextWaypoint = nearPoint2;
        } else {
            prevWaypoint = nearPoint2;
            nextWaypoint = nearPoint1;
        }
    }


    private LaneType selectMoveLine() {
        switch ((int) self.getId()) {
            case 1:
            case 2:
            case 6:
            case 7:
                return LaneType.TOP;
            case 3:
            case 8:
                return LaneType.MIDDLE;
            case 4:
            case 5:
            case 9:
            case 10:
                return LaneType.BOTTOM;
            default:
                return LaneType.MIDDLE;
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
        List<LivingUnit> targets = new ArrayList<>();
        targets.addAll(Arrays.asList(world.getBuildings()));
        targets.addAll(Arrays.asList(world.getMinions()));
        targets.addAll(Arrays.asList(world.getTrees()));

        int nextWaypointIndex = getWaypointIndex(nextWaypoint);
        if (StrategyHelper.countUnitByPath(targets, self, nextWaypoint) != 0) {
            Point2D viewPoint = getViewPoint(nextWaypoint, targets);
            if (viewPoint == null && nextWaypointIndex != 0 && nextWaypointIndex != waypoints.length -1) {
                Point2D nextNextWaypoint = waypoints[nextWaypointIndex + 1];
                viewPoint = getViewPoint(nextNextWaypoint, targets);
            }
            if (viewPoint != null) {
                return viewPoint;
            }
        }

        return nextWaypoint;
    }

    /**
     * Действие данного метода абсолютно идентично действию метода {@code getNextWaypoint}, если перевернуть массив
     * {@code waypoints}.
     */
    Point2D getPreviousWaypoint() {
        List<LivingUnit> targets = new ArrayList<>();
        targets.addAll(Arrays.asList(world.getBuildings()));
        targets.addAll(Arrays.asList(world.getMinions()));
        targets.addAll(Arrays.asList(world.getTrees()));

        int prevWaypointIndex = getWaypointIndex(prevWaypoint);
        if (StrategyHelper.countUnitByPath(targets, self, prevWaypoint) != 0) {
            Point2D viewPoint = getViewPoint(prevWaypoint, targets);
            if (viewPoint == null && prevWaypointIndex != 0 && prevWaypointIndex != waypoints.length -1) {
                Point2D nextNextWaypoint = waypoints[prevWaypointIndex - 1];
                viewPoint = getViewPoint(nextNextWaypoint, targets);
            }
            if (viewPoint != null) {
                return viewPoint;
            }
        }

        return prevWaypoint;
    }

    private Point2D getViewPoint(Point2D waypoint, List<LivingUnit> targets) {
        Point2D left = new Point2D(waypoint.getX() - INDENT_LENGTH, waypoint.getY());
        Point2D right = new Point2D(waypoint.getX() + INDENT_LENGTH, waypoint.getY());
        Point2D top = new Point2D(waypoint.getX(), waypoint.getY() - INDENT_LENGTH);
        Point2D bottom = new Point2D(waypoint.getX(), waypoint.getY() + INDENT_LENGTH);

        Point2D result = null;
        if (left.getDistanceTo(self) > WAYPOINT_RADIUS && StrategyHelper.countUnitByPath(targets, self, left) == 0) {
            result = left;
        } else if (bottom.getDistanceTo(self) > WAYPOINT_RADIUS && StrategyHelper.countUnitByPath(targets, self, bottom) != 0) {
            result = bottom;
        } else if (right.getDistanceTo(self) > WAYPOINT_RADIUS && StrategyHelper.countUnitByPath(targets, self, right) != 0) {
            result = right;
        } else if (top.getDistanceTo(self) > WAYPOINT_RADIUS && StrategyHelper.countUnitByPath(targets, self, top) != 0) {
            result = top;
        }
        return result;
    }

    private int getWaypointIndex(Point2D waypoint) {
        int i = 0;
        for (Point2D point : waypoints) {
            if (point.equals(waypoint))
                return i;
            i++;
        }
        return 0;
    }
}
