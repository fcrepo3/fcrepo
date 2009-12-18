
package fedora.test.fesl.restapi;

import java.io.File;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.JUnit4TestAdapter;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fedora.test.fesl.util.DataUtils;
import fedora.test.fesl.util.HttpUtils;
import fedora.test.fesl.util.LoadDataset;
import fedora.test.fesl.util.RemoveDataset;

public class TestREST {

    private static final Logger log = Logger.getLogger(TestREST.class);

    private static final String PROPERTIES = "fedora";

    private static final String RESOURCEBASE =
            "src/test/resources/test-objects/foxml";

    private static HttpUtils httpUtils = null;

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestREST.class);
    }

    @BeforeClass
    public static void setup() {
        PropertyResourceBundle prop =
                (PropertyResourceBundle) ResourceBundle.getBundle(PROPERTIES);
        String username = prop.getString("fedora.admin.username");
        String password = prop.getString("fedora.admin.password");
        String fedoraUrl = prop.getString("fedora.url");

        try {
            if (log.isDebugEnabled()) {
                log.debug("Setting up...");
            }

            httpUtils = new HttpUtils(fedoraUrl, username, password);

            LoadDataset.main(null);
        } catch (Exception e) {
            log.error(e.getMessage());
            Assert.fail(e.getMessage());
        }
    }

    @AfterClass
    public static void teardown() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Tearing down...");
            }

            RemoveDataset.main(null);
        } catch (Exception e) {
            log.error(e.getMessage());
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testFindObjects01() {
        log.info("[ testFindObjects01 ]");

        try {
            String url = "/fedora/objects?terms=*Helicopter";
            String response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            boolean check =
                    response
                            .contains("<a href=\"objects/test:1000003\">test:1000003</a>");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testFindObjects02() {
        log.info("[ testFindObjects02 ]");

        try {
            String url =
                    "/fedora/objects?terms=*Helicopter&"
                            + "pid=true&label=true&state=true&ownerId=true&title=true";
            String response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            boolean check =
                    response
                            .contains("<a href=\"objects/test:1000003\">test:1000003</a>");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testFindObjectsXML01() {
        log.info("[ testFindObjectsXML01 ]");

        try {
            String url = "/fedora/objects?terms=*Helicopter&resultFormat=xml";
            String response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            boolean check = response.contains("<pid>test:1000003</pid>");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testFindObjectsXML02() {
        log.info("[ testFindObjectsXML02 ]");

        try {
            String url =
                    "/fedora/objects?terms=*Helicopter&resultFormat=xml&pid=true"
                            + "&state=true&ownerId=true&title=true&label=true";
            String response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            boolean check = response.contains("<pid>test:1000003</pid>");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testGetDatastreamDissemination() {
        log.info("[ testGetDatastreamDissemination ]");

        try {
            String url = "/fedora/objects/test:1000003/datastreams/TV/content";
            String response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            boolean check = response.contains("tt1035917");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testGetObjectHistory() {
        log.info("[ testGetObjectHistory ]");

        try {
            String url = "/fedora/objects/test:1000003/versions";
            String response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            boolean check = response.contains("test:1000003");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testGetObjectHistoryXML() {
        log.info("[ testGetObjectHistoryXML ]");

        try {
            String url = "/fedora/objects/test:1000003/versions?format=xml";
            String response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            boolean check = response.contains("pid=\"test:1000003\"");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testGetObjectProfile() {
        log.info("[ testGetObjectProfile ]");

        try {
            String url = "/fedora/objects/test:1000001";
            String response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            boolean check = response.contains("<td align=\"left\">Chuck</td>");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testGetObjectProfileXML() {
        log.info("[ testGetObjectProfileXML ]");

        try {
            String url = "/fedora/objects/test:1000001?format=xml";
            String response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            boolean check = response.contains("<objLabel>Chuck</objLabel>");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testListDatastreams() {
        log.info("[ testListDatastreams ]");

        try {
            String url = "/fedora/objects/test:1000003/datastreams";
            String response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            boolean check =
                    response.contains("RDF Metadata")
                            && response.contains("Dublin Core")
                            && response.contains("TV Data");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testListDatastreamsXML() {
        log.info("[ testListDatastreamsXML ]");

        try {
            String url = "/fedora/objects/test:1000003/datastreams?format=xml";
            String response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            boolean check =
                    response.contains("RDF Metadata")
                            && response.contains("Dublin Core")
                            && response.contains("TV Data");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testListMethods() {
        log.info("[ testListMethods ]");

        try {
            String url = "/fedora/objects/test:1000003/methods";
            String response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            boolean check =
                    response.contains("viewObjectProfile")
                            && response.contains("viewMethodIndex")
                            && response.contains("viewItemIndex")
                            && response.contains("viewDublinCore");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testListMethodsXML() {
        log.info("[ testListMethodsXML ]");

        try {
            String url = "/fedora/objects/test:1000003/methods?format=xml";
            String response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            boolean check =
                    response.contains("viewObjectProfile")
                            && response.contains("viewMethodIndex")
                            && response.contains("viewItemIndex")
                            && response.contains("viewDublinCore");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testAddDatastream() {
        log.info("[ testAddDatastream ]");

        try {
            File f =
                    new File(RESOURCEBASE + "/fesl-test/test-AddDatastream.xml");
            byte[] data = DataUtils.loadFile(f);

            String url =
                    "/fedora/objects/test:1000001/datastreams/TESTAD?dsLabel=TESTAD";
            String response = httpUtils.post(url, null, data);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            url = "/fedora/objects/test:1000001/datastreams?format=xml";
            response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }
            boolean check = response.contains("TESTAD");
            Assert.assertTrue("Expected object data not found", check);

            url = "/fedora/objects/test:1000001/datastreams/TESTAD";
            httpUtils.delete(url, null);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testExport() {
        log.info("[ testExport ]");

        try {
            String url = "/fedora/objects/test:1000001/export";
            String response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            boolean check =
                    response.contains("info:fedora/test:1000001/RELS-EXT");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
            re.printStackTrace();
        }
    }

    @Test
    public void testGetDatastream() {
        log.info("[ testGetDatastream ]");

        try {
            String url = "/fedora/objects/test:1000001/datastreams/TV";
            String response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            boolean check =
                    response.contains("<td align=\"left\">TV Data</td>");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
            re.printStackTrace();
        }
    }

    @Test
    public void testGetDatastreamXML() {
        log.info("[ testGetDatastreamXML ]");

        try {
            String url =
                    "/fedora/objects/test:1000001/datastreams/TV?format=xml";
            String response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            boolean check = response.contains("<dsLabel>TV Data</dsLabel>");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
            re.printStackTrace();
        }
    }

    @Test
    public void testGetNextPID() {
        log.info("[ testGetNextPID ]");

        try {
            String url = "/fedora/objects/nextPID";
            String response = httpUtils.post(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            String regex = ".*<td align=\"left\">.+\\:.+</td>.*";
            Pattern p = Pattern.compile(regex, Pattern.DOTALL);
            Matcher m = p.matcher(response);
            Assert.assertTrue("Expected object data not found", m.find());
        } catch (Exception re) {
            Assert.fail(re.getMessage());
            re.printStackTrace();
        }
    }

    @Test
    public void testGetNextPIDXML() {
        log.info("[ testGetNextPIDXML ]");

        try {
            String url = "/fedora/objects/nextPID?format=xml";
            String response = httpUtils.post(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            String regex = ".*<pid>.+\\:.+</pid>.*";
            Pattern p = Pattern.compile(regex, Pattern.DOTALL);
            Matcher m = p.matcher(response);
            Assert.assertTrue("Expected object data not found", m.find());
        } catch (Exception re) {
            Assert.fail(re.getMessage());
            re.printStackTrace();
        }
    }

    @Test
    public void testGetObjectXML() {
        log.info("[ testGetObjectXML ]");

        try {
            String url = "/fedora/objects/test:1000003/objectXML";
            String response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            boolean check =
                    response
                            .contains("<title>Chuck Versus the Helicopter</title>");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
            re.printStackTrace();
        }
    }

    @Test
    public void testIngest() {
        log.info("[ testIngest ]");

        try {
            try {
                // ensure it does not exist
                httpUtils.get("/fedora/objects/test:1000003");

                // delete object
                httpUtils.delete("/fedora/objects/test:1000003", null);
            } catch (ClientProtocolException cpe) {
                // do nothing, expected exception
            }

            // ingest object
            byte[] data =
                    DataUtils.loadFile(RESOURCEBASE + "/fesl/test-1000003.xml");
            String url = "/fedora/objects/new";
            String response = httpUtils.post(url, null, data);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            // ensure it exists
            url = "/fedora/objects/test:1000003";
            response = httpUtils.get(url);
            boolean check = response.contains("Chuck Versus the Helicopter");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
            re.printStackTrace();
        }
    }

    @Test
    public void testModifyDatastream() {
        log.info("[ testModifyDatastream ]");

        try {
            File f =
                    new File(RESOURCEBASE + "/fesl-test/test-AddDatastream.xml");
            byte[] data = DataUtils.loadFile(f);

            // Add test datastream
            String url =
                    "/fedora/objects/test:1000000/datastreams/TESTAD?dsLabel=TESTAD";
            String response = httpUtils.post(url, null, data);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            // ensure it was added
            url = "/fedora/objects/test:1000000/datastreams?format=xml";
            response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }
            boolean check = response.contains("TESTAD");
            Assert.assertTrue("Expected object data not found", check);

            // modify the test datastream
            f = new File(RESOURCEBASE + "/fesl-test/test-modifyDatastream.xml");
            data = DataUtils.loadFile(f);
            url = "/fedora/objects/test:1000000/datastreams/TESTAD";
            response = httpUtils.put(url, null, data);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            // get the new datastream and check it...
            url = "/fedora/objects/test:1000000/datastreams/TESTAD/content";
            response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }
            check = response.contains("Modified datastream: 123998134");
            Assert.assertTrue("Expected object data not found", check);

            url = "/fedora/objects/test:1000000/datastreams/TESTAD";
            httpUtils.delete(url, null);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
            re.printStackTrace();
        }
    }

    @Test
    public void testModifyObject() {
        log.info("[ testModifyObject ]");

        try {
            // modify the object.
            String url =
                    "/fedora/objects/test:1000000?label=This+is+a+New+Label&state=I";
            String response = httpUtils.put(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            // ensure modifications were successful
            url = "/fedora/objects/test:1000000?format=xml";
            response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            boolean check =
                    response
                            .contains("<objLabel>This is a New Label</objLabel>");
            check = check && response.contains("<objState>I</objState>");
            Assert.assertTrue("Expected object data not found", check);

            // delete the changed object
            url = "/fedora/objects/test:1000000";
            httpUtils.delete(url, null);

            // ingest fresh object
            byte[] data =
                    DataUtils.loadFile(RESOURCEBASE + "/fesl/test-1000000.xml");
            url = "/fedora/objects/new";
            response = httpUtils.post(url, null, data);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }
        } catch (Exception re) {
            Assert.fail(re.getMessage());
            re.printStackTrace();
        }
    }

    @Test
    public void testPurgeDatastream() {
        log.info("[ testPurgeDatastream ]");

        try {
            File f =
                    new File(RESOURCEBASE + "/fesl-test/test-AddDatastream.xml");
            byte[] data = DataUtils.loadFile(f);

            // add datastream
            String url =
                    "/fedora/objects/test:1000001/datastreams/TESTAD?dsLabel=TESTAD";
            String response = httpUtils.post(url, null, data);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            // ensure it exists
            url = "/fedora/objects/test:1000001/datastreams?format=xml";
            response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }
            boolean check = response.contains("TESTAD");
            Assert.assertTrue("Expected object data not found [add]", check);

            // delete datastream
            url = "/fedora/objects/test:1000001/datastreams/TESTAD";
            httpUtils.delete(url, null);

            // ensure it is gone
            url = "/fedora/objects/test:1000001/datastreams?format=xml";
            response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }
            check = !response.contains("TESTAD");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
            re.printStackTrace();
        }
    }

    @Test
    public void testPurgeObject() {
        log.info("[ testPurgeDatastream ]");

        try {
            // ensure it exists
            if (log.isDebugEnabled()) {
                log.debug("checking if object exists");
            }
            String url = "/fedora/objects/test:1000000?format=xml";
            String response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }
            boolean check = response.contains("<objLabel>TV Series</objLabel>");
            Assert.assertTrue("Expected object data not found", check);

            // delete datastream
            if (log.isDebugEnabled()) {
                log.debug("deleting object");
            }
            url = "/fedora/objects/test:1000000";
            httpUtils.delete(url, null);

            // ingest object to reset state.
            if (log.isDebugEnabled()) {
                log.debug("ingesting object");
            }
            byte[] data =
                    DataUtils.loadFile(RESOURCEBASE + "/fesl/test-1000000.xml");
            url = "/fedora/objects/new";
            response = httpUtils.post(url, null, data);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            // ensure it exists
            if (log.isDebugEnabled()) {
                log.debug("checking that object now exists");
            }
            url = "/fedora/objects/test:1000000?format=xml";
            response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            check = response.contains("<objLabel>TV Series</objLabel>");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
            re.printStackTrace();
        }
    }

    @Test
    public void testSetDatastreamState() {
        log.info("[ testSetDatastreamState ]");

        try {
            File f =
                    new File(RESOURCEBASE + "/fesl-test/test-AddDatastream.xml");
            byte[] data = DataUtils.loadFile(f);

            // Add test datastream
            String url =
                    "/fedora/objects/test:1000000/datastreams/TESTDS?dsLabel=TESTDS";
            String response = httpUtils.post(url, null, data);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            // ensure it was added
            url = "/fedora/objects/test:1000000/datastreams?format=xml";
            response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }
            boolean check = response.contains("TESTDS");
            Assert.assertTrue("Expected object data not found", check);

            // set the test datastream state
            url = "/fedora/objects/test:1000000/datastreams/TESTDS?dsState=I";
            response = httpUtils.put(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            // get the new datastream and check it...
            url = "/fedora/objects/test:1000000/datastreams/TESTDS?format=xml";
            response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }
            check = response.contains("<dsState>I</dsState>");
            Assert.assertTrue("Expected object data not found", check);

            url = "/fedora/objects/test:1000000/datastreams/TESTDS";
            httpUtils.delete(url, null);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
            re.printStackTrace();
        }
    }

    @Test
    public void testSetDatastreamVersion() {
        log.info("[ testSetDatastreamVersion ]");

        try {
            File f =
                    new File(RESOURCEBASE + "/fesl-test/test-AddDatastream.xml");
            byte[] data = DataUtils.loadFile(f);

            // Add test datastream
            String url =
                    "/fedora/objects/test:1000000/datastreams/TESTDS?dsLabel=TESTDS";
            String response = httpUtils.post(url, null, data);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            // ensure it was added
            url = "/fedora/objects/test:1000000/datastreams?format=xml";
            response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }
            boolean check = response.contains("TESTDS");
            Assert.assertTrue("Expected object data not found", check);

            // set the test datastream state
            url =
                    "/fedora/objects/test:1000000/datastreams/TESTDS?versionable=false";
            response = httpUtils.put(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }

            // get the new datastream and check it...
            url = "/fedora/objects/test:1000000/datastreams/TESTDS?format=xml";
            response = httpUtils.get(url);
            if (log.isDebugEnabled()) {
                log.debug("http response:\n" + response);
            }
            check = response.contains("<dsVersionable>false</dsVersionable>");
            Assert.assertTrue("Expected object data not found", check);

            url = "/fedora/objects/test:1000000/datastreams/TESTDS";
            httpUtils.delete(url, null);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
            re.printStackTrace();
        }
    }
}
