/*
 * $Id: SUF2FeatureReader.java 9066 2008-09-30 15:01:19Z Richard $
 */
package nl.b3p.geotools.data.suf2;

import nl.b3p.suf2.SUF2ParseException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import java.io.EOFException;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import nl.b3p.suf2.SUF2RecordCollector;
import nl.b3p.suf2.records.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.opengis.geometry.PrecisionType;

/**
 * @author Gertjan Al, B3Partners
 */
public class SUF2FeatureReader implements FeatureReader {

    private static final Log log = LogFactory.getLog(SUF2FeatureReader.class);
    private GeometryFactory gf;
    private SimpleFeatureType ft;
    private Map<String, String[]> metadata = new HashMap<String, String[]>();
    private int featureID = 0;
    private SUF2RecordCollector recordCollector;
    private SimpleFeature feature;
    private SortedMap info = new TreeMap();

    public SUF2FeatureReader(URL url, String typeName, String srs) throws IOException, SUF2ParseException {
        gf = new GeometryFactory(new PrecisionModel(100));
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

            ftb.add(SUF2Record.RECORDTYPE, String.class);
            ftb.add(SUF2Record.LKI_CLASSIFICATIECODE, String.class);
            ftb.add(SUF2Record.ANGLE, Double.class);
            ftb.add(SUF2Record01.BESTANDSIDENTIFICATIE, String.class);
            ftb.add(SUF2Record01.VOLLEDIG_OF_MUTATIE, String.class);
            ftb.add(SUF2Record01.DATUM_HERZIENING, String.class);
            ftb.add(SUF2Record01.DATUM_ACTUALITEIT, String.class);
            ftb.add(SUF2Record01.UITWISSELING_DEELBESTANDEN, String.class);
            ftb.add(SUF2Record01.UITWISSELING_DEELBESTANDEN_HUIDIG, String.class);
            ftb.add(SUF2Record01.BESTANDSNAAM, String.class);
            ftb.add(SUF2Record02.RD, String.class);
            ftb.add(SUF2Record02.LKI, String.class);
            ftb.add(SUF2Record02.COORD_MILLIMETERS, String.class);
            ftb.add(SUF2Record02.RICHTINGEN_MICROGON, String.class);
            ftb.add(SUF2Record02.LKI_SYMBOOL, String.class);
            ftb.add(SUF2Record02.NAP, String.class);
            ftb.add(SUF2Record02.HEEFT_OPTEL_X, String.class);
            ftb.add(SUF2Record02.HEEFT_OPTEL_Y, String.class);
            ftb.add(SUF2Record02.HEEFT_OPTEL_Z, String.class);
            ftb.add(SUF2Record02.VERMENIGVULDIGINGSCONSTANTE_XY, String.class);
            ftb.add(SUF2Record02.VERMENIGVULDIGINGSCONSTANTE_Z, String.class);

            ftb.add(SUF2Record03.GEMEENTECODEPERCEELLINKS,String.class);
            ftb.add(SUF2Record03.SECTIEPERCEELLINKS,String.class);
            ftb.add(SUF2Record03.INDEXLETTERPERCEELLINKS,String.class);
            ftb.add(SUF2Record03.PERCEELNUMMERLINKS,String.class);
            ftb.add(SUF2Record03.INDEXNUMMERLINKS,String.class);
            ftb.add(SUF2Record03.GEMEENTECODEPERCEELRECHTS,String.class);
            ftb.add(SUF2Record03.SECTIEPERCEELRECHTS,String.class);
            ftb.add(SUF2Record03.INDEXLETTERPERCEELRECHTS,String.class);
            ftb.add(SUF2Record03.PERCEELNUMMERRECHTS,String.class);
            ftb.add(SUF2Record03.INDEXNUMMERRECHTS,String.class);
            
            ftb.add(SUF2Record03.G_STRINGSOORT, String.class);
            ftb.add(SUF2Record03.G_ZICHTBAARHEID, String.class);
            ftb.add(SUF2Record03.G_INWINNING, String.class);
            ftb.add(SUF2Record03.G_STATUS_VAN_OBJECT, String.class);
            ftb.add(SUF2Record03.D_OPNAMEDATUM, String.class);
            ftb.add(SUF2Record03.B_BRONVERMELDING, String.class);
            ftb.add(SUF2Record03.B_WIJZE_VERZEKERING, String.class);
            ftb.add(SUF2Record04.I_COORD_FUNCTIE, String.class);
            ftb.add(SUF2Record04.Q_PRECISIEKLASSE, String.class);
            ftb.add(SUF2Record04.Q_IDEALISATIEKLASSE, String.class);
            ftb.add(SUF2Record04.Q_BETROUWBAARHEID, String.class);
            ftb.add(SUF2Record05.TEXT_ALIGN, String.class);
            ftb.add(SUF2Record05.STATUS_PERCEEL, String.class);
            ftb.add(SUF2Record05.TEKST_OF_SYMBOOL, String.class);
            ftb.add(SUF2Record05.SYMBOOLTYPE, String.class);
            ftb.add(SUF2Record06.VELDLENGTE, String.class);
            ftb.add(SUF2Record06.TEKST, String.class);
            ftb.add(SUF2Record06.GEMEENTECODE, String.class);
            ftb.add(SUF2Record06.SECTIE, String.class);
            ftb.add(SUF2Record06.PERCEELNUMMER, String.class);
            ftb.add(SUF2Record06.INDEXLETTER, String.class);
            ftb.add(SUF2Record06.INDEXNUMMER, String.class);
            ftb.add(SUF2Record.ID, Integer.class);
            ft = ftb.buildFeatureType();

        } catch (Exception e) {
            log.error("Error creating SimpleFeature",e);
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
                        boolean error = false;
                        try {
                            feature = createFeature(record);
                        } catch (Exception ex) {
                            log.debug("Exception in record " + record.getLineNumber() + "; " + ex.getLocalizedMessage());
                            error = true;
                        } finally {
                            if (!error) {
                                break;
                            }
                        }
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
            log.error(ex);
            throw new IOException(ex);
        }

