/*
 * $Id: SUF2DataStore.java 8672 2008-07-17 16:37:57Z Matthijs $
 */
package nl.b3p.geotools.data.suf2;

import nl.b3p.suf2.SUF2ParseException;
import java.io.IOException;
import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.AbstractFileDataStore;
import org.geotools.data.FeatureReader;
import org.opengis.feature.simple.SimpleFeatureType;
//import org.geotools.Feature.SimpleFeatureType;

/**
 * DataStore for reading a SUF2 file produced by Autodesk SDF Loader which
 * supports the legacy SDF format (which FDO/MapGuide Open Source can't read). 
 * The SDF component toolkit can read those but COM objects are perhaps a 
 * lesser evil than Runtime.exec()'ing the SDF Loader.
 * 
 * Note that a single SUF2 file can contain point, line and polygon SimpleFeatures.
 * Although many files will only contain a single type, the parser can only 
 * determine this by looking through the entire file - which is not advisable in 
 * a streaming API. The same is true for a file containing only polygons or also 
 * multipolygons etc. 
 * 
 * Therefore always the same SimpleFeature schema is used:
 * the_geom_point: Point (SUF2 does not contains MultiPoints)
 * the_geom_line: MultiLineString (getNumGeometries() can be 1) 
 * the_geom_polygon: MultiPolygons (getNumGeometries() can be 1, can contain holes)
 * Where only one of three is not null. The attributes are always the same:
 * key: String
 * name: String
 * urlLink: String
 * entryLineNumber: Integer
 * parseError: Boolean
 * error: String
 * 
 * Note that especially polygons can contain parse errors due to randomly
 * duplicated coordinates. Not much that can be done about that, because sometimes
 * it is not possible to determine if the coordinate is duplicated or a closing
 * coordinate of a subgeometry.
 * 
 * See the SDF Loader Help for the description of the SUF2 file format.
 * 
 * @author Gertjan Al, B3Partners
 */
public class SUF2DataStore extends AbstractFileDataStore {

    private static final Log log = LogFactory.getLog(SUF2DataStore.class);
    private URL url;
    private String typename;
    private FeatureReader featureReader;
    private String srs;

    public SUF2DataStore(URL url, String srs) throws IOException {
        this.url = url;
        this.typename = getURLTypeName(url);
        this.srs = srs;
    }

    public String[] getTypeNames() throws IOException {
        return new String[]{getURLTypeName(url)};
    }

    static String getURLTypeName(URL url) throws IOException {
        String file = url.getFile();
        if (file.length() == 0) {
            return "unknown_nen";
        } else {
            int i = file.lastIndexOf('/');
            if (i != -1) {
                file = file.substring(i + 1);
            }
            if (file.toLowerCase().endsWith(".nen")) {
                file = file.substring(0, file.length() - 4);
            }
            return file;
        }
    }

    public SimpleFeatureType getSchema(String typeName) throws IOException {
        /* only one type */
        return getSchema();
    }

    public SimpleFeatureType getSchema() throws IOException {
        return (SimpleFeatureType)getFeatureReader().getFeatureType();
    }

    public FeatureReader getFeatureReader(String typeName) throws IOException {
        /* only one type */
        return getFeatureReader();
    }

    public FeatureReader getFeatureReader() throws IOException {
        if (featureReader == null) {
            try {
                featureReader = new SUF2FeatureReader(url, typename, srs);
            } catch (SUF2ParseException e) {
                throw new IOException("SUF2 parse exception" + e.getLocalizedMessage());
            }
        }
        return featureReader;
    }
}
