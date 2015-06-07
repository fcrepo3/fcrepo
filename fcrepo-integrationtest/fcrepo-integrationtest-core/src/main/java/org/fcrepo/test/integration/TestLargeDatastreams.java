/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.integration;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import junit.framework.JUnit4TestAdapter;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.util.EntityUtils;
import org.fcrepo.client.FedoraClient;

import org.fcrepo.server.access.FedoraAPIAMTOM;
import org.fcrepo.server.management.FedoraAPIMMTOM;
import org.fcrepo.server.types.mtom.gen.MIMETypedStream;
import org.fcrepo.server.utilities.TypeUtility;

import org.fcrepo.test.FedoraTestCase;


/**
 * <p>Performs ingest and export tests using large datastreams.
 * This test creates and moves very large files and as such
 * takes significant time to run.
 * </p><p>
 * Non-SSL transports *MUST* be available for all APIs in order
 * for this test to run properly.
 * </p>
 * @author Bill Branan
 */
public class TestLargeDatastreams
        extends FedoraTestCase {

    private FedoraClient fedoraClient;

    private FedoraAPIMMTOM apim;

    private FedoraAPIAMTOM apia;

    private static final String pid = "demo:LargeDatastreams";

    private static byte[] DEMO_FOXML;

    private static long gigabyte = 1073741824;
    private final long fileSize = gigabyte * 5;

    static {
        // Test FOXML object
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<foxml:digitalObject VERSION=\"1.1\" PID=\"" + pid + "\" ");
        sb.append("  xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\" ");
        sb.append("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        sb.append("  xsi:schemaLocation=\"info:fedora/fedora-system:def/foxml# ");
        sb.append("  http://www.fedora.info/definitions/1/0/foxml1-1.xsd\">");
        sb.append("  <foxml:objectProperties>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"A\"/>");
        sb.append("  </foxml:objectProperties>");
        sb.append("</foxml:digitalObject>");

        try {
            DEMO_FOXML = sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }
    }

    @Before
    public void setUp() throws Exception {
        fedoraClient = getFedoraClient();
        apim = fedoraClient.getAPIMMTOM();
        apia = fedoraClient.getAPIAMTOM();
    }
    
    @After
    public void tearDown() {
        fedoraClient.shutdown();
    }

    @Test
    public void testLargeDatastreamIO() throws Exception {
        System.out.println("Running testLargeDatastreams...");

        // Create the object, add the datastream
        System.out.println("  Uploading a file of size " + fileSize + " bytes...");
        String uploadId = upload();
        System.out.println("  Creating data object...");
        apim.ingest(TypeUtility.convertBytesToDataHandler(DEMO_FOXML), FOXML1_1.uri, "new foxml object");
        System.out.println("  Adding uploaded file as a datastream...");
        apim.addDatastream(pid,
                           "DS1",
                           null,
                           "Large Datastream",
                           false,
                           "text/plain",
                           null,
                           uploadId,
                           "M",
                           "A",
                           null,
                           null,
                           "Adding Large Datastream");

        // Export the datastream via API-A-Lite
        System.out.println("  Exporting datastream via API-A-Lite...");
        long exportFileAPIALiteSize = exportAPIALite("DS1");

        // Check the file size to make sure the entire file was retrieved
        assertEquals(fileSize, exportFileAPIALiteSize);

        // Export the datastream via API-A
        System.out.println("  Exporting datastream via API-A...");
        long exportFileAPIASize = exportAPIA("DS1");

        // If file was small enough to transfer via API-A
        if (exportFileAPIASize >= 0) {
            assertEquals(fileSize, exportFileAPIASize);
        }

        // Clean up
        apim.purgeObject(pid, "Removing Test Object", false);
        System.out.println("  Test Complete.");
    }

    private String upload() throws Exception {
        String url = fedoraClient.getUploadURL();
        HttpPost post = new HttpPost(url);
        post.setHeader(HttpHeaders.CONNECTION, "Keep-Alive");
        post.setHeader(HttpHeaders.TRANSFER_ENCODING, "chunked");
        MultipartEntity entity = new MultipartEntity();
        entity.addPart("file", new InputStreamBody(new SizedInputStream(), "file"));
        post.setEntity(entity);
        HttpClient client = fedoraClient.getHttpClient();
        HttpResponse response = null;
        try {
            response = client.execute(post);
            int status = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());

            if (status != HttpStatus.SC_CREATED) {
                throw new IOException("Upload failed: "
                                      + response.getStatusLine().getReasonPhrase()
                                      + ": " + replaceNewlines(body, " "));
            } else {
                body = body.replaceAll("\r", "").replaceAll("\n", "");
                return body;
            }
        } finally {
            if (response != null && response.getEntity() != null) {
                response.getEntity().getContent().close();
            }
        }
    }

    private long exportAPIALite(String dsId) throws Exception {
        String url = apia.describeRepository().getRepositoryBaseURL() +
                     "/get/" + pid + "/" + dsId;
        HttpGet httpMethod = new HttpGet(url);
        httpMethod.setHeader(HttpHeaders.CONNECTION, "Keep-Alive");

        HttpClient client = fedoraClient.getHttpClient();
        HttpResponse response = client.execute(httpMethod);
        BufferedInputStream dataStream =
                new BufferedInputStream(response.getEntity().getContent());

        long bytesRead = 0;
        while (dataStream.read() >= 0) {
            ++bytesRead;
        }

        return bytesRead;
    }

    private long exportAPIA(String dsId) throws Exception {
        MIMETypedStream fileStream = null;
        try {
            fileStream = apia.getDatastreamDissemination(pid, dsId, null);
        } catch (Exception e) {
            if (e.getMessage().indexOf("The datastream you are attempting " +
                                       "to retrieve is too large") > 0) {
                System.out.println("  Expected error generated in API-A export:");
                System.out.println("    Error text: " + e.getMessage());
                return -1;
            } else {
                fail("  Unexpected exception encountered in exportAPIA: " +
                     e.getMessage());
            }
        }

        return TypeUtility.convertDataHandlerToBytes(fileStream.getStream()).length;
    }

    /**
     * Replace newlines with the given string.
     */
    private static String replaceNewlines(String in, String replaceWith) {
        return in.replaceAll("\r", replaceWith).replaceAll("\n", replaceWith);
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestLargeDatastreams.class);
    }

    private class SizedInputStream extends InputStream {

        private long bytesRead = 0;
        Random generator = new Random(System.currentTimeMillis());

        @Override
        public int available() throws IOException {
            return (int) (fileSize - bytesRead);
        }

        @Override
        public int read() throws IOException {
            if (bytesRead < fileSize) {
                ++bytesRead;
                return generator.nextInt(100);
            } else {
                return -1;
            }
        }
    }
}