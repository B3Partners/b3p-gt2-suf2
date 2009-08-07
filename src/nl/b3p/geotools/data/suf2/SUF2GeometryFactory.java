package nl.b3p.geotools.data.suf2;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.awt.geom.Point2D;
import java.io.IOException;
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
        Coordinate[] coordinates = new Coordinate[coordinatePoints.size()];
        Map properties = record.getProperties();


        boolean isArc = false;

        for (int i = 0; i < coordinatePoints.size(); i++) {
            SUF2Coordinate coordinate = coordinatePoints.get(i);
            coordinates[i] = new Coordinate(coordinate.x, coordinate.y);

            if (coordinate.hasTag()) {
                if (coordinate.getTag().equals(SUF2Coordinate.Tag.I4)) {
                    isArc = true;
                }
            }
        }

        if (isArc) {
            return createArc(gf, record, coordinates);
        }


        // if isSymbol
        if (record.getType().equals(SUF2Record.Type.TEXT) || record.getType().equals(SUF2Record.Type.SYMBOL)) {
            //if (properties.containsKey(SUF2Record05.TEKST_OF_SYMBOOL)) {
            return createTextPoint(gf, record);
        }

        if (coordinates.length <= 0) {
            throw new IOException("No coordinates found");

            /*} else {
            return gf.createPoint(coordinates[0]);
            }
            /**/
        } else if (coordinates.length == 1) {
            return gf.createPoint(coordinates[0]);

        } else {
            return gf.createLineString(coordinates);
        }
    }

    private static Geometry createArc(GeometryFactory gf, SUF2Record record, Coordinate[] coordinate2s) throws Exception {
        List<SUF2Coordinate> coordinates = record.getCoordinates();

        SUF2Coordinate c1 = coordinates.get(0);
        SUF2Coordinate c2 = coordinates.get(1);
        SUF2Coordinate c3 = coordinates.get(2);

        SUF2Coordinate middle = SUF2Math.middle(c1, c3);
        double radius = SUF2Math.distance(c2, middle);

        return gf.createLineString(coordinate2s);
    }

    private static Geometry createTextPoint(GeometryFactory gf, SUF2Record record) throws Exception {
        Map properties = record.getProperties();

        List<SUF2Coordinate> coordinates = record.getCoordinates();
        properties.put(SUF2Record.ANGLE, SUF2Math.calculateAngle(coordinates));

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
}
