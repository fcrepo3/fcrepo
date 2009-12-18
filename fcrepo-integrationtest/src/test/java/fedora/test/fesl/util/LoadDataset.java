
package fedora.test.fesl.util;

import java.io.File;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

public class LoadDataset {

    private static Logger log = Logger.getLogger(LoadDataset.class);

    private static final String PROPERTIES = "fedora";

    private static final String RESOURCEBASE =
            "src/test/resources/test-objects/foxml";

    private static HttpUtils client = null;

    public static void main(String[] args) {
        PropertyResourceBundle prop =
                (PropertyResourceBundle) ResourceBundle.getBundle(PROPERTIES);
        String username = prop.getString("fedora.admin.username");
        String password = prop.getString("fedora.admin.password");
        String fedoraUrl = prop.getString("fedora.url");

        try {
            client = new HttpUtils(fedoraUrl, username, password);
        } catch (Exception e) {
            log.fatal("Could not instantiate HttpUtils.", e);
            return;
        }

        File dataDir = new File(RESOURCEBASE + "/fesl");
        File[] files = dataDir.listFiles(new XmlFilenameFilter());

        for (File f : files) {
            log.info("Loading foxml object: " + f.getName());
            try {
                byte[] data = DataUtils.loadFile(f);
                client.post("/fedora/objects/new", null, data);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
