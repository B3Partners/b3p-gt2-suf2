/*
 * $Id: SUF2FeatureReader.java 9066 2008-09-30 15:01:19Z Richard $
 */
package nl.b3p.geotools.data.suf2;

import com.vividsolutions.jts.geom.Coordinate;
import nl.b3p.suf2.SUF2ParseException;
import nl.b3p.suf2.records.SUF2Record;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.awt.Point;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import nl.b3p.suf2.SUF2RecordCollector;
import nl.b3p.suf2.records.SUF2Record06;
import org.geotools.data.DataSourceException;
import org.geotools.data.FeatureReader;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.commons.io.input.CountingInputStream;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * @author Gertjan Al, B3Partners
 */
public class SUF2FeatureReader implements FeatureReader {

    private GeometryFactory gf;
    private SimpleFeatureType ft;
//    private CountingInputStream cis;
//    private LineNumberReader lnr;
    // private String version;
    private Map<String, String[]> metadata = new HashMap<String, String[]>();
    private static final int MARK_SIZE = 8 * 1024;
    private int featureID = 0;
    private SUF2RecordCollector recordCollector;
    //private Map header;
    private SimpleFeature feature;
    private SortedMap info = new TreeMap();

    public SUF2FeatureReader(URL url, String typeName, String srs) throws IOException, SUF2ParseException {

        /* TODO for loading large files, obtain a total stream size from somewhere
         * and use an apache commons CountingInputStream to provide current
         * progress info.
         */

        /* Note that a LineNumberReader may read more bytes than are strictly
         * returned as characters of lines read.
         */
//        this.cis = new CountingInputStream(url.openStream());

        /* TODO provide param to override encoding! This uses the platform
         * default encoding, SDF Loader Help doesn't specify encoding
         */
//        this.lnr = new LineNumberReader(new InputStreamReader(cis));

        gf = new GeometryFactory();
//        recordCollector = new SUF2RecordCollector(lnr);



        //skipCommentsCheckEOF();
        createFeatureType(typeName, srs);
    }

    private void createFeatureType(String typeName, String srs) throws DataSourceException {
        CoordinateReferenceSystem crs = null;
        String[] csMetadata = metadata.get("coordinatesystem");
        if (csMetadata != null) {
            String wkt = csMetadata[0];
            try {
                /* parse WKT */
                CRSFactory crsFactory = ReferencingFactoryFinder.getCRSFactory(null);
                crs = crsFactory.createFromWKT(wkt);
            } catch (Exception e) {
                throw new DataSourceException("Error parsing CoordinateSystem WKT: \"" + wkt + "\"");
            }
        }

        /* override srs when provided */
        if (srs != null) {
            try {
                crs = CRS.decode(srs);
            } catch (Exception e) {
                throw new DataSourceException("Error parsing CoordinateSystem srs: \"" + srs + "\"");
            }
        }

        try {

            SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
            ftb.setName(typeName);
            ftb.setCRS(crs);

            ftb.add("the_geom", Geometry.class);
            ftb.add("name", String.class);
            ftb.add("key", String.class);
            //ftb.add("urlLink", String.class);
            ftb.add("entryLineNumber", Integer.class);
            ftb.add("parseError", Integer.class);
            ftb.add("error", String.class);

            ft = ftb.buildFeatureType();

            //GeometricAttributeType geometryType = new GeometricAttributeType("the_geom", Geometry.class, true, null, crs, null);


            /*
            gf = geometryType.getGeometryFactory();

            ft = FeatureTypes.newFeatureType(
            new AttributeType[]{
            geometryType,
            AttributeTypeFactory.newAttributeType("name", String.class),
            AttributeTypeFactory.newAttributeType("key", String.class),
            AttributeTypeFactory.newAttributeType("urlLink", String.class),
            AttributeTypeFactory.newAttributeType("entryLineNumber", Integer.class),
            AttributeTypeFactory.newAttributeType("parseError", Integer.class),
            AttributeTypeFactory.newAttributeType("error", String.class)
            }, typeName);
             */
        } catch (Exception e) {
            throw new DataSourceException("Error creating SimpleFeatureType", e);
        }
    }

    public SimpleFeatureType getFeatureType() {
        return ft;
    }

    /**
     * Skip empty and comment lines and return EOF status
     * @return true if EOF
     * @throws java.io.IOException
     */
//    private boolean skipCommentsCheckEOF() throws IOException {
//        String line;
//        do {
//            /* mark the start of the next line */
//            lnr.mark(MARK_SIZE);
//            line = lnr.readLine();
//            if (line == null || line.equals("99")) {
//                /* skipped comments till end of file */
//                return true;
//            }
//        } while (line.length() == 0);
//
//        /* EOF or the last line we read wasn't a empty line. reset
//         * the stream so the next readLine() call will return the line we just
//         * read
//         */
//        lnr.reset();
//        return false;
//    }
    public SimpleFeature next() throws IOException, IllegalAttributeException, NoSuchElementException {
        return feature;
    }

    public boolean hasNext() throws IOException {

        try {
            if (!recordCollector.hasNext()) {
                return false;

            } else {
                SUF2Record record;
                for (;;) {
                    // Loop till record with geometry is found
                    record = (SUF2Record) recordCollector.next();
                    if (record.hasGeometry()) {
                        feature = createFeature(record);
                        break;
                    } else {
                        // Record contains file information
                        try {
                            info.putAll(record.getProperties());
                        } catch (SUF2ParseException ex) {
                            throw new IOException(ex.getMessage());
                        }
                        if (!recordCollector.hasNext()) {
                            return false;
                        }
                    }
                }

            }
        } catch (EOFException ex) {
            return false;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
        return true;
    }

    private SimpleFeature createFeature(SUF2Record record) throws Exception {
        String key = "";
        Map properties = record.getProperties();

        if (properties.containsKey(SUF2Record06.TEKST)) {
            key = properties.get(SUF2Record06.TEKST).toString();
        }


        Object[] values = new Object[]{
            createGeometry(gf, record),
            "Naam",
            key,
            record.getLineNumber(),
            0,
            "leeg"
        };

        return SimpleFeatureBuilder.build(ft, values, Integer.toString(featureID++));

    }

    private Geometry createGeometry(GeometryFactory gf, SUF2Record record) throws Exception {
        List<Point> coordinatePoints = record.getCoordinates();
        Coordinate[] coordinates = new Coordinate[coordinatePoints.size()];

        for (int i = 0; i < coordinatePoints.size(); i++) {
            Point point = coordinatePoints.get(i);
            coordinates[i] = new Coordinate(point.x, point.y);
        }


        if (coordinates.length <= 0) {
            throw new IOException("No coordinates found");
        } else if (coordinates.length == 1) {

            return gf.createPoint(coordinates[0]);
        } else {
            return gf.createLineString(coordinates);
        }
    }

    public void close() throws IOException {
        recordCollector.close();
    }

}
