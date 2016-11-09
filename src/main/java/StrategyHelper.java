import model.LivingUnit;
import model.Wizard;

import java.util.ArrayList;
import java.util.List;


class StrategyHelper {


    /**
     * Рассчитывает точки пересечение отрезка проходящего через окружность.
     *
     * @param circle центр окружности
     * @param r  радиус окружности
     * @param a первая точка отрезка
     * @param b вторая точка отрезка
     * @return точки пересечения
     */
    private static List<Point2D> lineCircleIntersection(LivingUnit circle, double r, Wizard a, Point2D b) {
        List<Point2D> positions = new ArrayList<>();
        double q = Math.pow(circle.getX(), 2) + Math.pow(circle.getY(), 2) - r*r;
        double k = -2.0 * circle.getX();
        double l = -2.0 * circle.getY();

        double z = a.getX() * b.getY() - b.getX()*a.getY();
        double p = a.getY() - b.getY();
        double s = a.getX() - b.getX();

        if (equalDoubles(s, 0.0, 0.001)) {
            s = 0.001;
        }

        double A = s*s + p*p;
        double B = s*s*k + 2.0*z*p + s*l*p;
        double C = q*s*s + z*z + s*l*z;

        double D = B*B - 4.0*A*C;

        if (D > 0.0) {
            if (D < 0.001) {
                double x = -B / (2.0 * A);
                positions.add(new Point2D(x, (p * x + z) / s));
            } else {
                double x = (-B + Math.sqrt(D)) / (2.0 * A);
                double y = (p * x + z) / s;
                positions.add(new Point2D(x, y));

                x = (-B - Math.sqrt(D)) / (2.0 * A);
                y = (p * x + z) / s;
                positions.add(new Point2D(x, y));
            }
        }

        return positions;
    }

    private static boolean equalDoubles(double n1, double n2, double precision_) {
        return (Math.abs(n1-n2) <= precision_);
    }

    static int countUnitByPath(List<LivingUnit> targets, Wizard self, Point2D point) {
        int count = 0;
        for (LivingUnit unit : targets) {
            if (point.getDistanceTo(unit) < self.getDistanceTo(point.getX(), point.getY())) {
                List<Point2D> intersections = lineCircleIntersection(unit, unit.getRadius() * 1.5, self, point);
                if (intersections.size() > 0) {
                    count++;
                }
            }
        }

        return count;
    }
}
