/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.test.api;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.fcrepo.client.FedoraClient;

import org.fcrepo.server.access.FedoraAPIAMTOM;
import org.fcrepo.server.types.mtom.gen.GetDissemination.Parameters;
import org.fcrepo.server.types.mtom.gen.MIMETypedStream;
import org.fcrepo.server.types.mtom.gen.Property;
import org.fcrepo.server.utilities.TypeUtility;

import org.fcrepo.test.DemoObjectTestSetup;
import org.fcrepo.test.FedoraServerTestCase;




/**
 * Test API-A SOAP in configuration A (Authentication disabled on API-A).
 *
 * @author Chris Wilper
 */
public class TestAPIAConfigA
        extends FedoraServerTestCase {

    private FedoraAPIAMTOM apia;

    public static Test suite() {
        TestSuite suite = new TestSuite("APIAConfigA TestSuite");
        suite.addTestSuite(TestAPIAConfigA.class);
        return new DemoObjectTestSetup(suite);
    }

    public void testGetChainedDissemination() throws Exception {
        // test chained dissemination using local services
        // The object contains an E datastream that is a dissemination of the local SAXON service.
        // This datastream is input to another dissemination that uses the local FOP service.
        Parameters params = new Parameters();
        params.getParameter().add(new Property());
        MIMETypedStream diss =
                apia.getDissemination("demo:26",
                                      "demo:19",
                                      "getPDF",
                                      params,
                                      null);
        assertEquals(diss.getMIMEType(), "application/pdf");
        assertTrue(TypeUtility.convertDataHandlerToBytes(diss.getStream()).length > 0);
    }

    @Override
    public void setUp() throws Exception {
        FedoraClient client = getFedoraClient();
        apia = client.getAPIA();
    }

}
