/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.test.api;

import static junit.framework.Assert.assertEquals;
import junit.framework.JUnit4TestAdapter;

import org.fcrepo.client.FedoraClient;
import org.fcrepo.common.http.HttpInputStream;
import org.fcrepo.test.FedoraServerTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;

/**
 * Test API-A Lite in configuration A (Authentication disabled on API-A).
 *
 * @author Chris Wilper
 */
public class TestAPIALiteConfigA
        extends FedoraServerTestCase {

    private FedoraClient client;

    @Test
    public void testGetChainedDissemination() throws Exception {
        // test chained dissemination using local services
        // The object contains an E datastream that is a dissemination of the local SAXON service.
        // This datastream is input to another dissemination that uses the local FOP service.
        HttpInputStream his = client.get("/get/demo:26/demo:19/getPDF", false);
        String contentType = his.getContentType();
        his.close();
        assertEquals("application/pdf", contentType);
    }

    @Before
    public void setUp() throws Exception {
        client = getFedoraClient();
        // demo:26, demo:19
        ingestFormattingObjectsDemoObjects(client);
    }
    
    @After
    public void tearDown() throws Exception {
        purgeDemoObjects(client);
        client.shutdown();
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestAPIALiteConfigA.class);
    }

    public static void main(String[] args) {
        JUnitCore.runClasses(TestAPIALiteConfigA.class);
    }

}
