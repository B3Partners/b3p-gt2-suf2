package nl.b3p.geotools.data.suf2;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import nl.b3p.suf2.SUF2ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.DefaultServiceInfo;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.FileDataStore;
import org.geotools.data.LockingManager;
import org.geotools.data.Query;
import org.geotools.data.ServiceInfo;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

/**
 * @author Gertjan Al, B3Partners
 * @author mprins
 */
public class SUF2DataStore implements FileDataStore {

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
            if (file.toLowerCase().endsWith(".nen") || file.toLowerCase().endsWith(".sfn")) {
                file = file.substring(0, file.length() - 4);
            }
            return file;
        }
    }

    public SimpleFeatureType getSchema(String typeName) throws IOException {
        /* only one type */
        return getSchema();
    }

    public SimpleFeatureType getSchema(Name name) throws IOException {
        return getSchema();
    }

    public SimpleFeatureType getSchema() throws IOException {
        return (SimpleFeatureType) getFeatureReader().getFeatureType();
    }

    public List<Name> getNames() throws IOException {
        return Arrays.asList((Name) new NameImpl(getSchema().getTypeName()));
    }

    public FeatureReader getFeatureReader(String typeName) throws IOException {
        /* only one type */
        return getFeatureReader();
    }

    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(Query query, Transaction transaction) throws IOException {
        return getFeatureReader();
    }

    public FeatureReader getFeatureReader() throws IOException {
        try {
            featureReader = new SUF2FeatureReader(url, typename, srs);
            return featureReader;
        } catch (SUF2ParseException e) {
            throw new IOException("SUF2 parse exception" + e.getLocalizedMessage());
        }
    }

    public SimpleFeatureSource getFeatureSource() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public SimpleFeatureSource getFeatureSource(String typeName) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public SimpleFeatureSource getFeatureSource(Name typeName) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void createSchema(SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void updateSchema(SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }

    public void updateSchema(String typeName, SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }

    public void updateSchema(Name typeName, SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }

    public void removeSchema(String typeName) throws IOException {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }

    public void removeSchema(Name typeName) throws IOException {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(Filter filter, Transaction transaction) throws IOException {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(Transaction transaction) throws IOException {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(Transaction transaction) throws IOException {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(String typeName, Filter filter, Transaction transaction) throws IOException {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(String typeName, Transaction transaction) throws IOException {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(String typeName, Transaction transaction) throws IOException {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }

    public LockingManager getLockingManager() {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }

    public ServiceInfo getInfo() {
        DefaultServiceInfo serviceInfo = new DefaultServiceInfo();
        serviceInfo.setTitle("SUF2 DataStore");
        try {
            serviceInfo.setSource(this.url.toURI());
        } catch (URISyntaxException ex) {

        }
        return serviceInfo;
    }

    public void dispose() {
        try {
            this.featureReader.close();
        } catch (IOException | NullPointerException ex) {
            log.debug("Mogelijk probleem met sluiten van featureReader", ex);
        }
        this.featureReader = null;
    }

}
