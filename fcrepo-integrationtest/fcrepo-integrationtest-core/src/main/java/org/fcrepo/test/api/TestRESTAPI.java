/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.test.api;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.URL;
import java.net.URLEncoder;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.axis.types.NonNegativeInteger;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;

import org.junit.Test;

import org.antlr.stringtemplate.StringTemplate;

import junit.framework.TestSuite;

import org.fcrepo.common.PID;

import org.fcrepo.server.access.FedoraAPIA;
import org.fcrepo.server.management.FedoraAPIM;
import org.fcrepo.server.types.gen.Datastream;
import org.fcrepo.server.types.gen.FieldSearchQuery;
import org.fcrepo.server.types.gen.FieldSearchResult;
import org.fcrepo.server.types.gen.MIMETypedStream;
import org.fcrepo.server.types.gen.ObjectFields;

import org.fcrepo.test.DemoObjectTestSetup;
import org.fcrepo.test.FedoraServerTestCase;
import org.fcrepo.test.TemplatedResourceIterator;

import static org.apache.commons.httpclient.HttpStatus.SC_CREATED;
import static org.apache.commons.httpclient.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.commons.httpclient.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.commons.httpclient.HttpStatus.SC_NOT_FOUND;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.apache.commons.httpclient.HttpStatus.SC_UNAUTHORIZED;

/**
 * Tests of the REST API. Tests assume a running instance of Fedora with the
 * REST API enabled. //TODO: actually validate the ResponseBody instead of just
 * HTTP status codes
 *
 * @author Edwin Shin
 * @author Bill Branan
 * @version $Id$
 * @since 3.0
 */
