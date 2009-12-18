/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test.integration.cma;

import org.apache.axis.AxisFault;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fedora.client.FedoraClient;

import fedora.server.access.FedoraAPIA;
import fedora.server.types.gen.ObjectMethodsDef;

import fedora.test.FedoraServerTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import static fedora.test.integration.cma.Util.filterMethods;

public class SimpleDeploymentTests {

    private static FedoraClient m_client;

    private static final String OBJECT_1_PID =
            "demo:simple-deployment.object.1";

    private static final String SDEF_1_PID = "demo:simple-deployment.sdef.1";

    private static final String SDEF_2_PID = "demo:simple-deployment.sdef.2";

    private static final String SDEF_1_METHOD = "content";

    private static final String SDEF_2_METHOD = "content2";

    private static final String SIMPLE_DEPLOYMENT_BASE =
            "cma-examples/simple-deployment";

    private static final String SIMPLE_DEPLOYMENT_PUBLIC_OBJECTS =
            "cma-examples/simple-deployment/public-objects";

    private static final String SIMPLE_DEPLOYMENT_DEPLOYMENTS =
            "cma-examples/simple-deployment/deployments";

    public static junit.framework.Test suite() {
        // FIXME: The specified class should be 'Simple...' not 'Shared...'
        //        But test does not work when classname set correctly.
        return new junit.framework.JUnit4TestAdapter(SharedDeploymentTests.class);
    }

    @BeforeClass
    public static void bootstrap() throws Exception {

        m_client =
                new FedoraClient(FedoraServerTestCase.getBaseURL(),
                                 FedoraServerTestCase.getUsername(),
                                 FedoraServerTestCase.getPassword());
        Util.ingestTestObjects(SIMPLE_DEPLOYMENT_BASE);
    }

    /* Assure that listMethods works as advertised */
    @Test
    public void testListMethods() throws Exception {
        FedoraAPIA apia = m_client.getAPIA();
        ObjectMethodsDef[] methods;

        methods = filterMethods(apia.listMethods(OBJECT_1_PID, null));

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
        FedoraServerTestCase.purgeDemoObjects();
        Util
                .ingestTestObjects(SIMPLE_DEPLOYMENT_PUBLIC_OBJECTS);
        try {
            testListMethods();
        } finally {
            Util
                    .ingestTestObjects(SIMPLE_DEPLOYMENT_DEPLOYMENTS);
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
        } catch (AxisFault e) {
            /* Expected */
        }

        try {
            getDissemination(OBJECT_1_PID, SDEF_2_PID, SDEF_1_METHOD);
            fail("Should not have been able to disseminate");
        } catch (AxisFault e) {
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
        return Util.getDissemination(m_client, pid, sDef, method);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        FedoraServerTestCase.purgeDemoObjects();
    }
}
