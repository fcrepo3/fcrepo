/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test;

import org.fcrepo.client.FedoraClient;

import org.fcrepo.common.FedoraTestConstants;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.fcrepo.server.access.FedoraAPIAMTOM;
import org.fcrepo.server.management.FedoraAPIMMTOM;


public class DemoObjectTestSetup
        extends TestSetup
        implements FedoraTestConstants {
    
    private FedoraAPIAMTOM apia;
    private FedoraAPIMMTOM apim;

    public DemoObjectTestSetup(Test test) {
        super(test);
        try {
            FedoraClient client = FedoraTestCase.getFedoraClient();
            apim = client.getAPIMMTOM();
            apia = client.getAPIAMTOM();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setUp() throws Exception {
        System.out.println("Ingesting demo objects...");
        FedoraServerTestCase.ingestDemoObjects(apia, apim);
    }

    @Override
    public void tearDown() throws Exception {
        System.out.println("Purging demo objects...");
        FedoraServerTestCase.purgeDemoObjects(apim);
    }
}
