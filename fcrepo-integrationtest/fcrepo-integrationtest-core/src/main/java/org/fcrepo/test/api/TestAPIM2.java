/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.api;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import junit.framework.JUnit4TestAdapter;

import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.fcrepo.client.FedoraClient;
import org.fcrepo.common.Constants;
import org.fcrepo.server.access.FedoraAPIA;
import org.fcrepo.server.management.FedoraAPIM;
import org.fcrepo.server.types.gen.ComparisonOperator;
import org.fcrepo.server.types.gen.Condition;
import org.fcrepo.server.types.gen.FieldSearchQuery;
import org.fcrepo.server.types.gen.ObjectFactory;
import org.fcrepo.server.utilities.TypeUtility;
import org.fcrepo.test.FedoraServerTestCase;
import org.fcrepo.test.TemplatedResourceIterator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;

/**
 * Test APIM based on templating of resource files
 * These tests also use the non-MTOM WS client
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class TestAPIM2
        extends FedoraServerTestCase
        implements Constants {

    private static FedoraClient s_client;
    private FedoraAPIM apim;
    private FedoraAPIA apia;

    @BeforeClass
    public static void bootStrap() throws Exception {
        s_client = getFedoraClient();
        ingestDemoObjects(s_client);
    }
    
    @AfterClass
    public static void cleanUp() throws Exception {
        purgeDemoObjects(s_client);
        s_client.shutdown();
    }


    @Before
    public void setUp() throws Exception {
        apim = s_client.getAPIM();
        apia = s_client.getAPIA();


        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        nsMap.put("dc", "http://purl.org/dc/elements/1.1/");
        nsMap.put("foxml", "info:fedora/fedora-system:def/foxml#");
        NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
        XMLUnit.setXpathNamespaceContext(ctx);

        // not really necessary, but will cope with any junk left from other tests
        purgeDemoObjects(s_client);

    }

    @After
    public void tearDown() throws Exception {
        // assumes all our test objects are in the demo namespace
        purgeDemoObjects(s_client);

        XMLUnit.setXpathNamespaceContext(SimpleNamespaceContext.EMPTY_CONTEXT);
    }

    @Test
    public void testIngest() throws Exception {

        String resourceDirName = "src/test/resources/APIM2/foxml/";
        String[] resourceFilenames = new File(resourceDirName).list();

        // ingest resources, substituting from file "values"
        int count = 0; // count ingested objects
        for (String resourceFilename : resourceFilenames) {
            File resourceFile = new File(resourceDirName + resourceFilename);
            if (resourceFile.isFile()) {
                String resource = FileUtils.readFileToString(resourceFile, "UTF-8");
                TemplatedResourceIterator tri = new TemplatedResourceIterator(resource, "src/test/resources/APIM2/values");
                while (tri.hasNext()) {
                    byte[] foxml = tri.next().getBytes("UTF-8");
                    apim.ingest(foxml, FOXML1_1.uri,"ingesting new foxml object");
                    count++;
                }
            }
        }

        assertEquals("Ingested object count", count, getDemoObjects(s_client).size());

        // ingest resources, substituting from file "valuesplain"
        for (String resourceFilename : resourceFilenames) {
            File resourceFile = new File(resourceDirName + resourceFilename);
            if (resourceFile.isFile()) {
                String resource = FileUtils.readFileToString(resourceFile, "UTF-8");
                TemplatedResourceIterator tri = new TemplatedResourceIterator(resource, "src/test/resources/APIM2/valuesplain");
                while (tri.hasNext()) {
                    byte[] foxml = tri.next().getBytes("UTF-8");
                    apim.ingest(foxml, FOXML1_1.uri,"ingesting new foxml object");
                    count++;
                }
            }
        }

        assertEquals("Ingested object count", count, getDemoObjects(s_client).size());

    }

    @Test
    public void testFieldSearch() throws Exception {

        // get some sample objects ingested
        String resourceDirName = "src/test/resources/APIM2/foxml/";
        String[] resourceFilenames = new File(resourceDirName).list();

        for (String resourceFilename : resourceFilenames) {
            File resourceFile = new File(resourceDirName + resourceFilename);
            if (resourceFile.isFile()) {
                String resource = FileUtils.readFileToString(resourceFile, "UTF-8");
                TemplatedResourceIterator tri = new TemplatedResourceIterator(resource, "src/test/resources/APIM2/valuesplain");
                while (tri.hasNext()) {
                    byte[] foxml = tri.next().getBytes("UTF-8");
                    apim.ingest(foxml, FOXML1_1.uri,"ingesting new foxml object");
                }
            }
        }

        String[] resultFields = {"pid", "title"};
        java.math.BigInteger maxResults = new java.math.BigInteger("100");


        String termsTemplate = "$value$";
        TemplatedResourceIterator tri = new TemplatedResourceIterator(termsTemplate, "src/test/resources/APIM2/searchvalues");
        while (tri.hasNext()) {
            FieldSearchQuery query;
            // using conditions
            FieldSearchQuery.Conditions conds = new FieldSearchQuery.Conditions();
            Condition c = new Condition();
            c.setProperty("pid");
            c.setOperator(ComparisonOperator.fromValue("eq"));
            c.setValue(tri.getAttributeValue("value"));
            conds.getCondition().add(c);
            query = new FieldSearchQuery();
            ObjectFactory factory = new ObjectFactory();
            query.setConditions(factory.createFieldSearchQueryConditions(conds));
            apia.findObjects(TypeUtility.convertStringtoAOS(resultFields), maxResults, query);


            String terms = tri.next();
            query = new FieldSearchQuery();
            query.setTerms(factory.createFieldSearchQueryTerms(terms));
            apia.findObjects(TypeUtility.convertStringtoAOS(resultFields), maxResults, query);

        }

        purgeDemoObjects(s_client);

        for (String resourceFilename : resourceFilenames) {
            File resourceFile = new File(resourceDirName + resourceFilename);
            if (resourceFile.isFile()) {
                String resource = FileUtils.readFileToString(resourceFile, "UTF-8");
                tri = new TemplatedResourceIterator(resource, "src/test/resources/APIM2/valuesplain");
                while (tri.hasNext()) {
                    byte[] foxml = tri.next().getBytes("UTF-8");
                    apim.ingest(foxml, FOXML1_1.uri,"ingesting new foxml object");
                }
            }
        }

        purgeDemoObjects(s_client);

    }
    
    @Test
    public void testObjectMethods() throws Exception {
        // test object
        String resfile = "src/test/resources/APIM2/foxml/demo_SmileyBeerGlass.xml";

        File resourceFile = new File(resfile);
        String resource = FileUtils.readFileToString(resourceFile, "UTF-8");
        TemplatedResourceIterator tri = new TemplatedResourceIterator(resource, "src/test/resources/APIM2/valuesplain");
        while (tri.hasNext()) {
            String label2 = tri.getAttributeValue("label2");
            byte[] foxml = tri.next().getBytes("UTF-8");
            String pid = apim.ingest(foxml, FOXML1_1.uri,"ingesting new foxml object");

            // update object label with new value
            apim.modifyObject(pid, null, label2, null, "updating object label");

        }

        purgeDemoObjects(s_client);

        String resourceDirName = "src/test/resources/APIM2/foxml/";
        String[] resourceFilenames = new File(resourceDirName).list();
        for (String resourceFilename : resourceFilenames) {
            resourceFile = new File(resourceDirName + resourceFilename);
            if (resourceFile.isFile()) {
                resource = FileUtils.readFileToString(resourceFile, "UTF-8");
                tri = new TemplatedResourceIterator(resource, "src/test/resources/APIM2/valuesplain");
                while (tri.hasNext()) {
                    byte[] foxml = tri.next().getBytes("UTF-8");
                    apim.ingest(foxml, FOXML1_1.uri,"ingesting new foxml object");
                }
            }
        }

        purgeDemoObjects(s_client);

    }
    
    @Test
    public void testDatastreamMethods() throws Exception {
        // test object
        String resfile = "src/test/resources/APIM2/foxml/demo_SmileyBeerGlass.xml";

        File resourceFile = new File(resfile);
        String resource = FileUtils.readFileToString(resourceFile, "UTF-8");
        TemplatedResourceIterator tri = new TemplatedResourceIterator(resource, "src/test/resources/APIM2/valuesplain");
        while (tri.hasNext()) {
            String label2 = tri.getAttributeValue("label2");
            byte[] foxml = tri.next().getBytes("UTF-8");
            String pid = apim.ingest(foxml, FOXML1_1.uri,"ingesting new foxml object");

            // modify datastream label
            apim.modifyDatastreamByValue(pid, "DC", null, label2, null, null, null, null, null, "modify datastream label", false);

        }

        purgeDemoObjects(s_client);

        String resourceDirName = "src/test/resources/APIM2/foxml/";
        String[] resourceFilenames = new File(resourceDirName).list();
        for (String resourceFilename : resourceFilenames) {
            resourceFile = new File(resourceDirName + resourceFilename);
            if (resourceFile.isFile()) {
                resource = FileUtils.readFileToString(resourceFile, "UTF-8");
                tri = new TemplatedResourceIterator(resource, "src/test/resources/APIM2/valuesplain");
                while (tri.hasNext()) {
                    byte[] foxml = tri.next().getBytes("UTF-8");
                    apim.ingest(foxml, FOXML1_1.uri,"ingesting new foxml object");
                }
            }
        }

        purgeDemoObjects(s_client);

    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestAPIM2.class);
    }

    public static void main(String[] args) {
        JUnitCore.runClasses(TestAPIM2.class);
    }

}
