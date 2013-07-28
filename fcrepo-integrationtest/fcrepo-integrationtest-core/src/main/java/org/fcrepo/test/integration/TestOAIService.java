/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.integration;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import junit.framework.JUnit4TestAdapter;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.fcrepo.client.FedoraClient;
import org.fcrepo.server.management.FedoraAPIMMTOM;
import org.fcrepo.server.utilities.StreamUtility;
import org.fcrepo.server.utilities.TypeUtility;
import org.fcrepo.test.FedoraServerTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.w3c.dom.Document;


/**
 * @author Edwin Shin
 */
public class TestOAIService
        extends FedoraServerTestCase {

    private FedoraClient client;

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestOAIService.class);
    }

    @Before
    public void setUp() throws Exception {

        client = new FedoraClient(getBaseURL(), getUsername(), getPassword());

        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put(NS_FEDORA_TYPES_PREFIX, NS_FEDORA_TYPES);
        nsMap.put("oai", "http://www.openarchives.org/OAI/2.0/");
        NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
        XMLUnit.setXpathNamespaceContext(ctx);
    }

    @After
    public void tearDown() throws Exception {
        XMLUnit.setXpathNamespaceContext(SimpleNamespaceContext.EMPTY_CONTEXT);
        client.shutdown();
    }

    public void testListMetadataFormats() throws Exception {
        String request = "/oai?verb=ListMetadataFormats";
        Document result = getXMLQueryResult(client, request);
        assertXpathEvaluatesTo("oai_dc",
                               "/oai:OAI-PMH/oai:ListMetadataFormats/oai:metadataFormat/oai:metadataPrefix",
                               result);
    }

    @Test
    public void testListRecords() throws Exception {
        FedoraAPIMMTOM apim = client.getAPIMMTOM();
        FileInputStream in =
                new FileInputStream(FEDORA_HOME
                                    + "/client/demo/foxml/local-server-demos/simple-document-demo/obj_demo_31.xml");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamUtility.pipeStream(in, out, 4096);

        apim.ingest(TypeUtility.convertBytesToDataHandler(out.toByteArray()), FOXML1_1.uri, "for testing");

        String request = "/oai?verb=ListRecords&metadataPrefix=oai_dc";
        Document result = getXMLQueryResult(client, request);
        assertXpathExists("/oai:OAI-PMH/oai:ListRecords/oai:record", result);

        request = "/oai?verb=ListRecords&metadataPrefix=oai_dc&from=2000-01-01";
        result = getXMLQueryResult(client, request);
        assertXpathExists("/oai:OAI-PMH/oai:ListRecords/oai:record", result);

        apim.purgeObject("demo:31", "for testing", false);
    }

    public static void main(String[] args) {
        JUnitCore.runClasses(TestOAIService.class);
    }

}