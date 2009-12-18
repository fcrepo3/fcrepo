/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test.integration.cma;

import org.apache.axis.AxisFault;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
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

/**
 * Tests involving one SDep that deploys for more than one SDef.
 * <p>
 * The services and methods available to an object are determined, in the CMA,
 * by the paths formed between the object, its model, and the services for that
 * model. Furthermore, individual methods/operations/endpoints are defined
 * within each individual service object. In order to invoke a specific method,
 * the identites of the object, the sDef, and the method need to be specified.
 * In that light, this suite tests the following:
 * <ul>
 * <li>The system will enumerate (via iewItemIndex, listMethods, etc) ONLY those
 * services/methods/endpoints that are specified by the graph containing the
 * object, the model, and the service. Service Deployments should therefore be
 * irrelevant to this process.</li>
 * <li>A given service endpoint triplet (object + sDef + method name) uniquely
 * identifies a valid service on an object.</li>
 * <li>Service Deployments deploy multiple operations/methods - and there is no
 * specific correlation between an SDep's set of implemented methods, and the
 * methods declared in any related SDef that it is a deployment of. In other
 * words, a given SDep may implement a subset of an SDef's methods, and an SDef
 * may make use of multiple SDeps to provide full deployment coverage of all its
 * methods.</li>
 * </ul>
 * </p>
 *
 * @author birkland
 */
public class SharedDeploymentTests {

    private static final String SHARED_DEPLOYMENT_BASE =
            "cma-examples/shared-deployments";

    private static final String OBJECT_1_PID =
            "demo:shared-deployments.object.1";

    private static final String OBJECT_2_PID =
            "demo:shared-deployments.object.2";

    private static final String OBJECT_1_2_PID =
            "demo:shared-deployments.object.1-2";

    private static final String OBJECT_3_PID =
            "demo:shared-deployments.object.3";

    private static final String OBJECT_4_PID =
            "demo:shared-deployments.object.4";

    private static final String SDEF_1_PID = "demo:shared-deployments.sdef.1";

    private static final String SDEF_2_PID = "demo:shared-deployments.sdef.2";

    private static final String SDEF_4_PID = "demo:shared-deployments.sdef.4";

    private static final String SDEF_1_METHOD = "content";

    private static final String SDEF_2_METHOD = "content2";

    private static final String SDEF_4_METHOD = "content4";

