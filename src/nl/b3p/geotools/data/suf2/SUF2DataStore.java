package nl.b3p.geotools.data.suf2;

import nl.b3p.suf2.SUF2ParseException;
import java.io.IOException;
import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.AbstractFileDataStore;
import org.geotools.data.FeatureReader;
import org.opengis.feature.simple.SimpleFeatureType;

/**
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
        return (SimpleFeatureType) getFeatureReader().getFeatureType();
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
