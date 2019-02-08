package nl.b3p.geotools.data.suf2;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import nl.b3p.suf2.SUF2Coordinate;
import nl.b3p.suf2.SUF2Math;
import nl.b3p.suf2.records.SUF2Record;
import nl.b3p.suf2.records.SUF2Record.Type;
import nl.b3p.suf2.records.SUF2Record05;
import nl.b3p.suf2.records.SUF2Record06;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Gertjan Al, B3Partners
 */
public class SUF2GeometryFactory {

    private static final Log log = LogFactory.getLog(SUF2GeometryFactory.class);
    public static final double NUM_SEGMENTS = 32.0;

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
            if (record.getProperties().containsKey(SUF2Record05.TEKST_OF_SYMBOOL)) {
                if (record.getProperties().get(SUF2Record05.TEKST_OF_SYMBOOL).toString().equals("1")) {
                    return createTextPoint(gf, record);
                }
            }
        }

        if (coordinates.length <= 0) {
            throw new IOException("No coordinates found");

        } else if (coordinates.length == 1) {
            return gf.createPoint(coordinates[0]);

        } else if (isPolygon(record, coordinatePoints)) {
            CoordinateSequence coordinateSequence = new CoordinateArraySequence(coordinates);
            LinearRing linearRing = new LinearRing(coordinateSequence, gf);
            return gf.createPolygon(linearRing, new LinearRing[0]);
        } else {
            return gf.createLineString(coordinates);
        }
    }

    private static boolean isPolygon(SUF2Record record, List<SUF2Coordinate> coordinatePoints) {
        if (record.getType() == Type.POLYGON) {
            return true;
        } else {
            /*Niet meer doen. Een gesloten lijn is geen polygon. Gebruik de Polygonize action om van lijnen polygonen te maken.
             if (coordinatePoints.size() > 2) {
                if (coordinatePoints.get(0).equals(coordinatePoints.get(coordinatePoints.size() - 1))) {
                    return true;
                }
            }*/
            return false;
        }
    }

    public static Geometry createArc(GeometryFactory gf, SUF2Record record) throws Exception {
        List<SUF2Coordinate> coordinates = record.getCoordinates();
        return createArc(gf, coordinates);
    }

    public static Geometry createArc(GeometryFactory gf, List<SUF2Coordinate> coordinates) throws Exception {
        SUF2Coordinate p1 = coordinates.get(0);
        SUF2Coordinate p2 = coordinates.get(1);
        SUF2Coordinate p3 = coordinates.get(2);

        SUF2Coordinate pc;
        try {
            pc = circle(coordinates);
        } catch (Exception ex) {
            log.debug(ex.getLocalizedMessage() + "; converted arc to line");
            return gf.createLineString(new Coordinate[]{new Coordinate(p1.x, p1.y), new Coordinate(p3.x, p3.y)});
        }

        double radius = Math.sqrt((p1.x - pc.x) * (p1.x - pc.x) + (p1.y - pc.y) * (p1.y - pc.y));

        double angle1 = Math.toRadians(SUF2Math.angle(pc, p1));
        double angle2 = Math.toRadians(SUF2Math.angle(pc, p2));
        double angle3 = Math.toRadians(SUF2Math.angle(pc, p3));

        if (angle1 == angle3) { // G ? G
            angle1 = 0.0;
            angle3 = Math.PI * 2;

        } else if (angle2 == angle1 || angle2 == angle3) {
            //log.debug("Record at line " + record.getLineNumber() + ": Arc with middleCoordinate equal to startCoordinate or endCoordinate");
            log.debug("Record at line someline: Arc with middleCoordinate equal to startCoordinate or endCoordinate");

            Coordinate[] line = new Coordinate[]{new Coordinate(p1.x, p1.y), new Coordinate(p3.x, p3.y)};
            return gf.createLineString(line);

        }
        /*als hoek2 niet tussen hoek 1 en 3 ligt dan betekent dat, dat 2PI lijn(0/360 graden lijn) tussen
        punt 1 en 3 ligt en daarom de kleinste van de 2 moet worden opgehoogt met 2PI(360 graden)*/
        else if ((angle2 > angle1 && angle2 > angle3) ||
                (angle2 < angle1 && angle2 <angle3)){
            if (angle1 < angle3)
                angle1+= Math.PI*2;
            else
                angle3+= Math.PI*2;
        }
        /*Niet de angles verwisselen. Dan wordt de arc in een andere richting getekend.
           */
        /*else if (angle1 < angle2 && angle2 > angle3 && angle1 > angle3) { // M G K
            angle3 += Math.PI * 2;

        } else if (angle1 < angle2 && angle2 > angle3 && angle1 < angle3) { // K G M
            double temp = angle1;
            angle1 = angle3;
            angle3 = temp + Math.PI * 2;

        } else if (angle1 > angle2 && angle2 > angle3) { // G M K
            double temp = angle1;
            angle1 = angle3;
            angle3 = temp;

        } else if (angle1 > angle2 && angle2 < angle3 && angle3 < angle1) { // G K M
            angle3 += Math.PI * 2;

        } else if (angle1 > angle2 && angle2 < angle3 && angle3 > angle1) { // G K M
            double temp = angle1;
            angle1 = angle3;
            angle3 = temp + Math.PI * 2;
        }*/

        return gf.createLineString(toCoordinateArray(pc, radius, angle1, angle3, gf));
    }

    private static Geometry createTextPoint(GeometryFactory gf, SUF2Record record) throws Exception {
        Map properties = record.getProperties();

        List<SUF2Coordinate> coordinates = record.getCoordinates();
        if (coordinates.size() == 2) {
            properties.put(SUF2Record.ANGLE, SUF2Math.angle(coordinates.get(0), coordinates.get(1)));
        } else {
            properties.put(SUF2Record.ANGLE, new Double(0.0));
        }

        if (properties.get(SUF2Record05.TEKST_OF_SYMBOOL).toString().equals("2")) { // tekst = 1; symbool = 2
            if (properties.get(SUF2Record05.SYMBOOLTYPE).equals("")) {
                properties.put(SUF2Record06.TEKST, properties.get(SUF2Record05.LKI_CLASSIFICATIECODE));
            } else {
                properties.put(SUF2Record06.TEKST, properties.get(SUF2Record05.SYMBOOLTYPE));
            }
        }else{
            if(properties.containsKey(SUF2Record06.PERCEELNUMMER)){
                properties.put(SUF2Record06.TEKST, properties.get(SUF2Record06.PERCEELNUMMER));
            }else{
                properties.put(SUF2Record06.TEKST, properties.get(SUF2Record05.LKI_CLASSIFICATIECODE));
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

    public static Coordinate[] toCoordinateArray(SUF2Coordinate point, double radius, double startAngle, double endAngle, GeometryFactory gf) throws Exception {
        if (point == null) {
            throw new Exception("toCoordinateArray(...) point == null");
        } else if (radius <= 0) {
            throw new Exception("toCoordinateArray(...) radius is equal or below zero (radius=" + radius + ")");
        }
        /*zijn de angles clockwise (cw) of counterclockwise (ccw). Dit bepaalt of de radius segment angle
        moet oplopen of aflopen*/
        boolean ccw=startAngle < endAngle;

        List<Coordinate> lc = new ArrayList<Coordinate>();
        double segAngle = (2 * Math.PI) / NUM_SEGMENTS;
        double angle = startAngle;

        for (;;) {
            double x = point.x + radius * Math.cos(angle);
            double y = point.y + radius * Math.sin(angle);
            lc.add(new Coordinate(x, y));
            //if ccw dan optellen
            if (ccw){
                if (angle >= endAngle) {
                    break;
                }

                angle += segAngle;
                if (angle > endAngle) {
                    // snap arc to endAngle
                    angle = endAngle;
                }
            //anders aftrekken
            }else{
                if (angle <= endAngle) {
                    break;
                }

                angle -= segAngle;
                if (angle < endAngle) {
                    // snap arc to endAngle
                    angle = endAngle;
                }
            }

        }

        if (lc.size() <= 1) {
            throw new Exception("toCoordinateArray(...) returned " + lc.size() + " coordinates, value must be 2 or more");
        }

        return lc.toArray(new Coordinate[]{});
    }
}
