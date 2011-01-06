
package org.fcrepo.test.fesl.util;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadDataset {

    private static final Logger logger =
            LoggerFactory.getLogger(LoadDataset.class);

    private static final String PROPERTIES = "fedora";

    private static final String RESOURCEBASE =
            "src/test/resources/test-objects/foxml";

    private static HttpUtils client = null;

    public static void load(String fedoraUrl, String username, String password) {

        try {
            client = new HttpUtils(fedoraUrl, username, password);
        } catch (Exception e) {
            logger.error("Could not instantiate HttpUtils.", e);
            return;
        }

        File dataDir = new File(RESOURCEBASE + "/fesl");
        File[] files = dataDir.listFiles(new XmlFilenameFilter());

        for (File f : files) {
            try {
                byte[] data = DataUtils.loadFile(f);
                client.post("/fedora/objects/new", null, data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