        return true;
    }

    private SimpleFeature createFeature(SUF2Record record) throws Exception {
        Map properties = record.getProperties();

        Object[] values = new Object[]{
            SUF2GeometryFactory.createGeometry(gf, record), //the_geom
            record.getType().getDescription(), // Recordtype in plain text

            (String) properties.get(SUF2Record.RECORDTYPE),
            (String) properties.get(SUF2Record.LKI_CLASSIFICATIECODE),
            (properties.containsKey(SUF2Record06.ANGLE) ? (Double) properties.get(SUF2Record06.ANGLE) : new Double(0.0)),
            (String) properties.get(SUF2Record01.BESTANDSIDENTIFICATIE),
            (String) properties.get(SUF2Record01.VOLLEDIG_OF_MUTATIE),
            (String) properties.get(SUF2Record01.DATUM_HERZIENING),
            (String) properties.get(SUF2Record01.DATUM_ACTUALITEIT),
            (String) properties.get(SUF2Record01.UITWISSELING_DEELBESTANDEN),
            (String) properties.get(SUF2Record01.UITWISSELING_DEELBESTANDEN_HUIDIG),
            (String) properties.get(SUF2Record01.BESTANDSNAAM),
            (String) properties.get(SUF2Record02.RD),
            (String) properties.get(SUF2Record02.LKI),
            (String) properties.get(SUF2Record02.COORD_MILLIMETERS),
            (String) properties.get(SUF2Record02.RICHTINGEN_MICROGON),
            (String) properties.get(SUF2Record02.LKI_SYMBOOL),
            (String) properties.get(SUF2Record02.NAP),
            (String) properties.get(SUF2Record02.HEEFT_OPTEL_X),
            (String) properties.get(SUF2Record02.HEEFT_OPTEL_Y),
            (String) properties.get(SUF2Record02.HEEFT_OPTEL_Z),
            (String) properties.get(SUF2Record02.VERMENIGVULDIGINGSCONSTANTE_XY),
            (String) properties.get(SUF2Record02.VERMENIGVULDIGINGSCONSTANTE_Z),
            (String) properties.get(SUF2Record03.GEMEENTECODEPERCEELLINKS),
            (String) properties.get(SUF2Record03.SECTIEPERCEELLINKS),
            (String) properties.get(SUF2Record03.INDEXLETTERPERCEELLINKS),
            (String) properties.get(SUF2Record03.PERCEELNUMMERLINKS),
            (String) properties.get(SUF2Record03.INDEXNUMMERLINKS),
            (String) properties.get(SUF2Record03.GEMEENTECODEPERCEELRECHTS),
            (String) properties.get(SUF2Record03.SECTIEPERCEELRECHTS),
            (String) properties.get(SUF2Record03.INDEXLETTERPERCEELRECHTS),
            (String) properties.get(SUF2Record03.PERCEELNUMMERRECHTS),
            (String) properties.get(SUF2Record03.INDEXNUMMERRECHTS),
            (String) properties.get(SUF2Record03.G_STRINGSOORT),
            (String) properties.get(SUF2Record03.G_ZICHTBAARHEID),
            (String) properties.get(SUF2Record03.G_INWINNING),
            (String) properties.get(SUF2Record03.G_STATUS_VAN_OBJECT),
            (String) properties.get(SUF2Record03.D_OPNAMEDATUM),
            (String) properties.get(SUF2Record03.B_BRONVERMELDING),
            (String) properties.get(SUF2Record03.B_WIJZE_VERZEKERING),
            (String) properties.get(SUF2Record04.I_COORD_FUNCTIE),
            (String) properties.get(SUF2Record04.Q_PRECISIEKLASSE),
            (String) properties.get(SUF2Record04.Q_IDEALISATIEKLASSE),
            (String) properties.get(SUF2Record04.Q_BETROUWBAARHEID),
            (String) properties.get(SUF2Record05.TEXT_ALIGN),
            (String) properties.get(SUF2Record05.STATUS_PERCEEL),
            (String) properties.get(SUF2Record05.TEKST_OF_SYMBOOL),
            (String) properties.get(SUF2Record05.SYMBOOLTYPE),
            (String) properties.get(SUF2Record06.VELDLENGTE),
            (String) properties.get(SUF2Record06.TEKST),
            (String) properties.get(SUF2Record06.GEMEENTECODE),
            (String) properties.get(SUF2Record06.SECTIE),
            (String) properties.get(SUF2Record06.PERCEELNUMMER),
            (String) properties.get(SUF2Record06.INDEXLETTER),
            (String) properties.get(SUF2Record06.INDEXNUMMER),
                    (Integer) record.getLineNumber()
        };

        return SimpleFeatureBuilder.build(ft, values, Integer.toString(featureID++));
    }

    public void close() throws IOException {
        recordCollector.close();
    }
}
