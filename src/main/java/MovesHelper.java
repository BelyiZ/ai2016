import model.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

class MovesHelper {

    private static final double WAYPOINT_RADIUS = 100.0D;

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

    private Random random;
    private Point2D[] waypoints;
    private LineType line;

    /**
     * Ключевые точки для каждой линии, позволяющие упростить управление перемещением волшебника.
     * <p>
     * Если всё хорошо, двигаемся к следующей точке и атакуем противников.
     * Если осталось мало жизненной энергии, отступаем к предыдущей точке.
     */
    private final Map<LineType, Point2D[]> waypointsByLine = new EnumMap<>(LineType.class);


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

        waypointsByLine.put(LineType.MIDDLE, new Point2D[]{
                new Point2D(100.0D, mapSize - 100.0D),
                random.nextBoolean()
                        ? new Point2D(600.0D, mapSize - 200.0D)
                        : new Point2D(200.0D, mapSize - 600.0D),
                new Point2D(800.0D, mapSize - 800.0D),
                new Point2D(mapSize - 600.0D, 600.0D)
        });

        waypointsByLine.put(LineType.TOP, new Point2D[]{
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
        });

        waypointsByLine.put(LineType.BOTTOM, new Point2D[]{
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
        });

        switch ((int) self.getId()) {
            case 1:
            case 2:
            case 6:
            case 7:
                line = LineType.TOP;
                break;
            case 3:
            case 8:
                line = LineType.MIDDLE;
                break;
            case 4:
            case 5:
            case 9:
            case 10:
                line = LineType.BOTTOM;
                break;
            default:
        }

        waypoints = waypointsByLine.get(line);

        // Наша стратегия исходит из предположения, что заданные нами ключевые точки упорядочены по убыванию
        // дальности до последней ключевой точки. Сейчас проверка этого факта отключена, однако вы можете
        // написать свою проверку, если решите изменить координаты ключевых точек.

            /*Point2D lastWaypoint = waypoints[waypoints.length - 1];

            Preconditions.checkState(ArrayUtils.isSorted(waypoints, (waypointA, waypointB) -> Double.compare(
                    waypointB.getDistanceTo(lastWaypoint), waypointA.getDistanceTo(lastWaypoint)
            )));*/
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

        for (int waypointIndex = 0; waypointIndex < lastWaypointIndex; ++waypointIndex) {
            Point2D waypoint = waypoints[waypointIndex];

            if (waypoint.getDistanceTo(self) <= WAYPOINT_RADIUS) {
                return waypoints[waypointIndex + 1];
            }

            if (lastWaypoint.getDistanceTo(waypoint) < lastWaypoint.getDistanceTo(self)) {
                return waypoint;
            }
        }

        return lastWaypoint;
    }

    /**
     * Действие данного метода абсолютно идентично действию метода {@code getNextWaypoint}, если перевернуть массив
     * {@code waypoints}.
     */
    Point2D getPreviousWaypoint() {
        Point2D firstWaypoint = waypoints[0];

        for (int waypointIndex = waypoints.length - 1; waypointIndex > 0; --waypointIndex) {
            Point2D waypoint = waypoints[waypointIndex];

            if (waypoint.getDistanceTo(self) <= WAYPOINT_RADIUS) {
                return waypoints[waypointIndex - 1];
            }

            if (firstWaypoint.getDistanceTo(waypoint) < firstWaypoint.getDistanceTo(self)) {
                return waypoint;
            }
        }

        return firstWaypoint;
    }
}
