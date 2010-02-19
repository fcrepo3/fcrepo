
package org.fcrepo.test.fesl.restapi;

import java.io.File;

import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.fcrepo.test.fesl.util.DataUtils;
import org.fcrepo.test.fesl.util.FedoraUtil;
import org.fcrepo.test.fesl.util.HttpUtils;
import org.fcrepo.test.fesl.util.LoadDataset;
import org.fcrepo.test.fesl.util.RemoveDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.JUnit4TestAdapter;


public class TestREST {

    private static final Logger logger =
            LoggerFactory.getLogger(TestREST.class);

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
        //String fedoraUrl = prop.getString("fedora.url");
        String fedoraUrl = FedoraUtil.getBaseURL();

        try {
            logger.debug("Setting up...");

            httpUtils = new HttpUtils(fedoraUrl, username, password);

            LoadDataset.main(null);
        } catch (Exception e) {
            logger.error(e.getMessage());
            Assert.fail(e.getMessage());
        }
    }

    @AfterClass
    public static void teardown() {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Tearing down...");
            }

            RemoveDataset.main(null);
        } catch (Exception e) {
            logger.error(e.getMessage());
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testFindObjects01() {
        logger.info("[ testFindObjects01 ]");

        try {
            String url = "/fedora/objects?terms=*Helicopter";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
        logger.info("[ testFindObjects02 ]");

        try {
            String url =
                    "/fedora/objects?terms=*Helicopter&"
                            + "pid=true&label=true&state=true&ownerId=true&title=true";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
        logger.info("[ testFindObjectsXML01 ]");

        try {
            String url = "/fedora/objects?terms=*Helicopter&resultFormat=xml";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            boolean check = response.contains("<pid>test:1000003</pid>");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testFindObjectsXML02() {
        logger.info("[ testFindObjectsXML02 ]");

        try {
            String url =
                    "/fedora/objects?terms=*Helicopter&resultFormat=xml&pid=true"
                            + "&state=true&ownerId=true&title=true&label=true";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            boolean check = response.contains("<pid>test:1000003</pid>");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testGetDatastreamDissemination() {
        logger.info("[ testGetDatastreamDissemination ]");

        try {
            String url = "/fedora/objects/test:1000003/datastreams/TV/content";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            boolean check = response.contains("tt1035917");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testGetObjectHistory() {
        logger.info("[ testGetObjectHistory ]");

        try {
            String url = "/fedora/objects/test:1000003/versions";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            boolean check = response.contains("test:1000003");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testGetObjectHistoryXML() {
        logger.info("[ testGetObjectHistoryXML ]");

        try {
            String url = "/fedora/objects/test:1000003/versions?format=xml";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            boolean check = response.contains("pid=\"test:1000003\"");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testGetObjectProfile() {
        logger.info("[ testGetObjectProfile ]");

        try {
            String url = "/fedora/objects/test:1000001";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            boolean check = response.contains("<td align=\"left\">Chuck</td>");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testGetObjectProfileXML() {
        logger.info("[ testGetObjectProfileXML ]");

        try {
            String url = "/fedora/objects/test:1000001?format=xml";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            boolean check = response.contains("<objLabel>Chuck</objLabel>");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception re) {
            Assert.fail(re.getMessage());
        }
    }

    @Test
    public void testListDatastreams() {
        logger.info("[ testListDatastreams ]");

        try {
            String url = "/fedora/objects/test:1000003/datastreams";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
        logger.info("[ testListDatastreamsXML ]");

        try {
            String url = "/fedora/objects/test:1000003/datastreams?format=xml";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
        logger.info("[ testListMethods ]");

        try {
            String url = "/fedora/objects/test:1000003/methods";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
        logger.info("[ testListMethodsXML ]");

        try {
            String url = "/fedora/objects/test:1000003/methods?format=xml";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
        logger.info("[ testAddDatastream ]");

        try {
            File f =
                    new File(RESOURCEBASE + "/fesl-test/test-AddDatastream.xml");
            byte[] data = DataUtils.loadFile(f);

            String url =
                    "/fedora/objects/test:1000001/datastreams/TESTAD?dsLabel=TESTAD";
            String response = httpUtils.post(url, null, data);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            url = "/fedora/objects/test:1000001/datastreams?format=xml";
            response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
        logger.info("[ testExport ]");

        try {
            String url = "/fedora/objects/test:1000001/export";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
        logger.info("[ testGetDatastream ]");

        try {
            String url = "/fedora/objects/test:1000001/datastreams/TV";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
        logger.info("[ testGetDatastreamXML ]");

        try {
            String url =
                    "/fedora/objects/test:1000001/datastreams/TV?format=xml";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
        logger.info("[ testGetNextPID ]");

        try {
            String url = "/fedora/objects/nextPID";
            String response = httpUtils.post(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
        logger.info("[ testGetNextPIDXML ]");

        try {
            String url = "/fedora/objects/nextPID?format=xml";
            String response = httpUtils.post(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
        logger.info("[ testGetObjectXML ]");

        try {
            String url = "/fedora/objects/test:1000003/objectXML";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
        logger.info("[ testIngest ]");

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
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
        logger.info("[ testModifyDatastream ]");

        try {
            File f =
                    new File(RESOURCEBASE + "/fesl-test/test-AddDatastream.xml");
            byte[] data = DataUtils.loadFile(f);

            // Add test datastream
            String url =
                    "/fedora/objects/test:1000000/datastreams/TESTAD?dsLabel=TESTAD";
            String response = httpUtils.post(url, null, data);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            // ensure it was added
            url = "/fedora/objects/test:1000000/datastreams?format=xml";
            response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }
            boolean check = response.contains("TESTAD");
            Assert.assertTrue("Expected object data not found", check);

            // modify the test datastream
            f = new File(RESOURCEBASE + "/fesl-test/test-modifyDatastream.xml");
            data = DataUtils.loadFile(f);
            url = "/fedora/objects/test:1000000/datastreams/TESTAD";
            response = httpUtils.put(url, null, data);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            // get the new datastream and check it...
            url = "/fedora/objects/test:1000000/datastreams/TESTAD/content";
            response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
        logger.info("[ testModifyObject ]");

        try {
            // modify the object.
            String url =
                    "/fedora/objects/test:1000000?label=This+is+a+New+Label&state=I";
            String response = httpUtils.put(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            // ensure modifications were successful
            url = "/fedora/objects/test:1000000?format=xml";
            response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }
        } catch (Exception re) {
            Assert.fail(re.getMessage());
            re.printStackTrace();
        }
    }

    @Test
    public void testPurgeDatastream() {
        logger.info("[ testPurgeDatastream ]");

        try {
            File f =
                    new File(RESOURCEBASE + "/fesl-test/test-AddDatastream.xml");
            byte[] data = DataUtils.loadFile(f);

            // add datastream
            String url =
                    "/fedora/objects/test:1000001/datastreams/TESTAD?dsLabel=TESTAD";
            String response = httpUtils.post(url, null, data);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            // ensure it exists
            url = "/fedora/objects/test:1000001/datastreams?format=xml";
            response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }
            boolean check = response.contains("TESTAD");
            Assert.assertTrue("Expected object data not found [add]", check);

            // delete datastream
            url = "/fedora/objects/test:1000001/datastreams/TESTAD";
            httpUtils.delete(url, null);

            // ensure it is gone
            url = "/fedora/objects/test:1000001/datastreams?format=xml";
            response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
        logger.info("[ testPurgeDatastream ]");

        try {
            // ensure it exists
            if (logger.isDebugEnabled()) {
                logger.debug("checking if object exists");
            }
            String url = "/fedora/objects/test:1000000?format=xml";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }
            boolean check = response.contains("<objLabel>TV Series</objLabel>");
            Assert.assertTrue("Expected object data not found", check);

            // delete datastream
            if (logger.isDebugEnabled()) {
                logger.debug("deleting object");
            }
            url = "/fedora/objects/test:1000000";
            httpUtils.delete(url, null);

            // ingest object to reset state.
            if (logger.isDebugEnabled()) {
                logger.debug("ingesting object");
            }
            byte[] data =
                    DataUtils.loadFile(RESOURCEBASE + "/fesl/test-1000000.xml");
            url = "/fedora/objects/new";
            response = httpUtils.post(url, null, data);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            // ensure it exists
            if (logger.isDebugEnabled()) {
                logger.debug("checking that object now exists");
            }
            url = "/fedora/objects/test:1000000?format=xml";
            response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
        logger.info("[ testSetDatastreamState ]");

        try {
            File f =
                    new File(RESOURCEBASE + "/fesl-test/test-AddDatastream.xml");
            byte[] data = DataUtils.loadFile(f);

            // Add test datastream
            String url =
                    "/fedora/objects/test:1000000/datastreams/TESTDS?dsLabel=TESTDS";
            String response = httpUtils.post(url, null, data);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            // ensure it was added
            url = "/fedora/objects/test:1000000/datastreams?format=xml";
            response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }
            boolean check = response.contains("TESTDS");
            Assert.assertTrue("Expected object data not found", check);

            // set the test datastream state
            url = "/fedora/objects/test:1000000/datastreams/TESTDS?dsState=I";
            response = httpUtils.put(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            // get the new datastream and check it...
            url = "/fedora/objects/test:1000000/datastreams/TESTDS?format=xml";
            response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
        logger.info("[ testSetDatastreamVersion ]");

        try {
            File f =
                    new File(RESOURCEBASE + "/fesl-test/test-AddDatastream.xml");
            byte[] data = DataUtils.loadFile(f);

            // Add test datastream
            String url =
                    "/fedora/objects/test:1000000/datastreams/TESTDS?dsLabel=TESTDS";
            String response = httpUtils.post(url, null, data);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            // ensure it was added
            url = "/fedora/objects/test:1000000/datastreams?format=xml";
            response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }
            boolean check = response.contains("TESTDS");
            Assert.assertTrue("Expected object data not found", check);

            // set the test datastream state
            url =
                    "/fedora/objects/test:1000000/datastreams/TESTDS?versionable=false";
            response = httpUtils.put(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            // get the new datastream and check it...
            url = "/fedora/objects/test:1000000/datastreams/TESTDS?format=xml";
            response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
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
