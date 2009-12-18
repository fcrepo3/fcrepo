/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test.api;

import java.util.HashMap;
import java.util.Map;

import org.apache.axis.types.NonNegativeInteger;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;

import org.junit.After;

import junit.framework.Test;
import junit.framework.TestSuite;

import fedora.client.FedoraClient;

import fedora.common.Models;

import fedora.server.access.FedoraAPIA;
import fedora.server.types.gen.ComparisonOperator;
import fedora.server.types.gen.Condition;
import fedora.server.types.gen.DatastreamDef;
import fedora.server.types.gen.FieldSearchQuery;
import fedora.server.types.gen.FieldSearchResult;
import fedora.server.types.gen.MIMETypedStream;
import fedora.server.types.gen.ObjectFields;
import fedora.server.types.gen.ObjectMethodsDef;
import fedora.server.types.gen.ObjectProfile;
import fedora.server.types.gen.Property;
import fedora.server.types.gen.RepositoryInfo;

import fedora.test.DemoObjectTestSetup;
import fedora.test.FedoraServerTestCase;

/**
 * Test of the Fedora Access Service (API-A). describeRepository findObjects
 * getDatastreamDissemination getDissemination getObjectHistory getObjectProfile
 * listDatastreams listMethods resumeFindObjects See:
 * http://www.fedora.info/definitions/1/0/api/Fedora-API-A.html
 *
 * @author Edwin Shin
 */
public class TestAPIA
        extends FedoraServerTestCase {

    private FedoraAPIA apia;

    public static Test suite() {
        TestSuite suite = new TestSuite("APIA TestSuite");
        suite.addTestSuite(TestAPIA.class);
        return new DemoObjectTestSetup(suite);
    }

    public void testDescribeRepository() throws Exception {
        RepositoryInfo describe = apia.describeRepository();
        assertTrue(!describe.getRepositoryName().equals(""));
    }

    public void testFindObjects() throws Exception {
        // Test that a search for pid=demo:5 returns one result; demo:5
        String[] resultFields = {"pid"};
        NonNegativeInteger maxResults = new NonNegativeInteger("" + 100);
        Condition[] condition =
                {new Condition("pid", ComparisonOperator.eq, "demo:5")};
        FieldSearchQuery query = new FieldSearchQuery(condition, null);
        FieldSearchResult result =
                apia.findObjects(resultFields, maxResults, query);
        ObjectFields[] fields = result.getResultList();
        assertEquals(1, fields.length);
        assertEquals("demo:5", fields[0].getPid());
    }

    public void testGetDatastreamDissemination() throws Exception {
        MIMETypedStream ds = null;

        // test for type X datastream
        ds = apia.getDatastreamDissemination("demo:5", "DC", null);
        String xml = new String(ds.getStream(), "UTF-8");
        assertXpathExists("/oai_dc:dc", xml);
        assertXpathEvaluatesTo("demo:5", "/oai_dc:dc/dc:identifier/text( )", xml);
        assertEquals(ds.getMIMEType(), "text/xml");

        // test for type E datastream
        ds = apia.getDatastreamDissemination("demo:SmileyBeerGlass", "MEDIUM_SIZE", null);
        assertEquals(ds.getMIMEType(), "image/jpeg");
        assertTrue(ds.getStream().length > 0);

        // test for type R datastream
        ds = apia.getDatastreamDissemination("demo:31", "DS3", null);
        assertEquals(ds.getMIMEType(), "application/fedora-redirect");

        // test for type M datastream
        ds = apia.getDatastreamDissemination("demo:5", "THUMBRES_IMG", null);
        assertEquals(ds.getMIMEType(), "image/jpeg");
        assertTrue(ds.getStream().length > 0);
    }

    public void testGetDisseminationDefault() throws Exception {
        MIMETypedStream diss = null;
        diss = apia.getDissemination("demo:5",
                                      "fedora-system:3",
                                      "viewDublinCore",
                                      new Property[0],
                                      null);
        assertEquals(diss.getMIMEType(), "text/html");
        assertTrue(diss.getStream().length > 0);
    }

// FIXME: This test intermittently fails. See FCREPO-457
/*
    public void testGetDisseminationChained() throws Exception {
        MIMETypedStream diss = null;
        diss = apia.getDissemination("demo:26",
                                     "demo:19",
                                     "getPDF",
                                     new Property[0],
                                     null);
        assertEquals(diss.getMIMEType(), "application/pdf");
        assertTrue(diss.getStream().length > 0);
    }
*/

    public void testGetDisseminationUserInput() throws Exception {
        MIMETypedStream diss = null;
        Property[] userInput = new Property[1];
        userInput[0] = new Property("convertTo", "gif");
        diss = apia.getDissemination("demo:29",
                                     "demo:27",
                                     "convertImage",
                                     userInput,
                                     null);
        assertEquals(diss.getMIMEType(), "image/gif");
        assertTrue(diss.getStream().length > 0);
    }

    public void testObjectHistory() throws Exception {
        String[] timestamps = apia.getObjectHistory("demo:5");
        assertTrue(timestamps.length > 0);
    }

    public void testGetObjectProfile() throws Exception {
        ObjectProfile profile = apia.getObjectProfile("demo:5", null);
        assertEquals("demo:5", profile.getPid());
        assertTrue(!profile.getObjDissIndexViewURL().equals(""));
        assertTrue(!profile.getObjItemIndexViewURL().equals(""));
    }

    public void testGetObjectProfileBasicCModel() throws Exception {
        for (String pid : new String[] { "demo:SmileyPens",
                                         "demo:SmileyGreetingCard" }) {
            ObjectProfile profile = apia.getObjectProfile(pid, null);
            boolean found = false;
            for (String objModel : profile.getObjModels()) {
                if (objModel.equals(Models.FEDORA_OBJECT_CURRENT.uri)) {
                    found = true;
                }
            }
            assertTrue(found);
        }
    }

    public void testListDatastreams() throws Exception {
        DatastreamDef[] dsDefs = apia.listDatastreams("demo:5", null);
        assertEquals(6, dsDefs.length);
    }

    public void testListMethods() throws Exception {
        ObjectMethodsDef[] methodDefs = apia.listMethods("demo:5", null);
        assertEquals(8, methodDefs.length);
    }

    @Override
    public void setUp() throws Exception {
        FedoraClient client = getFedoraClient();
        apia = client.getAPIA();
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put(OAI_DC.prefix, OAI_DC.uri);
        nsMap.put(DC.prefix, DC.uri);
        NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
        XMLUnit.setXpathNamespaceContext(ctx);
    }

    @Override
    @After
    public void tearDown() {
        XMLUnit.setXpathNamespaceContext(SimpleNamespaceContext.EMPTY_CONTEXT);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestAPIA.class);
    }

}
