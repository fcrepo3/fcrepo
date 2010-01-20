/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.test.api;

import org.fcrepo.client.FedoraClient;
import org.fcrepo.server.access.FedoraAPIA;
import org.fcrepo.server.types.gen.MIMETypedStream;
import org.fcrepo.server.types.gen.Property;
import org.fcrepo.test.DemoObjectTestSetup;
import org.fcrepo.test.FedoraServerTestCase;

import junit.framework.Test;
import junit.framework.TestSuite;




/**
 * Test API-A SOAP in configuration A (Authentication disabled on API-A).
 *
 * @author Chris Wilper
 */
public class TestAPIAConfigA
        extends FedoraServerTestCase {

    private FedoraAPIA apia;

    public static Test suite() {
        TestSuite suite = new TestSuite("APIAConfigA TestSuite");
        suite.addTestSuite(TestAPIAConfigA.class);
        return new DemoObjectTestSetup(suite);
    }

    public void testGetChainedDissemination() throws Exception {
        // test chained dissemination using local services
        // The object contains an E datastream that is a dissemination of the local SAXON service.
        // This datastream is input to another dissemination that uses the local FOP service.
        MIMETypedStream diss =
                apia.getDissemination("demo:26",
                                      "demo:19",
                                      "getPDF",
                                      new Property[0],
                                      null);
        assertEquals(diss.getMIMEType(), "application/pdf");
        assertTrue(diss.getStream().length > 0);
    }

    @Override
    public void setUp() throws Exception {
        FedoraClient client = getFedoraClient();
        apia = client.getAPIA();
    }

}
