/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.api;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Set;

import junit.framework.JUnit4TestAdapter;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
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
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.fcrepo.client.FedoraClient;
import org.fcrepo.common.http.PreemptiveAuth;
import org.fcrepo.server.access.FedoraAPIAMTOM;
import org.fcrepo.server.management.FedoraAPIMMTOM;
import org.fcrepo.server.types.gen.Datastream;
import org.fcrepo.server.utilities.TypeUtility;
import org.fcrepo.test.FedoraServerTestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;


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
public class TestAdminAPI
        extends FedoraServerTestCase {

    private static FedoraClient s_client;
    
    private FedoraAPIAMTOM apia;

    private FedoraAPIMMTOM apim;

    // used for determining test configuration
    private static String authAccessProperty = "fedora.authorize.access";

    protected Boolean authorizeAccess = null;
    protected String url;

    private final boolean chunked = false;
    
    @BeforeClass
    public static void bootstrap() throws Exception {
        s_client = getFedoraClient();
        ingestSimpleImageDemoObjects(s_client);
        ingestSimpleDocumentDemoObjects(s_client);
    }
    
    @AfterClass
    public static void cleanUp() throws Exception {
        purgeDemoObjects(s_client);
        s_client.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        apia = s_client.getAPIAMTOM();
        apim = s_client.getAPIMMTOM();
    }

    @After
    public void tearDown() throws Exception {
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
                assertTrue("Failed to determine whether to perform authorization on Access requests from: " +
                           authAccessProperty, false);
                throw new RuntimeException(
                        "Failed to determine whether to perform authorization on Access requests from: " +
                        authAccessProperty);
            }

        }
        return authorizeAccess;

    }

    @Test
    public void testModifyControlGroup() throws Exception {

        //////////////////////////////////////////////////
        // tests on file of PIDs - XML
        //////////////////////////////////////////////////


        // get objects to modify from a basic search query, to provide xml input
        url = "/objects?pid=true&title=false&query=pid~demo%3A*&maxResults=1000&resultFormat=xml";

        HttpResponse res = get(getAuthAccess());
        assertEquals(SC_OK, res.getStatusLine().getStatusCode());

        File objectsListFile = null;

        try {
            // output to a file
            objectsListFile = File.createTempFile("TestAdminAPI", null);
            FileWriter fw = new FileWriter(objectsListFile);
            String queryResults = EntityUtils.toString(res.getEntity());
            fw.write(queryResults);
            fw.close();

            // run the tests
            testFileInput(objectsListFile);

        } finally {
            // clean up
            if (objectsListFile != null)
                if (!objectsListFile.delete())
                    objectsListFile.deleteOnExit();
        }

        purgeDemoObjects(s_client);
        ingestSimpleImageDemoObjects(s_client);
        ingestSimpleDocumentDemoObjects(s_client);

        //////////////////////////////////////////////////
        // tests on file of PIDs - flat file
        //////////////////////////////////////////////////

        objectsListFile = null;

        try {
            // generate flat file list of demo objects
            objectsListFile = File.createTempFile("TestAdminAPI", null);
            FileWriter fw = new FileWriter(objectsListFile);

            Set<String> objects = getDemoObjects(s_client);
            for (String pid : objects) {
                fw.write(pid + "\n");
            }
            fw.close();

            // run the tests
            testFileInput(objectsListFile);

        } finally {
            // clean up
            if (objectsListFile != null)
                if (!objectsListFile.delete())
                    objectsListFile.deleteOnExit();
        }

        purgeDemoObjects(s_client);
        ingestSimpleImageDemoObjects(s_client);
        ingestSimpleDocumentDemoObjects(s_client);

        //////////////////////////////////////////////////
        // tests on single object
        //////////////////////////////////////////////////

        Set<String> objects = getDemoObjects(s_client);

        // test on the first object we find
        String pid = objects.toArray(new String[0])[0];
        // test 404 on object not found
        url = this.modifyDatastreamControlGroupUrl(pid + "doesnotexist", "DC", "M", false, false, false);
        res = get(true);
        assertEquals(SC_NOT_FOUND, res.getStatusLine().getStatusCode());

        // test 404 on datastream not found
        url = this.modifyDatastreamControlGroupUrl(pid, "doesnotexist", "M", false, false, false);
        res = get(true);
        assertEquals(SC_NOT_FOUND, res.getStatusLine().getStatusCode());

        // TODO: getting stream contents?  (could use REST call instead)


        // datastream contents before modification
        byte[] before = TypeUtility.convertDataHandlerToBytes(apia.getDatastreamDissemination(pid, "DC", null).getStream());

        url = this.modifyDatastreamControlGroupUrl(pid, "DC", "M", false, false, false);
        res = get(true);
        String contents = EntityUtils.toString(res.getEntity());
        assertEquals(SC_OK, res.getStatusLine().getStatusCode());

        // check control group modified
        Datastream ds = apim.getDatastream(pid, "DC", null);
        assertEquals("ControlGroup", "M", ds.getControlGroup().value());


        // datastream contents after modification
        byte[] after = TypeUtility.convertDataHandlerToBytes(apia.getDatastreamDissemination(pid, "DC", null).getStream());

        // check they are the same
        // (comparing as strings as the assertEquals is a lot easier to read...)
        String beforeString = new String(before, "UTF-8");
        String afterString = new String(after, "UTF-8");
        assertEquals("Datastream contents ", beforeString, afterString);

        //////////////////////////////////////////////////
        // tests on list of pids
        //////////////////////////////////////////////////

        // test on the second and third objects we found
        String pid1 = objects.toArray(new String[0])[1];
        String pid2 = objects.toArray(new String[0])[2];


        // modify both
        url = this.modifyDatastreamControlGroupUrl(pid1 + "," + pid2, "DC", "M", false, false, false);
        res = get(true);
        assertEquals(SC_OK, res.getStatusLine().getStatusCode());

        contents = EntityUtils.toString(res.getEntity());
        int[] counts = getCounts(contents);

        // check for modification in result stream
        assertEquals("Object count", 2, counts[0]);
        assertEquals("Datastream count", 2, counts[1]);

        // check control groups actually modified
        ds = apim.getDatastream(pid1, "DC", null);
        assertEquals("ControlGroup", "M", ds.getControlGroup().value());

        ds = apim.getDatastream(pid2, "DC", null);
        assertEquals("ControlGroup", "M", ds.getControlGroup().value());
    }


    private void testFileInput(File objectsListFile) throws Exception {

        // count objects we know already have Managed content DC
        // (ingest creates some of these, ending in _M)
        // used later in checking results
        Set<String> objects = getDemoObjects(s_client);
        int managedObjects = 0;
        for (String pid : objects) {
            if (pid.endsWith("_M"))
                managedObjects++;
        }

        // modify datastreams, based on file input - DC
        url = this.modifyDatastreamControlGroupUrl("file:///" + objectsListFile.getAbsolutePath(), "DC", "M", false, false, false);
        HttpResponse res = get(true);
        int status = res.getStatusLine().getStatusCode();
        String modified = EntityUtils.toString(res.getEntity());
        assertEquals(SC_OK, status);

        // object and datastream count message expected
        String logExpected = "Updated " + (objects.size() - managedObjects) + " objects and " + (objects.size() - managedObjects) + " datastreams";
        assertTrue("Wrong number of objects/datastreams updated.  Expected " + logExpected + "\n" + "Log file shows:" + "\n" + modified, modified.contains(logExpected));

        // do again, this time we expect no modifications (already modified, so should be ignored)
        res = get(true);

        modified = EntityUtils.toString(res.getEntity());
        assertEquals(SC_OK, res.getStatusLine().getStatusCode());

        // object and datastream count message expected
        logExpected = "Updated " + 0 + " objects and " + 0 + " datastreams";
        assertTrue("Wrong number of objects/datastreams updated", modified.contains(logExpected));

        // do again modifying DC and RELS-EXT
        // DC is already M, so won't result in modifications
        // not all objects have RELS-EXT- the simple document demo objects don't
        // so we check that object and datastream count is greater than zero but 2 less than number of objects
        // FIXME: could iterate all objects before/after and do more exact tests
        url = this.modifyDatastreamControlGroupUrl("file:///" + objectsListFile.getAbsolutePath(), "DC,RELS-EXT", "M", false, false, false);
        res = get(true);
        modified = EntityUtils.toString(res.getEntity());
        assertEquals(SC_OK, res.getStatusLine().getStatusCode());

        int[] counts = getCounts(modified);
        int objectCount = counts[0];
        int datastreamCount = counts[1];

        if (objectCount <= 0 || objectCount > (objects.size() - managedObjects - 2) || datastreamCount <= 0 || datastreamCount > (objects.size() - managedObjects - 2))
            fail("Incorrect number of objects and datastreams modified: objects " + objectCount + " datastreams " + datastreamCount);

    }


    /**
     * Parses results, returns object count, datastream count
     * @param response
     * @return
     */
    private int[] getCounts(String response) {
        int objectCountPos = response.lastIndexOf("Updated ");
        int datastreamCountPos = response.lastIndexOf(" objects and ");
        int datastreamEndCountPos = response.lastIndexOf(" datastreams");

        int objectCount = Integer.parseInt(response.substring(objectCountPos + "Updated ".length(), datastreamCountPos));
        int datastreamCount = Integer.parseInt(response.substring(datastreamCountPos + " objects and ".length(), datastreamEndCountPos));

        int[] res = {objectCount, datastreamCount};

        return res;
    }

    private String modifyDatastreamControlGroupUrl(String pid, String dsID, String controlGroup, boolean addXMLHeader, boolean reformat, boolean setMIMETypeCharset) {

        String ret;
        try {
            ret = "/management/control" +
                "?action=modifyDatastreamControlGroup" +
                "&pid=" + URLEncoder.encode(pid, "UTF-8") +
                "&dsID=" + URLEncoder.encode(dsID, "UTF-8") +
                "&controlGroup=" + controlGroup +
                "&addXMLHeader=" + addXMLHeader +
                "&reformat=" + reformat +
                "&setMIMETypeCharset=" + setMIMETypeCharset;
        } catch (UnsupportedEncodingException e) {
            // should never happen
            throw new RuntimeException(e);
        }

        return ret;
    }


