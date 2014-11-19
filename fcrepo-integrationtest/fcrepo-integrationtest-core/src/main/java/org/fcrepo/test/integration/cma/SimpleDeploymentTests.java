/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.integration.cma;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.fcrepo.test.integration.cma.Util.filterMethods;

import org.fcrepo.client.FedoraClient;
import org.fcrepo.client.utility.AutoPurger;
import org.fcrepo.server.access.FedoraAPIAMTOM;
import org.fcrepo.server.management.FedoraAPIMMTOM;
import org.fcrepo.server.types.gen.ObjectMethodsDef;
import org.fcrepo.test.FedoraServerTestCase;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class SimpleDeploymentTests {

    private static FedoraClient s_client;

    private static int s_items_ingested = 0;

    private static final String OBJECT_1_PID =
            "demo:simple-deployment.object.1";

    private static final String SDEF_1_PID = "demo:simple-deployment.sdef.1";

    private static final String SDEF_2_PID = "demo:simple-deployment.sdef.2";

    private static final String SDEF_1_METHOD = "content";

    private static final String SDEF_2_METHOD = "content2";

    private static final String SDEP_1_PID = "demo:simple-deployment.sdep.1";

    private static final String SDEP_2_PID = "demo:simple-deployment.sdep.2";

    private static final String SIMPLE_DEPLOYMENT_BASE =
            "cma-examples/simple-deployment";

    private static final String SIMPLE_DEPLOYMENT_PUBLIC_OBJECTS =
            SIMPLE_DEPLOYMENT_BASE + "/public-objects";

    private static final String SIMPLE_DEPLOYMENT_DEPLOYMENTS =
            SIMPLE_DEPLOYMENT_BASE + "/deployments";

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(SimpleDeploymentTests.class);
    }

    @BeforeClass
    public static void bootstrap() throws Exception {

        s_client =
                new FedoraClient(FedoraServerTestCase.getBaseURL(),
                                 FedoraServerTestCase.getUsername(),
                                 FedoraServerTestCase.getPassword());
        s_items_ingested = Util.ingestTestObjects(s_client, SIMPLE_DEPLOYMENT_DEPLOYMENTS);
        s_items_ingested += Util.ingestTestObjects(s_client, SIMPLE_DEPLOYMENT_PUBLIC_OBJECTS);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        FedoraServerTestCase.purgeDemoObjects(s_client);
        s_client.shutdown();
    }

    @Before
    public void setUp() {
        assertTrue("Nothing was ingested from " + Util.resourcePath(SIMPLE_DEPLOYMENT_BASE), s_items_ingested > 0);
    }

    /* Assure that listMethods works as advertised */
    @Test
    public void testListMethods() throws Exception {
        FedoraAPIAMTOM apia = s_client.getAPIAMTOM();
        ObjectMethodsDef[] methods;

        methods = filterMethods(apia.listMethods(OBJECT_1_PID, null).toArray(new ObjectMethodsDef[0]));

        assertEquals("Wrong number of methods", 2, methods.length);
        assertNotSame("SDeps are not distinct", methods[0]
                .getServiceDefinitionPID(), methods[1]
                .getServiceDefinitionPID());
        assertNotSame("Methods are not distinct",
                      methods[0].getMethodName(),
                      methods[1].getMethodName());

        /* Order may not be deterministic, but that's OK */
        if (methods[0].getServiceDefinitionPID().equals(SDEF_1_PID)) {
            assertEquals(methods[1].getServiceDefinitionPID(), SDEF_2_PID);
            assertEquals(methods[0].getMethodName(), SDEF_1_METHOD);
            assertEquals(methods[1].getMethodName(), SDEF_2_METHOD);
        } else if (methods[0].getServiceDefinitionPID().equals(SDEF_2_PID)) {
            assertEquals(methods[1].getServiceDefinitionPID(), SDEF_1_PID);
            assertEquals(methods[0].getMethodName(), SDEF_2_METHOD);
            assertEquals(methods[1].getMethodName(), SDEF_1_METHOD);
        }
    }

    /* Assure that listMethods works without sDeps */
    @Test
    public void testListMethodsWithoutSDeps() throws Exception {
        FedoraAPIMMTOM apim = s_client.getAPIMMTOM();
        AutoPurger.purge(apim, SDEP_1_PID, null);
        AutoPurger.purge(apim, SDEP_2_PID, null);
        try {
            testListMethods();
        } finally {
            Util.ingestTestObjects(s_client, SIMPLE_DEPLOYMENT_DEPLOYMENTS);
        }
    }

    /* Assure that disseminations return expected content */
    @Test
    public void testDissemination1() throws Exception {
        assertTrue("Wrong dissemination content",
                   getDissemination(OBJECT_1_PID, SDEF_1_PID, SDEF_1_METHOD)
                           .contains("CONTENT_1"));
        try {
            getDissemination(OBJECT_1_PID, SDEF_1_PID, SDEF_2_METHOD);
            fail("Should not have been able to disseminate");
        } catch (javax.xml.ws.soap.SOAPFaultException e) {
            /* Expected */
        }

        try {
            getDissemination(OBJECT_1_PID, SDEF_2_PID, SDEF_1_METHOD);
            fail("Should not have been able to disseminate");
        } catch (javax.xml.ws.soap.SOAPFaultException e) {
            /* Expected */
        }
    }

    @Test
    public void testDissemination2() throws Exception {
        assertTrue("Wrong dissemination content",
                   getDissemination(OBJECT_1_PID, SDEF_2_PID, SDEF_2_METHOD)
                           .contains("CONTENT_2"));

        /* other permutations tested in testDissemination1 */
    }

    /* Assure that only methods in SDefs work */

    private String getDissemination(String pid, String sDef, String method)
            throws Exception {
        return Util.getDissemination(s_client, pid, sDef, method);
    }

}