public class TestRESTAPI
        extends FedoraServerTestCase {

    private FedoraAPIA apia;

    private FedoraAPIM apim;

    // used for determining test configuration
    private static String authAccessProperty = "fedora.authorize.access";

    protected Boolean authorizeAccess = null;

    private static String DEMO_REST;

    private static byte[] DEMO_REST_FOXML;

    private static String DEMO_MIN;

    private static String DEMO_MIN_PID;

    private final PID pid = PID.getInstance("demo:REST");

    private static final String REST_RESOURCE_PATH =
            System.getProperty("fcrepo-integrationtest-core.classes") != null ? System
                    .getProperty("fcrepo-integrationtest-core.classes")
                    + "rest" : "src/test/resources/rest";

    // various download filenames used in test object for content-disposition header test
    // datastreams in test object (using these) are:
    // DS1 with label; also has relationship in RELS-INT specifying filename
    // DS2 with label - MIMETYPE maps to an extension
    // DS3 with label - unknown MIMETYPE
    // DS4 with label containing illegal filename characters
    // DS5 with no label
    // DS6.xml with no label; datastream ID contains extension
    //
    // all datastreams text/xml apart from DS2 which is image/jpeg

    private static String DS1RelsFilename =
            "Datastream 1 filename from rels.extension";

    private static String DS2LabelFilename = "Datastream 2 filename from label";

    private static String DS3LabelFilename = "Datastream 3 filename from label";

    private static String DodgyChars = "\\/*?&lt;&gt;:|";

    private static String DS4LabelFilenameOriginal = "Datastream 4 filename "
            + DodgyChars + "from label";

    // this one in foxml
    private static String DS4LabelFilename = "Datastream 4 filename from label"; // this should be the cleaned version

    private static String DS5ID = "DS5";

    private static String DS6ID = "DS6.xml";

    protected String url;

    private static final String datetime =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .format(new Date());

    private boolean chunked = false;

    @Override
    public void setUp() throws Exception {

        DEMO_MIN =
                FileUtils.readFileToString(new File(REST_RESOURCE_PATH
                        + "/demo_min.xml"), "UTF-8");
        DEMO_MIN_PID =
                FileUtils.readFileToString(new File(REST_RESOURCE_PATH
                        + "/demo_min_pid.xml"), "UTF-8");

        StringTemplate tpl =
                new StringTemplate(FileUtils.readFileToString(new File(REST_RESOURCE_PATH
                                                                      + "/demo_rest.xml"),
                                                              "UTF-8"));
        tpl.setAttribute("MODEL_DOWNLOAD_FILENAME",
                         MODEL.DOWNLOAD_FILENAME.localName);
        tpl.setAttribute("DS1_RELS_FILENAME", DS1RelsFilename);
        tpl.setAttribute("MODEL_URI", MODEL.uri);
        tpl.setAttribute("DATETIME", datetime);
        tpl.setAttribute("FEDORA_BASE_URL", getBaseURL());
        tpl.setAttribute("DS2_LABEL_FILENAME", DS2LabelFilename);
        tpl.setAttribute("DS3_LABEL_FILENAME", DS3LabelFilename);
        tpl.setAttribute("DS4_LABEL_FILENAME_ORIGINAL",
                         DS4LabelFilenameOriginal);

        DEMO_REST = tpl.toString();
        apia = getFedoraClient().getAPIA();
        apim = getFedoraClient().getAPIM();
        apim.ingest(DEMO_REST.getBytes("UTF-8"),
                    FOXML1_1.uri,
                    "ingesting new foxml object");

    }

    @Override
    public void tearDown() throws Exception {
        apim.purgeObject(pid.toString(), "", false);
    }

    // determine if test config specifies that Access requests should be authorized

    private boolean getAuthAccess() {
        if (authorizeAccess == null) {
            String property = System.getProperty(authAccessProperty);
            if (property.equals("true")) {
                authorizeAccess = true;
            } else if (property.equals("false")) {
                authorizeAccess = false;
            } else {
                assertTrue("Failed to determine whether to perform authorization on Access requests from: "
                                   + authAccessProperty,
                           false);
                throw new RuntimeException("Failed to determine whether to perform authorization on Access requests from: "
                        + authAccessProperty);
            }

        }
        return authorizeAccess;

    }

    @Test
    public void testGetWADL() throws Exception {
        url = "/objects/application.wadl";

        if (getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_OK, get(getAuthAccess()).getStatusCode());
    }

    //public void testDescribeRepository() throws Exception {}

    // API-A

    @Test
    public void testGetObjectProfile() throws Exception {
        url = String.format("/objects/%s", pid.toString());
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_OK, get(getAuthAccess()).getStatusCode());

        url = String.format("/objects/%s?format=xml", pid.toString());
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        HttpResponse response = get(getAuthAccess());
        assertEquals(SC_OK, response.getStatusCode());
        String responseXML = new String(response.responseBody, "UTF-8");
        assertTrue(responseXML.contains("<objLabel>"));
        assertTrue(responseXML.contains("<objOwnerId>"));
        assertTrue(responseXML.contains("<objCreateDate>"));
        assertTrue(responseXML.contains("<objLastModDate>"));
        assertTrue(responseXML.contains("<objDissIndexViewURL>"));
        assertTrue(responseXML.contains("<objItemIndexViewURL>"));
        assertTrue(responseXML.contains("<objState>"));

        url =
                String.format("/objects/%s?asOfDateTime=%s",
                              pid.toString(),
                              datetime);
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_OK, get(getAuthAccess()).getStatusCode());

        // sanity check
        url = String.format("/objects/%s", "demo:BOGUS_PID");
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_NOT_FOUND, get(getAuthAccess()).getStatusCode());
    }

    public void testListMethods() throws Exception {
        url = String.format("/objects/%s/methods", pid.toString());
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_OK, get(getAuthAccess()).getStatusCode());

        url = String.format("/objects/%s/methods?format=xml", pid.toString());
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_OK, get(getAuthAccess()).getStatusCode());

        url =
                String.format("/objects/%s/methods?asOfDateTime=%s",
                              pid.toString(),
                              datetime);
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_OK, get(getAuthAccess()).getStatusCode());
    }

    public void testListMethodsForSDep() throws Exception {
        url =
                String.format("/objects/%s/methods/fedora-system:3",
                              pid.toString());
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_OK, get(getAuthAccess()).getStatusCode());

        url =
                String.format("/objects/%s/methods/fedora-system:3?format=xml",
                              pid.toString());
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        HttpResponse response = get(getAuthAccess());
        assertEquals(SC_OK, response.getStatusCode());
        String responseXML = new String(response.responseBody, "UTF-8");
        assertTrue(responseXML.contains("sDef=\"fedora-system:3\""));

        url =
                String.format("/objects/%s/methods/fedora-system:3?asOfDateTime=%s",
                              pid.toString(),
                              datetime);
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_OK, get(getAuthAccess()).getStatusCode());
    }

    //
    // GETs on built-in Service Definition methods
    //

    public void testGETMethodBuiltInBadMethod() throws Exception {
        url =
                String.format("/objects/%s/methods/fedora-system:3/noSuchMethod",
                              pid.toString());
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertFalse(SC_OK == get(getAuthAccess()).getStatusCode());
    }

    public void testGETMethodBuiltInBadUserArg() throws Exception {
        url =
                String.format("/objects/%s/methods/fedora-system:3/viewMethodIndex?noSuchArg=foo",
                              pid.toString());
        assertFalse(SC_OK == get(getAuthAccess()).getStatusCode());
    }

    public void testGETMethodBuiltInNoArg() throws Exception {
        url =
                String.format("/objects/%s/methods/fedora-system:3/viewMethodIndex",
                              pid.toString());
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_OK, get(getAuthAccess()).getStatusCode());
    }

    //
    // GETs on custom Service Definition methods
    //

    public void testGETMethodCustomBadMethod() throws Exception {
        url = "/objects/demo:14/methods/demo:12/noSuchMethod";
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertFalse(SC_OK == get(getAuthAccess()).getStatusCode());
    }

    public void testGETMethodCustomBadUserArg() throws Exception {
        url =
                "/objects/demo:14/methods/demo:12/getDocumentStyle1?noSuchArg=foo";
        assertFalse(SC_OK == get(getAuthAccess()).getStatusCode());
    }

    public void testGETMethodCustomNoArg() throws Exception {
        url = "/objects/demo:14/methods/demo:12/getDocumentStyle1";
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_OK, get(getAuthAccess()).getStatusCode());
    }

    public void testGETMethodCustomGoodUserArg() throws Exception {
        url = "/objects/demo:29/methods/demo:27/resizeImage?width=50";
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        HttpResponse response = get(getAuthAccess());
        assertEquals(SC_OK, response.getStatusCode());
        assertEquals(1486, response.getResponseBody().length);
        assertEquals("image/jpeg", response.getResponseHeader("Content-Type")
                .getValue());
    }

    public void testGETMethodCustomGoodUserArgGoodDate() throws Exception {
        url =
                "/objects/demo:29/methods/demo:27/resizeImage?width=50&asOfDateTime="
                        + datetime;
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        HttpResponse response = get(getAuthAccess());
        assertEquals(SC_OK, response.getStatusCode());
        assertEquals(1486, response.getResponseBody().length);
        assertEquals("image/jpeg", response.getResponseHeader("Content-Type")
                .getValue());
    }

    public void testGETMethodCustomUserArgBadDate() throws Exception {
        url =
                "/objects/demo:14/methods/demo:12/getDocumentStyle1?width=50&asOfDateTime=badDate";
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertFalse(SC_OK == get(getAuthAccess()).getStatusCode());
    }

    public void testGETMethodCustomUserArgEarlyDate() throws Exception {
        url =
                "/objects/demo:14/methods/demo:12/getDocumentStyle1?width=50&asOfDateTime=1999-11-21T16:38:32.200Z";
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_NOT_FOUND, get(getAuthAccess()).getStatusCode());
    }

    public void testGETMethodCustomGoodDate() throws Exception {
        url =
                "/objects/demo:14/methods/demo:12/getDocumentStyle1?asOfDateTime="
                        + datetime;
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_OK, get(getAuthAccess()).getStatusCode());
    }

    public void testGETMethodCustomBadDate() throws Exception {
        url =
                "/objects/demo:14/methods/demo:12/getDocumentStyle1?asOfDateTime=badDate";
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertFalse(SC_OK == get(getAuthAccess()).getStatusCode());
    }

    public void testGETMethodCustomEarlyDate() throws Exception {
        url =
                "/objects/demo:14/methods/demo:12/getDocumentStyle1?asOfDateTime=1999-11-21T16:38:32.200Z";
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_NOT_FOUND, get(getAuthAccess()).getStatusCode());
    }

    public void testListDatastreams() throws Exception {
        url = String.format("/objects/%s/datastreams", pid.toString());
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_OK, get(getAuthAccess()).getStatusCode());

        url =
                String.format("/objects/%s/datastreams?format=xml",
                              pid.toString());
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_OK, get(getAuthAccess()).getStatusCode());

        url =
                String.format("/objects/%s/datastreams?asOfDateTime=%s",
                              pid.toString(),
                              datetime);
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_OK, get(getAuthAccess()).getStatusCode());
    }

    // if an object is marked as deleted, only administrators have access (default XACML policy)
    // see FCREPO-753: this also tests that preemptive auth can still take place when api-a auth is not required
    public void testDeletedObject() throws Exception {
        // only test if api-a auth is disabled (configA)
        if (!this.getAuthAccess()) {

            // mark object as deleted
            apim.modifyObject(pid.toString(),
                              "D",
                              null,
                              null,
                              "Mark object as deleted");

            // verify no unauth access
            url = String.format("/objects/%s/datastreams", pid.toString());
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
            // verify auth access for administrator
            assertEquals(SC_OK, get(true).getStatusCode());

        }

    }

    public void testGetDatastreamProfile() throws Exception {
        // Datastream profile in HTML format
        url = String.format("/objects/%s/datastreams/RELS-EXT", pid.toString());
        assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        HttpResponse response = get(true);
        assertEquals(SC_OK, response.getStatusCode());
        String responseXML = new String(response.responseBody, "UTF-8");
        assertTrue(responseXML.contains("<html>"));

        // Datastream profile in XML format
        url =
                String.format("/objects/%s/datastreams/RELS-EXT?format=xml",
                              pid.toString());
        assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        response = get(true);
        assertEquals(SC_OK, response.getStatusCode());
        responseXML = new String(response.responseBody, "UTF-8");
        assertTrue(responseXML.contains("<dsLabel>"));

        // Datastream profile as of the current date-time (XML format)
        url =
                String.format("/objects/%s/datastreams/RELS-EXT?asOfDateTime=%s&format=xml",
                              pid.toString(),
                              datetime);
        assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        response = get(true);
        assertEquals(SC_OK, response.getStatusCode());
        responseXML = new String(response.responseBody, "UTF-8");
        assertTrue(responseXML.contains("<dsLabel>"));

        // sanity check
        url =
                String.format("/objects/%s/datastreams/BOGUS_DSID",
                              pid.toString());
        assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        assertEquals(SC_NOT_FOUND, get(true).getStatusCode());
    }

    public void testGetDatastreamHistory() throws Exception {

        // Ingest minimal object
        url = "/objects/new";
        assertEquals(SC_UNAUTHORIZED, post(DEMO_MIN_PID, false).getStatusCode());
        HttpResponse response = post(DEMO_MIN_PID, true);
        assertEquals(SC_CREATED, response.getStatusCode());

        // Get history in XML format
        url =
                String.format("/objects/demo:1234/datastreams/DS1/history?format=xml");
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        response = get(true);
        assertEquals(SC_OK, response.getStatusCode());
        String responseXML = new String(response.responseBody, "UTF-8");

        String control =
                FileUtils.readFileToString(new File(REST_RESOURCE_PATH
                        + "/datastreamHistory.xml"), "UTF-8");
        StringTemplate tpl = new StringTemplate(control);
        tpl.setAttribute("FEDORA_BASE_URL", getProtocol() + "://" + getHost()
                + ":" + getPort());

        // Diff must be identical
        XMLUnit.setIgnoreWhitespace(true);
        Diff xmldiff = new Diff(tpl.toString(), responseXML);
        assertTrue(xmldiff.toString(), xmldiff.identical());

        // Sanity check
        url = "/objects/demo:1234/datastreams/BOGUS_DSID";
        assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        assertEquals(SC_NOT_FOUND, get(true).getStatusCode());

        // Clean up
        url = "/objects/demo:1234";
        assertEquals(SC_UNAUTHORIZED, delete(false).getStatusCode());
        assertEquals(SC_OK, delete(true).getStatusCode());
    }

    public void testGetDatastreamDissemination() throws Exception {
        url =
                String.format("/objects/%s/datastreams/RELS-EXT/content",
                              pid.toString());
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        HttpResponse response = get(getAuthAccess());
        assertEquals(SC_OK, response.getStatusCode());
        String responseXML = new String(response.responseBody, "UTF-8");
        assertTrue(responseXML.contains("rdf:Description"));

        url =
                String.format("/objects/%s/datastreams/RELS-EXT/content?asOfDateTime=%s",
                              pid.toString(),
                              datetime);
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        response = get(getAuthAccess());
        assertEquals(SC_OK, response.getStatusCode());
        responseXML = new String(response.responseBody, "UTF-8");
        assertTrue(responseXML.contains("rdf:Description"));

        // sanity check
        url =
                String.format("/objects/%s/datastreams/BOGUS_DSID/content",
                              pid.toString());
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_NOT_FOUND, get(getAuthAccess()).getStatusCode());
    }

    public void testFindObjects() throws Exception {
        url =
                String.format("/objects?pid=true&terms=%s&query=&resultFormat=xml",
                              pid.toString());
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_OK, get(getAuthAccess()).getStatusCode());
    }

    /**
     * Disabled until FCREPO-798 is public public void testFindObjectsQuery()
     * throws Exception { String templateUrl = "/search?$value$";
     * TemplatedResourceIterator tri = new
     * TemplatedResourceIterator(templateUrl,
     * "src/test/resources/APIM2/restsearchvalues"); while (tri.hasNext()) { url
     * = tri.next(); HttpResponse resp = get(getAuthAccess());
     * //assertEquals(SC_OK, resp.getStatusCode()); url =
     * String.format("/objects/new"); HttpResponse response = post("", true);
     * assertEquals(SC_CREATED, response.getStatusCode()); } }
     */

    public void testResumeFindObjects() throws Exception {
        url = "/objects?pid=true&query=&resultFormat=xml";
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        HttpResponse response = get(getAuthAccess());
        assertEquals(SC_OK, response.getStatusCode());

        String responseXML = new String(response.responseBody, "UTF-8");
        String sessionToken =
                responseXML.substring(responseXML.indexOf("<token>") + 7,
                                      responseXML.indexOf("</token>"));

        url =
                String.format("/objects?pid=true&query=&format=xml&sessionToken=%s",
                              sessionToken);
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_OK, get(getAuthAccess()).getStatusCode());
    }

    public void testFindObjectsBadSyntax() throws Exception {
        url = "/objects?pid=true&query=label%3D%3F&maxResults=20";
        // Try > 100 times; will hang if the connection isn't properly released
        for (int i = 0; i < 101; i++) {
            assertEquals(SC_INTERNAL_SERVER_ERROR, get(getAuthAccess())
                    .getStatusCode());
        }
    }

    public void testGetObjectHistory() throws Exception {
        url = String.format("/objects/%s/versions", pid.toString());
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_OK, get(getAuthAccess()).getStatusCode());

        url = String.format("/objects/%s/versions?format=xml", pid.toString());
        if (this.getAuthAccess()) {
            assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        }
        assertEquals(SC_OK, get(getAuthAccess()).getStatusCode());
    }

    private String extractPid(String source) {
        Matcher m = Pattern.compile("^.*/([^/]+$)").matcher(source);
        String pid = null;
        if (m.find() && m.groupCount() == 1) {
            pid = m.group(1);
        }
        return pid;
    }

    // API-M

    public void testIngest() throws Exception {
        // Create new empty object
        url = String.format("/objects/new");
        assertEquals(SC_UNAUTHORIZED, post("", false).getStatusCode());
        HttpResponse response = post("", true);
        assertEquals(SC_CREATED, response.getStatusCode());

        String emptyObjectPid =
                extractPid(response.getResponseHeader("Location").getValue());
        assertNotNull(emptyObjectPid);
        emptyObjectPid =
                emptyObjectPid.replaceAll("\n", "").replaceAll("\r", "")
                        .replaceAll("%3A", ":");
        // PID should be returned as a header and as the response body
        String responseBody = new String(response.getResponseBody(), "UTF-8");
        assertTrue(responseBody.equals(emptyObjectPid));

        // Delete empty object
        url = String.format("/objects/%s", emptyObjectPid);
        assertEquals(SC_UNAUTHORIZED, delete(false).getStatusCode());
        assertEquals(SC_OK, delete(true).getStatusCode());

        // Ensure that GETs of the deleted object immediately give 404s (See FCREPO-594)
        assertEquals(SC_NOT_FOUND, get(getAuthAccess()).getStatusCode());

        // Create new empty object with a PID namespace specified
        url = String.format("/objects/new?namespace=test");
        assertEquals(SC_UNAUTHORIZED, post("", false).getStatusCode());
        response = post("", true);
        assertEquals(SC_CREATED, response.getStatusCode());

        emptyObjectPid =
                extractPid(response.getResponseHeader("Location").getValue());
        assertTrue(emptyObjectPid.startsWith("test"));

        // Delete empty "test" object
        url = String.format("/objects/%s", emptyObjectPid);
        assertEquals(SC_UNAUTHORIZED, delete(false).getStatusCode());
        assertEquals(SC_OK, delete(true).getStatusCode());

        // Delete the demo:REST object (ingested as part of setup)
        url = String.format("/objects/%s", pid.toString());
        assertEquals(SC_UNAUTHORIZED, delete(false).getStatusCode());
        assertEquals(SC_OK, delete(true).getStatusCode());

        // Ingest the demo:REST object
        url = String.format("/objects/%s", pid.toString());
        assertEquals(SC_UNAUTHORIZED, post(DEMO_REST, false).getStatusCode());
        response = post(DEMO_REST, true);
        assertEquals(SC_CREATED, response.getStatusCode());
        Header locationHeader = response.getResponseHeader("location");
        assertNotNull(locationHeader);
        assertTrue(locationHeader.getValue().contains(URLEncoder.encode(pid
                .toString(), "UTF-8")));
        responseBody = new String(response.getResponseBody(), "UTF-8");
        assertTrue(responseBody.equals(pid.toString()));

        // Ingest minimal object with no PID
        url = String.format("/objects/new");
        assertEquals(SC_UNAUTHORIZED, post(DEMO_MIN, false).getStatusCode());
        response = post(DEMO_MIN, true);
        assertEquals(SC_CREATED, response.getStatusCode());

        // Delete minimal object
        String minimalObjectPid =
                extractPid(response.getResponseHeader("Location").getValue());
        url = String.format("/objects/%s", minimalObjectPid);
        assertEquals(SC_UNAUTHORIZED, delete(false).getStatusCode());
        assertEquals(SC_OK, delete(true).getStatusCode());
    }

    // Tests FCREPO-509

    public void testIngestWithParameterPid() throws Exception {

        // Ingest minimal object with PID, use "new" as path parameter -> must succeed
        url = String.format("/objects/new");
        assertEquals(SC_UNAUTHORIZED, post(DEMO_MIN_PID, false).getStatusCode());
        HttpResponse response = post(DEMO_MIN_PID, true);
        assertEquals(SC_CREATED, response.getStatusCode());

        // clean up
        url = String.format("/objects/%s", "demo:1234");
        assertEquals(SC_UNAUTHORIZED, delete(false).getStatusCode());
        assertEquals(SC_OK, delete(true).getStatusCode());

        // Ingest minimal object with PID, use a different PID than the one
        // specified in the foxml -> must fail
        url = String.format("/objects/%s", "demo:234");
        assertEquals(SC_UNAUTHORIZED, post(DEMO_MIN_PID, false).getStatusCode());
        response = post(DEMO_MIN_PID, true);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());

        // Ingest minimal object with PID equals to the PID specified in the foxml
        url = String.format("/objects/%s", "demo:1234");
        assertEquals(SC_UNAUTHORIZED, post(DEMO_MIN_PID, false).getStatusCode());
        response = post(DEMO_MIN_PID, true);
        assertEquals(SC_CREATED, response.getStatusCode());

        // clean up
        url = String.format("/objects/%s", "demo:1234");
        assertEquals(SC_UNAUTHORIZED, delete(false).getStatusCode());
        assertEquals(SC_OK, delete(true).getStatusCode());

        // Ingest minimal object with no PID, specify a PID parameter in the request
        url = String.format("/objects/%s", "demo:234");
        assertEquals(SC_UNAUTHORIZED, post(DEMO_MIN, false).getStatusCode());
        response = post(DEMO_MIN, true);
        assertEquals(SC_CREATED, response.getStatusCode());

        // clean up
        url = String.format("/objects/%s", "demo:234");
        assertEquals(SC_UNAUTHORIZED, delete(false).getStatusCode());
        assertEquals(SC_OK, delete(true).getStatusCode());

    }

    public void testModifyObject() throws Exception {
        url = String.format("/objects/%s?label=%s", pid.toString(), "foo");
        assertEquals(SC_UNAUTHORIZED, put("", false).getStatusCode());
        HttpResponse response = put("", true);
        assertEquals(SC_OK, response.getStatusCode());
        assertEquals("foo", apia.getObjectProfile(pid.toString(), null)
                .getObjLabel());
    }

    public void testGetObjectXML() throws Exception {
        url = String.format("/objects/%s/objectXML", pid.toString());
        assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        assertEquals(SC_OK, get(true).getStatusCode());
    }

    public void testValidate() throws Exception {

        try {
            //ingestDemoObjects();

            String[] resultFields = {"pid"};
            NonNegativeInteger maxResults = new NonNegativeInteger("" + 1000);
            FieldSearchQuery query = new FieldSearchQuery(null, "*");
            FieldSearchResult result =
                    apia.findObjects(resultFields, maxResults, query);

            ObjectFields[] fields = result.getResultList();
            for (ObjectFields objectFields : fields) {
                String pid = objectFields.getPid();
                //System.out.println("validating object '" + pid.toString() + "'");
                url =
                        String.format("/objects/%s/validate", URLEncoder
                                .encode(pid.toString(), "UTF-8"));
                HttpResponse getTrue = get(true);
                assertEquals(pid.toString(), SC_UNAUTHORIZED, get(false)
                        .getStatusCode());
                assertEquals(pid.toString(), SC_OK, getTrue.getStatusCode());
                String responseXML = getTrue.getResponseBodyString();
                //System.out.println(responseXML);
                assertXpathExists("/validation[@valid='true']", responseXML);
            }
        } finally {
            //purgeDemoObjects();
        }
    }

    public void testExportObject() throws Exception {
        url = String.format("/objects/%s/export", pid.toString());
        assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        assertEquals(SC_OK, get(true).getStatusCode());

        url =
                String.format("/objects/%s/export?context=public",
                              pid.toString());
        assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        assertEquals(SC_OK, get(true).getStatusCode());
    }

    public void testPurgeObject() throws Exception {
        url = String.format("/objects/%s", "demo:TEST_PURGE");
        assertEquals(SC_UNAUTHORIZED, post("", false).getStatusCode());
        assertEquals(SC_CREATED, post("", true).getStatusCode());
        url = String.format("/objects/demo:TEST_PURGE");
        assertEquals(SC_UNAUTHORIZED, delete(false).getStatusCode());
        assertEquals(SC_OK, delete(true).getStatusCode());
    }

    public void testAddDatastream() throws Exception {
        // inline (X) datastream
        String xmlData = "<foo>bar</foo>";
        String dsPath = "/objects/" + pid + "/datastreams/FOO";
        url = dsPath + "?controlGroup=X&dsLabel=bar";
        assertEquals(SC_UNAUTHORIZED, post(xmlData, false).getStatusCode());
        HttpResponse response = post(xmlData, true);
        assertEquals(SC_CREATED, response.getStatusCode());
        Header locationHeader = response.getResponseHeader("location");
        assertNotNull(locationHeader);
        assertEquals(new URL(url.substring(0, url.indexOf('?'))).toString(),
                     locationHeader.getValue());
        assertEquals("text/xml", response.getResponseHeader("Content-Type")
                .getValue());
        url = dsPath + "?format=xml";
        assertEquals(response.getResponseBodyString(), get(true)
                .getResponseBodyString());

        // managed (M) datastream
        String mimeType = "text/plain";
        dsPath = "/objects/" + pid + "/datastreams/BAR";
        url = dsPath + "?controlGroup=M&dsLabel=bar&mimeType=" + mimeType;
        File temp = File.createTempFile("test", null);
        DataOutputStream os = new DataOutputStream(new FileOutputStream(temp));
        os.write(42);
        os.close();
        assertEquals(SC_UNAUTHORIZED, post(temp, false).getStatusCode());
        response = post(temp, true);
        assertEquals(SC_CREATED, response.getStatusCode());
        locationHeader = response.getResponseHeader("location");
        assertNotNull(locationHeader);
        assertEquals(new URL(url.substring(0, url.indexOf('?'))).toString(),
                     locationHeader.getValue());
        assertEquals("text/xml", response.getResponseHeader("Content-Type")
                .getValue());
        url = dsPath + "?format=xml";
        assertEquals(response.getResponseBodyString(), get(true)
                .getResponseBodyString());
        Datastream ds = apim.getDatastream(pid.toString(), "BAR", null);
        assertEquals(ds.getMIMEType(), mimeType);
    }

    public void testModifyDatastreamByReference() throws Exception {
        // Create BAR datastream
        url =
                String.format("/objects/%s/datastreams/BAR?controlGroup=M&dsLabel=bar",
                              pid.toString());
        File temp = File.createTempFile("test", null);
        DataOutputStream os = new DataOutputStream(new FileOutputStream(temp));
        os.write(42);
        os.close();
        assertEquals(SC_UNAUTHORIZED, post(temp, false).getStatusCode());
        assertEquals(SC_CREATED, post(temp, true).getStatusCode());

        // Update the content of the BAR datastream (using PUT)
        url = String.format("/objects/%s/datastreams/BAR", pid.toString());
        assertEquals(SC_UNAUTHORIZED, put(temp, false).getStatusCode());
        HttpResponse response = put(temp, true);
        assertEquals(SC_OK, response.getStatusCode());
        assertEquals("text/xml", response.getResponseHeader("Content-Type")
                .getValue());
        url = url + "?format=xml";
        assertEquals(response.getResponseBodyString(), get(true)
                .getResponseBodyString());

        // Ensure 404 on attempt to update BOGUS_DS via PUT
        url = "/objects/" + pid + "/datastreams/BOGUS_DS";
        assertEquals(SC_NOT_FOUND, put(temp, true).getStatusCode());

        // Update the content of the BAR datastream (using POST)
        url = String.format("/objects/%s/datastreams/BAR", pid.toString());
        response = post(temp, true);
        assertEquals(SC_CREATED, response.getStatusCode());
        Header locationHeader = response.getResponseHeader("location");
        assertNotNull(locationHeader);
        assertEquals(url, locationHeader.getValue());
        assertEquals("text/xml", response.getResponseHeader("Content-Type")
                .getValue());
        url = url + "?format=xml";
        assertEquals(response.getResponseBodyString(), get(true)
                .getResponseBodyString());

        // Update the label of the BAR datastream
        String newLabel = "tikibar";
        url =
                String.format("/objects/%s/datastreams/BAR?dsLabel=%s",
                              pid.toString(),
                              newLabel);
        assertEquals(SC_UNAUTHORIZED, put(false).getStatusCode());
        assertEquals(SC_OK, put(true).getStatusCode());
        assertEquals(newLabel, apim.getDatastream(pid.toString(), "BAR", null)
                .getLabel());

        // Update the location of the EXTDS datastream (E type datastream)
        String newLocation =
                "http://" + getHost() + ":" + getPort() + "/"
                        + getFedoraAppServerContext() + "/get/demo:REST/DC";
        url =
                String.format("/objects/%s/datastreams/EXTDS?dsLocation=%s",
                              pid.toString(),
                              newLocation);
        assertEquals(SC_UNAUTHORIZED, put(false).getStatusCode());
        assertEquals(SC_OK, put(true).getStatusCode());

        assertEquals(newLocation,
                     apim.getDatastream(pid.toString(), "EXTDS", null)
                             .getLocation());
        String dcDS =
                new String(apia.getDatastreamDissemination(pid.toString(),
                                                           "DC",
                                                           null).getStream());
        String extDS =
                new String(apia.getDatastreamDissemination(pid.toString(),
                                                           "EXTDS",
                                                           null).getStream());
        assertEquals(dcDS, extDS);

        // Update DS1 by reference (X type datastream)
        // Error expected because attempting to access internal DS with API-A auth on
        if (getAuthAccess()) {
            // only ConfigB has API-A auth on
            url =
                    String.format("/objects/%s/datastreams/DS1?dsLocation=%s",
                                  pid.toString(),
                                  newLocation);
            assertEquals(SC_UNAUTHORIZED, put(false).getStatusCode());
            assertEquals(SC_INTERNAL_SERVER_ERROR, put(true).getStatusCode());
        }

        // Update DS1 by reference (X type datastream) - Success expected
        newLocation = getBaseURL() + "/ri/index.xsl";
        url =
                String.format("/objects/%s/datastreams/DS1?dsLocation=%s",
                              pid.toString(),
                              newLocation);
        assertEquals(SC_UNAUTHORIZED, put(false).getStatusCode());
        assertEquals(SC_OK, put(true).getStatusCode());
    }

    public void testModifyDatastreamByValue() throws Exception {
        String xmlData = "<baz>quux</baz>";
        url = String.format("/objects/%s/datastreams/DS1", pid.toString());

        assertEquals(SC_UNAUTHORIZED, put(xmlData, false).getStatusCode());
        assertEquals(SC_OK, put(xmlData, true).getStatusCode());

        MIMETypedStream ds1 =
                apia.getDatastreamDissemination(pid.toString(), "DS1", null);
        assertXMLEqual(xmlData, new String(ds1.getStream(), "UTF-8"));
    }

    public void testModifyDatastreamNoContent() throws Exception {
        String label = "Label";
        url =
                String.format("/objects/%s/datastreams/DS1?dsLabel=%s",
                              pid.toString(),
                              label);

        assertEquals(SC_UNAUTHORIZED, put("", false).getStatusCode());
        assertEquals(SC_OK, put("", true).getStatusCode());

        Datastream ds1 = apim.getDatastream(pid.toString(), "DS1", null);
        assertEquals(label, ds1.getLabel());
    }

    public void testSetDatastreamState() throws Exception {
        String state = "D";
        url =
                String.format("/objects/%s/datastreams/DS1?dsState=%s",
                              pid.toString(),
                              state);
        assertEquals(SC_UNAUTHORIZED, put("", false).getStatusCode());
        assertEquals(SC_OK, put("", true).getStatusCode());

        Datastream ds1 = apim.getDatastream(pid.toString(), "DS1", null);
        assertEquals(state, ds1.getState());
    }

    public void testSetDatastreamVersionable() throws Exception {
        boolean versionable = false;
        url =
                String.format("/objects/%s/datastreams/DS1?versionable=%s",
                              pid.toString(),
                              versionable);
        assertEquals(SC_UNAUTHORIZED, put("", false).getStatusCode());
        assertEquals(SC_OK, put("", true).getStatusCode());

        Datastream ds1 = apim.getDatastream(pid.toString(), "DS1", null);
        assertEquals(versionable, ds1.isVersionable());
    }

    public void testPurgeDatastream() throws Exception {
        url = String.format("/objects/%s/datastreams/RELS-EXT", pid.toString());
        assertEquals(SC_UNAUTHORIZED, delete(false).getStatusCode());
        assertEquals(SC_OK, delete(true).getStatusCode());
    }

    public void testGetNextPID() throws Exception {
        url = "/objects/nextPID";
        assertEquals(SC_UNAUTHORIZED, post("", false).getStatusCode());
        assertEquals(SC_OK, post("", true).getStatusCode());
    }

    public void testLifecycle() throws Exception {
        HttpResponse response = null;

        // Get next PID
        url = "/objects/nextPID?format=xml";
        assertEquals(SC_UNAUTHORIZED, post("", false).getStatusCode());
        response = post("", true);
        assertEquals(SC_OK, response.getStatusCode());

        String responseXML = new String(response.responseBody, "UTF-8");
        String pid =
                responseXML.substring(responseXML.indexOf("<pid>") + 5,
                                      responseXML.indexOf("</pid>"));

        // Ingest object
        String label = "Lifecycle-Test-Label";
        url = String.format("/objects/%s?label=%s", pid, label);
        assertEquals(SC_UNAUTHORIZED, post("", false).getStatusCode());
        assertEquals(SC_CREATED, post("", true).getStatusCode());

        // Add datastream
        String datastreamData = "<test>Test Datastream</test>";
        url =
                String.format("/objects/%s/datastreams/TESTDS?controlGroup=X&dsLabel=Test",
                              pid.toString());
        assertEquals(SC_UNAUTHORIZED, post(datastreamData, false)
                .getStatusCode());
        assertEquals(SC_CREATED, post(datastreamData, true).getStatusCode());

        // Get object XML
        url = String.format("/objects/%s/objectXML", pid);
        assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        response = get(true);
        assertEquals(SC_OK, response.getStatusCode());
        responseXML = new String(response.responseBody, "UTF-8");
        assertTrue(responseXML.indexOf(label) > 0);
        assertTrue(responseXML.indexOf(datastreamData) > 0);

        // Modify object
        label = "Updated-Label";
        url = String.format("/objects/%s?label=%s", pid.toString(), label);
        assertEquals(SC_UNAUTHORIZED, put("", false).getStatusCode());
        assertEquals(SC_OK, put("", true).getStatusCode());

        // Modify datastream
        datastreamData = "<test>Update Test</test>";
        url = String.format("/objects/%s/datastreams/TESTDS", pid.toString());
        assertEquals(SC_UNAUTHORIZED, put(datastreamData, false)
                .getStatusCode());
        assertEquals(SC_OK, put(datastreamData, true).getStatusCode());

        // Export
        url = String.format("/objects/%s/export", pid.toString());
        assertEquals(SC_UNAUTHORIZED, get(false).getStatusCode());
        response = get(true);
        assertEquals(SC_OK, response.getStatusCode());
        responseXML = new String(response.responseBody, "UTF-8");
        assertTrue(responseXML.indexOf(label) > 0);
        assertTrue(responseXML.indexOf(datastreamData) > 0);

        // Purge datastream
        url = String.format("/objects/%s/datastreams/TESTDS", pid);
        assertEquals(SC_UNAUTHORIZED, delete(false).getStatusCode());
        assertEquals(SC_OK, delete(true).getStatusCode());

        // Purge object
        url = String.format("/objects/%s", pid);
        assertEquals(SC_UNAUTHORIZED, delete(false).getStatusCode());
        assertEquals(SC_OK, delete(true).getStatusCode());
    }

    public void testChunked() throws Exception {
        chunked = true;
        testIngest();
        testModifyDatastreamByValue();
        testModifyDatastreamNoContent();
        testLifecycle();
    }

    public void testResponseOverride() throws Exception {
        // Make request which returns error response
        url = String.format("/objects/%s", "BOGUS_PID");
        assertEquals(SC_INTERNAL_SERVER_ERROR, post("", true).getStatusCode());

        // With flash=true parameter response should be 200
        url = String.format("/objects/%s?flash=true", "BOGUS_PID");
        assertEquals(SC_OK, post("", true).getStatusCode());
    }

    // test correct content-disposition header on getDatastreamDissemination
    // Note that these tests are dependent on the following configuration in fedora.fcfg
    // Datastream filename sources: rels, label, id
    // Datastream extension preferences:
    // rels: never (filename always sourced from relationship)
    // label: always (filename always determined from mime-type to extension mapping)
    // id: ifmissing (filename sourced from mapping if none present in datastream id)

    @Test
    public void testDatastreamDisseminationContentDispositionFromRels()
            throws Exception {

        // filename from RELS-INT, no lookup of extension; no download
        url = "/objects/demo:REST/datastreams/DS1/content";
        HttpResponse response = get(getAuthAccess());
        assertEquals(SC_OK, response.getStatusCode());
        CheckCDHeader(response, "inline", TestRESTAPI.DS1RelsFilename);
        // again with download
        url = url + "?download=true";
        response = get(getAuthAccess());
        assertEquals(SC_OK, response.getStatusCode());
        CheckCDHeader(response, "attachment", TestRESTAPI.DS1RelsFilename);
    }

    @Test
    public void testDatastreamDisseminationContentDispositionFromLabel()
            throws Exception {

        // filename from label, known MIMETYPE
        url = "/objects/demo:REST/datastreams/DS2/content?download=true";
        HttpResponse response = get(getAuthAccess());
        assertEquals(SC_OK, response.getStatusCode());
        CheckCDHeader(response, "attachment", TestRESTAPI.DS2LabelFilename
                + ".jpg"); // jpg should be from MIMETYPE mapping

        // filename from label, unknown MIMETYPE
        url = "/objects/demo:REST/datastreams/DS3/content?download=true";
        response = get(getAuthAccess());
        assertEquals(SC_OK, response.getStatusCode());
        CheckCDHeader(response, "attachment", TestRESTAPI.DS3LabelFilename
                + ".bin"); // default extension from config

        // filename from label with illegal characters, known MIMETYPE
        url = "/objects/demo:REST/datastreams/DS4/content?download=true";
        response = get(getAuthAccess());
        assertEquals(SC_OK, response.getStatusCode());
        CheckCDHeader(response, "attachment", TestRESTAPI.DS4LabelFilename
                + ".xml"); // xml from mimetype mapping
    }

    @Test
    public void testDatastreamDisseminationContentDispositionFromId()
            throws Exception {

        // filename from id (no label present)
        url = "/objects/demo:REST/datastreams/DS5/content?download=true";
        HttpResponse response = get(getAuthAccess());
        assertEquals(SC_OK, response.getStatusCode());
        CheckCDHeader(response, "attachment", TestRESTAPI.DS5ID + ".xml"); // xml from mimetype mapping

        // filename from id, id contains extension (no label present)
        url = "/objects/demo:REST/datastreams/DS6.xml/content?download=true";
        response = get(getAuthAccess());
        assertEquals(SC_OK, response.getStatusCode());
        CheckCDHeader(response, "attachment", TestRESTAPI.DS6ID); // no extension, id contains it

    }

    @Test
    public void testUpload() throws Exception {
        String uploadUrl = "/upload";
        url = getBaseURL() + uploadUrl;

        HttpResponse response = _doUploadPost(url);
        if (response.getStatusCode() == SC_MOVED_TEMPORARILY) {
            url = response.getResponseHeader("Location").getValue();

            response = _doUploadPost(url);
        }

        assertEquals(202, response.getStatusCode());
        assertTrue(response.getResponseBodyString().startsWith("uploaded://"));

        // Test content not supplied
        PostMethod post = new PostMethod(url);
        MultipartRequestEntity entity = new MultipartRequestEntity(new Part[] {}, post.getParams());

        post.setRequestEntity(entity);
        getClient(true).executeMethod(post);
        response = new HttpResponse(post);

        assertEquals(500, response.getStatusCode());
    }

    private HttpResponse _doUploadPost(String path) throws Exception {
        PostMethod post = new PostMethod(path);
        File temp = File.createTempFile("test.txt", null);
        FileUtils.writeStringToFile(temp, "This is the upload test file");

        Part[] parts = {new FilePart("file", temp)};
        MultipartRequestEntity entity =
                new MultipartRequestEntity(parts, post.getParams());

        post.setRequestEntity(entity);
        getClient(true).executeMethod(post);
        return new HttpResponse(post);
    }

    // check content disposition header of response

    private void CheckCDHeader(HttpResponse response,
                               String expectedType,
                               String expectedFilename) {
        String contentDisposition = "";
        Header[] headers = response.responseHeaders;
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase("content-disposition")) {
                contentDisposition = header.getValue();
            }
        }
        assertEquals(expectedType + "; " + "filename=\"" + expectedFilename
                + "\"", contentDisposition);
    }

    // helper methods

    private HttpClient getClient(boolean auth) {
        HttpClient client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(auth);
        if (auth) {
            client.getState()
                    .setCredentials(new AuthScope(getHost(),
                                                  Integer.valueOf(_getPort()),
                                                  "realm"),
                                    new UsernamePasswordCredentials(getUsername(),
                                                                    getPassword()));
        }
        return client;
    }

    private String _getPort() {
        if (url != null && url.startsWith("https")) {
            return getServerConfiguration().getParameter("fedoraRedirectPort");
        }
        return getServerConfiguration().getParameter("fedoraServerPort");
    }

    /**
     * Issues an HTTP GET for the specified URL.
     *
     * @param authenticate
     * @return HttpResponse
     * @throws Exception
     */
    protected HttpResponse get(boolean authenticate) throws Exception {
        return getOrDelete("GET", authenticate);
    }

    protected HttpResponse delete(boolean authenticate) throws Exception {
        return getOrDelete("DELETE", authenticate);
    }

    /**
     * Issues an HTTP PUT to <code>url</code>. Callers are responsible for
     * calling releaseConnection() on the returned <code>HttpMethod</code>.
     *
     * @param authenticate
     * @return
     * @throws Exception
     */
    protected HttpResponse put(boolean authenticate) throws Exception {
        return putOrPost("PUT", null, authenticate);
    }

    protected HttpResponse put(String requestContent, boolean authenticate)
            throws Exception {
        return putOrPost("PUT", requestContent, authenticate);
    }

    protected HttpResponse post(String requestContent, boolean authenticate)
            throws Exception {
        return putOrPost("POST", requestContent, authenticate);
    }

    protected HttpResponse put(File requestContent, boolean authenticate)
            throws Exception {
        return putOrPost("PUT", requestContent, authenticate);
    }

    protected HttpResponse post(File requestContent, boolean authenticate)
            throws Exception {
        return putOrPost("POST", requestContent, authenticate);
    }

    private HttpResponse getOrDelete(String method, boolean authenticate)
            throws Exception {
        if (url == null || url.length() == 0) {
            throw new IllegalArgumentException("url must be a non-empty value");
        } else if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            url = getBaseURL() + url;
        }
        HttpMethod httpMethod = null;
        try {
            if (method.equals("GET")) {
                httpMethod = new GetMethod(url);
            } else if (method.equals("DELETE")) {
                httpMethod = new DeleteMethod(url);
            } else {
                throw new IllegalArgumentException("method must be one of GET or DELETE.");
            }
            httpMethod.setDoAuthentication(authenticate);
            httpMethod.setFollowRedirects(false);
            httpMethod.getParams().setParameter("Connection", "Keep-Alive");
            getClient(authenticate).executeMethod(httpMethod);
            HttpResponse response = new HttpResponse(httpMethod);

            if (response.getStatusCode() == SC_MOVED_TEMPORARILY) {
                String redir =
                        response.getResponseHeader("Location").getValue();
                if (redir != url) {
                    url = redir;
                    response = getOrDelete(method, authenticate);
                }
            }

            return response;
        } finally {
            if (httpMethod != null) {
                httpMethod.releaseConnection();
            }
        }
    }

    private HttpResponse putOrPost(String method,
                                   Object requestContent,
                                   boolean authenticate) throws Exception {

        if (url == null || url.length() == 0) {
            throw new IllegalArgumentException("url must be a non-empty value");
        } else if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            url = getBaseURL() + url;
        }

        EntityEnclosingMethod httpMethod = null;
        try {
            if (method.equals("PUT")) {
                httpMethod = new PutMethod(url);
            } else if (method.equals("POST")) {
                httpMethod = new PostMethod(url);
            } else {
                throw new IllegalArgumentException("method must be one of PUT or POST.");
            }
            httpMethod.setDoAuthentication(authenticate);
            httpMethod.getParams().setParameter("Connection", "Keep-Alive");
            if (requestContent != null) {
                httpMethod.setContentChunked(chunked);
                if (requestContent instanceof String) {
                    httpMethod
                            .setRequestEntity(new StringRequestEntity((String) requestContent,
                                                                      "text/xml",
                                                                      "utf-8"));
                } else if (requestContent instanceof File) {
                    Part[] parts =
                            {
                                    new StringPart("param_name", "value"),
                                    new FilePart(((File) requestContent).getName(),
                                                 (File) requestContent)};
                    httpMethod
                            .setRequestEntity(new MultipartRequestEntity(parts,
                                                                         httpMethod
                                                                                 .getParams()));
                } else {
                    throw new IllegalArgumentException("requestContent must be a String or File");
                }
            }
            getClient(authenticate).executeMethod(httpMethod);

            HttpResponse response = new HttpResponse(httpMethod);

            if (response.getStatusCode() == SC_MOVED_TEMPORARILY) {
                String redir =
                        response.getResponseHeader("Location").getValue();
                if (redir != url) {
                    url = redir;
                    response = putOrPost(method, requestContent, authenticate);
                }
            }

            return response;
        } finally {
            if (httpMethod != null) {
                httpMethod.releaseConnection();
            }
        }
    }

    // Supports legacy test runners

    public static junit.framework.Test suite() {
        TestSuite suite = new TestSuite("REST API TestSuite");
        suite.addTestSuite(TestRESTAPI.class);
        return new DemoObjectTestSetup(suite);
    }

    class HttpResponse {

        private final int statusCode;

        private final byte[] responseBody;

        private final Header[] responseHeaders;

        private final Header[] responseFooters;

        HttpResponse(int status, byte[] body, Header[] headers, Header[] footers) {
            statusCode = status;
            responseBody = body;
            responseHeaders = headers;
            responseFooters = footers;
        }

        HttpResponse(HttpMethod method)
                throws IOException {
            statusCode = method.getStatusCode();
            //responseBody = method.getResponseBody();
            responseBody =
                    IOUtils.toByteArray(method.getResponseBodyAsStream());
            responseHeaders = method.getResponseHeaders();
            responseFooters = method.getResponseFooters();
        }

        public int getStatusCode() {
            return statusCode;
        }

        public byte[] getResponseBody() {
            return responseBody;
        }

        public String getResponseBodyString() {
            try {
                return new String(responseBody, "UTF-8");
            } catch (UnsupportedEncodingException wontHappen) {
                throw new Error(wontHappen);
            }
        }

        public Header[] getResponseHeaders() {
            return responseHeaders;
        }

        public Header[] getResponseFooters() {
            return responseFooters;
        }

        public Header getResponseHeader(String headerName) {
            for (Header header : responseHeaders) {
                if (header.getName().equalsIgnoreCase(headerName)) {
                    return header;
                }
            }
            return null;
        }
    }
}
