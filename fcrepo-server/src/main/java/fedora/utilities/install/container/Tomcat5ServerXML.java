/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities.install.container;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.dom4j.Attribute;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import fedora.utilities.XMLDocument;
import fedora.utilities.install.InstallOptions;
import fedora.utilities.install.InstallationFailedException;

public class Tomcat5ServerXML
        extends XMLDocument {

    private static final String KEYSTORE_LOCATION =
            Tomcat.CONF + "/" + Tomcat.KEYSTORE;

    private static final String KEYSTORE_PASSWORD_DEFAULT = "changeit";

    private static final String KEYSTORE_TYPE_DEFAULT = "JKS";

    private static final String URI_ENCODING = "UTF-8";

    private static final String HTTP_CONNECTOR_XPATH = "/Server/Service[@name='Catalina']/Connector[not(@scheme='https' or contains(@protocol, 'AJP'))]";

    private static final String HTTPS_CONNECTOR_XPATH = "/Server/Service[@name='Catalina']/Connector[@scheme='https' and not(contains(@protocol, 'AJP'))]";

    private static final String AJP_CONNECTOR_XPATH = "/Server/Service[@name='Catalina']/Connector[contains(@protocol, 'AJP')]";

    private final InstallOptions options;

    public Tomcat5ServerXML(File serverXML, InstallOptions installOptions)
            throws FileNotFoundException, DocumentException {
        this(new FileInputStream(serverXML), installOptions);
    }

    public Tomcat5ServerXML(InputStream serverXML, InstallOptions installOptions)
            throws FileNotFoundException, DocumentException {
        super(serverXML);
        options = installOptions;
    }

    public void update() throws InstallationFailedException {
        setHTTPPort();
        setShutdownPort();
        setSSLPort();
        setURIEncoding();
    }

    public void setHTTPPort() throws InstallationFailedException {
        // Note this very significant assumption: this xpath will select exactly one connector
        Element httpConnector =
                (Element) getDocument()
                        .selectSingleNode(HTTP_CONNECTOR_XPATH);

        if (httpConnector == null) {
            throw new InstallationFailedException("Unable to set server.xml HTTP Port. XPath for Connector element failed.");
        }

        httpConnector.addAttribute("port", options
                .getValue(InstallOptions.TOMCAT_HTTP_PORT));
        httpConnector.addAttribute("enableLookups", "true"); // supports client dns/fqdn in xacml authz policies
    }

    public void setShutdownPort() throws InstallationFailedException {
        Element server =
                (Element) getDocument()
                        .selectSingleNode("/Server[@shutdown and @port]");

        if (server == null) {
            throw new InstallationFailedException("Unable to set server.xml shutdown port. XPath for Server element failed.");
        }

        server.addAttribute("port", options
                .getValue(InstallOptions.TOMCAT_SHUTDOWN_PORT));
    }

    /**
     * Sets the port and keystore information on the SSL connector if it already
     * exists; creates a new SSL connector, otherwise. Also sets the
     * redirectPort on the non-SSL connector to match.
     *
     * @throws InstallationFailedException
     */
    public void setSSLPort() throws InstallationFailedException {
        Element httpsConnector =
                (Element) getDocument()
                        .selectSingleNode(HTTPS_CONNECTOR_XPATH);
        if (options.getBooleanValue(InstallOptions.SSL_AVAILABLE, true)) {
            if (httpsConnector == null) {
                Element service =
                        (Element) getDocument()
                                .selectSingleNode("/Server/Service[@name='Catalina']");
                httpsConnector = service.addElement("Connector");
                httpsConnector.addAttribute("maxThreads", "150");
                httpsConnector.addAttribute("minSpareThreads", "25");
                httpsConnector.addAttribute("maxSpareThreads", "75");
                httpsConnector.addAttribute("disableUploadTimeout", "true");
                httpsConnector.addAttribute("acceptCount", "100");
                httpsConnector.addAttribute("debug", "0");
                httpsConnector.addAttribute("scheme", "https");
                httpsConnector.addAttribute("secure", "true");
                httpsConnector.addAttribute("clientAuth", "false");
                httpsConnector.addAttribute("sslProtocol", "TLS");
            }
            httpsConnector.addAttribute("port", options
                    .getValue(InstallOptions.TOMCAT_SSL_PORT));
            httpsConnector.addAttribute("enableLookups", "true"); // supports client dns/fqdn in xacml authz policies

            String keystore = options.getValue(InstallOptions.KEYSTORE_FILE);
            if (keystore.equals(InstallOptions.INCLUDED)) {
                keystore = KEYSTORE_LOCATION;
            }

            addAttribute(httpsConnector,
                         "keystoreFile",
                         keystore,
                         InstallOptions.DEFAULT);
            addAttribute(httpsConnector,
                         "keystorePass",
                         options.getValue(InstallOptions.KEYSTORE_PASSWORD),
                         KEYSTORE_PASSWORD_DEFAULT);
            addAttribute(httpsConnector,
                         "keystoreType",
                         options.getValue(InstallOptions.KEYSTORE_TYPE),
                         KEYSTORE_TYPE_DEFAULT);

            // The redirectPort for the non-SSL connector should match the port on
            // the SSL connector, per:
            // http://tomcat.apache.org/tomcat-5.0-doc/ssl-howto.html
            Element httpConnector =
                    (Element) getDocument()
                            .selectSingleNode(HTTP_CONNECTOR_XPATH);
            if (httpConnector != null) {
                httpConnector.addAttribute("redirectPort", options
                        .getValue(InstallOptions.TOMCAT_SSL_PORT));
            } else {
                throw new InstallationFailedException("Unable to set server.xml SSL Port. XPath for Connector element failed.");
            }
        } else if (httpsConnector != null) {
            httpsConnector.getParent().remove(httpsConnector);
        }
    }

    public void setURIEncoding() throws InstallationFailedException {
        // http connector
        // Note this very significant assumption: this xpath will select exactly one connector
        Element httpConnector =
                (Element) getDocument()
                        .selectSingleNode(HTTP_CONNECTOR_XPATH);
        httpConnector.addAttribute("URIEncoding", URI_ENCODING);

        // https connector
        httpConnector =
            (Element) getDocument()
                .selectSingleNode(HTTPS_CONNECTOR_XPATH);
        if (httpConnector != null ) {
            httpConnector.addAttribute("URIEncoding", URI_ENCODING);
        }

        // AJP connector
        httpConnector =
            (Element) getDocument()
                .selectSingleNode(AJP_CONNECTOR_XPATH);
        if (httpConnector != null ) {
            httpConnector.addAttribute("URIEncoding", URI_ENCODING);
        }
    }


    /**
     * Adds the attribute to the element if the attributeValue is not equal to
     * defaultValue. If attributeValue is null or equals defaultValue, remove
     * the attribute from the element if it is present.
     *
     * @param element
     * @param attributeName
     * @param attributeValue
     * @param defaultValue
     */
    private void addAttribute(Element element,
                              String attributeName,
                              String attributeValue,
                              String defaultValue) {
        if (attributeValue == null || attributeValue.equals(defaultValue)) {
            Attribute attribute =
                    (Attribute) element.selectSingleNode(attributeName);
            if (attribute != null) {
                element.remove(attribute);
            }
        } else {
            element.addAttribute(attributeName, attributeValue);
        }
    }
}
