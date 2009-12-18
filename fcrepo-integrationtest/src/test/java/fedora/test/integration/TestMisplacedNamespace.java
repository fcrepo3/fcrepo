/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test.integration;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fedora.client.FedoraClient;

import fedora.server.utilities.StreamUtility;

import fedora.test.FedoraTestCase;
import fedora.test.api.RISearchUtil;

/**
 * Tests live behaviour when ingesting malformed foxml.
 * <p>
 * This test is to verify repository sanity when ingesting a foxml file with an
 * inline datastream containing an element such that the namespace of the
 * element is defined outside the scope of the inline xmlContent. The repository
 * should reject the file upon ingest without any irreperable harm to the object
 * registry.
 * </p>
 *
 * @author Aaron Birkland
 * @version $Id$
 * @see <a
 *      href="https://fedora-commons.org/jira/browse/FCREPO-537">FCREPO-537</a>
 */
public class TestMisplacedNamespace
        extends FedoraTestCase {

    private FedoraClient m_client;

    private static final String OFFENDING_FOXML =
            "test-objects/foxml/TestMisplacedNamespace/DemoFail.xml";

    private static final String GOOD_FOXML =
            "test-objects/foxml/TestMisplacedNamespace/DemoSuccess.xml";

    private static final String PID = "demo:failObject";

    @Override
    @Before
    public void setUp() throws Exception {
        m_client = getFedoraClient();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        m_client.getAPIM().purgeObject(PID, "Cleanup", false);
    }

    @Test
    public void testIngestAndPurge() throws Exception {
        try {
            /* Ingest of the offending foxml should fail */
            m_client.getAPIM().ingest(getFoxml(OFFENDING_FOXML),
                                      FOXML1_1.uri,
                                      "malformed foxml object");
            fail("Sould have failed initial ingest!");
        } catch (Exception e) {
            /* Make sure the RI contains no trace of this object */
            RISearchUtil.checkSPOCount(m_client, "<info:fedora/" + PID
                    + "> * *", 0);
        }

        /* Ingest the good object. Should succeed */
        m_client.getAPIM().ingest(getFoxml(GOOD_FOXML),
                                  FOXML1_1.uri,
                                  "non-malformed foxml object");
    }

    private byte[] getFoxml(String file) throws IOException {
        return StreamUtility.getBytes(getClass().getClassLoader()
                .getResourceAsStream(file));
    }
}
