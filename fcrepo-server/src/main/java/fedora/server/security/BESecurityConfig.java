/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fedora.common.Constants;

/**
 * Security configuration for backend services.
 * 
 * @author Chris Wilper
 */
public class BESecurityConfig
        implements Constants {

    private static final String _CONFIG = "serviceSecurityDescription";

    private static final String _INTERNAL_PREFIX = "fedoraInternalCall-";

    private final static String _ROLE = "role";

    private final static String _CALLSSL = "callSSL";

    private final static String _CALLBASICAUTH = "callBasicAuth";

    private final static String _CALLUSERNAME = "callUsername";

    private final static String _CALLPASSWORD = "callPassword";

    private final static String _CALLBACKSSL = "callbackSSL";

    private final static String _CALLBACKBASICAUTH = "callbackBasicAuth";

    private final static String _IPLIST = "iplist";

    /**
     * The default role configuration, specifying the values to be assumed for
     * any internal call or SDep configuration value which is null.
     */
    private DefaultRoleConfig m_defaultConfig;

    /**
     * Whether Fedora-to-self calls should use SSL.
     */
    private Boolean m_internalSSL;

    /**
     * Whether Fedora-to-self calls should use basic auth.
     */
    private Boolean m_internalBasicAuth;

    /**
     * The username to be used for basic-authenticaed Fedora-to-self calls. This
     * value, along with the username, should also be configured in
     * tomcat-users.xml or whatever other authentication database is in effect.
     */
    private String m_internalUsername;

    /**
     * The password to be used for basic-authenticaed Fedora-to-self calls. This
     * value, along with the password, should also be configured in
     * tomcat-users.xml or whatever other authentication database is in effect.
     */
    private String m_internalPassword;

    /**
     * The list of IP addresses that are allowed for Fedora-to-self calls. This
     * should normally contain 127.0.0.1 and the external IP address of the
     * running server, if known.
     */
    private String[] m_internalIPList;

    /**
     * A sorted, PID-keyed map of <code>ServiceDeploymentRoleConfig</code>s.
     */
    private final SortedMap<String, ServiceDeploymentRoleConfig> m_sDepConfigs;

    /**
     * Create an empty BESecurityConfig with an empty map of
     * <code>ServiceDeploymentRoleConfig</code>s and <code>null</code> values for
     * everything else.
     */
    public BESecurityConfig() {
        m_sDepConfigs = new TreeMap<String, ServiceDeploymentRoleConfig>();
    }

    /**
     * Get the default role configuration.
     */
    public DefaultRoleConfig getDefaultConfig() {
        return m_defaultConfig;
    }

    /**
     * Set the default role configuration.
     */
    public void setDefaultConfig(DefaultRoleConfig config) {
        m_defaultConfig = config;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get whether SSL should be used for Fedora-to-self calls. This should be
     * true if API-A is only available via SSL.
     */
    public Boolean getInternalSSL() {
        return m_internalSSL;
    }

    /**
     * Get whether SSL is effectively used for Fedora-to-self calls. This will
     * be the internalSSL value, if set, or the inherited call value from the
     * default role, if set, or Boolean.FALSE.
     */
    public Boolean getEffectiveInternalSSL() {
        if (m_internalSSL != null) {
            return m_internalSSL;
        } else if (m_defaultConfig != null) {
            return m_defaultConfig.getEffectiveCallSSL();
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * Set whether SSL is used for Fedora-to-self calls.
     */
    public void setInternalSSL(Boolean value) {
        m_internalSSL = value;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get whether basic auth should be used for Fedora-to-self calls. This
     * should be true if API-A requires basic auth.
     */
    public Boolean getInternalBasicAuth() {
        return m_internalBasicAuth;
    }

    /**
     * Get whether basic auth is effectively used for Fedora-to-self calls. This
     * will be the internalBasicAuth value, if set, or the inherited call value
     * from the default role, if set, or Boolean.FALSE.
     */
    public Boolean getEffectiveInternalBasicAuth() {
        if (m_internalBasicAuth != null) {
            return m_internalBasicAuth;
        } else if (m_defaultConfig != null) {
            return m_defaultConfig.getEffectiveCallBasicAuth();
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * Set whether basic auth is used for Fedora-to-self calls.
     */
    public void setInternalBasicAuth(Boolean value) {
        m_internalBasicAuth = value;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get the internal username.
     */
    public String getInternalUsername() {
        return m_internalUsername;
    }

    /**
     * Get the effective internal username for basic auth Fedora-to-self calls.
     * This will be the internal username, if set, or the inherited call value
     * from the default role, if set, or null.
     */
    public String getEffectiveInternalUsername() {
        if (m_internalUsername != null) {
            return m_internalUsername;
        } else if (m_defaultConfig != null) {
            return m_defaultConfig.getEffectiveCallUsername();
        } else {
            return null;
        }
    }

    /**
     * Set the internal username.
     */
    public void setInternalUsername(String username) {
        m_internalUsername = username;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get the internal password.
     */
    public String getInternalPassword() {
        return m_internalPassword;
    }

    /**
     * Get the effective internal password for basic auth Fedora-to-self calls.
     * This will be the internal password, if set, or the inherited call value
     * from the default role, if set, or null.
     */
    public String getEffectiveInternalPassword() {
        if (m_internalPassword != null) {
            return m_internalPassword;
        } else if (m_defaultConfig != null) {
            return m_defaultConfig.getEffectiveCallPassword();
        } else {
            return null;
        }
    }

    /**
     * Set the internal password.
     */
    public void setInternalPassword(String password) {
        m_internalPassword = password;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get the list of internal IP addresses.
     */
    public String[] getInternalIPList() {
        return m_internalIPList;
    }

    /**
     * Get the effective list of internal IP addresses. This will be the
     * internalIPList value, if set, or the inherited value from the default
     * role, if set, or null.
     */
    public String[] getEffectiveInternalIPList() {
        if (m_internalIPList != null) {
            return m_internalIPList;
        } else if (m_defaultConfig != null) {
            return m_defaultConfig.getEffectiveIPList();
        } else {
            return null;
        }
    }

    /**
     * Set the list of internal IP addresses.
     */
    public void setInternalIPList(String[] ips) {
        m_internalIPList = ips;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get the mutable, sorted, PID-keyed map of <code>ServiceDeploymentRoleConfig</code>s.
     */
    public SortedMap<String, ServiceDeploymentRoleConfig> getServiceDeploymentConfigs() {
        return m_sDepConfigs;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Add empty sDep and method configurations given by the map if they are
     * not already already defined.
     */
    public void addEmptyConfigs(Map pidToMethodList) {
        Iterator pIter = pidToMethodList.keySet().iterator();
        while (pIter.hasNext()) {
            String sDepPID = (String) pIter.next();
            // add the sDep indicated by the key if it doesn't exist
            ServiceDeploymentRoleConfig sDepRoleConfig = m_sDepConfigs.get(sDepPID);
            if (sDepRoleConfig == null) {
                sDepRoleConfig =
                        new ServiceDeploymentRoleConfig(m_defaultConfig, sDepPID);
                m_sDepConfigs.put(sDepPID, sDepRoleConfig);
            }
            // add each method indicated by the List which doesn't already exist
            Iterator mIter = ((List) pidToMethodList.get(sDepPID)).iterator();
            while (mIter.hasNext()) {
                String methodName = (String) mIter.next();
                MethodRoleConfig methodRoleConfig =
                        sDepRoleConfig.getMethodConfigs().get(methodName);
                if (methodRoleConfig == null) {
                    methodRoleConfig =
                            new MethodRoleConfig(sDepRoleConfig, methodName);
                    sDepRoleConfig.getMethodConfigs().put(methodName,
                                                           methodRoleConfig);
                }
            }
        }
    }

    //
    // Deserialization/serialization to/from XML streams.
    //

    /**
     * Instantiate a <code>BESecurityConfig</code> from an XML stream.
     */
    public static BESecurityConfig fromStream(InputStream in) throws Exception {

        BESecurityConfig config = new BESecurityConfig();

        // instantiate DOM
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(in);
        Element root = doc.getDocumentElement();

        // set default role configuration
        DefaultRoleConfig defaultRoleConfig = new DefaultRoleConfig();
        setValuesFromElement(defaultRoleConfig, root);
        config.setDefaultConfig(defaultRoleConfig);

        // get all child config nodes for repeated use
        NodeList nodes = root.getElementsByTagName(_CONFIG);

        // parse and add all explicitly configured sdef configurations
        // while also parsing fedoraInternalCall-1 and setting appropriate vals
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            String role = e.getAttribute(_ROLE);
            if (role.indexOf(":") != -1 && role.indexOf("/") == -1) {
                ServiceDeploymentRoleConfig sDepRoleConfig =
                        new ServiceDeploymentRoleConfig(defaultRoleConfig, role);
                setValuesFromElement(sDepRoleConfig, e);
                config.getServiceDeploymentConfigs().put(role, sDepRoleConfig);
            } else if (role.equals(_INTERNAL_PREFIX + "1")) {
                config.setInternalSSL(getBoolean(e, _CALLSSL));
                config.setInternalBasicAuth(getBoolean(e, _CALLBASICAUTH));
                config.setInternalUsername(getString(e, _CALLUSERNAME));
                config.setInternalPassword(getString(e, _CALLPASSWORD));
                config.setInternalIPList(getStringArray(e, _IPLIST));
            }
        }

        // finally, parse and add all configured methods, first adding
        // a blank sdef role configuration if needed
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            String[] parts = e.getAttribute(_ROLE).split("/");
            if (parts.length == 2) {
                String sDepPID = parts[0];
                String methodName = parts[1];
                ServiceDeploymentRoleConfig sDepRoleConfig =
                        config.getServiceDeploymentConfigs().get(sDepPID);
                if (sDepRoleConfig == null) {
                    sDepRoleConfig =
                            new ServiceDeploymentRoleConfig(defaultRoleConfig, sDepPID);
                    config.getServiceDeploymentConfigs().put(sDepPID, sDepRoleConfig);
                }
                MethodRoleConfig methodRoleConfig =
                        new MethodRoleConfig(sDepRoleConfig, methodName);
                setValuesFromElement(methodRoleConfig, e);
                sDepRoleConfig.getMethodConfigs().put(methodName,
                                                       methodRoleConfig);
            }
        }

        return config;
    }

    private static void setValuesFromElement(BERoleConfig roleConfig, Element e)
            throws Exception {
        roleConfig.setCallSSL(getBoolean(e, _CALLSSL));
        roleConfig.setCallBasicAuth(getBoolean(e, _CALLBASICAUTH));
        roleConfig.setCallUsername(getString(e, _CALLUSERNAME));
        roleConfig.setCallPassword(getString(e, _CALLPASSWORD));
        roleConfig.setCallbackSSL(getBoolean(e, _CALLBACKSSL));
        roleConfig.setCallbackBasicAuth(getBoolean(e, _CALLBACKBASICAUTH));
        roleConfig.setIPList(getStringArray(e, _IPLIST));
    }

    private static String getString(Element e, String name) {
        Attr a = e.getAttributeNode(name);
        if (a != null) {
            return a.getValue();
        } else {
            return null;
        }
    }

    private static Boolean getBoolean(Element e, String name) {
        String s = getString(e, name);
        if (s != null) {
            return new Boolean(s);
        } else {
            return null;
        }
    }

    private static String[] getStringArray(Element e, String name) {
        String s = getString(e, name);
        if (s != null) {
            String[] array = s.split(" +");
            if (array.length == 1 && array[0].length() == 0) {
                return null;
            }
            return array;
        } else {
            return null;
        }
    }

    /**
     * Serialize to the given stream, closing it when finished. If
     * skipNonOverrides is true, any configuration whose values are all null
     * will not be written.
     */
    public void toStream(boolean skipNonOverrides, OutputStream out)
            throws Exception {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
            write(skipNonOverrides, true, writer);
        } finally {
            try {
                writer.close();
            } catch (Throwable th) {
            }
            try {
                out.close();
            } catch (Throwable th) {
            }
        }
    }

    /**
     * Serialize to the given writer, keeping it open when finished. If
     * skipNonOverrides is true, any configuration whose values are all null
     * will not be written.
     */
    public void write(boolean skipNonOverrides,
                      boolean withXMLDeclaration,
                      PrintWriter writer) {

        final String indent = "                           ";

        // header
        if (withXMLDeclaration) {
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        }
        writer.println("<" + _CONFIG + " xmlns=\"" + BE_SECURITY.uri + "\"");
        writer.println(indent + " xmlns:xsi=\"" + XSI.uri + "\"");
        writer.println(indent + " xsi:schemaLocation=\"" + BE_SECURITY.uri
                + " " + BE_SECURITY1_0.xsdLocation + "\"");

        // default values
        writer.print(indent);
        write(m_defaultConfig, false, skipNonOverrides, writer);
        writer.println(">");

        // fedoraInternalCall-1 and -2
        writeInternalConfig(1,
                            m_internalSSL,
                            m_internalBasicAuth,
                            m_internalUsername,
                            m_internalPassword,
                            m_internalIPList,
                            writer);
        writeInternalConfig(2,
                            Boolean.FALSE,
                            Boolean.FALSE,
                            null,
                            null,
                            m_internalIPList,
                            writer);

        // sDep roles
        Iterator bIter = m_sDepConfigs.keySet().iterator();
        while (bIter.hasNext()) {
            String role = (String) bIter.next();
            ServiceDeploymentRoleConfig bConfig = m_sDepConfigs.get(role);
            write(bConfig, true, skipNonOverrides, writer);
            // per-method roles
            Iterator mIter = bConfig.getMethodConfigs().keySet().iterator();
            while (mIter.hasNext()) {
                String methodName = (String) mIter.next();
                MethodRoleConfig mConfig =
                        bConfig.getMethodConfigs().get(methodName);
                write(mConfig, true, skipNonOverrides, writer);
            }
        }

        // closing element for entire doc
        writer.println("</" + _CONFIG + ">");
    }

    private static void writeInternalConfig(int n,
                                            Boolean ssl,
                                            Boolean basicAuth,
                                            String username,
                                            String password,
                                            String[] ipList,
                                            PrintWriter writer) {
        writer.print("  <" + _CONFIG);
        writeAttribute(_ROLE, _INTERNAL_PREFIX + n, writer);
        writeAttribute(_CALLSSL, ssl, writer);
        writeAttribute(_CALLBASICAUTH, basicAuth, writer);
        writeAttribute(_CALLUSERNAME, username, writer);
        writeAttribute(_CALLPASSWORD, password, writer);
        writeAttribute(_CALLBACKSSL, ssl, writer);
        writeAttribute(_CALLBACKBASICAUTH, basicAuth, writer);
        writeAttribute(_IPLIST, ipList, writer);
        writer.println("/>");
    }

    /**
     * Write all the defined attributes of the given <code>BERoleConfig</code>,
     * surrounding them with the appropriate element start/end text if
     * <code>wholeElement</code> is true. Skip the entire element if
     * skipIfAllNull is true.
     */
    private static void write(BERoleConfig config,
                              boolean wholeElement,
                              boolean skipIfAllNull,
                              PrintWriter writer) {

        if (wholeElement) {
            if (skipIfAllNull && config.getCallSSL() == null
                    && config.getCallBasicAuth() == null
                    && config.getCallUsername() == null
                    && config.getCallPassword() == null
                    && config.getCallbackSSL() == null
                    && config.getCallbackBasicAuth() == null
                    && config.getIPList() == null) {
                return;
            }
            writer.print("  <" + _CONFIG);
        }

        writeAttribute(_ROLE, config.getRole(), writer);
        writeAttribute(_CALLSSL, config.getCallSSL(), writer);
        writeAttribute(_CALLBASICAUTH, config.getCallBasicAuth(), writer);
        writeAttribute(_CALLUSERNAME, config.getCallUsername(), writer);
        writeAttribute(_CALLPASSWORD, config.getCallPassword(), writer);
        writeAttribute(_CALLBACKSSL, config.getCallbackSSL(), writer);
        writeAttribute(_CALLBACKBASICAUTH,
                       config.getCallbackBasicAuth(),
                       writer);
        writeAttribute(_IPLIST, config.getIPList(), writer);

        if (wholeElement) {
            writer.println("/>");
        }
    }

    /**
     * Write (space)name="value" to the given PrintWriter if value is defined.
     */
    private static void writeAttribute(String name,
                                       Object value,
                                       PrintWriter writer) {
        if (value != null) {
            String s;
            if (value instanceof String || value instanceof Boolean) {
                // for String/Boolean we can just use toString()
                s = value.toString();
            } else {
                // otherwise its a String[], so space-delimit the values
                String[] tokens = (String[]) value;
                StringBuffer buf = new StringBuffer();
                for (int i = 0; i < tokens.length; i++) {
                    if (i > 0) {
                        buf.append(' ');
                    }
                    buf.append(tokens[i]);
                }
                s = buf.toString();
            }
            writer.print(" " + name + "=\"" + s + "\"");
        }
    }

}