/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test;

import org.fcrepo.client.FedoraClient;

import org.fcrepo.common.FedoraTestConstants;

public abstract class DemoObjectTestSetup
        implements FedoraTestConstants {
    
    public static void ingestDemoObjects(FedoraClient client)
        throws Exception {
        System.out.println("Ingesting demo objects...");
        FedoraServerTestCase
             .ingestDemoObjects(client.getAPIAMTOM(), client.getAPIMMTOM());
    }

    public static void purgeDemoObjects(FedoraClient client)
        throws Exception {
        System.out.println("Purging demo objects...");
        FedoraServerTestCase
            .purgeDemoObjects(client);
    }
}
