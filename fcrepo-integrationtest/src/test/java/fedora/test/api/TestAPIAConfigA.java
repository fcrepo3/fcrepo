/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.test.api;

import junit.framework.Test;
import junit.framework.TestSuite;

import fedora.client.FedoraClient;

import fedora.server.access.FedoraAPIA;
import fedora.server.types.gen.MIMETypedStream;
import fedora.server.types.gen.Property;

import fedora.test.DemoObjectTestSetup;
import fedora.test.FedoraServerTestCase;

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
