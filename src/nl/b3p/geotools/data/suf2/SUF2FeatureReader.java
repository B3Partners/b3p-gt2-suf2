/*
 * $Id: SUF2FeatureReader.java 9066 2008-09-30 15:01:19Z Richard $
 */
package nl.b3p.geotools.data.suf2;

import nl.b3p.suf2.SUF2ParseException;
import nl.b3p.suf2.records.SUF2Record;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.io.EOFException;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
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
    private int featureID = 0;
    private SUF2RecordCollector recordCollector;
    //private Map header;
    private SimpleFeature feature;
    private SortedMap info = new TreeMap();

    public SUF2FeatureReader(URL url, String typeName, String srs) throws IOException, SUF2ParseException {

        gf = new GeometryFactory();
        recordCollector = new SUF2RecordCollector(url);

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
            ftb.add("type", String.class);
            ftb.add("classificatie", String.class);
            ftb.add("text", String.class);
            ftb.add("entryLineNumber", Integer.class);
            ftb.add("angle", Double.class);
            ftb.add("gemeentecode", String.class);
            ftb.add("sectie", String.class);
            ftb.add("perceelnummer", String.class);

            ft = ftb.buildFeatureType();

        } catch (Exception e) {
            throw new DataSourceException("Error creating SimpleFeatureType", e);
        }
    }

    public SimpleFeatureType getFeatureType() {
        return ft;
    }

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
                    record = recordCollector.next();
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
        String text = "";
        String perceelnummer = "";
        String gemeentecode = "";
        String sectie = "";


        String classificatiecode = "";
        double angle = 0.0;

        Map properties = record.getProperties();
        Geometry geometry = SUF2GeometryFactory.createGeometry(gf, record);


        if(record.getType()== SUF2Record.Type.PERCEEL){
            perceelnummer = properties.get(SUF2Record06.PERCEELNUMMER).toString();
            sectie = properties.get(SUF2Record06.SECTIE).toString();
            gemeentecode = properties.get(SUF2Record06.GEMEENTECODE).toString();
            text = perceelnummer + " " + sectie + " " + gemeentecode;
        }

        if (properties.containsKey(SUF2Record06.TEKST)) {
            text = properties.get(SUF2Record06.TEKST).toString();
        }

        if (properties.containsKey(SUF2Record.LKI_CLASSIFICATIECODE)) {
            classificatiecode = properties.get(SUF2Record.LKI_CLASSIFICATIECODE).toString();
        }

        if (properties.containsKey(SUF2Record06.ANGLE)) {
            angle = (Double) properties.get(SUF2Record06.ANGLE);
        }

        Object[] values = new Object[]{
            geometry, //the_geom
            record.getType().getDescription(),
            classificatiecode, //classificatie
            text,// text
            record.getLineNumber(), // record linenumber
            angle, // text angle
            gemeentecode,
            sectie,
            perceelnummer
        };

        return SimpleFeatureBuilder.build(ft, values, Integer.toString(featureID++));
    }

    public void close() throws IOException {
        recordCollector.close();
    }
}
