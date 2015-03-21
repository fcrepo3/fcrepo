/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.api;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import java.util.HashMap;
import java.util.Map;

import junit.framework.JUnit4TestAdapter;

import org.apache.http.HttpHeaders;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.fcrepo.client.FedoraClient;
import org.fcrepo.common.Models;
import org.fcrepo.common.http.HttpInputStream;
import org.fcrepo.test.FedoraServerTestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.w3c.dom.Document;




/**
 * @author Edwin Shin
 */
public class TestAPIALite
        extends FedoraServerTestCase {

    private static FedoraClient s_client;

    @BeforeClass
    public static void bootStrap() throws Exception {
        s_client = getFedoraClient();
        // demo:5
        ingestSimpleImageDemoObjects(s_client);
        // demo:31
        ingestSimpleDocumentDemoObjects(s_client);
        // demo:27, demo:29
        ingestImageManipulationDemoObjects(s_client);
        // smiley objects
        ingestImageCollectionDemoObjects(s_client);
    }
    
    @AfterClass
    public static void cleanUp() throws Exception {
        purgeDemoObjects(s_client);
        s_client.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put(NS_FEDORA_TYPES_PREFIX, NS_FEDORA_TYPES);
        nsMap.put(OAI_DC.prefix, OAI_DC.uri);
        nsMap.put(DC.prefix, DC.uri);
        nsMap.put(ACCESS.prefix, ACCESS.uri);
        NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
        XMLUnit.setXpathNamespaceContext(ctx);
    }

    @After
    public void tearDown() {
        XMLUnit.setXpathNamespaceContext(SimpleNamespaceContext.EMPTY_CONTEXT);
    }

    @Test
    public void testDescribeRepository() throws Exception {
        Document result;
        result = getXMLQueryResult(s_client, "/describe?xml=true");
        assertXpathExists(String.format("/%s:fedoraRepository/%s:repositoryName",
                                        ACCESS.prefix, ACCESS.prefix), result);
    }

    @Test
    public void testGetDatastreamDissemination() throws Exception {
        Document result;

        // test for type X datastream
        result = getXMLQueryResult(s_client, "/get/demo:5/DC");
        assertXpathExists("/oai_dc:dc", result);
        assertXpathEvaluatesTo("demo:5", "/oai_dc:dc/dc:identifier/text()", result);

        // test for type E datastream
        HttpInputStream in = s_client.get("/get/demo:SmileyBeerGlass/MEDIUM_SIZE", true);
        String actualType = in.getContentType();
        long actualLength = in.getContentLength();
        in.close();
        assertEquals("image/jpeg", actualType);
        assertTrue(actualLength > 0);

        // test for type R datastream
        in = s_client.get("/get/demo:31/DS3", false, false);
        int actualCode = in.getStatusCode();
        in.close();
        assertEquals(302, actualCode);

        // test for type M datastream
        in = s_client.get("/get/demo:5/THUMBRES_IMG", true);
        actualType = in.getContentType();
        in.close();
        assertEquals("image/jpeg", actualType);
    }

    @Test
    public void testGetDisseminationDefault() throws Exception {
        HttpInputStream his =
                s_client.get("/get/demo:5/fedora-system:3/viewDublinCore", true);
        String actual = his.getContentType(); 
        his.close();
        assertEquals("text/html", actual);
    }

// FIXME: This test intermittently fails. See FCREPO-457
/*
    public void testGetDisseminationChained() throws Exception {
        HttpInputStream his =
                client.get("/get/demo:26/demo:19/getPDF", true);
        assertEquals(his.getContentType(), "application/pdf");
        his.close();
    }
*/

    @Test
    public void testGetDisseminationUserInput() throws Exception {
        HttpInputStream his =
                s_client.get("/get/demo:29/demo:27/convertImage?convertTo=gif", true);
        String actual = his.getContentType(); 
        his.close();
        assertEquals("image/gif", actual);
    }

    @Test
    public void testObjectHistory() throws Exception {
        Document result;
        result = getXMLQueryResult(s_client, "/getObjectHistory/demo:5?xml=true");
        assertXpathExists(String.format("/%s:fedoraObjectHistory/%s:objectChangeDate",
                                        ACCESS.prefix, ACCESS.prefix), result);
    }

    @Test
    public void testGetObjectProfile() throws Exception {
        Document result;
        result = getXMLQueryResult(s_client, "/get/demo:5?xml=true");
        assertXpathEvaluatesTo("demo:5",
                               String.format("/%s:objectProfile/attribute::pid",
                                             ACCESS.prefix),
                               result);
    }

    @Test
    public void testGetObjectProfileBasicCModel() throws Exception {
        String testExpression = String.format("count("
                + "/%s:objectProfile/%s:objModels/%s:model[normalize-space()='"
                + Models.FEDORA_OBJECT_CURRENT.uri + "'])",
                ACCESS.prefix, ACCESS.prefix, ACCESS.prefix);
        for (String pid : new String[] { "demo:SmileyPens",
                                         "demo:SmileyGreetingCard" }) {
            Document result = getXMLQueryResult(s_client, "/get/" + pid + "?xml=true");
            assertXpathEvaluatesTo("1", testExpression, result);
        }
    }

    @Test
    public void testListDatastreams() throws Exception {
        Document result;
        result = getXMLQueryResult(s_client, "/listDatastreams/demo:5?xml=true");
        assertXpathEvaluatesTo("6",
                               String.format("count(/%s:objectDatastreams/%s:datastream)",
                                             ACCESS.prefix, ACCESS.prefix),
                               result);
    }

    @Test
    public void testListMethods() throws Exception {
        Document result;
        result = getXMLQueryResult(s_client, "/listMethods/demo:5?xml=true");
        assertXpathEvaluatesTo("8", String.format("count(/%s:objectMethods/%s:sDef/%s:method)",
                                                  ACCESS.prefix, ACCESS.prefix, ACCESS.prefix), result);
    }

    @Test
    public void testAccessParmResolver() throws Exception {
        String location = "/getAccessParmResolver?PID=fedora-system:ContentModel-3.0&sDefPID=fedora-system:3&methodName=viewObjectProfile";
        HttpInputStream result = s_client.get(getBaseURL() + location, false, false);
        assertEquals(302, result.getStatusCode());
        String expected = getBaseURL() + "/get/fedora-system:ContentModel-3.0/fedora-system:3/viewObjectProfile/";
        assertEquals(expected, result.getResponseHeader(HttpHeaders.LOCATION).getValue());
    }
    @Test
    public void testConcurrentRequests() throws Exception {
        GetCallable[] callables = {
                new GetCallable(s_client,"/get/demo:29/demo:27/convertImage?convertTo=gif"),
                new GetCallable(s_client,"/get/demo:SmileyBeerGlass/MEDIUM_SIZE"),
                new GetCallable(s_client,"/get/demo:5?xml=true")
        };
        runConcurrent(callables);
        assertEquals("image/gif",callables[0].lastType);
        assertEquals(356909,callables[0].lastLength);
        assertEquals(callables[1].lastType,"image/jpeg");
        assertEquals(17109,callables[1].lastLength);
        assertEquals(callables[2].lastType,"text/xml;charset=UTF-8");
        assertEquals(callables[2].lastLength,924);
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestAPIALite.class);
    }

    public static void main(String[] args) {
        JUnitCore.runClasses(TestAPIALite.class);
    }

}
