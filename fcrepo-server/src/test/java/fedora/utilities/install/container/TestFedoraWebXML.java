/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities.install.container;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the generation of Fedora's web.xml based on install options.
 *
 * @author Edwin Shin
 */
public class TestFedoraWebXML {

    private String webXMLFilePath;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        File f = new File("../fcrepo-webapp/fcrepo-webapp-fedora/src/main/webapp/WEB-INF/web.xml");
        assertTrue("Couldn't find source web.xml file", f.exists());
        webXMLFilePath = f.getAbsolutePath();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
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
        XMLAssert.assertXpathExists("//web-app/filter-mapping[filter-name='EnforceAuthnFilter']", configA);
        XMLAssert.assertXpathExists("//web-app/filter-mapping[filter-name='RestApiAuthnFilter']", configA);
        XMLAssert.assertXpathExists("//web-app/filter-mapping[filter-name='SetupFilter']", configA);
        XMLAssert.assertXpathExists("//web-app/filter-mapping[filter-name='XmlUserfileFilter']", configA);
        XMLAssert.assertXpathExists("//web-app/filter-mapping[filter-name='FinalizeFilter']", configA);
        XMLAssert.assertXpathNotExists("//web-app/filter-mapping[filter-name='AuthFilterJAAS']", configA);
        XMLAssert.assertXpathNotExists("//web-app/filter-mapping[filter-name='PEPFilter']", configA);
        XMLAssert.assertXpathEvaluatesTo("/foo/bar", "//web-app/servlet[servlet-name='ControlServlet']/init-param[param-name='fedora.home']/param-value", configA);

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
        XMLAssert.assertXpathExists("//web-app/filter-mapping[filter-name='EnforceAuthnFilter']", configB);
        XMLAssert.assertXpathExists("//web-app/filter-mapping[filter-name='SetupFilter']", configB);
        XMLAssert.assertXpathExists("//web-app/filter-mapping[filter-name='XmlUserfileFilter']", configB);
        XMLAssert.assertXpathExists("//web-app/filter-mapping[filter-name='FinalizeFilter']", configB);
        XMLAssert.assertXpathNotExists("//web-app/filter-mapping[filter-name='RestApiAuthnFilter']", configB);
        XMLAssert.assertXpathNotExists("//web-app/filter-mapping[filter-name='AuthFilterJAAS']", configB);
        XMLAssert.assertXpathNotExists("//web-app/filter-mapping[filter-name='PEPFilter']", configB);

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
        XMLAssert.assertXpathExists("//web-app/filter-mapping[filter-name='AuthFilterJAAS']", configC);
        XMLAssert.assertXpathExists("//web-app/filter-mapping[filter-name='PEPFilter']", configC);
        XMLAssert.assertXpathNotExists("//web-app/filter-mapping[filter-name='EnforceAuthnFilter']", configC);
        XMLAssert.assertXpathNotExists("//web-app/filter-mapping[filter-name='RestApiAuthnFilter']", configC);
        XMLAssert.assertXpathNotExists("//web-app/filter-mapping[filter-name='SetupFilter']", configC);
        XMLAssert.assertXpathNotExists("//web-app/filter-mapping[filter-name='XmlUserfileFilter']", configC);
        XMLAssert.assertXpathNotExists("//web-app/filter-mapping[filter-name='FinalizeFilter']", configC);
        XMLAssert.assertXpathEvaluatesTo("false", "//web-app/filter[filter-name='AuthFilterJAAS']/init-param[param-name='authnAPIA']/param-value", configC);

        // TestConfigQ
        webXML =
        	new FedoraWebXML(webXMLFilePath, getOptions(false,
        												false,
        												false,
        												false,
        												""));

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