// helper methods

    private HttpClient getClient(boolean auth) {
        DefaultHttpClient client = new PreemptiveAuth();
        if (auth) {
            client
                    .getCredentialsProvider()
                    .setCredentials(new AuthScope(getHost(), Integer
                            .valueOf(getPort())),
                                    new UsernamePasswordCredentials(getUsername(),
                                                                    getPassword()));
        }
        return client;
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
        HttpUriRequest httpMethod = null;
        if (method.equals("GET")) {
            httpMethod = new HttpGet(url);
        } else if (method.equals("DELETE")) {
            httpMethod = new HttpDelete(url);
        } else {
            throw new IllegalArgumentException("method must be one of GET or DELETE.");
        }
        httpMethod.setHeader(HttpHeaders.CONNECTION, "Keep-Alive");

        return getClient(authenticate).execute(httpMethod);
    }

    private HttpResponse putOrPost(String method,
                                   Object requestContent,
                                   boolean authenticate) throws Exception {


        if (url == null || url.length() == 0) {
            throw new IllegalArgumentException("url must be a non-empty value");
        } else if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            url = getBaseURL() + url;
        }


        HttpEntityEnclosingRequestBase httpMethod = null;
        try {
            if (method.equals("PUT")) {
                httpMethod = new HttpPut(url);
            } else if (method.equals("POST")) {
                httpMethod = new HttpPost(url);
            } else {
                throw new IllegalArgumentException("method must be one of PUT or POST.");
            }
            httpMethod.setHeader(HttpHeaders.CONNECTION, "Keep-Alive");
            if (requestContent != null) {
                if (requestContent instanceof String) {
                    StringEntity entity = new StringEntity((String) requestContent,
                            Charset.forName("UTF-8"));
                    entity.setChunked(chunked);
                    httpMethod.setEntity(entity);
                } else if (requestContent instanceof File) {
                    MultipartEntity entity =
                            new MultipartEntity();
                    entity.addPart(((File) requestContent)
                            .getName(), new FileBody((File) requestContent));
                    entity.addPart("param_name", new StringBody("value"));
                    httpMethod.setEntity(entity);
                } else {
                    throw new IllegalArgumentException("requestContent must be a String or File");
                }
            }
            return getClient(authenticate).execute(httpMethod);
        } finally {
            if (httpMethod != null) {
                httpMethod.releaseConnection();
            }
        }
    }


    class HttpResponseInfo {

        private final int statusCode;

        private final byte[] responseBody;

        private final Header[] responseHeaders;

        HttpResponseInfo(int status, byte[] body, Header[] headers) {
            statusCode = status;
            responseBody = body;
            responseHeaders = headers;
        }

        HttpResponseInfo(HttpResponse response)
                throws IOException {
            statusCode = response.getStatusLine().getStatusCode();
            //responseBody = method.getResponseBody();
            InputStream is = response.getEntity().getContent();
            responseBody = IOUtils.toByteArray(is);
            IOUtils.closeQuietly(is);
            responseHeaders = response.getAllHeaders();
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

        public Header getResponseHeader(String headerName) {
            for (Header header : responseHeaders) {
                if (header.getName().equalsIgnoreCase(headerName)) {
                    return header;
                }
            }
            return null;
        }
    }
    
    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestAdminAPI.class);
    }

    public static void main(String[] args) {
        JUnitCore.runClasses(TestAdminAPI.class);
    }

}
