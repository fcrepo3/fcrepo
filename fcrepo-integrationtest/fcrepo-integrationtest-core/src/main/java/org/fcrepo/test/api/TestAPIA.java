/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;

import org.junit.After;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.fcrepo.client.FedoraClient;

import org.fcrepo.common.Models;

import org.fcrepo.server.access.FedoraAPIAMTOM;
import org.fcrepo.server.types.mtom.gen.ComparisonOperator;
import org.fcrepo.server.types.mtom.gen.Condition;
import org.fcrepo.server.types.mtom.gen.DatastreamDef;
import org.fcrepo.server.types.mtom.gen.FieldSearchQuery;
import org.fcrepo.server.types.mtom.gen.FieldSearchQuery.Conditions;
import org.fcrepo.server.types.mtom.gen.FieldSearchResult;
import org.fcrepo.server.types.mtom.gen.FieldSearchResult.ResultList;
import org.fcrepo.server.types.mtom.gen.GetDissemination.Parameters;
import org.fcrepo.server.types.mtom.gen.MIMETypedStream;
import org.fcrepo.server.types.mtom.gen.ObjectFactory;
import org.fcrepo.server.types.mtom.gen.ObjectFields;
import org.fcrepo.server.types.mtom.gen.ObjectMethodsDef;
import org.fcrepo.server.types.mtom.gen.ObjectProfile;
import org.fcrepo.server.types.mtom.gen.ObjectProfile.ObjModels;
import org.fcrepo.server.types.mtom.gen.Property;
import org.fcrepo.server.types.mtom.gen.RepositoryInfo;
import org.fcrepo.server.utilities.TypeUtility;

import org.fcrepo.test.DemoObjectTestSetup;
import org.fcrepo.test.FedoraServerTestCase;





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

    private FedoraAPIAMTOM apia;

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
        java.math.BigInteger maxResults = new java.math.BigInteger("" + 100);
        FieldSearchQuery query = new FieldSearchQuery();
        Conditions conds = new Conditions();
        Condition cond = new Condition();
        cond.setOperator(ComparisonOperator.EQ);
        cond.setProperty("pid");
        cond.setValue("demo:5");
        conds.getCondition().add(cond);
        ObjectFactory factory = new ObjectFactory();
        query.setConditions(factory.createFieldSearchQueryConditions(conds));
        FieldSearchResult result =
                apia.findObjects(TypeUtility.convertStringtoAOS(resultFields), maxResults, query);
        ResultList resultList = result.getResultList();
        List<ObjectFields> fields = resultList.getObjectFields();
        assertEquals(1, fields.size());
        assertEquals("demo:5", fields.get(0).getPid());
    }

    public void testGetDatastreamDissemination() throws Exception {
        MIMETypedStream ds = null;

        // test for type X datastream
        ds = apia.getDatastreamDissemination("demo:5", "DC", null);
        byte[] bytes = TypeUtility.convertDataHandlerToBytes(ds.getStream());
        String xml = new String(bytes, "UTF-8");
        assertXpathExists("/oai_dc:dc", xml);
        assertXpathEvaluatesTo("demo:5", "/oai_dc:dc/dc:identifier/text( )", xml);
        assertEquals(ds.getMIMEType(), "text/xml");

        // test for type E datastream
        ds = apia.getDatastreamDissemination("demo:SmileyBeerGlass", "MEDIUM_SIZE", null);
        bytes = TypeUtility.convertDataHandlerToBytes(ds.getStream());
        assertEquals(ds.getMIMEType(), "image/jpeg");
        assertTrue(bytes.length > 0);

        // test for type R datastream
        ds = apia.getDatastreamDissemination("demo:31", "DS3", null);
        assertEquals(ds.getMIMEType(), "application/fedora-redirect");

        // test for type M datastream
        ds = apia.getDatastreamDissemination("demo:5", "THUMBRES_IMG", null);
        bytes = TypeUtility.convertDataHandlerToBytes(ds.getStream());
        assertEquals(ds.getMIMEType(), "image/jpeg");
        assertTrue(bytes.length > 0);
    }

    public void testGetDisseminationDefault() throws Exception {
        MIMETypedStream diss = null;
        Parameters params = new Parameters();
        params.getParameter().add(new Property());
        diss = apia.getDissemination("demo:5",
                                      "fedora-system:3",
                                      "viewDublinCore",
                                      params,
                                      null);
        assertEquals(diss.getMIMEType(), "text/html");
        assertTrue(TypeUtility.convertDataHandlerToBytes(diss.getStream()).length > 0);
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
        Parameters params = new Parameters();
        Property prop = new Property();
        prop.setName("convertTo");
        prop.setValue("gif");
        params.getParameter().add(prop);
        diss = apia.getDissemination("demo:29",
                                     "demo:27",
                                     "convertImage",
                                     params,
                                     null);
        assertEquals(diss.getMIMEType(), "image/gif");
        assertTrue(TypeUtility.convertDataHandlerToBytes(diss.getStream()).length > 0);
    }

    public void testObjectHistory() throws Exception {
        List<String> timestamps = apia.getObjectHistory("demo:5");
        assertTrue(timestamps.size() > 0);
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
            ObjModels objModels = profile.getObjModels();
            for (String objModel : objModels.getModel()) {
                if (objModel.equals(Models.FEDORA_OBJECT_CURRENT.uri)) {
                    found = true;
                }
            }
            assertTrue(found);
        }
    }

    public void testListDatastreams() throws Exception {
        List<DatastreamDef> dsDefs = apia.listDatastreams("demo:5", null);
        assertEquals(6, dsDefs.size());
    }

    public void testListMethods() throws Exception {
        List<ObjectMethodsDef> methodDefs = apia.listMethods("demo:5", null);
        assertEquals(8, methodDefs.size());
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
