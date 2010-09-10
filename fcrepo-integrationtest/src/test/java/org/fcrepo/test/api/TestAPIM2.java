/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.api;

import java.io.File;
import java.io.FileInputStream;

import java.rmi.RemoteException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;

import org.apache.axis.types.NonNegativeInteger;

import org.apache.commons.io.FileUtils;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;

import org.antlr.stringtemplate.StringTemplate;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.fcrepo.common.Constants;

import org.fcrepo.server.access.FedoraAPIA;
import org.fcrepo.server.management.FedoraAPIM;
import org.fcrepo.server.types.gen.ComparisonOperator;
import org.fcrepo.server.types.gen.Condition;
import org.fcrepo.server.types.gen.FieldSearchQuery;
import org.fcrepo.server.types.gen.FieldSearchResult;

import org.fcrepo.test.FedoraServerTestCase;

/**
 * Test APIM based on templating of resource files
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class TestAPIM2
        extends FedoraServerTestCase
        implements Constants {

    private FedoraAPIM apim;
    private FedoraAPIA apia;

    public static Test suite() {
        TestSuite suite = new TestSuite("TestAPIM2 TestSuite");
        suite.addTestSuite(TestAPIM2.class);
        return suite;
    }

    @Override
    public void setUp() throws Exception {
        apim = getFedoraClient().getAPIM();
        apia = getFedoraClient().getAPIA();


        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        nsMap.put("dc", "http://purl.org/dc/elements/1.1/");
        nsMap.put("foxml", "info:fedora/fedora-system:def/foxml#");
        NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
        XMLUnit.setXpathNamespaceContext(ctx);

        // not really necessary, but will cope with any junk left from other tests
        purgeDemoObjects();

    }

    @Override
    public void tearDown() throws Exception {
        // assumes all our test objects are in the demo namespace
        purgeDemoObjects();

        XMLUnit.setXpathNamespaceContext(SimpleNamespaceContext.EMPTY_CONTEXT);
    }

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

        assertEquals("Ingested object count", count, getDemoObjects().size());

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

        assertEquals("Ingested object count", count, getDemoObjects().size());

    }

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
        NonNegativeInteger maxResults = new NonNegativeInteger("100");


        String termsTemplate = "demo:*$value$";
        TemplatedResourceIterator tri = new TemplatedResourceIterator(termsTemplate, "src/test/resources/APIM2/searchvalues");
        while (tri.hasNext()) {
            FieldSearchQuery query;
            FieldSearchResult res;

            // using conditions
            Condition[] conditions = {new Condition("pid", ComparisonOperator.fromString("eq"), "demo:1" + tri.getAttributeValue("value"))};
            query = new FieldSearchQuery(conditions, null);
            try {
                res = apia.findObjects(resultFields, maxResults, query);
            } catch (RemoteException e) {
                if (!e.getMessage().startsWith("org.fcrepo.server.errors.QueryParseException"))
                    throw e;
            }

            // using query
            String terms = tri.next();
            query = new FieldSearchQuery(null, terms);
            try {
                res = apia.findObjects(resultFields, maxResults, query);
            } catch (RemoteException e) {
                if (!e.getMessage().startsWith("org.fcrepo.server.errors.QueryParseException"))
                    throw e;
            }

        }

        purgeDemoObjects();

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

        purgeDemoObjects();

    }
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

        purgeDemoObjects();

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

        purgeDemoObjects();

    }
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

        purgeDemoObjects();

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

        purgeDemoObjects();

    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestAPIM2.class);
    }

    /**
     * Iterator that takes a set of attribute values from a values file
     * and iterates by repeatedly substituting in the next set of values into the
     * template
     *
     * values file format: java properties, with
     * template.attribute.sets.count=n :
     *   number of sets to iterate over
     * template.attribute.set.0.someattribute1=value1:
     * template.attribute.set.0.someattribute2=value2:
     *   when on set iteration 0, substitute $someattribute1$ with value1
     *   and similarly $someattribute2$ with value2
     * nb: sets are zero-indexed, numbered 0 to count - 1
     *     escape backslashes to \\
     *
     * @author Stephen Bayliss
     * @version $Id$
     */
    // TODO: consider moving out to utils, could be used by other tests
    class TemplatedResourceIterator implements Iterator<String> {

        // template source
        private final String m_templateSource;

        // properties file contains attributes and values to substitute
        private final String m_valuesFilename;
        private final Properties m_values;
        private final Set<String> m_propertyNames;

        // number of sets to iterate
        private final int m_setCount;
        // index of next set to return with next()
        private int m_nextSet = 0;

        // for validation of consistent attribute counts across sets
        int m_lastAttributeCount = -1;

        // character escaping to perform
        private final boolean m_escapeXML;

        TemplatedResourceIterator(String template, String valuesFilename) throws Exception {
            this(template, valuesFilename, false);
        }
        TemplatedResourceIterator(String template, String valuesFilename, boolean escapeXML) throws Exception {
            m_escapeXML = escapeXML;

            m_values = new Properties();
            m_valuesFilename = valuesFilename;
            m_values.load(new FileInputStream(m_valuesFilename));

            m_setCount = Integer.parseInt(m_values.getProperty("template.attribute.sets.count"));
            m_propertyNames = m_values.stringPropertyNames();

            // the stuff to template
            m_templateSource = template;

        }

        @Override
        public boolean hasNext() {
            return (m_nextSet < m_setCount);
        }

        @Override
        public String next() {

            if (m_nextSet >= m_setCount)
                throw new NoSuchElementException();

            // all properties in the current set start with...
            String setPropertyNamePrefix = "template.attribute.set." + m_nextSet + ".";

            StringTemplate tpl = new StringTemplate(m_templateSource);

            int attributeCount = 0; // count number of attributes specified for replacement
            for (String propertyName : m_propertyNames) {
                // property for an attribute for the current set?
                if (propertyName.startsWith(setPropertyNamePrefix)) {
                    // get attribute name and value
                    String attributeName = propertyName.replace(setPropertyNamePrefix, "");
                    String attributeValue = m_values.getProperty(propertyName);

                    // escape value if necessary
                    String value;
                    if (m_escapeXML) {
                        value = escapeXML(attributeValue);
                    } else {
                        value = attributeValue;
                    }

                    tpl.setAttribute(attributeName, value);

                    attributeCount++;
                }
            }
            // some checks
            if (attributeCount == 0)
                throw new RuntimeException("No attributes found for set " + m_nextSet + " (" + m_valuesFilename + ")" );
            if (m_lastAttributeCount != -1) {
                if (m_lastAttributeCount != attributeCount)
                    throw new RuntimeException("Number of attributes is different in sets " + (m_nextSet -1) + " and " + m_nextSet + " (" + m_valuesFilename + ")" );
            }
            m_lastAttributeCount = attributeCount;

            m_nextSet++;
            return tpl.toString();
        }

        public String getAttributeValue(String attributeName) {
            // gets a named attribute for the current set
            String propertyName = "template.attribute.set." + m_nextSet + "." + attributeName;
            return m_values.getProperty(propertyName);
        }

        @Override
        public void remove() {
            throw new RuntimeException("Method remove() is not supported");

        }

        // TODO: make generic, allow different forms of escaping?
        private String escapeXML(String toEscape) {
            String res = toEscape;
            // standard xml entities
            // nb cast to CharSequence to prevent regex matching
            res = res.replace("\"", "&quot;");
            res = res.replace("'", "&apos;");
            res = res.replace("&", "&amp;");
            res = res.replace("<", "&lt;");
            res = res.replace(">", "&gt;");

            return res;
        }
    }
}
