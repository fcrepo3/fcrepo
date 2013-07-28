
package org.fcrepo.test.fesl.util;

import java.io.File;

import org.w3c.dom.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveDataset {

    private static final Logger logger =
            LoggerFactory.getLogger(RemoveDataset.class);

    private static final String RESOURCEBASE =
            System.getProperty("fcrepo-integrationtest-core.classes") != null ? System
                    .getProperty("fcrepo-integrationtest-core.classes")
                    + "test-objects/foxml"
                    : "src/test/resources/test-objects/foxml";

    private static HttpUtils client = null;

    public static void remove(String subdir, String fedoraUrl, String username, String password)
            throws Exception {

        try {
            client = new HttpUtils(fedoraUrl, username, password);
        } catch (Exception e) {
            logger.error("Could not instantiate HttpUtils.", e);
            return;
        }

        File dataDir = new File(RESOURCEBASE + "/" + subdir);
        File[] files = dataDir.listFiles(new XmlFilenameFilter());

        try {
            for (File f : files) {
                // try {
                Document doc = DataUtils.getDocumentFromFile(f);
                String pid = doc.getDocumentElement().getAttribute("PID");
                if (logger.isDebugEnabled()) {
                    logger.debug("Deleting object: " + pid);
                }
                client.delete("/fedora/objects/" + pid, null);
                //} catch (Exception e) {
                //    logger.error(e.getMessage(), e);
                //}
            }
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }
}
