/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test.integration.cma;

import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;

import fedora.client.FedoraClient;
import fedora.client.utility.ingest.Ingest;
import fedora.client.utility.ingest.IngestCounter;

import fedora.server.types.gen.ObjectMethodsDef;

import fedora.test.FedoraTestCase;

import static fedora.common.Constants.FOXML1_1;

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
        return new String(client.getAPIA().getDissemination(pid,
                                                            sDef,
                                                            method,
                                                            null,
                                                            null).getStream(),
                          "UTF-8");

    }

    public static void ingestTestObjects(String path) throws Exception {
        File dir = null;

        String specificPath = File.separator + path;

        System.out.println("Ingesting test objects in FOXML format from "
                + specificPath);
        dir = new File("src/test/resources/test-objects/foxml" + specificPath);

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
