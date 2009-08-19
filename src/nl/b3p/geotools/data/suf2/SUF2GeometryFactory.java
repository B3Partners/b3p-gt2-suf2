package nl.b3p.geotools.data.suf2;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import nl.b3p.suf2.SUF2Coordinate;
import nl.b3p.suf2.SUF2Math;
import nl.b3p.suf2.records.SUF2Record;
import nl.b3p.suf2.records.SUF2Record05;
import nl.b3p.suf2.records.SUF2Record06;

/**
 * @author Gertjan Al, B3Partners
 */
public class SUF2GeometryFactory {

    public static Geometry createGeometry(GeometryFactory gf, SUF2Record record) throws Exception {
        List<SUF2Coordinate> coordinatePoints = record.getCoordinates();

        // Record is a arc
        if (record.getType() == SUF2Record.Type.ARC) {
            return createArc(gf, record);
        }

        // Record is a line, text or symbol
        Coordinate[] coordinates = new Coordinate[coordinatePoints.size()];
        for (int i = 0; i < coordinatePoints.size(); i++) {
            SUF2Coordinate coordinate = coordinatePoints.get(i);
            coordinates[i] = new Coordinate(coordinate.x, coordinate.y);
        }


        // If text or symbol
        if (record.getType().equals(SUF2Record.Type.TEXT) || record.getType().equals(SUF2Record.Type.SYMBOL)) {
            if (record.getProperties().containsKey(SUF2Record05.TEKST_OF_SYMBOOL)) {
                return createTextPoint(gf, record);
            }
        }


        // If lineStart == lineEnd; convert to point
        if (coordinates.length == 2) {
            if (coordinates[0].equals(coordinates[1])) {
                return gf.createPoint(coordinates[0]);
            }
        }


        if (coordinates.length <= 0) {
            throw new IOException("No coordinates found");

        } else if (coordinates.length == 1) {
            return gf.createPoint(coordinates[0]);

        } else {
            return gf.createLineString(coordinates);
        }
    }

    private static Geometry createArc(GeometryFactory gf, SUF2Record record) throws Exception {
        List<SUF2Coordinate> coordinates = record.getCoordinates();

        SUF2Coordinate p1 = coordinates.get(0);
        SUF2Coordinate p2 = coordinates.get(1);
        SUF2Coordinate p3 = coordinates.get(2);

        SUF2Coordinate pc = circle(record.getCoordinates());
        double radius = Math.sqrt((p1.x - pc.x) * (p1.x - pc.x) + (p1.y - pc.y) * (p1.y - pc.y));

        double angle1 = Math.toRadians(SUF2Math.angle(pc, p1));
        double angle2 = Math.toRadians(SUF2Math.angle(pc, p2));
        double angle3 = Math.toRadians(SUF2Math.angle(pc, p3));

        /*
         * G grootste getal
         * M medium
         * K kleinste getal
         *
         * Volgorde = angle1, angle2, angle3
         */

        String type = "none";

        record.getProperties().put(SUF2Record06.PERCEELNUMMER, "orig: angle1=" + Double.toString(angle1) + " angle2=" + Double.toString(angle2) + " angle3=" + Double.toString(angle3));

        if (angle1 == angle3) { // G ? G
            angle1 = 0.0;
            angle3 = Math.PI * 2;
            type = "0";

        } else if (angle1 < angle2 && angle2 > angle3 && angle1 > angle3) { // M G K
//            double temp = angle1;
//            angle1 = angle3;
            angle3 += Math.PI *2;

            type = "1";

        } else if (angle1 < angle2 && angle2 > angle3 && angle1 < angle3) { // K G M
            double temp = angle1;
            angle1 = angle3;
            angle3 = temp + Math.PI * 2;
            type = "2";

        } else if (angle1 > angle2 && angle2 > angle3) { // G M K
            double temp = angle1;
            angle1 = angle3;
            angle3 = temp;
            type = "3";

        } else if (angle1 > angle2 && angle2 < angle3 && angle3 < angle1) { // G K M
            angle3 += Math.PI * 2;
            type = "4";

        } else if (angle1 > angle2 && angle2 < angle3 && angle3 > angle1) { // G K M
            double temp = angle1;
            angle1 = angle3;
            angle3 = temp + Math.PI * 2;
            type = "5";
        }

        record.getProperties().put(SUF2Record06.GEMEENTECODE, "arctype = " + type);
        record.getProperties().put(SUF2Record06.TEKST, "angle1=" + Double.toString(angle1) + " angle2=" + Double.toString(angle2) + " angle3=" + Double.toString(angle3));

        return gf.createLineString(toCoordinateArray(pc, radius, angle1, angle3, gf));
    }

