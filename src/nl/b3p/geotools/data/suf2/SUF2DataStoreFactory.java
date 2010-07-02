package nl.b3p.geotools.data.suf2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

/**
 * @author Gertjan Al, B3Partners
 */
public class SUF2DataStoreFactory implements FileDataStoreFactorySpi {

    public static final DataStoreFactorySpi.Param PARAM_URL = new Param("url", URL.class, "url to a .nen file");
    public static final DataStoreFactorySpi.Param PARAM_SRS = new Param("srs", String.class, "override srs");

    public String getDisplayName() {
        return "SUF2 File";
    }

    public String getDescription() {
        return "SUF2 NEN 1878 format";
    }

    public String[] getFileExtensions() {
        return new String[]{".nen", ".sfn"};
    }

    /**
     * @return true if the file of the f parameter exists
     */
    public boolean canProcess(URL f) {
        return f.getFile().toLowerCase().endsWith(".nen") || f.getFile().toLowerCase().endsWith(".sfn");
    }

    /**
     * @return true if srs can be resolved
     */
    public boolean canProcess(String srs) throws NoSuchAuthorityCodeException, FactoryException {
        return CRS.decode(srs) != null;
    }

    /**
     * @return true if the file in the url param exists
     */
    public boolean canProcess(Map params) {
        boolean result = false;
        if (params.containsKey(PARAM_URL.key)) {
            try {
                URL url = (URL) PARAM_URL.lookUp(params);
                result = canProcess(url);
            } catch (IOException ioe) {
                /* return false on any exception */
            }
        }
        if (result && params.containsKey(PARAM_SRS.key)) {
            try {
                String srs = (String) PARAM_SRS.lookUp(params);
                result = canProcess(srs);
            } catch (NoSuchAuthorityCodeException ex) {
                /* return false on any exception */
            } catch (FactoryException ex) {
                /* return false on any exception */
            } catch (IOException ioe) {
                /* return false on any exception */
            }
        }
        return result;
    }

    /*
     * Always returns true, no additional libraries needed
     */
    public boolean isAvailable() {
        return true;
    }

    public Param[] getParametersInfo() {
        return new Param[]{PARAM_URL};
    }

    public Map getImplementationHints() {
        /* XXX do we need to put something in this map? */
        return Collections.EMPTY_MAP;
    }

    public String getTypeName(URL url) throws IOException {
        return SUF2DataStore.getURLTypeName(url);
    }

    public FileDataStore createDataStore(URL url) throws IOException {
        Map params = new HashMap();
        params.put(PARAM_URL.key, url);

        boolean isLocal = url.getProtocol().equalsIgnoreCase("file");
        if (isLocal && !(new File(url.getFile()).exists())) {
            throw new UnsupportedOperationException("Specified SUF2 file \"" + url + "\" does not exist, this plugin is read-only so no new file will be created");
        } else {
            return createDataStore(params);
        }
    }

    public FileDataStore createDataStore(Map params) throws IOException {
        if (!canProcess(params)) {
            throw new FileNotFoundException("SUF2 file not found: " + params);
        }
        return new SUF2DataStore((URL) params.get(PARAM_URL.key), (String) params.get(PARAM_SRS.key));
    }

    public DataStore createNewDataStore(Map params) throws IOException {
        throw new UnsupportedOperationException("This plugin is read-only");
    }
}