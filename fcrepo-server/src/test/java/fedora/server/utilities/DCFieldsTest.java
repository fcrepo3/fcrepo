/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities;

import java.io.ByteArrayInputStream;

import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;

import org.junit.Test;

import org.w3c.dom.Document;

import static fedora.common.Constants.DC;
import static fedora.common.Constants.OAI_DC;


/**
 *
 * @author Edwin Shin
 * @since 3.0.1
 * @version $Id$
 */
public class DCFieldsTest extends XMLTestCase {
    private final static String dcWithXmlLang;
    private XpathEngine engine;

    static {
        StringBuilder sb = new StringBuilder();
        sb.append("<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" ");
        sb.append("    xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
        sb.append("<dc:subject xml:lang=\"da\">Tidsinterval</dc:subject>");
        sb.append("<dc:subject xml:lang=\"en\">Time interval</dc:subject>");
        sb.append("<dc:subject>interval</dc:subject>");
        sb.append("</oai_dc:dc> ");
        dcWithXmlLang = sb.toString();
    }

    @Override
    public void setUp() throws Exception {
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put(OAI_DC.prefix, OAI_DC.uri);
        nsMap.put(DC.prefix, DC.uri);
        NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
        engine = XMLUnit.newXpathEngine();
        engine.setNamespaceContext(ctx);
    }

    @Test
    public void testDCFieldsInputStream() throws Exception {
        DCFields dc;
        Document dcXML;

        dc = new DCFields(new ByteArrayInputStream(dcWithXmlLang.getBytes("UTF-8")));
        dcXML = XMLUnit.buildControlDocument(dc.getAsXML());
        assertEquals("Time interval",
                     engine.evaluate("//dc:subject[@xml:lang='en']", dcXML));
        assertEquals("Tidsinterval",
                     engine.evaluate("//dc:subject[@xml:lang='da']", dcXML));

        dc = new DCFields();
        dcXML = XMLUnit.buildControlDocument(dc.getAsXML());
        assertEquals("Expected empty element",
                     "",
                     engine.evaluate("//oai_dc:dc", dcXML).trim());
    }

}
