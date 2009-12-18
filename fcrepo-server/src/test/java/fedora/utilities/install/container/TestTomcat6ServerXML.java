/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities.install.container;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fedora.utilities.install.InstallOptions;


/**
 *
 * @author Edwin Shin
 */
public class TestTomcat6ServerXML {
    private InputStream is;
    private InstallOptions installOptions;

    /** The default server.xml shipped with Tomcat 6.0.20 (stripped of comments) */
    private static byte[] defaultServerXML;

    static {
        StringBuilder sb = new StringBuilder();
        sb.append("<Server port=\"8005\" shutdown=\"SHUTDOWN\">");
        sb.append("  <Listener className=\"org.apache.catalina.core.AprLifecycleListener\" SSLEngine=\"on\"/>");
        sb.append("  <Listener className=\"org.apache.catalina.core.JasperListener\" />");
        sb.append("  <Listener className=\"org.apache.catalina.mbeans.ServerLifecycleListener\" />");
        sb.append("  <Listener className=\"org.apache.catalina.mbeans.GlobalResourcesLifecycleListener\"/>");
        sb.append("  <GlobalNamingResources>");
        sb.append("    <Resource name=\"UserDatabase\" auth=\"Container\"");
        sb.append("      type=\"org.apache.catalina.UserDatabase\"");
        sb.append("      description=\"User database that can be updated and saved\"");
        sb.append("      factory=\"org.apache.catalina.users.MemoryUserDatabaseFactory\"");
        sb.append("      pathname=\"conf/tomcat-users.xml\"/>");
        sb.append("  </GlobalNamingResources>");
        sb.append("  <Service name=\"Catalina\">");
        sb.append("    <Connector port=\"8080\" protocol=\"HTTP/1.1\"");
        sb.append("      connectionTimeout=\"20000\" redirectPort=\"8443\"/>");
        sb.append("    <Connector port=\"8009\" protocol=\"AJP/1.3\"");
        sb.append("      redirectPort=\"8443\"/>");
        sb.append("    <Engine name=\"Catalina\" defaultHost=\"localhost\">");
        sb.append("      <Realm className=\"org.apache.catalina.realm.UserDatabaseRealm\"");
        sb.append("        resourceName=\"UserDatabase\"/>");
        sb.append("      <Host name=\"localhost\" appBase=\"webapps\"");
        sb.append("        unpackWARs=\"true\" autoDeploy=\"true\"");
        sb.append("        xmlValidation=\"false\" xmlNamespaceAware=\"false\">");
        sb.append("      </Host>");
        sb.append("    </Engine>");
        sb.append("  </Service>");
        sb.append("</Server>");

        try {
            defaultServerXML = sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // should never happen with UTF-8
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        is = new ByteArrayInputStream(defaultServerXML);
        installOptions = getDefaultOptions();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        if (is != null) {
            is.close();
        }
    }

    @Test
    public void testUpdate() throws Exception {
        TomcatServerXML serverXML;
        serverXML = new TomcatServerXML(is, installOptions);
        serverXML.update();
    }

    private InstallOptions getDefaultOptions() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put(InstallOptions.TOMCAT_HTTP_PORT, "8080");
        map.put(InstallOptions.TOMCAT_SHUTDOWN_PORT, "8005");
        map.put(InstallOptions.SSL_AVAILABLE, "true");
        map.put(InstallOptions.TOMCAT_SSL_PORT, "8443");
        map.put(InstallOptions.KEYSTORE_FILE, "included");

        return new InstallOptions(null, map);
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestTomcat6ServerXML.class);
    }
}
