/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utilities.install.container;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.fcrepo.utilities.install.container.FedoraWebXML;
import org.fcrepo.utilities.install.container.WebXMLOptions;

/**
 * Test the generation of Fedora's web.xml based on install options.
 *
 * @author Edwin Shin
 */
public class TestFedoraWebXML {

    private String webXMLFilePath;
    private static NamespaceContext oldCtx;

    /**
     * Set the global namespace context for XMLUnit so we can continue
     * to use the various XMLAssert convenience methods.
     */
    @BeforeClass
    public static void setNamespaceContext() {
        oldCtx = XMLUnit.getXpathNamespaceContext();

        Map<String,String> m = new HashMap<String,String>();
        // Because of a bug (confirmed w/ XMLUnit 1.3), default namespace
        // (i.e., empty string) doesn't work for XPath evaluation.
        m.put("w", "http://java.sun.com/xml/ns/j2ee");
        NamespaceContext ctx = new SimpleNamespaceContext(m);
        XMLUnit.setXpathNamespaceContext(ctx);
    }

    /**
     * Restore XMLUnit's namespace context.
     */
    @AfterClass
    public static void restoreNamespaceContext() {
        XMLUnit.setXpathNamespaceContext(oldCtx);
    }


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        File f = new File("../fcrepo-webapp/fcrepo-webapp-fedora/src/main/webapp/WEB-INF/web.xml");
        assertTrue("Couldn't find source web.xml file", f.exists());
        webXMLFilePath = f.getAbsolutePath();

        // Save global namespace context for XMLUnit
        oldCtx = XMLUnit.getXpathNamespaceContext();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        // Restore global namespace context
        XMLUnit.setXpathNamespaceContext(oldCtx);
    }

    @Test
    public void testOptions() throws Exception {
        // TODO this is just the stub for a proper test


        FedoraWebXML webXML;
        Writer writer;

        // TestConfigA
        webXML =
                new FedoraWebXML(webXMLFilePath, getOptions(false,
                                                            false,
                                                            true,
                                                            false,
                                                            "/foo/bar"));
        assertNotNull(webXML);
        writer = new StringWriter();
        webXML.write(writer);
        String configA = writer.toString();
        XMLAssert.assertXpathExists("//w:web-app/w:filter-mapping[w:filter-name='EnforceAuthnFilter']", configA);
        XMLAssert.assertXpathExists("//w:web-app/w:filter-mapping[w:filter-name='RestApiAuthnFilter']", configA);
        XMLAssert.assertXpathExists("//w:web-app/w:filter-mapping[w:filter-name='SetupFilter']", configA);
        XMLAssert.assertXpathExists("//w:web-app/w:filter-mapping[w:filter-name='XmlUserfileFilter']", configA);
        XMLAssert.assertXpathExists("//w:web-app/w:filter-mapping[w:filter-name='FinalizeFilter']", configA);
        XMLAssert.assertXpathNotExists("//w:web-app/w:filter-mapping[w:filter-name='AuthFilterJAAS']", configA);
        XMLAssert.assertXpathNotExists("//w:web-app/w:filter-mapping[w:filter-name='PEPFilter']", configA);
        XMLAssert.assertXpathEvaluatesTo("/foo/bar", "//w:web-app/w:servlet[w:servlet-name='ControlServlet']/w:init-param[w:param-name='fedora.home']/w:param-value", configA);

        // TestConfigB
        webXML =
                new FedoraWebXML(webXMLFilePath, getOptions(true,
                                                            false,
                                                            true,
                                                            false,
                                                            ""));
        assertNotNull(webXML);
        writer = new StringWriter();
        webXML.write(writer);
        String configB = writer.toString();
        XMLAssert.assertXpathExists("//w:web-app/w:filter-mapping[w:filter-name='EnforceAuthnFilter']", configB);
        XMLAssert.assertXpathExists("//w:web-app/w:filter-mapping[w:filter-name='SetupFilter']", configB);
        XMLAssert.assertXpathExists("//w:web-app/w:filter-mapping[w:filter-name='XmlUserfileFilter']", configB);
        XMLAssert.assertXpathExists("//w:web-app/w:filter-mapping[w:filter-name='FinalizeFilter']", configB);
        XMLAssert.assertXpathNotExists("//w:web-app/w:filter-mapping[w:filter-name='RestApiAuthnFilter']", configB);
        XMLAssert.assertXpathNotExists("//w:web-app/w:filter-mapping[w:filter-name='AuthFilterJAAS']", configB);
        XMLAssert.assertXpathNotExists("//w:web-app/w:filter-mapping[w:filter-name='PEPFilter']", configB);

        // TestConfigC
        webXML =
            new FedoraWebXML(webXMLFilePath, getOptions(false,
                                                        false,
                                                        true,
                                                        true,
                                                        ""));
        assertNotNull(webXML);
        writer = new StringWriter();
        webXML.write(writer);
        String configC = writer.toString();
        XMLAssert.assertXpathExists("//w:web-app/w:filter-mapping[w:filter-name='AuthFilterJAAS']", configC);
        XMLAssert.assertXpathExists("//w:web-app/w:filter-mapping[w:filter-name='PEPFilter']", configC);
        XMLAssert.assertXpathNotExists("//w:web-app/w:filter-mapping[w:filter-name='EnforceAuthnFilter']", configC);
        XMLAssert.assertXpathNotExists("//w:web-app/w:filter-mapping[w:filter-name='RestApiAuthnFilter']", configC);
        XMLAssert.assertXpathNotExists("//w:web-app/w:filter-mapping[w:filter-name='SetupFilter']", configC);
        XMLAssert.assertXpathNotExists("//w:web-app/w:filter-mapping[w:filter-name='XmlUserfileFilter']", configC);
        XMLAssert.assertXpathNotExists("//w:web-app/w:filter-mapping[w:filter-name='FinalizeFilter']", configC);
        XMLAssert.assertXpathEvaluatesTo("false", "//w:web-app/w:filter[w:filter-name='AuthFilterJAAS']/w:init-param[w:param-name='authnAPIA']/w:param-value", configC);

        // TestConfigQ
        webXML =
        	new FedoraWebXML(webXMLFilePath, getOptions(false,
        												false,
        												false,
        												false,
        												""));
        writer = new StringWriter();
        webXML.write(writer);

    }

    /**
     * Set Fedora installer options for web.xml
     *
     * @param apiaA require AuthN for APIA
     * @param apiaS require SSL for APIA
     * @param apimS require SSL for APIM
     * @param fesl require FeSL
     * @param fedoraHome path to FEDORA_HOME
     * @return
     */
    private WebXMLOptions getOptions(boolean apiaA,
                                     boolean apiaS,
                                     boolean apimS,
                                     boolean fesl,
                                     String fedoraHome) {
        WebXMLOptions options = new WebXMLOptions();
        options.setApiaAuth(apiaA);
        options.setApiaSSL(apiaS);
        options.setApimSSL(apimS);
        options.setFesl(fesl);
        options.setFedoraHome(new File(fedoraHome));
        return options;
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestFedoraWebXML.class);
    }
}