    private static FedoraClient m_client;

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(SharedDeploymentTests.class);
    }

    @BeforeClass
    public static void bootstrap() throws Exception {

        m_client =
                new FedoraClient(FedoraServerTestCase.getBaseURL(),
                                 FedoraServerTestCase.getUsername(),
                                 FedoraServerTestCase.getPassword());
        Util.ingestTestObjects(SHARED_DEPLOYMENT_BASE);
    }

    @Test
    public void testListMethods1() throws Exception {
        FedoraAPIA apia = m_client.getAPIA();
        ObjectMethodsDef[] methods;

        methods = filterMethods(apia.listMethods(OBJECT_1_PID, null));

        assertEquals("Wrong number of methods", 1, methods.length);
        assertEquals("Wrong method SDep", SDEF_1_PID, methods[0]
                .getServiceDefinitionPID());
        assertEquals("Wrong method", "content", methods[0].getMethodName());
    }

    @Test
    public void testListMethods2() throws Exception {
        FedoraAPIA apia = m_client.getAPIA();
        ObjectMethodsDef[] methods;

        methods = filterMethods(apia.listMethods(OBJECT_2_PID, null));

        assertEquals("Wrong number of methods!", 1, methods.length);
        assertEquals("Wrong method SDef!", SDEF_2_PID, methods[0]
                .getServiceDefinitionPID());
        assertEquals("Wrong method", "content2", methods[0].getMethodName());
    }

    @Test
    public void testListMethods1_2() throws Exception {
        FedoraAPIA apia = m_client.getAPIA();
        ObjectMethodsDef[] methods;

        methods = filterMethods(apia.listMethods(OBJECT_1_2_PID, null));
        assertEquals("Too many methods!", 2, methods.length);
        assertNotSame("SDefs are duplicated", methods[0]
                .getServiceDefinitionPID(), methods[1]
                .getServiceDefinitionPID());
        assertNotSame("methods are duplicated",
                      methods[0].getMethodName(),
                      methods[1].getMethodName());
    }

    @Test
    public void testListMethods3() throws Exception {
        FedoraAPIA apia = m_client.getAPIA();
        ObjectMethodsDef[] methods;

        methods = filterMethods(apia.listMethods(OBJECT_3_PID, null));
        assertEquals("Too many methods!", 2, methods.length);
        assertNotSame("SDefs are duplicated", methods[0]
                .getServiceDefinitionPID(), methods[1]
                .getServiceDefinitionPID());
        assertNotSame("methods are duplicated",
                      methods[0].getMethodName(),
                      methods[1].getMethodName());
    }

    @Test
    public void testListMethods4() throws Exception {
        FedoraAPIA apia = m_client.getAPIA();
        ObjectMethodsDef[] methods;

        methods = filterMethods(apia.listMethods(OBJECT_4_PID, null));
        assertEquals("Too many methods!", 3, methods.length);
        assertNotSame("SDefs are duplicated", methods[0]
                .getServiceDefinitionPID(), methods[1]
                .getServiceDefinitionPID());
        assertNotSame("SDefs are duplicated", methods[0]
                .getServiceDefinitionPID(), methods[2]
                .getServiceDefinitionPID());
        assertNotSame("SDefs are duplicated", methods[1]
                .getServiceDefinitionPID(), methods[2]
                .getServiceDefinitionPID());

        assertNotSame("methods are duplicated",
                      methods[0].getMethodName(),
                      methods[1].getMethodName());
        assertNotSame("methods are duplicated",
                      methods[0].getMethodName(),
                      methods[2].getMethodName());
        assertNotSame("methods are duplicated",
                      methods[1].getMethodName(),
                      methods[2].getMethodName());
    }

    @Ignore("Depends on fixing bug 201903")
    @Test
    public void testDissemination1() throws Exception {
        assertTrue("Wrong dissemination content",
                   getDissemination(OBJECT_1_PID, SDEF_1_PID, SDEF_1_METHOD)
                           .contains("CONTENT_1"));

        try {
            getDissemination(OBJECT_1_PID, SDEF_1_PID, SDEF_2_METHOD);
            fail("Was able to call wrong method on SDef1");
        } catch (AxisFault e) {
            /* Expected */
        }

        try {
            getDissemination(OBJECT_1_PID, SDEF_2_PID, SDEF_2_METHOD);
            fail("Was able use the wrong SDef!");
        } catch (AxisFault e) {
            /* Expected */
        }

        try {
            getDissemination(OBJECT_1_PID, SDEF_2_PID, SDEF_1_METHOD);
            fail("Was able use the wrong SDef and method!");
        } catch (AxisFault e) {
            /* Expected */
        }
    }

    @Ignore("Depends on fixing bug 201903")
    @Test
    public void testDissemination2() throws Exception {
        assertTrue("Wrong dissemination content",
                   getDissemination(OBJECT_2_PID, SDEF_2_PID, SDEF_2_METHOD)
                           .contains("CONTENT_2"));

        try {
            getDissemination(OBJECT_2_PID, SDEF_2_PID, SDEF_1_METHOD);
            fail("Was able to call wrong method on SDef2");
        } catch (AxisFault e) {
            /* Expected */
        }

        try {
            getDissemination(OBJECT_2_PID, SDEF_1_PID, SDEF_1_METHOD);
            fail("Was able use the wrong SDef!");
        } catch (AxisFault e) {
            /* Expected */
        }

        try {
            getDissemination(OBJECT_2_PID, SDEF_1_PID, SDEF_2_METHOD);
            fail("Was able use the wrong SDef and method!");
        } catch (AxisFault e) {
            /* Expected */
        }
    }

    @Ignore("Depends on fixing bug 201903")
    @Test
    public void testDissemination1_2() throws Exception {
        assertTrue("Wrong dissemination content",
                   getDissemination(OBJECT_1_2_PID, SDEF_1_PID, SDEF_1_METHOD)
                           .contains("CONTENT_1"));

        assertTrue("Wrong dissemination content",
                   getDissemination(OBJECT_1_2_PID, SDEF_2_PID, SDEF_2_METHOD)
                           .contains("CONTENT_2"));
        try {
            getDissemination(OBJECT_1_2_PID, SDEF_1_PID, SDEF_2_METHOD);
            fail("Was able to call wrong method on SDef1");
        } catch (AxisFault e) {
            /* Expected */
        }

        try {
            getDissemination(OBJECT_1_2_PID, SDEF_2_PID, SDEF_1_METHOD);
            fail("Was able use the wrong method on SDef2!");
        } catch (AxisFault e) {
            /* Expected */
        }
    }

    @Ignore("Depends on fixing bug 201903")
    @Test
    public void testDissemination3() throws Exception {
        assertTrue("Wrong dissemination content",
                   getDissemination(OBJECT_3_PID, SDEF_1_PID, SDEF_1_METHOD)
                           .contains("CONTENT_1"));

        assertTrue("Wrong dissemination content",
                   getDissemination(OBJECT_3_PID, SDEF_2_PID, SDEF_2_METHOD)
                           .contains("CONTENT_2"));
        try {
            getDissemination(OBJECT_3_PID, SDEF_1_PID, SDEF_2_METHOD);
            fail("Was able to call wrong method on SDef1");
        } catch (AxisFault e) {
            /* Expected */
        }

        try {
            getDissemination(OBJECT_3_PID, SDEF_2_PID, SDEF_1_METHOD);
            fail("Was able use the wrong method on SDef2!");
        } catch (AxisFault e) {
            /* Expected */
        }
    }

    @Ignore("Depends on fixing bug 201903")
    @Test
    public void testDissemination4() throws Exception {
        assertTrue("Wrong dissemination content",
                   getDissemination(OBJECT_4_PID, SDEF_1_PID, SDEF_1_METHOD)
                           .contains("CONTENT_1"));

        assertTrue("Wrong dissemination content",
                   getDissemination(OBJECT_4_PID, SDEF_2_PID, SDEF_2_METHOD)
                           .contains("CONTENT_2"));

        assertTrue("Wrong dissemination content",
                   getDissemination(OBJECT_4_PID, SDEF_4_PID, SDEF_4_METHOD)
                           .contains("CONTENT_4"));

        try {
            getDissemination(OBJECT_4_PID, SDEF_1_PID, SDEF_2_METHOD);
            fail("Was able to call wrong method on SDef1");
        } catch (AxisFault e) {
            /* Expected */
        }

        try {
            getDissemination(OBJECT_4_PID, SDEF_1_PID, SDEF_4_METHOD);
            fail("Was able to call wrong method on SDef1");
        } catch (AxisFault e) {
            /* Expected */
        }

        try {
            getDissemination(OBJECT_4_PID, SDEF_2_PID, SDEF_1_METHOD);
            fail("Was able use the wrong method on SDef2!");
        } catch (AxisFault e) {
            /* Expected */
        }

        try {
            getDissemination(OBJECT_4_PID, SDEF_2_PID, SDEF_4_METHOD);
            fail("Was able use the wrong method on SDef2!");
        } catch (AxisFault e) {
            /* Expected */
        }

        try {
            getDissemination(OBJECT_4_PID, SDEF_4_PID, SDEF_1_METHOD);
            fail("Was able use the wrong method on SDef4!");
        } catch (AxisFault e) {
            /* Expected */
        }

        try {
            getDissemination(OBJECT_4_PID, SDEF_4_PID, SDEF_2_METHOD);
            fail("Was able use the wrong method on SDef4!");
        } catch (AxisFault e) {
            /* Expected */
        }

    }

    private String getDissemination(String pid, String sDef, String method)
            throws Exception {
        return Util.getDissemination(m_client, pid, sDef, method);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        FedoraServerTestCase.purgeDemoObjects();
    }

}
