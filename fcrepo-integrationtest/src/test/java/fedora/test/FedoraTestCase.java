/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import javax.xml.transform.TransformerFactory;

import net.sf.saxon.FeatureKeys;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;

import fedora.client.FedoraClient;

import fedora.common.FedoraTestConstants;

import fedora.server.config.ServerConfiguration;
import fedora.server.config.ServerConfigurationParser;

/**
 * Base class for Fedora Test Cases
 *
 * @author Edwin Shin
 */
public abstract class FedoraTestCase
        extends XMLTestCase
        implements FedoraTestConstants {

    public static String ssl = "http";

    public FedoraTestCase() {
        super();
        TransformerFactory factory = XMLUnit.getTransformerFactory();
        if (factory.getClass().getName()
                .equals("net.sf.saxon.TransformerFactoryImpl")) {
            factory.setAttribute(FeatureKeys.VERSION_WARNING, Boolean.FALSE);
        }
    }

    public FedoraTestCase(String name) {
        super(name);
        TransformerFactory factory = XMLUnit.getTransformerFactory();
        if (factory.getClass().getName()
                .equals("net.sf.saxon.TransformerFactoryImpl")) {
            factory.setAttribute(FeatureKeys.VERSION_WARNING, Boolean.FALSE);
        }
    }

    public static ServerConfiguration getServerConfiguration() {
        try {
            return new ServerConfigurationParser(new FileInputStream(FCFG))
                    .parse();
        } catch (Exception e) {
            fail(e.getMessage());
            return null;
        }
    }

    public static String getDemoBaseURL() {
        if (System.getProperty("fedora.baseURLDemo") != null) {
            return System.getProperty("fedora.baseURLDemo");
        } else {
            return getProtocol() + "://" + getHost() + ":" + getPort() + "/"
                    + getDemoAppServerContext();
        }
    }

    public static String getBaseURL() {
        if (System.getProperty("fedora.baseURL") != null) {
            return System.getProperty("fedora.baseURL");
        } else {
            return getProtocol() + "://" + getHost() + ":" + getPort() + "/"
                    + getFedoraAppServerContext();
        }
    }

    public static String getHost() {
        return getServerConfiguration().getParameter("fedoraServerHost")
                .getValue();
    }

    public static String getPort() {
        String port = null;
        if (getProtocol().equals("http")) {
            port =
                    getServerConfiguration().getParameter("fedoraServerPort")
                            .getValue();
        } else {
            port =
                    getServerConfiguration().getParameter("fedoraRedirectPort")
                            .getValue();
        }
        return port;
    }

    public static String getFedoraAppServerContext() {
        if (System.getProperty("fedoraAppServerContext") != null) {
            return System.getProperty("fedoraAppServerContext");
        }
        return getServerConfiguration().getParameter("fedoraAppServerContext")
                .getValue();
    }

    public static String getDemoAppServerContext() {
        return getServerConfiguration()
                .getParameter("fedoraDemoAppServerContext") != null ? getServerConfiguration()
                .getParameter("fedoraDemoAppServerContext").getValue()
                : "fedora-demo";
    }

    // hack to dynamically set protocol based on settings in beSecurity
    // Settings for fedoraInternalCall-1 should have callSSL=true when server is secure
    public static String getProtocol() {
        BufferedReader br = null;
        try {
            br =
                    new BufferedReader(new InputStreamReader(new FileInputStream(BESECURITY)));
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.indexOf("role=\"fedoraInternalCall-1\"") > 0
                        && line.indexOf("callSSL=\"true\"") > 0) {
                    ssl = "https";
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("fedora.home: " + FEDORA_HOME);
            fail("beSecurity file Not found: " + BESECURITY.getAbsolutePath());
        } finally {
            try {
                if (br != null) {
                    br.close();
                    br = null;
                }
            } catch (Exception e) {
                System.out.println("Unable to close BufferdReader");
            }
        }
        return ssl;
    }

    public static String getUsername() {
        return FEDORA_USERNAME;
    }

    public static String getPassword() {
        return FEDORA_PASSWORD;
    }

    public static FedoraClient getFedoraClient() throws Exception {
        return getFedoraClient(getBaseURL(), getUsername(), getPassword());
    }

    public static FedoraClient getFedoraClient(String baseURL,
                                               String username,
                                               String password)
            throws Exception {
        return new FedoraClient(baseURL, username, password);
    }
}
