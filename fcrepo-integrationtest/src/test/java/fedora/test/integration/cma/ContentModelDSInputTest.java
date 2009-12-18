/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test.integration.cma;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fedora.client.FedoraClient;

import fedora.server.access.FedoraAPIA;
import fedora.server.types.gen.ObjectMethodsDef;

import fedora.test.FedoraServerTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import static fedora.test.integration.cma.Util.filterMethods;

/**
 * Tests disseminations where the sDep's DSInputSpec requires a datastream
 * from the content model object.
 *
 * @author Edwin Shin
 * @since 3.1
 * @version $Id$
 */
public class ContentModelDSInputTest {

    private static FedoraClient m_client;

    private static final String OBJECT_PID = "demo:dc2mods.1";

    private static final String SDEF_PID = "demo:dc2mods.sdef";

    private static final String SDEF_METHOD = "transform";

    private static final String DC2MODS_DEPLOYMENT_BASE = "cma-examples/dc2mods";

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(ContentModelDSInputTest.class);
    }

    @BeforeClass
    public static void bootstrap() throws Exception {

        m_client =
                new FedoraClient(FedoraServerTestCase.getBaseURL(),
                                 FedoraServerTestCase.getUsername(),
                                 FedoraServerTestCase.getPassword());
        Util.ingestTestObjects(DC2MODS_DEPLOYMENT_BASE);
    }

    /* Assure that listMethods works as advertised */
    @Test
    public void testListMethods() throws Exception {
        FedoraAPIA apia = m_client.getAPIA();
        ObjectMethodsDef[] methods;

        methods = filterMethods(apia.listMethods(OBJECT_PID, null));

        assertEquals("Wrong number of methods", 1, methods.length);
        assertEquals(methods[0].getServiceDefinitionPID(), SDEF_PID);
        assertEquals(methods[0].getMethodName(), SDEF_METHOD);
    }

    /* Assure that disseminations return expected content */
    @Test
    public void testDissemination() throws Exception {
        // TODO use XMLUnit to validate content
        assertTrue("Wrong dissemination content",
                   getDissemination(OBJECT_PID, SDEF_PID, SDEF_METHOD)
                           .contains("<mods xmlns="));
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
