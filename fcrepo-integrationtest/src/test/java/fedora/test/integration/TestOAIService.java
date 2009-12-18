/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test.integration;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;

import org.w3c.dom.Document;

import junit.framework.Test;
import junit.framework.TestSuite;

import fedora.client.FedoraClient;

import fedora.server.management.FedoraAPIM;
import fedora.server.utilities.StreamUtility;

import fedora.test.FedoraServerTestCase;

/**
 * @author Edwin Shin
 */
public class TestOAIService
        extends FedoraServerTestCase {

    private DocumentBuilderFactory factory;

    private DocumentBuilder builder;

    private FedoraClient client;

    public static Test suite() {
        TestSuite suite = new TestSuite("Test OAI Service");
        suite.addTestSuite(TestOAIService.class);
        return suite;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        client = new FedoraClient(getBaseURL(), getUsername(), getPassword());

        factory = DocumentBuilderFactory.newInstance();
        builder = factory.newDocumentBuilder();

        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put(NS_FEDORA_TYPES_PREFIX, NS_FEDORA_TYPES);
        nsMap.put("oai", "http://www.openarchives.org/OAI/2.0/");
        NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
        XMLUnit.setXpathNamespaceContext(ctx);
    }

    @Override
    public void tearDown() throws Exception {
        XMLUnit.setXpathNamespaceContext(SimpleNamespaceContext.EMPTY_CONTEXT);
        super.tearDown();
    }

    public void testListMetadataFormats() throws Exception {
        String request = "/oai?verb=ListMetadataFormats";
        Document result = getXMLQueryResult(request);
        assertXpathEvaluatesTo("oai_dc",
                               "/oai:OAI-PMH/oai:ListMetadataFormats/oai:metadataFormat/oai:metadataPrefix",
                               result);
    }

    public void testListRecords() throws Exception {
        FedoraAPIM apim = client.getAPIM();
        FileInputStream in =
                new FileInputStream(FEDORA_HOME
                        + "/client/demo/foxml/local-server-demos/simple-document-demo/obj_demo_31.xml");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamUtility.pipeStream(in, out, 4096);

        apim.ingest(out.toByteArray(), FOXML1_1.uri, "for testing");

        String request = "/oai?verb=ListRecords&metadataPrefix=oai_dc";
        Document result = getXMLQueryResult(request);
        assertXpathExists("/oai:OAI-PMH/oai:ListRecords/oai:record", result);

        request = "/oai?verb=ListRecords&metadataPrefix=oai_dc&from=2000-01-01";
        result = getXMLQueryResult(request);
        assertXpathExists("/oai:OAI-PMH/oai:ListRecords/oai:record", result);

        apim.purgeObject("demo:31", "for testing", false);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestOAIService.class);
    }

}