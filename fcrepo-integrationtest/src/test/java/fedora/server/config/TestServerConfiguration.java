/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;

import org.w3c.dom.Document;

import fedora.test.FedoraTestCase;

/**
 * @author Edwin Shin
 */
public class TestServerConfiguration
        extends FedoraTestCase {

    // FIXME: Refactor 'FedoraTestCase' so this test can go back to '/server'.
    private static final File FCFG_BASE =
            new File("../fcrepo-server/src/main/resources/fcfg/server/fedora-base.fcfg");

    private static final String NS_FCFG_PREFIX = "fcfg";

    private DocumentBuilder builder;

    private ByteArrayOutputStream out;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestServerConfiguration.class);
    }

    @Override
    protected void setUp() throws Exception {
        out = new ByteArrayOutputStream();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        builder = factory.newDocumentBuilder();

        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put(NS_FCFG_PREFIX, NS_FCFG);
        NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
        XMLUnit.setXpathNamespaceContext(ctx);
        XMLUnit.setIgnoreWhitespace(false);
    }

    @Override
    protected void tearDown() throws Exception {
        XMLUnit.setXpathNamespaceContext(SimpleNamespaceContext.EMPTY_CONTEXT);
        XMLUnit.setIgnoreWhitespace(false);
        out.close();
    }

    /*
     * public void testServerConfiguration() { //TODO Implement
     * ServerConfiguration(). } public void testCopy() { //TODO Implement
     * copy(). }
     */

    public void testApplyProperties() throws Exception {
        ServerConfiguration config =
                new ServerConfigurationParser(new FileInputStream(FCFG_BASE))
                        .parse();

        String testVal = "9999";
        String xpath =
                "/" + NS_FCFG_PREFIX + ":server/" + NS_FCFG_PREFIX
                        + ":param[@name='fedoraServerPort'][@value='" + testVal
                        + "']";

        // ensure the new property is really new
        config.serialize(out);
        assertXpathNotExists(xpath, getDocument(out));

        // apply the new property and ensure it is present in the serialized
        // output
        out.reset();
        Properties props = new Properties();
        props.put("server:fedoraServerPort", testVal);
        config.applyProperties(props);
        config.serialize(out);
        assertXpathExists(xpath, getDocument(out));
    }

    public void testSerialize() throws Exception {
        ServerConfiguration config =
                new ServerConfigurationParser(new FileInputStream(FCFG_BASE))
                        .parse();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        config.serialize(out);

        Document original = builder.parse(FCFG_BASE);
        Document generated =
                builder.parse(new ByteArrayInputStream(out.toByteArray()));
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(original, generated);
    }

    private Document getDocument(ByteArrayOutputStream out) throws Exception {
        return XMLUnit.buildControlDocument(new String(out.toByteArray(), "UTF-8"));
    }
}
