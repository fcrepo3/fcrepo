/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test.api;

import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;

import org.junit.After;

import org.w3c.dom.Document;

import junit.framework.Test;
import junit.framework.TestSuite;

import fedora.client.FedoraClient;

import fedora.server.access.FedoraAPIA;
import fedora.server.management.FedoraAPIM;

import fedora.test.FedoraServerTestCase;
import fedora.test.OneEmptyObjectTestSetup;

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

    private final static String TEST_PID = "demo:AuthNTestObject";

    private final static int TIMES_PER_TEST = 50;

    private static FedoraClient CLIENT_VALID_USER_VALID_PASS;

    private static FedoraClient CLIENT_VALID_USER_BOGUS_PASS;

    private static FedoraClient CLIENT_BOGUS_USER;

    public static Test suite() {
        TestSuite suite = new TestSuite("TestAuthentication TestSuite");
        suite.addTestSuite(TestAuthentication.class);
        return new OneEmptyObjectTestSetup(suite, TEST_PID);
    }

    //---
    // API-M SOAP Tests
    //---

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

    public void testAPIMLiteAuthNValidUserValidPass() throws Exception {
        int failCount = getNextPID(getClient(true, true), TIMES_PER_TEST);
        assertEquals("Getting next PID using valid user, valid pass failed "
                             + failCount + " times out of " + TIMES_PER_TEST
                             + " attempts",
                     0,
                     failCount);
    }

    public void testAPIMLiteAuthNValidUserBogusPass() throws Exception {
        int failCount = getNextPID(getClient(true, false), TIMES_PER_TEST);
        int successCount = TIMES_PER_TEST - failCount;
        assertEquals("Getting next PID using valid user, bogus pass succeeded "
                + successCount + " times out of " + TIMES_PER_TEST
                + " attempts", 0, successCount);
    }

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

    public void testAPIALiteAuthNValidUserValidPass() throws Exception {
        int failCount = getDCContent(getClient(true, true), TIMES_PER_TEST);
        assertEquals("Getting DC content using valid user, valid pass failed "
                             + failCount + " times out of " + TIMES_PER_TEST
                             + " attempts",
                     0,
                     failCount);
    }

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

    @Override
    public void setUp() throws Exception {
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
        XMLUnit.setXpathNamespaceContext(ctx);
    }

    @Override
    @After
    public void tearDown() {
        XMLUnit.setXpathNamespaceContext(SimpleNamespaceContext.EMPTY_CONTEXT);
    }

    private static FedoraClient getClient(boolean validUser, boolean validPass)
            throws Exception {
        if (validUser) {
            if (validPass) {
                System.out
                        .println("Using Fedora Client with valid user, valid pass");
                if (CLIENT_VALID_USER_VALID_PASS == null) {
                    CLIENT_VALID_USER_VALID_PASS = getFedoraClient();
                }
                return CLIENT_VALID_USER_VALID_PASS;
            } else {
                System.out
                        .println("Using Fedora Client with valid user, bogus pass");
                if (CLIENT_VALID_USER_BOGUS_PASS == null) {
                    CLIENT_VALID_USER_BOGUS_PASS =
                            getFedoraClient(getBaseURL(),
                                            getUsername(),
                                            "bogus");
                }
                return CLIENT_VALID_USER_BOGUS_PASS;
            }
        } else {
            System.out.println("Using Fedora Client with bogus user");
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
        System.out.println("Modifying object label via API-M SOAP " + numTimes
                + " times...");
        int failCount = 0;
        FedoraAPIM apim = null;
        for (int i = 0; i < numTimes; i++) {
            try {
                if (apim == null) {
                    apim = client.getAPIM();
                }
                apim.modifyObject(TEST_PID, null, null, null, "i=" + i);
            } catch (Exception e) {
                failCount++;
            }
        }
        System.out.println("Failed " + failCount + " times");
        return failCount;
    }

    // returns failCount
    private int getNextPID(FedoraClient client, int numTimes) {
        System.out.println("Getting next PID via API-M Lite " + numTimes
                + " times...");
        int failCount = 0;
        for (int i = 0; i < numTimes; i++) {
            try {
                Document result =
                        getXMLQueryResult(client,
                                          "/management/getNextPID?xml=true");
                assertXpathEvaluatesTo("1", "count(/pidList/pid)", result);
            } catch (Exception e) {
                failCount++;
            }
        }
        System.out.println("Failed " + failCount + " times");
        return failCount;
    }

    // returns failCount
    private int listDatastreams(FedoraClient client, int numTimes) {
        System.out.println("Listing object datastreams via API-A SOAP "
                + numTimes + " times...");
        int failCount = 0;
        FedoraAPIA apia = null;
        for (int i = 0; i < numTimes; i++) {
            try {
                if (apia == null) {
                    apia = client.getAPIA();
                }
                apia.listDatastreams(TEST_PID, null);
            } catch (Exception e) {
                failCount++;
            }
        }
        System.out.println("Failed " + failCount + " times");
        return failCount;
    }

    // returns failCount
    private int getDCContent(FedoraClient client, int numTimes) {
        System.out.println("Getting DC content via API-A Lite " + numTimes
                + " times...");
        int failCount = 0;
        for (int i = 0; i < numTimes; i++) {
            try {
                Document result =
                        getXMLQueryResult(client, "/get/" + TEST_PID + "/DC");
                assertXpathExists("/oai_dc:dc", result);
            } catch (Exception e) {
                failCount++;
            }
        }
        System.out.println("Failed " + failCount + " times");
        return failCount;
    }

    //---
    // Command-line entry point
    //---

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestAuthentication.class);
    }

}