    private static Geometry createTextPoint(
            GeometryFactory gf, SUF2Record record) throws Exception {
        Map properties = record.getProperties();

        List<SUF2Coordinate> coordinates = record.getCoordinates();
        // properties.put(SUF2Record.ANGLE, SUF2Math.angle(coordinates));
        properties.put(SUF2Record.ANGLE, 0.0);

        if (properties.get(SUF2Record05.TEKST_OF_SYMBOOL).toString().equals("2")) { // tekst = 1; symbool = 2
            if (properties.get(SUF2Record05.SYMBOOLTYPE).equals("")) {
                properties.put(SUF2Record06.TEKST, properties.get(SUF2Record05.LKI_CLASSIFICATIECODE));
            } else {
                properties.put(SUF2Record06.TEKST, properties.get(SUF2Record05.SYMBOOLTYPE));
            }

        }

        SUF2Coordinate coordinate = SUF2Math.middle(coordinates);
        return gf.createPoint(new Coordinate(coordinate.x, coordinate.y));
    }

    private static SUF2Coordinate circle(List<SUF2Coordinate> coordinates) throws Exception {
        SUF2Coordinate c1 = coordinates.get(0);
        SUF2Coordinate c2 = coordinates.get(1);
        SUF2Coordinate c3 = coordinates.get(2);

        double x1, x2, x3, y1, y2, y3, a12, a23, b12, b23, x, y;

        x1 = c1.x;
        x2 = c2.x;
        x3 = c3.x;
        y1 = c1.y;
        y2 = c2.y;
        y3 = c3.y;

        SUF2Coordinate p12 = SUF2Math.middle(c1, c2);
        SUF2Coordinate p23 = SUF2Math.middle(c2, c3);


        if (y2 == y1) {
            b12 = p12.y;
            a12 = 0.0;
        } else {
            b12 = p12.y + ((x2 - x1) / (y2 - y1)) * p12.x;
            a12 = (x2 - x1) / (y2 - y1);
        }

        if (y3 == y2) {
            b23 = p23.y;
            a23 = 0.0;
        } else {
            b23 = p23.y + ((x3 - x2) / (y3 - y2)) * p23.x;
            a23 = (x3 - x2) / (y3 - y2);
        }

        if (a12 == a23) {
            throw new Exception("Devide by zero, unable to create arc");
        } else {
            x = (b23 - b12) / (a12 - a23);
        }

        y = a12 * x + b12;

        return new SUF2Coordinate(-x, y);
    }

    public static Coordinate[] toCoordinateArray(SUF2Coordinate point, double radius, double startAngle, double endAngle, GeometryFactory gf) {
        if (point == null || radius <= 0) {
            return new Coordinate[]{new Coordinate(0, 0), new Coordinate(1, 0)};
        }

        List<Coordinate> lc = new ArrayList<Coordinate>();
        //double segAngle = 2 * Math.PI / radius;
        double segAngle = 1 / (2 * Math.PI);

        /*
        if (radius < 10) {
        segAngle = 1;
        }
         * */

        double angle = startAngle;
        for (;;) {
            double x = point.x + radius * Math.cos(angle);
            double y = point.y + radius * Math.sin(angle);

            Coordinate c = new Coordinate(x, y);
            lc.add(c);

            if (angle >= endAngle) {
                break;
            }

            angle += segAngle;
            if (angle > endAngle) {
                angle = endAngle;
            }

        }

        if (lc.size() <= 1) {
            int z = 0;
        }

        return lc.toArray(new Coordinate[]{});
    }
}
