
package org.fcrepo.test.fesl.util;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadDataset {

    private static final Logger logger =
            LoggerFactory.getLogger(LoadDataset.class);

    private static final String PROPERTIES = "fedora";

    private static final String RESOURCEBASE =
        System.getProperty("fcrepo-integrationtest-core.classes") != null ? System
                .getProperty("fcrepo-integrationtest-core.classes")
                + "test-objects/foxml"
                : "src/test/resources/test-objects/foxml";

    private static HttpUtils client = null;

    public static void load(String subdirectory, String fedoraUrl, String username, String password) throws Exception {

        try {
            client = new HttpUtils(fedoraUrl, username, password);
        } catch (Exception e) {
            logger.error("Could not instantiate HttpUtils.", e);
            return;
        }

        // subdir was "fesl", now a parameter
        File dataDir = new File(RESOURCEBASE + "/" + subdirectory);
        File[] files = dataDir.listFiles(new XmlFilenameFilter());

        for (File f : files) {
            //try {
                byte[] data = DataUtils.loadFile(f);
                client.post("/fedora/objects/new", null, data);
            //} catch (Exception e) {
            //    logger.error(e.getMessage(), e);
            //}
        }
        // need to ensure resource index is up-to-date
        // spo query, flush=true, querying on a non-existent subject, limit=1
        String riSearchFlush = "/fedora/risearch?type=triples&flush=true&lang=spo&format=Turtle&limit=1&query=%3cinfo%3afedora%2fdoes%3anotexist%3e%20*%20*";
        try {
            client.get(riSearchFlush);
        } catch (Exception e) {
            // ignore exceptions, resource index might not be enabled
            System.out.println("Exception on flushing resource index (loading test fesl objects) " + e.getMessage());
        }

    }
}
