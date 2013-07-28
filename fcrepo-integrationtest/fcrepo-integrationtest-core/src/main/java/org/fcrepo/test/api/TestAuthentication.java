/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.api;

import static junit.framework.Assert.assertEquals;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.fcrepo.test.OneEmptyObjectTestSetup.ingestOneEmptyObject;
import static org.fcrepo.test.OneEmptyObjectTestSetup.purgeOneEmptyObject;

import java.util.HashMap;
import java.util.Map;

import junit.framework.JUnit4TestAdapter;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import org.fcrepo.client.FedoraClient;

import org.fcrepo.server.access.FedoraAPIAMTOM;
import org.fcrepo.server.management.FedoraAPIMMTOM;

import org.fcrepo.test.FedoraServerTestCase;




/**
 * Tests correct/incorrect authentication against API-A/Lite and API-M/Lite.
 * <p>
 * These tests use basic authentication. To exercise the running server
 * properly, it should be configured to require basic authentication on all
 * interfaces.
 * <p>
 * By default, the tests will run against the "default" Fedora base URL, but
 * this can be overridden by setting the "fedora.baseURL" system property.
 *
 * @author Chris Wilper
 */
public class TestAuthentication
        extends FedoraServerTestCase {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(TestAuthentication.class);
    
    private final static String TEST_PID = "demo:AuthNTestObject";

    private final static int TIMES_PER_TEST = 50;

    private static FedoraClient CLIENT_VALID_USER_VALID_PASS;

    private static FedoraClient CLIENT_VALID_USER_BOGUS_PASS;

    private static FedoraClient CLIENT_BOGUS_USER;

    //---
    // API-M SOAP Tests
    //---

    @Test
    public void testAPIMSOAPAuthNValidUserValidPass() throws Exception {
        int failCount = modifyLabel(getClient(true, true), TIMES_PER_TEST);
        assertEquals("Modifying object label using valid user, valid pass failed "
                             + failCount
                             + " times out of "
                             + TIMES_PER_TEST
                             + " attempts",
                     0,
                     failCount);
    }

    @Test
    public void testAPIMSOAPAuthNValidUserBogusPass() throws Exception {
        int failCount = modifyLabel(getClient(true, false), TIMES_PER_TEST);
        int successCount = TIMES_PER_TEST - failCount;
        assertEquals("Modifying object label using valid user, bogus pass succeeded "
                             + successCount
                             + " times out of "
                             + TIMES_PER_TEST
                             + " attempts",
                     0,
                     successCount);
    }

    @Test
    public void testAPIMSOAPAuthNBogusUser() throws Exception {
        int failCount = modifyLabel(getClient(false, false), TIMES_PER_TEST);
        int successCount = TIMES_PER_TEST - failCount;
        assertEquals("Modifying object label using bogus user, bogus pass succeeded "
                             + successCount
                             + " times out of "
                             + TIMES_PER_TEST
                             + " attempts",
                     0,
                     successCount);
    }

    //---
    // API-M Lite Tests
    //---

    @Test
    public void testAPIMLiteAuthNValidUserValidPass() throws Exception {
        int failCount = getNextPID(getClient(true, true), TIMES_PER_TEST);
        assertEquals("Getting next PID using valid user, valid pass failed "
                             + failCount + " times out of " + TIMES_PER_TEST
                             + " attempts",
                     0,
                     failCount);
    }

    @Test
    public void testAPIMLiteAuthNValidUserBogusPass() throws Exception {
        int failCount = getNextPID(getClient(true, false), TIMES_PER_TEST);
        int successCount = TIMES_PER_TEST - failCount;
        assertEquals("Getting next PID using valid user, bogus pass succeeded "
                + successCount + " times out of " + TIMES_PER_TEST
                + " attempts", 0, successCount);
    }

    @Test
    public void testAPIMLiteAuthNBogusUser() throws Exception {
        int failCount = getNextPID(getClient(false, false), TIMES_PER_TEST);
        int successCount = TIMES_PER_TEST - failCount;
        assertEquals("Getting next PID using bogus user, bogus pass succeeded "
                + successCount + " times out of " + TIMES_PER_TEST
                + " attempts", 0, successCount);
    }

    //---
    // API-A SOAP Tests
    //---

    @Test
    public void testAPIASOAPAuthNValidUserValidPass() throws Exception {
        int failCount = listDatastreams(getClient(true, true), TIMES_PER_TEST);
        assertEquals("Listing object datastreams using valid user, valid pass failed "
                             + failCount
                             + " times out of "
                             + TIMES_PER_TEST
                             + " attempts",
                     0,
                     failCount);
    }

    @Test
    public void testAPIASOAPAuthNValidUserBogusPass() throws Exception {
        int failCount = listDatastreams(getClient(true, false), TIMES_PER_TEST);
        int successCount = TIMES_PER_TEST - failCount;
        assertEquals("Listing object datastreams using valid user, bogus pass succeeded "
                             + successCount
                             + " times out of "
                             + TIMES_PER_TEST
                             + " attempts",
                     0,
                     successCount);
    }

    @Test
    public void testAPIASOAPAuthNBogusUser() throws Exception {
        int failCount =
                listDatastreams(getClient(false, false), TIMES_PER_TEST);
        int successCount = TIMES_PER_TEST - failCount;
        assertEquals("Listing object datastreams using bogus user, bogus pass succeeded "
                             + successCount
                             + " times out of "
                             + TIMES_PER_TEST
                             + " attempts",
                     0,
                     successCount);
    }

    //---
    // API-A Lite Tests
    //---

    @Test
    public void testAPIALiteAuthNValidUserValidPass() throws Exception {
        int failCount = getDCContent(getClient(true, true), TIMES_PER_TEST);
        assertEquals("Getting DC content using valid user, valid pass failed "
                             + failCount + " times out of " + TIMES_PER_TEST
                             + " attempts",
                     0,
                     failCount);
    }

    @Test
    public void testAPIALiteAuthNValidUserBogusPass() throws Exception {
        int failCount = getDCContent(getClient(true, false), TIMES_PER_TEST);
        int successCount = TIMES_PER_TEST - failCount;
        assertEquals("Getting DC content using valid user, bogus pass succeeded "
                             + successCount
                             + " times out of "
                             + TIMES_PER_TEST
                             + " attempts",
                     0,
                     successCount);
    }

    @Test
    public void testAPIALiteAuthNBogusUser() throws Exception {
        int failCount = getDCContent(getClient(false, false), TIMES_PER_TEST);
        int successCount = TIMES_PER_TEST - failCount;
        assertEquals("Getting DC content using bogus user, bogus pass succeeded "
                             + successCount
                             + " times out of "
                             + TIMES_PER_TEST
                             + " attempts",
                     0,
                     successCount);
    }

    //---
    // Static helpers
    //---

    @Before
    public void setUp() throws Exception {
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("management", "http://www.fedora.info/definitions/1/0/management/");
        nsMap.put("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
        XMLUnit.setXpathNamespaceContext(ctx);
    }
    
    @BeforeClass
    public static void bootstrap() throws Exception {
        CLIENT_VALID_USER_VALID_PASS = getFedoraClient();
        ingestOneEmptyObject(CLIENT_VALID_USER_VALID_PASS, TEST_PID);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        XMLUnit.setXpathNamespaceContext(SimpleNamespaceContext.EMPTY_CONTEXT);
        purgeOneEmptyObject(CLIENT_VALID_USER_VALID_PASS, TEST_PID);
        if (CLIENT_VALID_USER_VALID_PASS != null) {
            CLIENT_VALID_USER_VALID_PASS.shutdown();
        }
        if (CLIENT_VALID_USER_BOGUS_PASS != null) {
            CLIENT_VALID_USER_BOGUS_PASS.shutdown();
        }
        if (CLIENT_BOGUS_USER != null) {
            CLIENT_BOGUS_USER.shutdown();
        }
    }

    private FedoraClient getClient(boolean validUser, boolean validPass)
            throws Exception {
        if (validUser) {
            if (validPass) {
                LOGGER.info("Using Fedora Client with valid user, valid pass");
                if (CLIENT_VALID_USER_VALID_PASS == null) {
                    CLIENT_VALID_USER_VALID_PASS = getFedoraClient();
                }
                return CLIENT_VALID_USER_VALID_PASS;
            } else {
                LOGGER.info("Using Fedora Client with valid user, bogus pass");
                if (CLIENT_VALID_USER_BOGUS_PASS == null) {
                    CLIENT_VALID_USER_BOGUS_PASS =
                            getFedoraClient(getBaseURL(),
                                            getUsername(),
                                            "bogus");
                }
                return CLIENT_VALID_USER_BOGUS_PASS;
            }
        } else {
            LOGGER.info("Using Fedora Client with bogus user");
            if (CLIENT_BOGUS_USER == null) {
                CLIENT_BOGUS_USER =
                        getFedoraClient(getBaseURL(), "bogus", "bogus");
            }
            return CLIENT_BOGUS_USER;
        }
    }

    //---
    // Instance helpers
    //---

    // returns failCount
    private int modifyLabel(FedoraClient client, int numTimes) {
        LOGGER.info("Modifying object label via API-M SOAP {} times...",
                numTimes);
        int failCount = 0;
        FedoraAPIMMTOM apim = null;
        for (int i = 0; i < numTimes; i++) {
            try {
                if (apim == null) {
                    apim = client.getAPIMMTOM();
                }
                apim.modifyObject(TEST_PID, null, null, null, "i=" + i);
            } catch (Exception e) {
                failCount++;
            }
        }
        LOGGER.info("Failed {} times", failCount);
        return failCount;
    }

    // returns failCount
    private int getNextPID(FedoraClient client, int numTimes) {
        LOGGER.info("Getting next PID via API-M Lite {} times...", numTimes);
        int failCount = 0;
        for (int i = 0; i < numTimes; i++) {
            try {
                Document result =
                        getXMLQueryResult(client,
                                          "/management/getNextPID?xml=true");
                assertXpathEvaluatesTo("1", "count(//management:pid)", result);
            } catch (Exception e) {
                failCount++;
            }
        }
        LOGGER.info("Failed {} times", failCount);
        return failCount;
    }

    // returns failCount
    private int listDatastreams(FedoraClient client, int numTimes) {
        LOGGER.info("Listing object datastreams via API-A SOAP {} times...", numTimes);
        int failCount = 0;
        FedoraAPIAMTOM apia = null;
        for (int i = 0; i < numTimes; i++) {
            try {
                if (apia == null) {
                    apia = client.getAPIAMTOM();
                }
                apia.listDatastreams(TEST_PID, null);
            } catch (Exception e) {
                failCount++;
            }
        }
        LOGGER.info("Failed {} times", failCount);
        return failCount;
    }

    // returns failCount
    private int getDCContent(FedoraClient client, int numTimes) {
        LOGGER.debug("Getting DC content via API-A Lite {} times...", numTimes);
        int failCount = 0;
        for (int i = 0; i < numTimes; i++) {
            try {
                Document result =
                        getXMLQueryResult(client, "/get/" + TEST_PID + "/DC");
                assertXpathExists("/oai_dc:dc", result);
            } catch (Exception e) {
                LOGGER.info(e.getMessage());

                failCount++;
            }
        }
        LOGGER.debug("Failed {} times", failCount);
        return failCount;
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestAuthentication.class);
    }

    public static void main(String[] args) {
        JUnitCore.runClasses(TestAuthentication.class);
    }

}
