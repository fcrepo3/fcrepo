/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import melcoe.xacml.pdp.data.DbXmlPolicyDataManager;
import melcoe.xacml.pdp.data.PolicyDataManager;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.w3c.dom.Document;

import junit.framework.Test;
import junit.framework.TestSuite;

import fedora.client.FedoraClient;
import fedora.client.HttpInputStream;

import fedora.server.security.servletfilters.xmluserfile.FedoraUsers;

import fedora.test.DemoObjectTestSetup;
import fedora.test.FedoraServerTestCase;
import fedora.test.fesl.util.DataUtils;

/**
 * Common tests for correct/incorrect http status codes with api requests over
 * API-A/API-M Lite. For non-200 requests, this also tests the response body for
 * the string "Fedora: # " (where # is the status code) to ensure that the
 * correct jsp has been delivered.
 *
 * @author Chris Wilper
 */
public class TestHTTPStatusCodesConfigC
        extends FedoraServerTestCase {

    public static final String TEST_OBJ = "demo:SmileyBucket";

    public static final String BOGUS_DS = "NonExistingDS";

    public static final String BOGUS_METHOD = "nonExistingMethod";

    public static final String BOGUS_OBJ = "demo:NonExistingObject";

    public static final String BOGUS_SDEF = "demo:NonExistingSDef";

    public static final String GET_NEXT_PID_PATH =
            "/management/getNextPID?xml=true";

    public static final String DESCRIBE_REPOSITORY_PATH = "/describe?xml=true";

    public static final String GET_DS_DISSEM_PATH = "/get/" + TEST_OBJ + "/DC";

    public static final String GET_DS_DISSEM_BOGUS_DS_PATH =
            "/get/" + TEST_OBJ + "/" + BOGUS_DS;

    public static final String GET_DS_DISSEM_BOGUS_OBJ_PATH =
            "/get/" + BOGUS_OBJ + "/DC";

    public static final String GET_DEFAULT_DISSEM_PATH =
            "/get/" + TEST_OBJ + "/fedora-system:3/viewDublinCore";

    public static final String GET_DEFAULT_DISSEM_BOGUS_METHOD_PATH =
            "/get/" + TEST_OBJ + "/fedora-system:3/" + BOGUS_METHOD;

    public static final String GET_DEFAULT_DISSEM_BOGUS_OBJ_PATH =
            "/get/" + BOGUS_OBJ + "/fedora-system:3/viewDublinCore";

    public static final String GET_CUSTOM_DISSEM_PATH =
            "/get/" + TEST_OBJ + "/demo:DualResolution/mediumSize";

    public static final String GET_CUSTOM_DISSEM_BOGUS_METHOD_PATH =
            "/get/" + TEST_OBJ + "/demo:DualResolution/" + BOGUS_METHOD;

    public static final String GET_CUSTOM_DISSEM_BOGUS_SDEF_PATH =
            "/get/" + TEST_OBJ + "/" + BOGUS_SDEF + "/" + BOGUS_METHOD;

    public static final String GET_CUSTOM_DISSEM_BOGUS_OBJ_PATH =
            "/get/" + BOGUS_OBJ + "/demo:DualResolution/mediumSize";

    public static final String GET_OBJ_HISTORY_PATH =
            "/getObjectHistory/" + TEST_OBJ + "?xml=true";

    public static final String GET_OBJ_HISTORY_BOGUS_OBJ_PATH =
            "/getObjectHistory/" + BOGUS_OBJ + "?xml=true";

    public static final String GET_OBJ_PROFILE_PATH =
            "/get/" + TEST_OBJ + "?xml=true";

    public static final String GET_OBJ_PROFILE_BOGUS_OBJ_PATH =
            "/get/" + BOGUS_OBJ + "?xml=true";

    public static final String LIST_DATASTREAMS_PATH =
            "/listDatastreams/" + TEST_OBJ + "?xml=true";

    public static final String LIST_DATASTREAMS_BOGUS_OBJ_PATH =
            "/listDatastreams/" + BOGUS_OBJ + "?xml=true";

    public static final String LIST_METHODS_PATH =
            "/listMethods/" + TEST_OBJ + "?xml=true";

    public static final String LIST_METHODS_BOGUS_OBJ_PATH =
            "/listMethods/" + BOGUS_OBJ + "?xml=true";

    public static final String FIND_OBJECTS_PATH =
            "/search?pid=true&terms=&query=&maxResults=120&xml=true";

    public static final String FIND_OBJECTS_BADREQ_PATH =
            "/search?pid=true&terms=&query=&maxResults=unparsable&xml=true";

    public static final String RI_SEARCH_PATH =
            "/risearch?type=triples&lang=spo&format=N-Triples&limit=&dt=on&stream=on&query=%3Cinfo%3Afedora%2Fdemo%3ASmileyStuff%3E+*+*";

    private static FedoraClient CLIENT_VALID_USER_VALID_PASS;

    private static FedoraClient CLIENT_VALID_USER_VALID_PASS_UNAUTHORIZED;

    private static FedoraClient CLIENT_VALID_USER_BOGUS_PASS;

    private static FedoraClient CLIENT_BOGUS_USER;

    //---
    // Test suite setup
    //---

    public static Test suite() {
        TestSuite suite = new TestSuite("TestHTTPStatusCodes TestSuite");
        suite.addTestSuite(TestHTTPStatusCodesConfigC.class);
        return new DemoObjectTestSetup(suite);
    }

    //---
    // Test utility methods
    //---

    public static void checkOK(String requestPath) throws Exception {
        checkGetCode(getClient(true, true, true),
                     requestPath,
                     "Expected HTTP 200 (OK) response for authenticated, "
                             + "authorized request",
                     200);
    }

    public static void checkError(String requestPath) throws Exception {
        checkGetCode(getClient(true, true, true),
                     requestPath,
                     "Expected HTTP 500 (Internal Server Error) response for "
                             + "authenticated, authorized request",
                     500);
    }

    public static void checkBadAuthN(String requestPath) throws Exception {
        checkGetCode(getClient(true, false, true),
                     requestPath,
                     "Expected HTTP 401 (Unauthorized) response for bad "
                             + "authentication (valid user, bad pass) request",
                     401);
        checkGetCode(getClient(false, false, true),
                     requestPath,
                     "Expected HTTP 401 (Unauthorized) response for bad "
                             + "authentication (invalid user) request",
                     401);
    }

    public static void checkBadAuthZ(String requestPath) throws Exception {
        try {
            activateUnauthorizedUserAndPolicy();
            checkGetCode(getClient(true, true, false),
                         requestPath,
                         "Expected HTTP 403 (Forbidden) response for "
                                 + "authenticated, unauthorized request",
                         403);
        } finally {
            deactivateUnauthorizedUserAndPolicy();
        }
    }

    public static void checkNotFound(String requestPath) throws Exception {
        checkGetCode(getClient(true, true, true),
                     requestPath,
                     "Expected HTTP 404 (Not Found) response for authenticated, "
                             + "authorized request",
                     404);
    }

    public static void checkBadRequest(String requestPath) throws Exception {
        checkGetCode(getClient(true, true, true),
                     requestPath,
                     "Expected HTTP 400 (Bad Request) response for authenticated, "
                             + "authorized request",
                     400);
    }

    //---
    // API-M Lite: getNextPID
    //---

    public void testGetNextPID_OK() throws Exception {
        checkOK(GET_NEXT_PID_PATH);
    }

    public void testGetNextPID_BadAuthN() throws Exception {
        checkBadAuthN(GET_NEXT_PID_PATH);
    }
    public void testGetNextPID_BadAuthZ() throws Exception {
        checkBadAuthZ(GET_NEXT_PID_PATH);
    }

    //---
    // API-M Lite: upload
    //---

    public void testUpload_Created() throws Exception {
        checkUploadCode(getClient(true, true, true),
                        "file",
                        "Expected HTTP 201 (Created) response for authenticated, "
                                + "authorized request",
                        201);
    }

    public void testUpload_BadAuthN() throws Exception {
        checkUploadCode(getClient(true, false, true),
                        "file",
                        "Expected HTTP 401 (Unauthorized) response for bad "
                                + "authentication (valid user, bad pass) request",
                        401);
        checkUploadCode(getClient(false, false, true),
                        "file",
                        "Expected HTTP 401 (Unauthorized) response for bad "
                                + "authentication (invalid user) request",
                        401);
    }

    public void testUpload_BadRequest() throws Exception {
        checkUploadCode(getClient(true, true, true),
                        "badparam",
                        "Expected HTTP 400 (Bad Request) response for authenticated, "
                                + "authorized request",
                        400);
    }

    //---
    // API-A Lite: describeRepository
    //---

    public void testDescribeRepository_OK() throws Exception {
        checkOK(DESCRIBE_REPOSITORY_PATH);
    }

    //---
    // API-A Lite: getDatastreamDissemination
    //---

    public void testGetDatastreamDissemination_OK() throws Exception {
        checkOK(GET_DS_DISSEM_PATH);
    }

    public void testGetDatastreamDissemination_Datastream_NotFound()
            throws Exception {
        checkNotFound(GET_DS_DISSEM_BOGUS_DS_PATH);
    }

    public void testGetDatastreamDissemination_Object_NotFound()
            throws Exception {
        checkNotFound(GET_DS_DISSEM_BOGUS_OBJ_PATH);
    }

    //---
    // API-A Lite: getDissemination (default)
    //---

    public void testGetDissemination_Default_OK() throws Exception {
        checkOK(GET_DEFAULT_DISSEM_PATH);
    }

    public void testGetDissemination_Default_Method_NotFound() throws Exception {
        checkNotFound(GET_DEFAULT_DISSEM_BOGUS_METHOD_PATH);
    }

    public void testGetDissemination_Default_Object_NotFound() throws Exception {
        checkNotFound(GET_DEFAULT_DISSEM_BOGUS_OBJ_PATH);
    }

    //---
    // API-A Lite: getDissemination (custom)
    //---

    public void testGetDissemination_Custom_OK() throws Exception {
        checkOK(GET_CUSTOM_DISSEM_PATH);
    }

    public void testGetDissemination_Custom_Method_NotFound() throws Exception {
        checkNotFound(GET_CUSTOM_DISSEM_BOGUS_METHOD_PATH);
    }

    public void testGetDissemination_Custom_Object_NotFound() throws Exception {
        checkNotFound(GET_CUSTOM_DISSEM_BOGUS_OBJ_PATH);
    }

    //---
    // API-A Lite: getObjectHistory
    //---

    public void testGetObjectHistory_OK() throws Exception {
        checkOK(GET_OBJ_HISTORY_PATH);
    }

    public void testGetObjectHistory_Object_NotFound() throws Exception {
        checkNotFound(GET_OBJ_HISTORY_BOGUS_OBJ_PATH);
    }

    //---
    // API-A Lite: getObjectProfile
    //---

    public void testGetObjectProfile_OK() throws Exception {
        checkOK(GET_OBJ_PROFILE_PATH);
    }

    public void testGetObjectProfile_Object_NotFound() throws Exception {
        checkNotFound(GET_OBJ_PROFILE_BOGUS_OBJ_PATH);
    }

    //---
    // API-A Lite: listDatastreams
    //---

    public void testListDatastreams_OK() throws Exception {
        checkOK(LIST_DATASTREAMS_PATH);
    }

    public void testListDatastreams_Object_NotFound() throws Exception {
        checkNotFound(LIST_DATASTREAMS_BOGUS_OBJ_PATH);
    }

    //---
    // API-A Lite: listMethods
    //---

    public void testListMethods_OK() throws Exception {
        checkOK(LIST_METHODS_PATH);
    }

    public void testListMethods_Object_NotFound() throws Exception {
        checkNotFound(LIST_METHODS_BOGUS_OBJ_PATH);
    }

    //---
    // API-A Lite: findObjects
    //---
    public void testFindObjects_OK() throws Exception {
        checkOK(FIND_OBJECTS_PATH);
    }

    public void testFindObjects_BadRequest() throws Exception {
        checkBadRequest(FIND_OBJECTS_BADREQ_PATH);
    }

    //---
    // Static helpers
    //---

    private static int getStatus(FedoraClient client, String requestPath)
            throws Exception {
        HttpInputStream in = client.get(requestPath, false);
        try {
            return in.getStatusCode();
        } finally {
            in.close();
        }
    }

    private static FedoraClient getClient(boolean validUser,
                                          boolean validPass,
                                          boolean authorized) throws Exception {
        if (validUser) {
            if (validPass) {
                System.out
                        .println("Using Fedora Client with valid user, valid pass");
                if (authorized) {
                    if (CLIENT_VALID_USER_VALID_PASS == null) {
                        CLIENT_VALID_USER_VALID_PASS = getFedoraClient();
                    }
                    return CLIENT_VALID_USER_VALID_PASS;
                } else {
                    if (CLIENT_VALID_USER_VALID_PASS_UNAUTHORIZED == null) {
                        CLIENT_VALID_USER_VALID_PASS_UNAUTHORIZED =
                                getFedoraClient(getBaseURL(),
                                                "untrustedUser",
                                                "password");
                    }
                    return CLIENT_VALID_USER_VALID_PASS_UNAUTHORIZED;
                }
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

    private static void activateUnauthorizedUserAndPolicy() throws Exception {
        backupFedoraUsersFile();
        writeFedoraUsersFile("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<fedora-users>\n" + "  <user name=\"" + getUsername()
                + "\" password=\"" + getPassword() + "\">\n"
                + "    <attribute name=\"fedoraRole\">\n"
                + "      <value>administrator</value>\n" + "    </attribute>\n"
                + "  </user>\n"
                + "  <user name=\"fedoraIntCallUser\" password=\"changeme\">\n"
                + "    <attribute name=\"fedoraRole\">\n"
                + "      <value>fedoraInternalCall-1</value>\n"
                + "      <value>fedoraInternalCall-2</value>\n"
                + "    </attribute>\n" + "  </user>\n"
                + "  <user name=\"untrustedUser\" password=\"password\">\n"
                + "    <attribute name=\"fedoraRole\">\n"
                + "      <value>unauthorized</value>\n" + "    </attribute>\n"
                + "  </user>\n" + "</fedora-users>");
        addSystemWidePolicyFile("deny-all-if-unauthorized.xml",
                                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                        + "<Policy xmlns=\"urn:oasis:names:tc:xacml:1.0:policy\"\n"
                                        + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                                        + "    PolicyId=\"deny-all-if-unauthorized\""
                                        + "    RuleCombiningAlgId=\"urn:oasis:names:tc:xacml:1.0:rule-combining-algorithm:first-applicable\">\n"
                                        + "  <Description>deny all api-a and api-m access if subject has fedoraRole unauthorized</Description>\n"
                                        + "  <Target>\n"
                                        + "    <Subjects>\n"
                                        + "      <AnySubject/>\n"
                                        + "    </Subjects>\n"
                                        + "    <Resources>\n"
                                        + "      <AnyResource/>\n"
                                        + "    </Resources>\n"
                                        + "    <Actions>\n"
                                        + "      <AnyAction/>\n"
                                        + "    </Actions>\n"
                                        + "  </Target>\n"
                                        + "  <Rule RuleId=\"1\" Effect=\"Deny\">\n"
                                        + "    <Condition FunctionId=\"urn:oasis:names:tc:xacml:1.0:function:string-is-in\">\n"
                                        + "      <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">unauthorized</AttributeValue>\n"
                                        + "      <SubjectAttributeDesignator AttributeId=\"fedoraRole\" DataType=\"http://www.w3.org/2001/XMLSchema#string\"/>\n"
                                        + "    </Condition>\n" + "  </Rule>\n"
                                        + "</Policy>");
        reloadPolicies();
    }

    private static void deactivateUnauthorizedUserAndPolicy() throws Exception {
        restoreFedoraUsersFile();
        removeSystemWidePolicyFile("deny-all-if-unauthorized.xml");
        reloadPolicies();
    }

    private static void backupFedoraUsersFile() throws Exception {
        File sourceFile = FedoraUsers.fedoraUsersXML;
        File destFile =
                new File(FedoraUsers.fedoraUsersXML.getPath() + ".backup");
        copyFile(sourceFile, destFile);
    }

    private static void copyFile(File sourceFile, File destFile)
            throws Exception {
        FileInputStream in = new FileInputStream(sourceFile);
        FileOutputStream out = new FileOutputStream(destFile);
        byte[] buf = new byte[4096];
        int len;
        try {
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            in.close();
            out.close();
        }
    }

    private static void writeFedoraUsersFile(String xml) throws Exception {
        writeStringToFile(xml, FedoraUsers.fedoraUsersXML);
    }

    private static void writeStringToFile(String string, File file)
            throws Exception {
        FileOutputStream out = new FileOutputStream(file);
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        try {
            writer.print(string);
        } finally {
            writer.close();
        }
    }

    private static void restoreFedoraUsersFile() throws Exception {
        File sourceFile =
                new File(FedoraUsers.fedoraUsersXML.getPath() + ".backup");
        File destFile = FedoraUsers.fedoraUsersXML;
        copyFile(sourceFile, destFile);
    }

    private static void addSystemWidePolicyFile(String filename, String xml)
            throws Exception {
        final String policyDir =
                "data/fedora-xacml-policies/repository-policies/junit";
        File dir = new File(FEDORA_HOME, policyDir);
        dir.mkdir();
        File policyFile = new File(dir, filename);
        writeStringToFile(xml, policyFile);

        // for new policy store...
        addPolicy(xml);
    }

    private static void removeSystemWidePolicyFile(String filename)
            throws Exception {
    	final String policyDir =
                "data/fedora-xacml-policies/repository-policies/junit";
        File dir = new File(FEDORA_HOME, policyDir);
        File policyFile = new File(dir, filename);

        // new policy store thing. capture the file...
        byte[] xml = DataUtils.loadFile(policyFile);

        policyFile.delete();
        dir.delete(); // succeeds if empty

        // new policy store thing - get policyId and delete it.
        String policyId = getPolicyId(xml);
        delPolicy(policyId);
    }

    private static void reloadPolicies() throws Exception {
        getClient(true, true, true).reloadPolicies();
    }

    private static void checkGetCode(FedoraClient client,
                                     String requestPath,
                                     String errorMessage,
                                     int expectedCode) throws Exception {
        HttpInputStream in = client.get(requestPath, false);
        try {
            int gotCode = in.getStatusCode();
            assertEquals(errorMessage + " (" + requestPath + ")",
                         expectedCode,
                         gotCode);
            if (expectedCode != 200) {
                String expectedString = "Fedora: " + expectedCode + " ";
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(in));
                boolean foundExpectedString = false;
                String line = reader.readLine();
                while (line != null) {
                    if (line.indexOf(expectedString) != -1) {
                        foundExpectedString = true;
                    }
                    line = reader.readLine();
                }
                assertTrue("HTTP status code was correct (" + expectedCode
                        + "), but body did not contain " + "the string \""
                        + expectedString + "\"", foundExpectedString);
            }
        } finally {
            in.close();
        }
    }

    private static void checkUploadCode(FedoraClient client,
                                        String partName,
                                        String errorMessage,
                                        int expectedCode) throws Exception {
        File file = File.createTempFile("fedora-junit", ".txt");
        try {
            writeStringToFile("test", file);
            int gotCode =
                    getUploadCode(client,
                                  getBaseURL() + "/management/upload",
                                  file,
                                  partName);
            assertEquals(errorMessage + " (/management/upload, partName="
                    + partName + ")", expectedCode, gotCode);
        } finally {
            file.delete();
        }
    }

    private static int getUploadCode(FedoraClient client,
                                     String url,
                                     File file,
                                     String partName) throws Exception {
        PostMethod post = null;
        try {
            post = new PostMethod(url);
            post.setDoAuthentication(true);
            post.getParams().setParameter("Connection", "Keep-Alive");
            post.setContentChunked(true);
            Part[] parts = {new FilePart(partName, file)};
            post.setRequestEntity(new MultipartRequestEntity(parts, post
                    .getParams()));
            int responseCode = client.getHttpClient().executeMethod(post);
            if (responseCode > 299 && responseCode < 400) {
                String location = post.getResponseHeader("location").getValue();
                System.out.println("Redirected to " + location);
                return getUploadCode(client, location, file, partName);
            } else {
                return responseCode;
            }
        } finally {
            if (post != null) {
                post.releaseConnection();
            }
        }
    }

	private static String getPolicyId(byte[] data) throws Exception
	{
		Document doc = DataUtils.getDocumentFromBytes(data);
		String pid = doc.getDocumentElement().getAttribute("PolicyId");

		return pid;
	}

	private static String addPolicy(String policy) throws Exception
	{
		PolicyDataManager polMan = new DbXmlPolicyDataManager();
		String policyId = getPolicyId(policy.getBytes());
		polMan.addPolicy(new String(policy), policyId);
		Thread.sleep(1000);

		return policyId;
	}

	private static void delPolicy(String policyId) throws Exception
	{
		PolicyDataManager polMan = new DbXmlPolicyDataManager();
		polMan.deletePolicy(policyId);
		Thread.sleep(1000);
	}
}
