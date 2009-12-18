/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities.install.container;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
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

        // TestConfigA
        webXML =
                new FedoraWebXML(webXMLFilePath, getOptions(false,
                                                            true,
                                                            true,
                                                            false,
                                                            ""));
        assertNotNull(webXML);

        // TestConfigB
        webXML =
                new FedoraWebXML(webXMLFilePath, getOptions(true,
                                                            false,
                                                            true,
                                                            false,
                                                            ""));
        
        // TestConfigC
        webXML =
            new FedoraWebXML(webXMLFilePath, getOptions(true,
                                                        false,
                                                        true,
                                                        true,
                                                        ""));

        // TestConfigQ
        webXML = 
        	new FedoraWebXML(webXMLFilePath, getOptions(false, 
        												false, 
        												false, 
        												false, 
        												""));
        
    }

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
