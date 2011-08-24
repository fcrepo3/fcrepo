/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.test.integration.cma;

import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;

import org.fcrepo.client.FedoraClient;
import org.fcrepo.client.utility.ingest.Ingest;
import org.fcrepo.client.utility.ingest.IngestCounter;

import org.fcrepo.server.types.gen.ObjectMethodsDef;
import org.fcrepo.server.utilities.TypeUtility;

import org.fcrepo.test.FedoraTestCase;

import static org.fcrepo.common.Constants.FOXML1_1;

public abstract class Util {

    /* Remove any system methods */
    public static ObjectMethodsDef[] filterMethods(ObjectMethodsDef[] initial) {
        ArrayList<ObjectMethodsDef> desiredDefs =
                new ArrayList<ObjectMethodsDef>();

        for (ObjectMethodsDef def : initial) {
            if (!def.getServiceDefinitionPID().startsWith("fedora-system:")
                    && def != null) {
                desiredDefs.add(def);
            }
        }

        return desiredDefs.toArray(new ObjectMethodsDef[0]);
    }

    /* Get a given dissemination as a string */
    public static String getDissemination(FedoraClient client,
                                          String pid,
                                          String sDef,
                                          String method) throws Exception {
        return new String(TypeUtility.convertDataHandlerToBytes(client
                .getAPIA().getDissemination(pid, sDef, method, null, null)
                .getStream()), "UTF-8");

    }

    public static void ingestTestObjects(String path) throws Exception {
        File dir = null;

        String specificPath = File.separator + path;

        System.out.println("Ingesting test objects in FOXML format from "
                + specificPath);

        String base = "src/test/resources/";

        if (System.getProperty("fcrepo-integrationtest-core.classes") != null) {
            base = System.getProperty("fcrepo-integrationtest-core.classes");
        }

        dir = new File(base + "test-objects/foxml" + specificPath);

        FedoraClient client = FedoraTestCase.getFedoraClient();

        Ingest.multiFromDirectory(dir,
                                  FOXML1_1.uri,
                                  client.getAPIA(),
                                  client.getAPIM(),
                                  null,
                                  new PrintStream(File.createTempFile("demo",
                                                                      null)),
                                  new IngestCounter());
    }
}
