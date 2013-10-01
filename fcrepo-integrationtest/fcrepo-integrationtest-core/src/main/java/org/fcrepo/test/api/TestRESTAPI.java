/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.test.api;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.JUnit4TestAdapter;

import org.antlr.stringtemplate.StringTemplate;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.fcrepo.client.FedoraClient;
import org.fcrepo.common.Constants;
import org.fcrepo.common.Models;
import org.fcrepo.common.PID;
import org.fcrepo.server.access.FedoraAPIAMTOM;
import org.fcrepo.server.management.FedoraAPIMMTOM;
import org.fcrepo.server.types.gen.Datastream;
import org.fcrepo.server.types.gen.FieldSearchQuery;
import org.fcrepo.server.types.gen.FieldSearchResult;
import org.fcrepo.server.types.gen.ObjectFields;
import org.fcrepo.server.types.mtom.gen.MIMETypedStream;
import org.fcrepo.server.utilities.TypeUtility;
import org.fcrepo.test.FedoraServerTestCase;
import org.jrdf.graph.Triple;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trippi.RDFFormat;
import org.trippi.TripleIterator;
import org.trippi.TrippiException;
import org.trippi.io.TripleIteratorFactory;


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
    
    private static final String TEXT_XML = "text/xml";

    private static final Logger LOGGER =
        LoggerFactory.getLogger(TestRESTAPI.class);

    private static FedoraClient s_client;
    
    private FedoraAPIAMTOM apia;

    private FedoraAPIMMTOM apim;

    // used for determining test configuration
    private static String authAccessProperty = "fedora.authorize.access";

    protected Boolean authorizeAccess = null;

    private static String DEMO_OWNERID = "nondefaultOwner";

    private static String DEMO_REST;

    private static byte[] DEMO_REST_FOXML;

    private static String DEMO_MIN;

    private static String DEMO_MIN_PID;

    private final static PID DEMO_REST_PID = PID.getInstance("demo:REST");

    private static final String REST_RESOURCE_PATH =
            System.getProperty("fcrepo-integrationtest-core.classes") != null ? System
                    .getProperty("fcrepo-integrationtest-core.classes")
                    + "rest"
                    : "src/test/resources/rest";

    private static String DS1RelsFilename =
            "Datastream 1 filename from rels.extension";

    private static String DS2LabelFilename = "Datastream 2 filename from label";

    private static String DS3LabelFilename = "Datastream 3 filename from label";

    private static String DodgyChars = "\\/*?&lt;&gt;:|";

    private static String DS4LabelFilenameOriginal =
            "Datastream 4 filename " + DodgyChars + "from label";

    // this one in foxml
    private static String DS4LabelFilename = "Datastream 4 filename from label"; // this should be the cleaned version

    private static String DS5ID = "DS5";

    private static String DS6ID = "DS6.xml";

    // note: since we are explicitly formatting the date as "Z" (literal), need to ensure the formatter operates in GMT/UTC
    static SimpleDateFormat df;
    static {
        df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    
    private static final ValidatorHelper validator =
            new ValidatorHelper();

    private static final Date now = new Date();

    private static final String datetime = df.format(now);

    // tests for this must not be dependent on which object, so we will
    // use a constant that predates the system objects
    private static final String earlierDateTime =
            "2001-01-01T00:00:00.000Z";

    @SuppressWarnings("unused")
    private boolean chunked = false;
    
    protected static FedoraClient initClient() throws Exception {
        s_client = getFedoraClient();
        return s_client;
    }
    
    protected static void stopClient() {
        s_client.shutdown();
        s_client = null;
    }

    @BeforeClass
    public static void bootStrap() throws Exception {
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("management",
                  "http://www.fedora.info/definitions/1/0/management/");
        nsMap.put("access", "http://www.fedora.info/definitions/1/0/access/");
        NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
        XMLUnit.setXpathNamespaceContext(ctx);

        initClient();

        ingestImageManipulationDemoObjects(s_client);
        ingestDocumentTransformDemoObjects(s_client);
        
        if (DEMO_MIN == null) DEMO_MIN =
                FileUtils.readFileToString(new File(REST_RESOURCE_PATH
                        + "/demo_min.xml"), "UTF-8");
        if (DEMO_MIN_PID == null) DEMO_MIN_PID =
                FileUtils.readFileToString(new File(REST_RESOURCE_PATH
                        + "/demo_min_pid.xml"), "UTF-8");
        if (DEMO_REST == null) {
            StringTemplate tpl =
                    new StringTemplate(FileUtils
                                       .readFileToString(new File(REST_RESOURCE_PATH
                                                                  + "/demo_rest.xml"), "UTF-8"));
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
            DEMO_REST_FOXML = DEMO_REST.getBytes("UTF-8");
        }
    }
    
    @AfterClass
    public static void cleanUp() throws Exception {
        purgeDemoObjects(s_client);
        stopClient();
        XMLUnit.setXpathNamespaceContext(SimpleNamespaceContext.EMPTY_CONTEXT);
    }

    @Before
    public void setUp() throws Exception {
        apia = s_client.getAPIAMTOM();
        apim = s_client.getAPIMMTOM();
        apim.ingest(TypeUtility.convertBytesToDataHandler(DEMO_REST_FOXML),
                    FOXML1_1.uri, "TestRESTAPI.setUp: ingesting new foxml object " + DEMO_REST_PID);
    }

    @After
    public void tearDown() throws Exception {
        apim.purgeObject(DEMO_REST_PID.toString(), "TestRESTAPI.tearDown: purging " + DEMO_REST_PID, false);
    }
    
    protected static byte[] readBytes(HttpResponse response) {
        byte[] body = new byte[0];
        if (response.getEntity() != null) {
            try {
                body = EntityUtils.toByteArray(response.getEntity());
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return body;
    }

    protected static String readString(HttpResponse response) {
        return readString(response, Charset.forName("UTF-8"));
    }
    
    protected static String readString(HttpResponse response, Charset charset) {
        String body = null;
        if (response.getEntity() != null) {
            try {
                body = EntityUtils.toString(response.getEntity(), charset);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return (body != null) ? body : "";
    }

    /**
     * Do Access requests require auth based on current test configuration?
     *
     * @return true if auth is required for API-requests
     */
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
    
    protected void verifyDELETEStatusOnly(URI url, int expected, boolean authenticate) throws Exception {
        HttpDelete get = new HttpDelete(url);
        HttpResponse response = getOrDelete(get, authenticate, false);
        int status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(expected, status);        
    }

    protected void verifyGETStatusOnly(URI url, int expected, boolean authenticate, boolean validate) throws Exception {
        HttpGet get = new HttpGet(url);
        HttpResponse response = getOrDelete(get, authenticate, validate);
        int status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(expected, status);        
    }

    protected void verifyGETStatusOnly(URI url, int expected, boolean validate) throws Exception {
        verifyGETStatusOnly(url, expected, getAuthAccess(), validate);        
    }

    protected void verifyGETStatusOnly(URI url, int expected) throws Exception {
        verifyGETStatusOnly(url, expected, false);        
    }
    
    protected String verifyGETStatusString(URI url, int expected,
            boolean authenticate, boolean validate)
            throws Exception {
        HttpGet get = new HttpGet(url);
        HttpResponse response = getOrDelete(get, authenticate, validate);
        int status = response.getStatusLine().getStatusCode();
        String result = readString(response);
        assertEquals(expected, status);      
        return result;
    }

    protected byte[] verifyGETStatusBytes(URI url, int expected,
            boolean authenticate, boolean validate)
            throws Exception {
        HttpGet get = new HttpGet(url);
        HttpResponse response = getOrDelete(get, authenticate, validate);
        int status = response.getStatusLine().getStatusCode();
        byte[] result = readBytes(response);
        assertEquals(expected, status);      
        return result;
    }
    
    protected static StringEntity getStringEntity(
        String content, String contentType) {
        if (content == null) {
            return null;
        }
        
        StringEntity entity =
            new StringEntity(content, Charset.forName("UTF-8"));
        if (contentType != null) {
            entity.setContentType(contentType);
        }
        return entity;
    }
    
    protected void verifyPOSTStatusOnly(URI url, int expected,
        StringEntity content, boolean authenticate) throws Exception {
        HttpPost post = new HttpPost(url);
        HttpResponse response = putOrPost(post, content, authenticate);
        int status = response.getStatusLine().getStatusCode();
        if (status == SC_MOVED_TEMPORARILY) {
            String original = url.toString();
            url = URI.create(response.getFirstHeader(HttpHeaders.LOCATION).getValue());
            if (!original.equals(url.toString())) {
                EntityUtils.consumeQuietly(response.getEntity());
                post = new HttpPost(url);
                response = putOrPost(post, content, true);
                status = response.getStatusLine().getStatusCode();
            }
        }
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(expected, status);        
    }

    protected void verifyPOSTStatusOnly(URI url, int expected,
            StringEntity content, boolean authenticate, boolean validate) throws Exception {
            HttpPost post = new HttpPost(url);
            HttpResponse response = putOrPost(post, content, authenticate);
            int status = response.getStatusLine().getStatusCode();
            if (status == SC_MOVED_TEMPORARILY) {
                String original = url.toString();
                url = URI.create(response.getFirstHeader(HttpHeaders.LOCATION).getValue());
                if (!original.equals(url.toString())) {
                    EntityUtils.consumeQuietly(response.getEntity());
                    post = new HttpPost(url);
                    response = putOrPost(post, content, true);
                    status = response.getStatusLine().getStatusCode();
                }
            }
            if (validate) {
                validateResponse(url, response);
            } else {
                EntityUtils.consumeQuietly(response.getEntity());
            }
            assertEquals(expected, status);        
        }

    protected void verifyPUTStatusOnly(URI url, int expected,
            StringEntity content, boolean authenticate) throws Exception {
            HttpPut put = new HttpPut(url);
            HttpResponse response = putOrPost(put, content, authenticate);
            int status = response.getStatusLine().getStatusCode();
            if (status == SC_MOVED_TEMPORARILY) {
                String original = url.toString();
                url = URI.create(response.getFirstHeader(HttpHeaders.LOCATION).getValue());
                if (!original.equals(url.toString())) {
                    EntityUtils.consumeQuietly(response.getEntity());
                    put = new HttpPut(url);
                    response = putOrPost(put, content, true);
                    status = response.getStatusLine().getStatusCode();
                }
            }
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(expected, status);        
        }

    protected void verifyNoAuthFailOnAPIAAuth(URI url) throws Exception {
        if (this.getAuthAccess()) {
            HttpGet get = new HttpGet(url);
            HttpResponse response = getOrDelete(get, false, false);
            int status = response.getStatusLine().getStatusCode();
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(SC_UNAUTHORIZED, status);
        }
    }

    @Test
    public void testGetWADL() throws Exception {
        URI url = getURI("/objects/application.wadl");

        verifyNoAuthFailOnAPIAAuth(url);
        verifyGETStatusOnly(url, SC_OK);
    }

    //public void testDescribeRepository() throws Exception {}

    // API-A

    @Test
    public void testGetObjectProfile() throws Exception {
        URI url = getURI(String.format("/objects/%s", DEMO_REST_PID.toString()));
        verifyNoAuthFailOnAPIAAuth(url);
        HttpGet get = new HttpGet(url);
        HttpResponse response;
        int status = 0;
        response = getOrDelete(get, getAuthAccess(), false);
        status = response.getStatusLine().getStatusCode();
        assertEquals(SC_OK, status);

        url = getURI(String.format("/objects/%s?format=xml", DEMO_REST_PID.toString()));
        verifyNoAuthFailOnAPIAAuth(url);
        get = new HttpGet(url);
        response = getOrDelete(get, getAuthAccess(), true);
        status = response.getStatusLine().getStatusCode();
        String responseXML = readString(response);
        assertEquals(SC_OK, status);
        assertTrue(responseXML.contains("<objLabel>"));
        assertTrue(responseXML.contains("<objOwnerId>"));
        assertTrue(responseXML.contains("<objCreateDate>"));
        assertTrue(responseXML.contains("<objLastModDate>"));
        assertTrue(responseXML.contains("<objDissIndexViewURL>"));
        assertTrue(responseXML.contains("<objItemIndexViewURL>"));
        assertTrue(responseXML.contains("<objState>"));

        url = getURI(
                String.format("/objects/%s?asOfDateTime=%s",
                              DEMO_REST_PID.toString(),
                              datetime));
        verifyNoAuthFailOnAPIAAuth(url);
        verifyGETStatusOnly(url, SC_OK);

        // sanity check
        url = getURI(String.format("/objects/%s", "demo:BOGUS_PID"));
        verifyNoAuthFailOnAPIAAuth(url);
        verifyGETStatusOnly(url, SC_NOT_FOUND);
    }

    @Test
    public void testListMethods() throws Exception {
        URI url = getURI(String.format("/objects/%s/methods", DEMO_REST_PID.toString()));
        verifyNoAuthFailOnAPIAAuth(url);
        verifyGETStatusOnly(url, SC_OK);

        url = getURI(
                String.format("/objects/%s/methods?format=xml", DEMO_REST_PID.toString()));
        verifyNoAuthFailOnAPIAAuth(url);
        verifyGETStatusOnly(url, SC_OK);

        url = getURI(
                String.format("/objects/%s/methods?asOfDateTime=%s", DEMO_REST_PID
                        .toString(), datetime));
        verifyNoAuthFailOnAPIAAuth(url);
        verifyGETStatusOnly(url, SC_OK);
    }

    @Test
    public void testListMethodsForSDep() throws Exception {
        URI url = getURI(
            String.format("/objects/%s/methods/fedora-system:3",
                DEMO_REST_PID.toString()));
        verifyNoAuthFailOnAPIAAuth(url);
        HttpGet get = new HttpGet(url);
        HttpResponse response;
        int status = 0;
        get = new HttpGet(url);
        response = getOrDelete(get, getAuthAccess(), false);
        status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_OK, status);

        url = getURI(
                String.format("/objects/%s/methods/fedora-system:3?format=xml",
                    DEMO_REST_PID.toString()));
        verifyNoAuthFailOnAPIAAuth(url);
        get = new HttpGet(url);
        response = getOrDelete(get, getAuthAccess(), true);
        assertEquals(SC_OK, response.getStatusLine().getStatusCode());
        String responseXML = readString(response);
        assertTrue(responseXML.contains("sDef=\"fedora-system:3\""));

        url = getURI(
            String.format("/objects/%s/methods/fedora-system:3?asOfDateTime=%s",
                DEMO_REST_PID.toString(),
                datetime));
        verifyNoAuthFailOnAPIAAuth(url);
        get = new HttpGet(url);
        response = getOrDelete(get, getAuthAccess(), false);
        status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_OK, status);
    }

    //
    // GETs on built-in Service Definition methods
    //

    @Test
    public void testGETMethodBuiltInBadMethod() throws Exception {
        URI url = getURI(
            String.format("/objects/%s/methods/fedora-system:3/noSuchMethod",
            DEMO_REST_PID.toString()));
        verifyNoAuthFailOnAPIAAuth(url);
        HttpGet get = new HttpGet(url);
        HttpResponse response;
        int status = 0;
        response = getOrDelete(get, getAuthAccess(), false);
        status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertFalse(SC_OK == status);
    }

    @Test
    public void testGETMethodBuiltInBadUserArg() throws Exception {
        URI url = getURI(
            String.format("/objects/%s/methods/fedora-system:3/viewMethodIndex?noSuchArg=foo",
            DEMO_REST_PID.toString()));
        verifyNoAuthFailOnAPIAAuth(url);
        HttpGet get = new HttpGet(url);
        HttpResponse response = getOrDelete(get, getAuthAccess(), false);
        int status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertFalse(SC_OK == status);
    }

    @Test
    public void testGETMethodBuiltInNoArg() throws Exception {
        URI url = getURI(
            String.format("/objects/%s/methods/fedora-system:3/viewMethodIndex",
            DEMO_REST_PID.toString()));
        verifyNoAuthFailOnAPIAAuth(url);
        HttpGet get = new HttpGet(url);
        HttpResponse response;
        int status = 0;
        response = getOrDelete(get, getAuthAccess(), false);
        status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_OK, status);
    }

    //
    // GETs on custom Service Definition methods
    //

    @Test
    public void testGETMethodCustomBadMethod() throws Exception {
        URI url = getURI("/objects/demo:14/methods/demo:12/noSuchMethod");
        verifyNoAuthFailOnAPIAAuth(url);
        HttpGet get = new HttpGet(url);
        HttpResponse response;
        int status = 0;
        response = getOrDelete(get, getAuthAccess(), false);
        status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertFalse(SC_OK == status);
    }

    @Test
    public void testGETMethodCustomBadUserArg() throws Exception {
        URI url = getURI(
                "/objects/demo:14/methods/demo:12/getDocumentStyle1?noSuchArg=foo");
        verifyNoAuthFailOnAPIAAuth(url);
        HttpGet get = new HttpGet(url);
        HttpResponse response = getOrDelete(get, getAuthAccess(), false);
        int status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertFalse(SC_OK == status);
    }

    @Test
    public void testGETMethodCustomNoArg() throws Exception {
        URI url = getURI("/objects/demo:14/methods/demo:12/getDocumentStyle1");
        verifyNoAuthFailOnAPIAAuth(url);
        HttpGet get = new HttpGet(url);
        HttpResponse response;
        int status = 0;
        response = getOrDelete(get, getAuthAccess(), false);
        status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_OK, status);
    }

    @Test
    public void testGETMethodCustomGoodUserArg() throws Exception {
        URI url = getURI("/objects/demo:29/methods/demo:27/resizeImage?width=50");
        verifyNoAuthFailOnAPIAAuth(url);
        HttpGet get = new HttpGet(url);
        HttpResponse response;
        int status = 0;
        response = getOrDelete(get, getAuthAccess(), true);
        status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_OK, status);
        // imageManip service does not return content length
        //assertEquals(1486, response.getResponseBody().length);
        //assertEquals("1486", response.getResponseHeader("Content-Length").getValue());
        assertEquals("image/jpeg", response.getFirstHeader(HttpHeaders.CONTENT_TYPE)
                .getValue());
    }

    @Test
    public void testGETMethodCustomGoodUserArgGoodDate() throws Exception {
        URI url = getURI(
                "/objects/demo:29/methods/demo:27/resizeImage?width=50&asOfDateTime="
                        + datetime);
        verifyNoAuthFailOnAPIAAuth(url);
        HttpGet get = new HttpGet(url);
        HttpResponse response;
        int status = 0;
        response = getOrDelete(get, getAuthAccess(), true);
        status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_OK, status);
        // imageManip service does not return content length
        //assertEquals(1486, response.getResponseBody().length);
        //assertEquals("1486", response.getResponseHeader("Content-Length").getValue());
        assertEquals("image/jpeg", response.getFirstHeader(HttpHeaders.CONTENT_TYPE)
                .getValue());
    }

    @Test
    public void testGETMethodCustomUserArgBadDate() throws Exception {
        URI url = getURI(
                "/objects/demo:14/methods/demo:12/getDocumentStyle1?width=50&asOfDateTime=badDate");
        verifyNoAuthFailOnAPIAAuth(url);
        HttpGet get = new HttpGet(url);
        HttpResponse response;
        int status = 0;
        response = getOrDelete(get, getAuthAccess(), false);
        status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertFalse(SC_OK == status);
    }

   @Test
   public void testGETMethodCustomUserArgEarlyDate() throws Exception {
        URI url = getURI(
                "/objects/demo:14/methods/demo:12/getDocumentStyle1?width=50&asOfDateTime=1999-11-21T16:38:32.200Z");
        verifyNoAuthFailOnAPIAAuth(url);
        HttpGet get = new HttpGet(url);
        HttpResponse response;
        int status = 0;
        response = getOrDelete(get, getAuthAccess(), false);
        status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_NOT_FOUND, status);
    }

   @Test
    public void testGETMethodCustomGoodDate() throws Exception {
        URI url = getURI(
                "/objects/demo:14/methods/demo:12/getDocumentStyle1?asOfDateTime="
                        + datetime);
        verifyNoAuthFailOnAPIAAuth(url);
        HttpGet get = new HttpGet(url);
        HttpResponse response;
        int status = 0;
        response = getOrDelete(get, getAuthAccess(), true);
        status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_OK, status);
    }

   @Test
    public void testGETMethodCustomBadDate() throws Exception {
        URI url = getURI(
                "/objects/demo:14/methods/demo:12/getDocumentStyle1?asOfDateTime=badDate");
        verifyNoAuthFailOnAPIAAuth(url);
        HttpGet get = new HttpGet(url);
        HttpResponse response;
        int status = 0;
        response = getOrDelete(get, getAuthAccess(), false);
        status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertFalse(SC_OK == status);
    }

   @Test
    public void testGETMethodCustomEarlyDate() throws Exception {
        URI url = getURI(
                "/objects/demo:14/methods/demo:12/getDocumentStyle1?asOfDateTime=1999-11-21T16:38:32.200Z");
        verifyNoAuthFailOnAPIAAuth(url);
        HttpGet get = new HttpGet(url);
        HttpResponse response;
        int status = 0;
        response = getOrDelete(get, getAuthAccess(), false);
        status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_NOT_FOUND, status);
    }

   @Test
    public void testListDatastreams() throws Exception {
        URI url = getURI(String.format("/objects/%s/datastreams", DEMO_REST_PID.toString()));
        verifyNoAuthFailOnAPIAAuth(url);
        verifyGETStatusOnly(url, SC_OK, true, true);

        url = getURI(
                String.format("/objects/%s/datastreams?format=xml", DEMO_REST_PID
                        .toString()));
        verifyNoAuthFailOnAPIAAuth(url);
        verifyGETStatusOnly(url, SC_OK, true, true);


        url = getURI(
                String.format("/objects/%s/datastreams?asOfDateTime=%s", DEMO_REST_PID
                        .toString(), datetime));
        verifyNoAuthFailOnAPIAAuth(url);
        verifyGETStatusOnly(url, SC_OK, true, true);

        url = getURI(
                String.format("/objects/%s/datastreams?format=xml&profiles=true", DEMO_REST_PID
                        .toString()));
        // this will always require authN with the profiles option on,
        // as it is an apim function
        verifyGETStatusOnly(url, SC_UNAUTHORIZED, false, false);
        String responseString = verifyGETStatusString(url, SC_OK, true, true);
        assertTrue(responseString.indexOf("datastreamProfile") > -1);
    }

    // if an object is marked as deleted, only administrators have access (default XACML policy)
    // see FCREPO-753: this also tests that preemptive auth can still take place when api-a auth is not required
   @Test
    public void testDeletedObject() throws Exception {
        // only test if api-a auth is disabled (configA)
        if (!this.getAuthAccess()) {

            // mark object as deleted
            String modResponse =
                    apim.modifyObject(DEMO_REST_PID.toString(),
                              "D",
                              null,
                              null,
                              "Mark object as deleted");

            LOGGER.info(modResponse);
            // verify no unauth access
            URI url = getURI(
                    String.format("/objects/%s/datastreams", DEMO_REST_PID.toString()));
            LOGGER.info("Testing object marked deleted at {}", url);
            verifyGETStatusString(url, SC_UNAUTHORIZED, false, false);
            verifyGETStatusString(url, SC_OK, true, true);
        }

    }

   @Test
    public void testGetDatastreamProfile() throws Exception {
        // Datastream profile in HTML format
        URI url = getURI(
                String.format("/objects/%s/datastreams/RELS-EXT", DEMO_REST_PID.toString()));
        verifyGETStatusString(url, SC_UNAUTHORIZED, false, false);
        String responseXML = verifyGETStatusString(url, SC_OK, true, true);
        assertTrue(responseXML.contains("<html>"));

        // Datastream profile in XML format
        url = getURI(
                String.format("/objects/%s/datastreams/RELS-EXT?format=xml",
                              DEMO_REST_PID.toString()));
        verifyGETStatusString(url, SC_UNAUTHORIZED, false, false);
        responseXML = verifyGETStatusString(url, SC_OK, true, true);
        assertTrue(responseXML.contains("<dsLabel>"));

        // Datastream profile as of the current date-time (XML format)
        url = getURI(
                String
                        .format("/objects/%s/datastreams/RELS-EXT?asOfDateTime=%s&format=xml",
                                DEMO_REST_PID.toString(),
                                datetime));
        verifyGETStatusString(url, SC_UNAUTHORIZED, false, false);
        // FIXME: validation disabled
        // response is currently not schema-valid, see fcrepo-866, fcrepo-612
        // -> get(true) once fixed to enable validation
        responseXML = verifyGETStatusString(url, SC_OK, true, false);
        assertTrue(responseXML.contains("<dsLabel>"));

        // sanity check
        url = getURI(
                String.format("/objects/%s/datastreams/BOGUS_DSID", DEMO_REST_PID
                        .toString()));
        verifyGETStatusString(url, SC_UNAUTHORIZED, false, false);
        verifyGETStatusString(url, SC_NOT_FOUND, true, true);
    }

   @Test
    public void testGetDatastreamHistory() throws Exception {

        // Ingest minimal object
        URI url = getURI("/objects/new");
        StringEntity entity = getStringEntity(DEMO_MIN_PID, TEXT_XML);
        verifyPOSTStatusOnly(url, SC_UNAUTHORIZED, entity, false);
        verifyPOSTStatusOnly(url, SC_CREATED, entity, true);

        // Get history in XML format
        url =
              getURI(String
                        .format("/objects/demo:1234/datastreams/DS1/history?format=xml"));
        verifyNoAuthFailOnAPIAAuth(url);
        
        String responseXML = verifyGETStatusString(url, SC_OK, true, true);

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
        url = getURI("/objects/demo:1234/datastreams/BOGUS_DSID");
        verifyGETStatusString(url, SC_UNAUTHORIZED, false, false);
        // APIM function requires authentication
        verifyGETStatusOnly(url, SC_NOT_FOUND, true, false);

        // Clean up
        url = getURI("/objects/demo:1234");
        verifyDELETEStatusOnly(url, SC_UNAUTHORIZED, false);
        verifyDELETEStatusOnly(url, SC_OK, true);
    }

   @Test
    public void testGetDatastreamDissemination() throws Exception {
        URI url =
                getURI(String.format("/objects/%s/datastreams/RELS-EXT/content", DEMO_REST_PID
                        .toString()));
        verifyNoAuthFailOnAPIAAuth(url);
        HttpGet get = new HttpGet(url);
        HttpResponse response = getOrDelete(get, getAuthAccess(), false);
        int status = response.getStatusLine().getStatusCode();
        Header length = response.getFirstHeader(HttpHeaders.CONTENT_LENGTH);
        String responseXML = readString(response);
        assertEquals(SC_OK, status);
        assertNotNull(length);
        assertNotNull(length.getValue());
        long lengthlong = Long.parseLong(length.getValue());
        assertTrue(lengthlong > 0);
        assertTrue(responseXML.contains("rdf:Description"));

        url =
                getURI(String
                        .format("/objects/%s/datastreams/RELS-EXT/content?asOfDateTime=%s",
                                DEMO_REST_PID.toString(),
                                datetime));
        verifyNoAuthFailOnAPIAAuth(url);
        response = getOrDelete(get, getAuthAccess(), false);
        status = response.getStatusLine().getStatusCode();
        length = response.getFirstHeader(HttpHeaders.CONTENT_LENGTH);
        responseXML = readString(response);
        assertEquals(SC_OK, status);
        assertNotNull(length);
        assertNotNull(length.getValue());
        lengthlong = Long.parseLong(length.getValue());
        assertTrue(lengthlong > 0);

        assertTrue(responseXML.contains("rdf:Description"));

        // sanity check
        url =
                getURI(String.format("/objects/%s/datastreams/BOGUS_DSID/content", DEMO_REST_PID
                        .toString()));
        verifyNoAuthFailOnAPIAAuth(url);
        verifyGETStatusString(url, SC_NOT_FOUND, getAuthAccess(), false);
    }

   @Test
    public void testFindObjects() throws Exception {
        URI url =
                getURI(String
                        .format("/objects?pid=true&terms=%s&query=&resultFormat=xml",
                                DEMO_REST_PID.toString()));
        verifyNoAuthFailOnAPIAAuth(url);
        // FIXME: findObjects should have a schema?  remove "false" to enable validation
        verifyGETStatusOnly(url, SC_OK, false);
    }

    /**
     * test case for FCREPO-867. Since all SQL statements have been switched to
     * PreparedStatement it's safe to use a singlequote in a search query.
     * @throws Exception
     */
   @Test
    public void testFindObjectWithSingleQuote() throws Exception {
        URI url =
                getURI(String
                        .format("/objects?pid=true&description=true&terms='Coliseum'&query&maxResults=20&resultFormat=xml",
                                DEMO_REST_PID.toString()));
        verifyNoAuthFailOnAPIAAuth(url);
        // FIXME: findObjects should have a schema?  remove "false" to enable validation
        String xmlResult = verifyGETStatusString(url, SC_OK, getAuthAccess(), false);
//        System.out.println("ResponseBody: " + xmlResult);
        assertTrue(xmlResult.indexOf("<pid>demo:REST</pid>") > 0);
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

   @Test
    public void testResumeFindObjects() throws Exception {
       // there are only the system objects and 8 demo objects, so maxResults must be constrained
        URI url = getURI("/objects?pid=true&query=&resultFormat=xml&maxResults=4");
        verifyNoAuthFailOnAPIAAuth(url);
        // FIXME: resumeFindObjects should have a schema?  remove "false" to enable validation
        String responseXML = verifyGETStatusString(url, SC_OK, getAuthAccess(), false);
        String sessionToken =
                responseXML.substring(responseXML.indexOf("<token>") + 7,
                                      responseXML.indexOf("</token>"));

        url =
                getURI(String
                        .format("/objects?pid=true&query=&resultFormat=xml&sessionToken=%s",
                                sessionToken));
        verifyNoAuthFailOnAPIAAuth(url);
        verifyGETStatusOnly(url, SC_OK);
    }

   @Test
    public void testFindObjectsBadSyntax() throws Exception {
        URI url = getURI("/objects?pid=true&query=label%3D%3F&maxResults=20");
        // Try > 100 times; will hang if the connection isn't properly released
        for (int i = 0; i < 101; i++) {
            verifyGETStatusOnly(url, SC_INTERNAL_SERVER_ERROR, false);
        }
        
    }

   @Test
    public void testGetObjectHistory() throws Exception {
        URI url = getURI(String.format("/objects/%s/versions", DEMO_REST_PID.toString()));
        verifyNoAuthFailOnAPIAAuth(url);
        verifyGETStatusOnly(url, SC_OK);

        url = getURI(String.format("/objects/%s/versions?format=xml", DEMO_REST_PID.toString()));
        verifyNoAuthFailOnAPIAAuth(url);
        verifyGETStatusOnly(url, SC_OK);
    }

    private String extractPid(String source) {
        Matcher m = Pattern.compile("^.*/([^/]+$)").matcher(source);
        String pid = null;
        if (m.find() && m.groupCount() == 1) {
            pid = m.group(1);
        }
        pid = pid.replaceAll("\n", "").replaceAll("\r", "").replaceAll("%3A", ":");
        return pid;
    }

    // API-M

    @Test
    public void testIngest() throws Exception {
        // Create new empty object
        URI url = getURI(String.format("/objects/new"));
        StringEntity entity = getStringEntity("", TEXT_XML);
        verifyPOSTStatusOnly(url, SC_UNAUTHORIZED, entity, false);
        HttpPost post = new HttpPost(url);
        HttpResponse response = putOrPost(post, entity, true);

        String responseBody = readString(response);
        assertEquals(SC_CREATED, response.getStatusLine().getStatusCode());

        String emptyObjectPid =
                extractPid(response.getFirstHeader(HttpHeaders.LOCATION).getValue());
        assertNotNull(emptyObjectPid);
        // PID should be returned as a header and as the response body
        assertTrue(responseBody.equals(emptyObjectPid));

        // Delete empty object
        url = getURI(String.format("/objects/%s", emptyObjectPid));
        verifyDELETEStatusOnly(url, SC_UNAUTHORIZED, false);
        verifyDELETEStatusOnly(url, SC_OK, true);

        // Ensure that GETs of the deleted object immediately give 404s (See FCREPO-594)
        verifyGETStatusOnly(url, SC_NOT_FOUND);

        // Create new empty object with a PID namespace specified
        url = getURI(String.format("/objects/new?namespace=test"));
        verifyPOSTStatusOnly(url, SC_UNAUTHORIZED, entity, false);
        post = new HttpPost(url);
        response = putOrPost(post, entity, true);

        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_CREATED, response.getStatusLine().getStatusCode());

        emptyObjectPid =
                extractPid(response.getFirstHeader(HttpHeaders.LOCATION).getValue());
        assertTrue(emptyObjectPid.startsWith("test"));

        // Delete empty "test" object
        url = getURI(String.format("/objects/%s", emptyObjectPid));
        verifyDELETEStatusOnly(url, SC_UNAUTHORIZED, false);
        verifyDELETEStatusOnly(url, SC_OK, true);

        // Delete the demo:REST object (ingested as part of setup)
        url = getURI(String.format("/objects/%s", DEMO_REST_PID.toString()));
        verifyDELETEStatusOnly(url, SC_UNAUTHORIZED, false);
        verifyDELETEStatusOnly(url, SC_OK, true);

        // Create a new empty demo:REST object with parameterized ownerId
        url = getURI(String.format("/objects/%s?ownerId=%s", DEMO_REST_PID.toString(), DEMO_OWNERID));
        verifyPOSTStatusOnly(url, SC_UNAUTHORIZED, null, false);
        post = new HttpPost(url);
        response = putOrPost(post, entity, true);

        responseBody = readString(response);
        assertEquals(SC_CREATED, response.getStatusLine().getStatusCode());
        Header locationHeader = response.getFirstHeader(HttpHeaders.LOCATION);
        assertNotNull(locationHeader);
        assertTrue(locationHeader.getValue().contains(URLEncoder.encode(DEMO_REST_PID
                .toString(), "UTF-8")));
        assertTrue(responseBody.equals(DEMO_REST_PID.toString()));
        // verify ownerId
        responseBody = verifyGETStatusString(url, SC_OK, true, true);
        assertTrue(responseBody.indexOf(DEMO_OWNERID) > 0);

        // Delete the demo:REST object (ingested as part of setup)
        url = getURI(String.format("/objects/%s", DEMO_REST_PID.toString()));
        verifyDELETEStatusOnly(url, SC_UNAUTHORIZED, false);
        verifyDELETEStatusOnly(url, SC_OK, true);

        // Ingest the demo:REST object
        url = getURI(String.format("/objects/%s", DEMO_REST_PID.toString()));
        entity = getStringEntity(DEMO_REST, TEXT_XML);
        verifyPOSTStatusOnly(url, SC_UNAUTHORIZED, entity, false);
        post = new HttpPost(url);
        response = putOrPost(post, entity, true);
        responseBody = readString(response);
        assertEquals(SC_CREATED, response.getStatusLine().getStatusCode());
        locationHeader = response.getFirstHeader(HttpHeaders.LOCATION);
        assertNotNull(locationHeader);
        assertTrue(locationHeader.getValue().contains(URLEncoder.encode(DEMO_REST_PID
                .toString(), "UTF-8")));
        assertTrue(responseBody.equals(DEMO_REST_PID.toString()));

        // Ingest minimal object with no PID
        url = getURI(String.format("/objects/new"));
        entity = getStringEntity(DEMO_MIN, TEXT_XML);
        verifyPOSTStatusOnly(url, SC_UNAUTHORIZED, entity, false);
        post = new HttpPost(url);
        response = putOrPost(post, entity, true);
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_CREATED, response.getStatusLine().getStatusCode());

        // Delete minimal object
        String minimalObjectPid =
                extractPid(response.getFirstHeader(HttpHeaders.LOCATION).getValue());
        url = getURI(String.format("/objects/%s", minimalObjectPid));
        verifyDELETEStatusOnly(url, SC_UNAUTHORIZED, false);
        verifyDELETEStatusOnly(url, SC_OK, true);
    }

    // Tests FCREPO-509

    @Test
    public void testIngestWithParameterPid() throws Exception {

        // Ingest minimal object with PID, use "new" as path parameter -> must succeed
        URI url = getURI(String.format("/objects/new"));
        StringEntity entity = getStringEntity(DEMO_MIN_PID, TEXT_XML);
        verifyPOSTStatusOnly(url, SC_UNAUTHORIZED, entity, false);
        verifyPOSTStatusOnly(url, SC_CREATED, entity, true);

        // clean up
        url = getURI(String.format("/objects/%s", "demo:1234"));
        verifyDELETEStatusOnly(url, SC_UNAUTHORIZED, false);
        verifyDELETEStatusOnly(url, SC_OK, true);

        // Ingest minimal object with PID, use a different PID than the one
        // specified in the foxml -> must fail
        url = getURI(String.format("/objects/%s", "demo:234"));
        verifyPOSTStatusOnly(url, SC_UNAUTHORIZED, entity, false);
        verifyPOSTStatusOnly(url, SC_INTERNAL_SERVER_ERROR, entity, true);

        // Ingest minimal object with PID equals to the PID specified in the foxml
        url = getURI(String.format("/objects/%s", "demo:1234"));
        verifyPOSTStatusOnly(url, SC_UNAUTHORIZED, entity, false);
        verifyPOSTStatusOnly(url, SC_CREATED, entity, true);

        // clean up
        url = getURI(String.format("/objects/%s", "demo:1234"));
        verifyDELETEStatusOnly(url, SC_UNAUTHORIZED, false);
        verifyDELETEStatusOnly(url, SC_OK, true);

        // Ingest minimal object with no PID, specify a PID parameter in the request
        url = getURI(String.format("/objects/%s", "demo:234"));
        entity = getStringEntity(DEMO_MIN, TEXT_XML);
        verifyPOSTStatusOnly(url, SC_UNAUTHORIZED, entity, false);
        verifyPOSTStatusOnly(url, SC_CREATED, entity, true);

        // clean up
        url = getURI(String.format("/objects/%s", "demo:234"));
        verifyDELETEStatusOnly(url, SC_UNAUTHORIZED, false);
        verifyDELETEStatusOnly(url, SC_OK, true);

    }

    @Test
    public void testModifyObject() throws Exception {
        URI url = getURI(String.format("/objects/%s?label=%s", DEMO_REST_PID.toString(), "foo"));
        StringEntity entity = getStringEntity("", TEXT_XML);
        verifyPUTStatusOnly(url, SC_UNAUTHORIZED, entity, false);
        verifyPUTStatusOnly(url, SC_OK, entity, true);
        assertEquals("foo", apia.getObjectProfile(DEMO_REST_PID.toString(), null)
                .getObjLabel());
    }

    @Test
    public void testGetObjectXML() throws Exception {
        URI url = getURI(String.format("/objects/%s/objectXML", DEMO_REST_PID.toString()));
        verifyGETStatusString(url, SC_UNAUTHORIZED, false, true);
        verifyGETStatusString(url, SC_OK, true, true);
    }

    @Test
    public void testValidate() throws Exception {
        String[] resultFields = {"pid"};
        java.math.BigInteger maxResults = new java.math.BigInteger("" + 1000);
        FieldSearchQuery query = new FieldSearchQuery();
        org.fcrepo.server.types.gen.ObjectFactory factory = new org.fcrepo.server.types.gen.ObjectFactory();
        query.setTerms(factory.createFieldSearchQueryTerms("*"));
        FieldSearchResult result =
                apia.findObjects(TypeUtility.convertStringtoAOS(resultFields),
                                 maxResults,
                                 query);

        List<ObjectFields> fields = result.getResultList().getObjectFields();
        String pid = "";
        URI url = null;
        for (ObjectFields objectFields : fields) {
            if (objectFields != null) {
                pid = objectFields.getPid() != null ? objectFields.getPid().getValue() : "";
                url = getURI(
                        String.format("/objects/%s/validate", pid.toString()));
                verifyGETStatusString(
                        url, SC_UNAUTHORIZED, false, false);
                String responseXML = verifyGETStatusString(
                        url, SC_OK, true, true);
                assertXpathExists("/management:validation[@valid='true']", responseXML);
            }
        }
        // test with asOfDateTime set (just on the last object validated above)

        // fcrepo-947 - explicitly test demo:REST (pid) - was re-using last pid from above, which may change if test objects change
        // and demo:REST was the one that was failing
        // demo:REST has its datastream date/time values set to now (new Date()).

        // after ingest time - should pass
        url = getURI(
                String.format("/objects/%s/validate?asOfDateTime=%s",
                              pid.toString(),
                              df.format(new Date())));
        verifyGETStatusString(
                url, SC_UNAUTHORIZED, false, false);
        String responseXML = verifyGETStatusString(
                url, SC_OK, true, true);
        assertXpathExists("/management:validation[@valid='true']", responseXML);


        // date/time tests


        // before ingest time - should fail
        // (DC datastream version won't exist for example)
        url =
            getURI(String.format("/objects/%s/validate?asOfDateTime=%s", pid
                          .toString(), earlierDateTime));
        verifyGETStatusString(
                url, SC_UNAUTHORIZED, false, false);
        responseXML = verifyGETStatusString(
                url, SC_OK, true, true);
        assertXpathExists("/management:validation[@valid='false']", responseXML);

        // original - testing at exact ingets time - fails under postgres
        // see fcrepo-947
        /*
        url =
            String.format("/objects/%s/validate?asOfDateTime=%s", pid
                          .toString(), datetime);
        getTrue = get(true);
        assertEquals(pid.toString(), SC_UNAUTHORIZED, get(false)
                     .getStatusCode());
        assertEquals(pid.toString(), SC_OK, getTrue.getStatusCode());
        responseXML = getTrue.getResponseBodyString();
        assertXpathExists("/management:validation[@valid='true']", responseXML);
        */

    }

    @Test
    public void testExportObject() throws Exception {
        URI url = getURI(String.format("/objects/%s/export", DEMO_REST_PID.toString()));
        verifyGETStatusString(
                url, SC_UNAUTHORIZED, false, false);
        verifyGETStatusString(
                url, SC_OK, true, true);

        url =
            getURI(
                String.format("/objects/%s/export?context=public", DEMO_REST_PID
                        .toString()));
        verifyGETStatusString(
                url, SC_UNAUTHORIZED, false, false);
        verifyGETStatusString(
                url, SC_OK, true, true);
    }

    @Test
    public void testPurgeObject() throws Exception {
        URI url = getURI("/objects/demo:TEST_PURGE");
        StringEntity entity = getStringEntity("", TEXT_XML);
        verifyPOSTStatusOnly(url, SC_UNAUTHORIZED, entity, false);
        verifyPOSTStatusOnly(url, SC_CREATED, entity, true);
        verifyDELETEStatusOnly(url, SC_UNAUTHORIZED, false);
        verifyDELETEStatusOnly(url, SC_OK, true);
    }

    @Test
    public void testAddDatastream() throws Exception {
        // inline (X) datastream
        String xmlData = "<foo>bar</foo>";
        String dsPath = "/objects/" + DEMO_REST_PID + "/datastreams/FOO";
        URI url = getURI(dsPath + "?controlGroup=X&dsLabel=foo");
        StringEntity entity = getStringEntity(xmlData, TEXT_XML);
        verifyPOSTStatusOnly(url, SC_UNAUTHORIZED, entity, false);
        HttpPost post = new HttpPost(url);
        HttpResponse response = putOrPost(post, entity, true);
        String expected = readString(response);
        assertEquals(SC_CREATED, response.getStatusLine().getStatusCode());
        Header locationHeader = response.getFirstHeader(HttpHeaders.LOCATION);
        assertNotNull(locationHeader);
        assertEquals(getURI(dsPath),
                     URI.create(locationHeader.getValue()));
        assertEquals(TEXT_XML, response.getFirstHeader(HttpHeaders.CONTENT_TYPE)
                .getValue());
        url = getURI(dsPath + "?format=xml");
        String actual = verifyGETStatusString(url, SC_OK, true, true);
        assertEquals(expected, actual);

        // managed (M) datastream
        String mimeType = "text/plain";
        Datastream ds = apim.getDatastream(DEMO_REST_PID.toString(),"BAR",null);
        assertNull(ds);
        dsPath = "/objects/" + DEMO_REST_PID + "/datastreams/BAR";
        url = getURI(dsPath + "?controlGroup=M&dsLabel=bar&mimeType=" + mimeType);
        File temp = File.createTempFile("test", null);
        DataOutputStream os = new DataOutputStream(new FileOutputStream(temp));
        os.write(42);
        os.close();
        response = post(url, temp, false);
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
        response = post(url, temp, true);
        expected = readString(response);
        assertEquals(SC_CREATED, response.getStatusLine().getStatusCode());
        locationHeader = response.getFirstHeader(HttpHeaders.LOCATION);
        assertNotNull(locationHeader);
        assertEquals(getURI(dsPath),
                     URI.create(locationHeader.getValue()));
        assertEquals(TEXT_XML, response.getFirstHeader(HttpHeaders.CONTENT_TYPE)
                .getValue());
        url = getURI(dsPath + "?format=xml");
        actual = verifyGETStatusString(url, SC_OK, true, true);
        assertEquals(expected, actual);
        ds = apim.getDatastream(DEMO_REST_PID.toString(), "BAR", null);
        assertNotNull(ds);
        assertEquals(ds.getMIMEType(), mimeType);
    }

    @Test
    public void testModifyDatastreamByReference() throws Exception {
        // Create BAR datastream
        URI url = getURI(
            String.format(
                "/objects/%s/datastreams/BAR?controlGroup=M&dsLabel=testModifyDatastreamByReference(bar)",
                DEMO_REST_PID.toString()));
        File temp = File.createTempFile("test", null);
        DataOutputStream os = new DataOutputStream(new FileOutputStream(temp));
        os.write(42);
        os.close();
        HttpResponse response = post(url, temp, false);
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
        response = post(url, temp, true);
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_CREATED, response.getStatusLine().getStatusCode());

        // Update the content of the BAR datastream (using PUT)
        url = getURI(
                String.format("/objects/%s/datastreams/BAR", DEMO_REST_PID.toString()));
        assertEquals(SC_UNAUTHORIZED, put(url, temp, false)
                .getStatusLine().getStatusCode());
        response = put(url, temp, true);
        String expected = readString(response);
        assertEquals(SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(TEXT_XML, response.getFirstHeader(HttpHeaders.CONTENT_TYPE)
                .getValue());
        url = getURI(url.toString() + "?format=xml");
        String actual = verifyGETStatusString(url, SC_OK, true, true);
        assertEquals(expected, actual);

        // Ensure 404 on attempt to update BOGUS_DS via PUT
        url = getURI("/objects/" + DEMO_REST_PID + "/datastreams/BOGUS_DS");
        response = put(url, temp, true);
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_NOT_FOUND, response.getStatusLine().getStatusCode());

        // Update the content of the BAR datastream (using POST)
        url = getURI(String.format("/objects/%s/datastreams/BAR", DEMO_REST_PID.toString()));
        response = post(url, temp, true);
        expected = readString(response);
        assertEquals(SC_CREATED, response.getStatusLine().getStatusCode());
        Header locationHeader = response.getFirstHeader(HttpHeaders.LOCATION);
        assertNotNull(locationHeader);
        assertEquals(url.toString(), locationHeader.getValue());
        assertEquals(TEXT_XML, response.getFirstHeader(HttpHeaders.CONTENT_TYPE)
                .getValue());
        url = getURI(url.toString() + "?format=xml");
        actual = verifyGETStatusString(url, SC_OK, true, true);
        assertEquals(expected, actual);

        // Update the label of the BAR datastream
        String newLabel = "tikibar";
        url =
                getURI(String.format("/objects/%s/datastreams/BAR?dsLabel=%s", DEMO_REST_PID
                        .toString(), newLabel));
        verifyPUTStatusOnly(url, SC_UNAUTHORIZED, null, false);
        verifyPUTStatusOnly(url, SC_OK, null, true);
        assertEquals(newLabel, apim.getDatastream(DEMO_REST_PID.toString(), "BAR", null)
                .getLabel());

        // Update the location of the EXTDS datastream (E type datastream)
        String newLocation =
                "http://" + getHost() + ":" + getPort() + "/"
                        + getFedoraAppServerContext() + "/get/demo:REST/DC";
        url =
            getURI(
                String.format("/objects/%s/datastreams/EXTDS?dsLocation=%s",
                              DEMO_REST_PID.toString(),
                              newLocation));
        verifyPUTStatusOnly(url, SC_UNAUTHORIZED, null, false);
        verifyPUTStatusOnly(url, SC_OK, null, true);

        assertEquals(newLocation, apim.getDatastream(DEMO_REST_PID.toString(),
                                                     "EXTDS",
                                                     null).getLocation());
        String dcDS =
                new String(TypeUtility.convertDataHandlerToBytes(apia
                        .getDatastreamDissemination(DEMO_REST_PID.toString(), "DC", null)
                        .getStream()));
        String extDS =
                new String(TypeUtility.convertDataHandlerToBytes(apia
                        .getDatastreamDissemination(DEMO_REST_PID.toString(),
                                                    "EXTDS",
                                                    null).getStream()));
        assertEquals(dcDS, extDS);

        // Update DS1 by reference (X type datastream)
        // Error expected because attempting to access internal DS with API-A auth on
        if (getAuthAccess()) {
            // only ConfigB has API-A auth on
            url =
                getURI(
                    String.format("/objects/%s/datastreams/DS1?dsLocation=%s",
                                  DEMO_REST_PID.toString(),
                                  newLocation));
            verifyPUTStatusOnly(url, SC_UNAUTHORIZED, null, false);
            verifyPUTStatusOnly(url, SC_INTERNAL_SERVER_ERROR, null, true);
        }

        // Update DS1 by reference (X type datastream) - Success expected
        newLocation = getBaseURL() + "/ri/index.xsl";
        url =
            getURI(
                String.format("/objects/%s/datastreams/DS1?dsLocation=%s", DEMO_REST_PID
                        .toString(), newLocation));
        verifyPUTStatusOnly(url, SC_UNAUTHORIZED, null, false);
        verifyPUTStatusOnly(url, SC_OK, null, true);
    }

    @Test
    public void testModifyDatastreamByValue() throws Exception {
        String xmlData = "<baz>quux</baz>";
        StringEntity entity = getStringEntity(xmlData, TEXT_XML);
        URI url = getURI(
            String.format(
                "/objects/%s/datastreams/DS1?dsLabel=testModifyDatastreamByValue",
                DEMO_REST_PID.toString()));

        verifyPUTStatusOnly(url, SC_UNAUTHORIZED, entity, false);
        verifyPUTStatusOnly(url, SC_OK, entity, true);

        MIMETypedStream ds1 =
                apia.getDatastreamDissemination(DEMO_REST_PID.toString(), "DS1", null);
        assertXMLEqual(xmlData,
                       new String(TypeUtility.convertDataHandlerToBytes(ds1
                               .getStream()), "UTF-8"));
    }

    @Test
    public void testModifyDatastreamNoContent() throws Exception {
        String label = "testModifyDatastreamNoContent";
        URI url = getURI(
                String.format("/objects/%s/datastreams/DS1?dsLabel=%s", DEMO_REST_PID
                        .toString(), label));

        StringEntity entity = getStringEntity("", TEXT_XML);
        verifyPUTStatusOnly(url, SC_UNAUTHORIZED, entity, false);
        verifyPUTStatusOnly(url, SC_OK, entity, true);

        Datastream ds1 = apim.getDatastream(DEMO_REST_PID.toString(), "DS1", null);
        assertEquals(label, ds1.getLabel());
    }

    @Test
    public void testSetDatastreamState() throws Exception {
        String state = "D";
        URI url = getURI(
                String.format("/objects/%s/datastreams/DS1?dsState=%s", DEMO_REST_PID
                        .toString(), state));
        StringEntity entity = getStringEntity("", TEXT_XML);
        verifyPUTStatusOnly(url, SC_UNAUTHORIZED, entity, false);
        verifyPUTStatusOnly(url, SC_OK, entity, true);

        Datastream ds1 = apim.getDatastream(DEMO_REST_PID.toString(), "DS1", null);
        assertEquals(state, ds1.getState());
    }

    @Test
    public void testSetDatastreamVersionable() throws Exception {
        boolean versionable = false;
        URI url =
                getURI(String.format("/objects/%s/datastreams/DS1?versionable=%s", DEMO_REST_PID
                        .toString(), versionable));
        StringEntity entity = getStringEntity("", TEXT_XML);
        verifyPUTStatusOnly(url, SC_UNAUTHORIZED, entity, false);
        verifyPUTStatusOnly(url, SC_OK, entity, true);

        Datastream ds1 = apim.getDatastream(DEMO_REST_PID.toString(), "DS1", null);
        assertEquals(versionable, ds1.isVersionable());
    }

    @Test
    public void testPurgeDatastream() throws Exception {
        URI url = getURI(
            String.format(
                "/objects/%s/datastreams/RELS-EXT", DEMO_REST_PID.toString()));
        verifyDELETEStatusOnly(url, SC_UNAUTHORIZED, false);
        verifyDELETEStatusOnly(url, SC_OK, true);
    }

    @Test
    public void testGetNextPID() throws Exception {
        URI url = getURI("/objects/nextPID");
        verifyPOSTStatusOnly(url, SC_UNAUTHORIZED, null, false);
        // FIXME: not schema-validated, it should be
        // fcrepo-808 - when schema is available online, post("", true, false) -> post("", true)
        verifyPOSTStatusOnly(url, SC_OK, null, true, false);
    }

    @Test
    public void testLifecycle() throws Exception {
        HttpResponse response = null;

        // Get next PID
        URI url = getURI("/objects/nextPID?format=xml");
        verifyPOSTStatusOnly(url, SC_UNAUTHORIZED, null, false);
        HttpPost post = new HttpPost(url);
        // FIXME: fcrepo-808, validation disabled currently
        response = putOrPost(post, null, true);
        String responseXML = readString(response);        
        assertEquals(SC_OK, response.getStatusLine().getStatusCode());

        String pid =
                responseXML.substring(responseXML.indexOf("<pid>") + 5,
                                      responseXML.indexOf("</pid>"));

        // Ingest object
        String label = "Lifecycle-Test-Label";
        url = getURI(String.format("/objects/%s?label=%s", pid, label));
        verifyPOSTStatusOnly(url, SC_UNAUTHORIZED, null, false);
        verifyPOSTStatusOnly(url, SC_CREATED, null, true);

        // Add datastream
        String datastreamData = "<test>Test Datastream</test>";
        StringEntity entity = getStringEntity(datastreamData, TEXT_XML);
        url =
                getURI(String
                        .format("/objects/%s/datastreams/TESTDS?controlGroup=X&dsLabel=Test",
                                pid.toString()));
        verifyPOSTStatusOnly(url, SC_UNAUTHORIZED, entity, false);
        verifyPOSTStatusOnly(url, SC_CREATED, entity, true);

        // Get object XML
        url = getURI(String.format("/objects/%s/objectXML", pid));
        verifyGETStatusString(url, SC_UNAUTHORIZED, false, false);
        responseXML = verifyGETStatusString(url, SC_OK, true, true);
        assertTrue(responseXML.indexOf(label) > 0);
        assertTrue(responseXML.indexOf(datastreamData) > 0);

        // Modify object
        label = "Updated-Label";
        url = getURI(String.format("/objects/%s?label=%s", pid.toString(), label));
        verifyPUTStatusOnly(url, SC_UNAUTHORIZED, null, false);
        verifyPUTStatusOnly(url, SC_OK, null, true);

        // Modify datastream
        datastreamData = "<test>Update Test</test>";
        entity = getStringEntity(datastreamData, TEXT_XML);
        url = getURI(String.format("/objects/%s/datastreams/TESTDS", pid.toString()));
        verifyPUTStatusOnly(url, SC_UNAUTHORIZED, entity, false);
        verifyPUTStatusOnly(url, SC_OK, entity, true);

        // Export
        url = getURI(String.format("/objects/%s/export", pid.toString()));
        verifyGETStatusString(url, SC_UNAUTHORIZED, false, false);
        responseXML = verifyGETStatusString(url, SC_OK, true, true);
        assertTrue(responseXML.indexOf(label) > 0);
        assertTrue(responseXML.indexOf(datastreamData) > 0);

        // Purge datastream
        url = getURI(String.format("/objects/%s/datastreams/TESTDS", pid));
        verifyDELETEStatusOnly(url, SC_UNAUTHORIZED, false);
        verifyDELETEStatusOnly(url, SC_OK, true);

        // Purge object
        url = getURI(String.format("/objects/%s", pid));
        verifyDELETEStatusOnly(url, SC_UNAUTHORIZED, false);
        verifyDELETEStatusOnly(url, SC_OK, true);
    }

    @Test
    public void testChunked() throws Exception {
        chunked = true;
        testIngest();
        testModifyDatastreamByValue();
        testModifyDatastreamNoContent();
        testLifecycle();
    }

    @Test
    public void testResponseOverride() throws Exception {
        // Make request which returns error response
        URI url = getURI(String.format("/objects/%s", "demo:BOGUS_PID"));
        verifyPUTStatusOnly(url, SC_NOT_FOUND, null, true);

        // With flash=true parameter response should be 200
        url = getURI(String.format("/objects/%s?flash=true", "demo:BOGUS_PID"));
        verifyPUTStatusOnly(url, SC_OK, null, true);
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
        URI url = getURI("/objects/demo:REST/datastreams/DS1/content");
        HttpGet get = new HttpGet(url);
        HttpResponse response = getOrDelete(get, getAuthAccess(), false);
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_OK, response.getStatusLine().getStatusCode());
        CheckCDHeader(response, "inline", TestRESTAPI.DS1RelsFilename);
        // again with download
        url = getURI(url.toString() + "?download=true");
        get = new HttpGet(url);
        response = getOrDelete(get, getAuthAccess(), false);
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_OK, response.getStatusLine().getStatusCode());
        CheckCDHeader(response, "attachment", TestRESTAPI.DS1RelsFilename);
    }

    @Test
    public void testDatastreamDisseminationContentDispositionFromLabel()
            throws Exception {

        HttpGet get;
        HttpResponse response;
        int status = 0;
        // filename from label, known MIMETYPE
        URI url =
            getURI("/objects/demo:REST/datastreams/DS2/content?download=true");
        get = new HttpGet(url);
        response = getOrDelete(get, getAuthAccess(), false);
        status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_OK, status);
        CheckCDHeader(response, "attachment", TestRESTAPI.DS2LabelFilename
                + ".jpg"); // jpg should be from MIMETYPE mapping

        // filename from label, unknown MIMETYPE
        url = getURI("/objects/demo:REST/datastreams/DS3/content?download=true");
        get = new HttpGet(url);
        response = getOrDelete(get, getAuthAccess(), false);
        status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_OK, status);
        CheckCDHeader(response, "attachment", TestRESTAPI.DS3LabelFilename
                + ".bin"); // default extension from config

        // filename from label with illegal characters, known MIMETYPE
        url = getURI("/objects/demo:REST/datastreams/DS4/content?download=true");
        get = new HttpGet(url);
        response = getOrDelete(get, getAuthAccess(), false);
        status = response.getStatusLine().getStatusCode();
        EntityUtils.consumeQuietly(response.getEntity());
        assertEquals(SC_OK, status);
        CheckCDHeader(response, "attachment", TestRESTAPI.DS4LabelFilename
                + ".xml"); // xml from mimetype mapping
    }

    @Test
    public void testDatastreamDisseminationContentDispositionFromId()
            throws Exception {

        // filename from id (no label present)
        URI url =
            getURI("/objects/demo:REST/datastreams/DS5/content?download=true");
        HttpGet get = new HttpGet(url);
        HttpResponse response = getOrDelete(get, getAuthAccess(), false);
        assertEquals(SC_OK, response.getStatusLine().getStatusCode());
        // xml from mimetype mapping
        CheckCDHeader(response, "attachment", TestRESTAPI.DS5ID + ".xml");
        get.releaseConnection();

        // filename from id, id contains extension (no label present)
        url =
            getURI("/objects/demo:REST/datastreams/DS6.xml/content?download=true");
        get = new HttpGet(url);
        response = getOrDelete(get, getAuthAccess(), false);
        assertEquals(SC_OK, response.getStatusLine().getStatusCode());
        CheckCDHeader(response, "attachment", TestRESTAPI.DS6ID); // no extension, id contains it
        get.releaseConnection();
    }

    @Test
    public void testUpload() throws Exception {
        String uploadUrl = "/upload";
        String url = getBaseURL() + uploadUrl;

        MultipartEntity entity = _doUploadPost();
        HttpPost post = new HttpPost(url);
        
        HttpResponse response = putOrPost(post, entity, true);
        if (response.getStatusLine().getStatusCode() == SC_MOVED_TEMPORARILY) {
            url = response.getFirstHeader(HttpHeaders.LOCATION).getValue();
            post = new HttpPost(url);
            entity = _doUploadPost();
            response = putOrPost(post, entity, true);
        }

        assertEquals(202, response.getStatusLine().getStatusCode());
        assertTrue(readString(response).startsWith("uploaded://"));

        // Test content not supplied
        entity = _doUploadPost(new HashMap<String, AbstractContentBody>(0));
        response = putOrPost(post, entity, true);
        assertEquals(400, response.getStatusLine().getStatusCode());
        post.releaseConnection();
    }

    /////////////////////////////////////////////////
    // Relationships methods
    /////////////////////////////////////////////////

    @Test
    public void testGetRelationships() throws Exception {
        String s = "info:fedora/" + DEMO_REST_PID;
        String p = Constants.MODEL.HAS_MODEL.uri;
        String o = Models.FEDORA_OBJECT_CURRENT.uri;

        // get all CModel relationships
        URI url =
                getURI("/objects/" + DEMO_REST_PID + "/relationships" + "?subject="
                        + URLEncoder.encode(s, "UTF-8") + "&predicate="
                        + URLEncoder.encode(p, "UTF-8"));
        byte [] response = verifyGETStatusBytes(url, SC_OK, true, false);

        // check Fedora object CModel found
        checkRelationship(response, s, p, o, true);

    }

    @Test
    public void testAddRelationship() throws Exception {
        String s = "info:fedora/" + DEMO_REST_PID;
        String p = "http://www.example.org/test#relationship";
        String o = "addRelationship";
        // check relationship not present
        URI url =
                getURI("/objects/" + DEMO_REST_PID + "/relationships" + "?subject="
                        + URLEncoder.encode(s, "UTF-8") + "&predicate="
                        + URLEncoder.encode(p, "UTF-8"));
        byte [] bytes = verifyGETStatusBytes(url, SC_OK, true, false);

        checkRelationship(bytes, s, p, o, false);

        // add relationship
        HttpPost post = new HttpPost();
        url =
                getURI("/objects/" + DEMO_REST_PID + "/relationships/new" + "?subject="
                        + URLEncoder.encode(s, "UTF-8") + "&predicate="
                        + URLEncoder.encode(p, "UTF-8") + "&object="
                        + URLEncoder.encode(o, "UTF-8") + "&isLiteral=true");
        post.setURI(url);
        HttpResponse response = putOrPost(post, null, true);
        assertEquals(SC_OK, response.getStatusLine().getStatusCode());
        post.releaseConnection();

        // check relationship present
        url = getURI("/objects/" + DEMO_REST_PID + "/relationships");  // +
        //"?subject=" + URLEncoder.encode(s, "UTF-8") +
        //"&predicate=" + URLEncoder.encode(p, "UTF-8");
        bytes = verifyGETStatusBytes(url, SC_OK, true, false);

        checkRelationship(bytes, s, p, o, true);

        // check the same operation with URL-encoded PID
        o = "addRelationshipUrlEncoded";
        // check relationship not present
        url =
                getURI("/objects/" + URLEncoder.encode(DEMO_REST_PID.toString(), "UTF-8")
                    + "/relationships" + "?subject="
                    + URLEncoder.encode(s, "UTF-8") + "&predicate="
                    + URLEncoder.encode(p, "UTF-8"));
        bytes = verifyGETStatusBytes(url, SC_OK, true, false);

        checkRelationship(bytes, s, p, o, false);

        // add relationship
        url =
                getURI("/objects/" + URLEncoder.encode(DEMO_REST_PID.toString(), "UTF-8")
                    + "/relationships/new" + "?subject="
                    + URLEncoder.encode(s, "UTF-8") + "&predicate="
                    + URLEncoder.encode(p, "UTF-8") + "&object="
                    + URLEncoder.encode(o, "UTF-8") + "&isLiteral=true");
        post = new HttpPost(url);
        response = putOrPost(post, null, true);
        assertEquals(SC_OK, response.getStatusLine().getStatusCode());
        post.releaseConnection();

        // check relationship present
        url = getURI("/objects/"
            + URLEncoder.encode(DEMO_REST_PID.toString(), "UTF-8")
            + "/relationships");// +
        //"?subject=" + URLEncoder.encode(s, "UTF-8") +
        //"&predicate=" + URLEncoder.encode(p, "UTF-8");
        bytes = verifyGETStatusBytes(url, SC_OK, true, false);

        checkRelationship(bytes, s, p, o, true);

    }

    @Test
    public void testPurgeRelationship() throws Exception {
        String s = "info:fedora/" + DEMO_REST_PID;
        String p = "http://www.example.org/test#relationship";
        String o = "foo";

        // add relationship
        HttpPost post = new HttpPost();
        URI url =
                getURI("/objects/" + DEMO_REST_PID + "/relationships/new" + "?subject="
                        + URLEncoder.encode(s, "UTF-8") + "&predicate="
                        + URLEncoder.encode(p, "UTF-8") + "&object="
                        + URLEncoder.encode(o, "UTF-8") + "&isLiteral=true");
        post.setURI(url);
        HttpResponse response = putOrPost(post, null, true);
        
        int status = response.getStatusLine().getStatusCode();
        post.releaseConnection();
        assertEquals(SC_OK, status);
        HttpGet get = null;
        HttpDelete delete = null;
        try {
            // check present
            url =
                    getURI("/objects/" + DEMO_REST_PID + "/relationships" + "?subject="
                            + URLEncoder.encode(s, "UTF-8") + "&predicate="
                            + URLEncoder.encode(p, "UTF-8"));
            get = new HttpGet(url);
            response = getOrDelete(get, true, false);
            status = response.getStatusLine().getStatusCode();
            byte [] responseBytes = readBytes(response);
            get.releaseConnection();
            assertEquals(SC_OK, status);
            checkRelationship(responseBytes, s, p, o, true);

            // purge it
            url =
                    getURI("/objects/" + DEMO_REST_PID + "/relationships" + "?subject="
                            + URLEncoder.encode(s, "UTF-8") + "&predicate="
                            + URLEncoder.encode(p, "UTF-8") + "&object="
                            + URLEncoder.encode(o, "UTF-8") + "&isLiteral=true");
            delete = new HttpDelete(url);
            response = getOrDelete(delete, true, false);
            status = response.getStatusLine().getStatusCode();
            String responseString = readString(response);
            assertEquals(SC_OK, status);
            assertEquals("Purge relationship",
                    "true",
                    responseString);

            // check not present
            url =
                    getURI("/objects/" + DEMO_REST_PID + "/relationships" + "?subject="
                            + URLEncoder.encode(s, "UTF-8") + "&predicate="
                            + URLEncoder.encode(p, "UTF-8"));
            get.setURI(url);
            response = getOrDelete(get, true, false);
            status = response.getStatusLine().getStatusCode();
            responseBytes = readBytes(response);
            assertEquals(SC_OK, response.getStatusLine().getStatusCode());
            checkRelationship(responseBytes, s, p, o, false);

            // purge again
            url =
                    getURI("/objects/" + DEMO_REST_PID + "/relationships" + "?subject="
                            + URLEncoder.encode(s, "UTF-8") + "&predicate="
                            + URLEncoder.encode(p, "UTF-8") + "&object="
                            + URLEncoder.encode(o, "UTF-8") + "&isLiteral=true");
            delete.setURI(url);
            response = getOrDelete(delete, true, true);
            status = response.getStatusLine().getStatusCode();
            responseString = readString(response);
            assertEquals(SC_OK, status);
            assertEquals("Purge relationship", "false", responseString);
        } finally {
            if (get != null) get.releaseConnection();
            if (delete != null) delete.releaseConnection();
        }
    }

    @Test
    public void testDisseminationContentLengthWhenKnown() throws Exception {
        URI url =
                getURI(
                    "/objects/demo:14/methods/demo:12/getDocument");
        HttpGet get = new HttpGet(url);
        HttpResponse response;
        verifyNoAuthFailOnAPIAAuth(url);
        // default to validated response
        response = getOrDelete(get, getAuthAccess(), true);
        int status = response.getStatusLine().getStatusCode();
        String cLen = (response.containsHeader(HttpHeaders.CONTENT_LENGTH)) ?
            response.getFirstHeader(HttpHeaders.CONTENT_LENGTH).getValue() :
            null;
        EntityUtils.consumeQuietly(response.getEntity());
        get.releaseConnection();
        assertEquals(SC_OK, status);
        assertEquals("19498", cLen);
    }

    private MultipartEntity _doUploadPost() throws Exception {
        File temp = File.createTempFile("test.txt", null);
        FileUtils.writeStringToFile(temp, "This is the upload test file");

        FileBody part = new FileBody(temp);
        Map<String, AbstractContentBody> parts =
            new HashMap<String, AbstractContentBody>(1);
        parts.put("file", part);
        return _doUploadPost(parts);
    }

    private MultipartEntity _doUploadPost(Map<String, AbstractContentBody> parts) throws Exception {
        MultipartEntity entity =
                new MultipartEntity();

        for (String name: parts.keySet()) {
            entity.addPart(name, parts.get(name));
        }
        return entity;
    }

    // check content disposition header of response

    private void CheckCDHeader(HttpResponse response,
                               String expectedType,
                               String expectedFilename) {
        String contentDisposition = "";
        Header[] headers = response.getAllHeaders();
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase("content-disposition")) {
                contentDisposition = header.getValue();
            }
        }
        assertEquals(expectedType + "; " + "filename=\"" + expectedFilename
                + "\"", contentDisposition);
    }

    // helper methods

    private HttpClient getClient(boolean followRedirects, boolean auth) {
        DefaultHttpClient result =
            s_client.getHttpClient(followRedirects, auth);
        if (auth) {
            String host = getHost();
            LOGGER.debug("credentials set for scope of {}:[ANY PORT]", host);
            result
                    .getCredentialsProvider()
                    .setCredentials(
                            new AuthScope(host, AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                            new UsernamePasswordCredentials(getUsername(),
                                                                    getPassword()));
        } else {
            result.getCredentialsProvider().clear();
        }
        return result;
    }

    protected static URI getURI(String url) {
        if (url == null || url.length() == 0) {
            throw new IllegalArgumentException("url must be a non-empty value");
        } else if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            url = getBaseURL() + url;
        }

        return URI.create(url);
    }

    protected void validateResponse(URI url, HttpResponse res) throws Exception {
        // if response was ok... (and not doing flash override of error response)
        int sc = res.getStatusLine().getStatusCode();
        if (sc >= 200 && sc <= 299 && !url.toString().contains("flash=true")) {
            // if response is xml
            Header contentType = res.getFirstHeader(HttpHeaders.CONTENT_TYPE);

            if (contentType != null
                    && (contentType.getValue()
                            .contains(TEXT_XML) || contentType.getValue()
                            .contains("application/xml"))) {
                String xmlResponse = EntityUtils.toString(res.getEntity());
                // put the response right back, in case we need it elsewhere
                res.setEntity(new StringEntity(xmlResponse, Charset.forName("UTF-8")));
                // if a schema location is specified
                if (xmlResponse.contains(":schemaLocation=\"")) {

                    //Validate online if we can
                    String offline = System.getProperty("offline");
                    String online = System.getProperty("online");
                    if (!"false".equals(online)
                            && (offline == null || !"true".equals(offline))) {
                        validator.onlineValidate(url.toString(), xmlResponse);
                    }

                    // Also validate offline unless explicitly disabled
                    if (!"false".equals(offline)) {
                        /*
                         * Offline schema validation does not work due to bug in
                         * xerces:
                         * https://issues.apache.org/jira/browse/XERCESJ-1130
                         */
                        //validator.offlineValidate(url, xmlResponse, getSchemaFiles(xmlResponse, null));
                    }
                } else {
                    // for now, requiring a schema
                    fail("No schema location specified in response - " + url);
                }
            }
        }
    }

    @Deprecated
    protected HttpResponse put(URI url, File requestContent, boolean authenticate)
            throws Exception {
        HttpPut method = new HttpPut(url);
        MultipartEntity entity = new MultipartEntity();
        entity.addPart("param_name",new StringBody("value"));
        entity.addPart(((File) requestContent)
                .getName(), new FileBody((File) requestContent));
        HttpResponse response = putOrPost(method, entity, authenticate);
        int status = response.getStatusLine().getStatusCode();
        if (status == SC_MOVED_TEMPORARILY) {
            String original = url.toString();
            url = URI.create(response.getFirstHeader(HttpHeaders.LOCATION).getValue());
            if (!original.equals(url.toString())) {
                EntityUtils.consumeQuietly(response.getEntity());
                method = new HttpPut(url);
                entity = new MultipartEntity();
                entity.addPart("param_name",new StringBody("value"));
                entity.addPart(((File) requestContent)
                        .getName(), new FileBody((File) requestContent));
                response = putOrPost(method, entity, true);
            }
        }

        return response;
    }

    @Deprecated
    protected HttpResponse post(URI url, File requestContent, boolean authenticate)
            throws Exception {
        HttpPost method = new HttpPost(url);
        MultipartEntity entity = new MultipartEntity();
        entity.addPart("param_name",new StringBody("value"));
        entity.addPart(((File) requestContent)
                .getName(), new FileBody((File) requestContent));
        HttpResponse response = putOrPost(method, entity, authenticate);
        int status = response.getStatusLine().getStatusCode();
        if (status == SC_MOVED_TEMPORARILY) {
            String original = url.toString();
            url = URI.create(response.getFirstHeader(HttpHeaders.LOCATION).getValue());
            if (!original.equals(url.toString())) {
                EntityUtils.consumeQuietly(response.getEntity());
                method = new HttpPost(url);
                entity = new MultipartEntity();
                entity.addPart("param_name",new StringBody("value"));
                entity.addPart(((File) requestContent)
                        .getName(), new FileBody((File) requestContent));
                response = putOrPost(method, entity, true);
            }
        }

        return response;
    }

    private HttpResponse getOrDelete(HttpRequestBase method,
            boolean authenticate, boolean validate)
            throws Exception {
        HttpClient client = getClient(true, authenticate);
        HttpResponse response =
            getOrDelete(client, method, authenticate, validate);
        return response;
    }

    private HttpResponse getOrDelete(HttpClient client, HttpRequestBase method,
            boolean authenticate, boolean validate)
            throws Exception {

        LOGGER.debug(method.getURI().toString());

        if (!(method instanceof HttpGet || method instanceof HttpDelete)) {
            throw new IllegalArgumentException("method must be one of GET or DELETE.");
        }
        HttpResponse response = client.execute(method);

        if (response.getStatusLine().getStatusCode() == SC_MOVED_TEMPORARILY) {
            String redir =
                    response.getFirstHeader(HttpHeaders.LOCATION).getValue();
            if (!method.getURI().toString().equals(redir)) {
                method.setURI(getURI(redir));
                response = getOrDelete(client, method, authenticate, validate);
            }
        }
        
        if (validate) {
            validateResponse(method.getURI(), response);
        }

        return response;
    }

    private HttpResponse putOrPost(HttpEntityEnclosingRequestBase method,
                                   HttpEntity requestContent,
                                   boolean authenticate) throws Exception {
        HttpClient client = getClient(false, authenticate);
        if (method == null) {
            throw new IllegalArgumentException("method must be a non-empty value");
        }

        if (requestContent != null) {
            method.setEntity(requestContent);
        }


        HttpResponse response = client.execute(method);

        return response;
    }
    
    private void checkRelationship(byte[] rdf,
                                   String s,
                                   String p,
                                   String o,
                                   boolean exists) throws TrippiException,
            UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append("RDF: " + new String(rdf, "UTF-8"));
        TripleIterator it =
                TripleIteratorFactory.defaultInstance().fromStream(new ByteArrayInputStream(rdf), null,
                RDFFormat.RDF_XML);

        boolean found = false;
        while (it.hasNext()) {
            Triple t = it.next();

            sb.append(t.getSubject().stringValue() + ", ");
            sb.append(t.getPredicate().stringValue() + ", ");
            sb.append(t.getObject().stringValue() + "\n");

            sb.append("matching: " + s + " " + t.getSubject().stringValue()
                    + " " + (s.equals(t.getSubject().stringValue())) + "\n");
            sb.append("matching: " + p + " " + t.getPredicate().stringValue()
                    + " " + (p.equals(t.getPredicate().stringValue())) + "\n");
            sb.append("matching: " + o + " " + t.getObject().stringValue()
                    + " " + (o.equals(t.getObject().stringValue())) + "\n");

            if (s.equals(t.getSubject().stringValue())
                    && p.equals(t.getPredicate().stringValue())
                    && o.equals(t.getObject().stringValue())) {
                sb.append("Matched\n");
                found = true;
            }
        }

        assertTrue("Testing if relationship present: " + exists + " [ " + s
                           + ", " + p + ", " + o + " ] \n " + sb.toString(),
                   exists == found);
    }

    // Supports legacy test runners

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestRESTAPI.class);
    }

    public static void main(String[] args) {
        JUnitCore.runClasses(TestRESTAPI.class);
    }

}
