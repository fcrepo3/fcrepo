/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.test.api;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import junit.framework.JUnit4TestAdapter;

import org.fcrepo.client.FedoraClient;
import org.fcrepo.server.access.FedoraAPIAMTOM;
import org.fcrepo.server.types.mtom.gen.GetDissemination.Parameters;
import org.fcrepo.server.types.mtom.gen.MIMETypedStream;
import org.fcrepo.server.utilities.TypeUtility;
import org.fcrepo.test.FedoraServerTestCase;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;




/**
 * Test API-A SOAP in configuration A (Authentication disabled on API-A).
 *
 * @author Chris Wilper
 */
public class TestAPIAConfigA
        extends FedoraServerTestCase {

    private static FedoraClient s_client;
    
    private FedoraAPIAMTOM apia;

    @Test
    public void testGetChainedDissemination() throws Exception {
        // test chained dissemination using local services
        // The object contains an E datastream that is a dissemination of the local SAXON service.
        // This datastream is input to another dissemination that uses the local FOP service.
        Parameters params = new Parameters();
        //params.getParameter().add(new Property());
        MIMETypedStream diss =
                apia.getDissemination("demo:26",
                                      "demo:19",
                                      "getPDF",
                                      params,
                                      null);
        assertEquals(diss.getMIMEType(), "application/pdf");
        assertTrue(TypeUtility.convertDataHandlerToBytes(diss.getStream()).length > 0);
    }
    
    @BeforeClass
    public static void bootstrap() throws Exception {
        s_client = getFedoraClient();
        // demo:19, demo:26
        ingestFormattingObjectsDemoObjects(s_client);
    }
    
    @AfterClass
    public static void cleanUp() throws Exception {
        purgeDemoObjects(s_client);
        s_client.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        apia = s_client.getAPIAMTOM();
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestAPIAConfigA.class);
    }

    public static void main(String[] args) {
        JUnitCore.runClasses(TestAPIAConfigA.class);
    }

}
