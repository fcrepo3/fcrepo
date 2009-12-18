/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fedora.common.Constants;

import fedora.server.errors.GeneralException;
import fedora.server.errors.StreamIOException;

/**
 * SAX parser to deserialize the beSecurity XML file that contains configuration
 * properties for backend services.
 * 
 * @author Sandy Payette
 */
public class BackendSecurityDeserializer
        extends DefaultHandler
        implements Constants {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(BackendSecurityDeserializer.class.getName());

    /** Attribute names in the beSecurity spec file */
    public static final String CALL_BASIC_AUTH = "callBasicAuth";

    public static final String CALL_SSL = "callSSL";

    public static final String CALL_USERNAME = "callUsername";

    public static final String CALL_PASSWORD = "callPassword";

    public static final String CALLBACK_BASIC_AUTH = "callbackBasicAuth";

    public static final String CALLBACK_SSL = "callbackSSL";

    //public static final String CALLBACK_USERNAME = "callbackUsername";
    //public static final String CALLBACK_PASSWORD = "callbackPassword";
    public static final String IPLIST = "iplist";

    public static final String ROLE = "role";

    /** Target objects for deserialization. */
    private final BackendSecuritySpec beSS;

    private Hashtable<String, String> beProperties =
            new Hashtable<String, String>();

    /** Temp variables for SAX parse */
    private final SAXParser tmp_parser;

    private boolean tmp_rootElementFound;

    private int tmp_level;

    private String tmp_parentRole;

    private Hashtable<String, String> tmp_rootProperties;

    private Hashtable<String, String> tmp_serviceProperties;

    private String tmp_role;

    public BackendSecurityDeserializer(String characterEncoding,
                                       boolean validate)
            throws FactoryConfigurationError, ParserConfigurationException,
            SAXException, UnsupportedEncodingException {
        StringBuffer buf = new StringBuffer();
        buf.append("test");
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setValidating(validate);
        spf.setNamespaceAware(true);
        tmp_parser = spf.newSAXParser();

        // set up objects for the parsed information
        beSS = new BackendSecuritySpec();
        beProperties = new Hashtable<String, String>();
    }

    public BackendSecuritySpec deserialize(String inFilePath)
            throws GeneralException, StreamIOException,
            UnsupportedEncodingException {

        LOG.debug("Parsing beSecurity file...");

        tmp_level = 0;
        try {
            FileInputStream fis = new FileInputStream(new File(inFilePath));
            tmp_parser.parse(fis, this);
        } catch (IOException ioe) {
            throw new StreamIOException("BackendSecurityDeserializer: "
                    + "Stream IO problem while parsing backend security config file.");
        } catch (SAXException se) {
            throw new GeneralException("BackendSecurityDeserializer: "
                    + "Error parsing backend security config file. "
                    + se.getMessage());
        }
        if (!tmp_rootElementFound) {
            throw new GeneralException("BackendSecurityDeserializer: "
                    + "Root element not found in backend security config file.");
        }

        LOG.debug("Parse successful.");
        return beSS;
    }

    @Override
    public void startElement(String uri,
                             String localName,
                             String qName,
                             Attributes a) throws SAXException {

        if (uri.equals(BE_SECURITY.uri)
                && localName.equals("serviceSecurityDescription")) {

            LOG.debug("start element uri=" + uri + " localName=" + localName
                    + " tmp_level=" + tmp_level);

            tmp_role = grab(a, BE_SECURITY.uri, ROLE);
            beProperties = new Hashtable<String, String>();
            setProperty(CALL_BASIC_AUTH, grab(a,
                                              BE_SECURITY.uri,
                                              CALL_BASIC_AUTH));
            setProperty(CALL_SSL, grab(a, BE_SECURITY.uri, CALL_SSL));
            setProperty(CALL_USERNAME, grab(a, BE_SECURITY.uri, CALL_USERNAME));
            setProperty(CALL_PASSWORD, grab(a, BE_SECURITY.uri, CALL_PASSWORD));
            setProperty(CALLBACK_BASIC_AUTH, grab(a,
                                                  BE_SECURITY.uri,
                                                  CALLBACK_BASIC_AUTH));
            setProperty(CALLBACK_SSL, grab(a, BE_SECURITY.uri, CALLBACK_SSL));
            setProperty(IPLIST, grab(a, BE_SECURITY.uri, IPLIST));

            try {
                if (tmp_level == 0) {
                    tmp_rootElementFound = true;
                    tmp_rootProperties = new Hashtable<String, String>();
                    tmp_rootProperties.putAll(beProperties);
                    validateProperties();
                    beSS.setSecuritySpec("default", null, beProperties);
                } else if (tmp_level == 1) {
                    tmp_parentRole = tmp_role;
                    tmp_serviceProperties = new Hashtable<String, String>();
                    tmp_serviceProperties.putAll(beProperties);
                    inheritProperties(tmp_rootProperties);
                    validateProperties();
                    beSS.setSecuritySpec(tmp_role, null, beProperties);
                } else if (tmp_level == 2) {
                    inheritProperties(tmp_serviceProperties);
                    inheritProperties(tmp_rootProperties);
                    validateProperties();
                    beSS
                            .setSecuritySpec(tmp_parentRole,
                                             tmp_role,
                                             beProperties);
                } else {
                    LOG.debug("xml element depth exceeded");
                    throw new SAXException("BackendSecurityDeserializer: "
                            + "serviceSecurityDescription elements can only "
                            + "be nested two levels deep from root element!");
                }
            } catch (Exception e) {
                throw new SAXException("BackendSecurityDeserializer: "
                        + "Error setting properties for role " + tmp_role
                        + ". " + e.getMessage());
            }
            tmp_level++;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {

        LOG.debug("end element uri=" + uri + " localName=" + localName
                + " tmp_level=" + tmp_level);
        if (uri.equals(BE_SECURITY.uri)
                && localName.equals("serviceSecurityDescription")) {
            tmp_level--;
        }
    }

    private static String grab(Attributes a,
                               String namespace,
                               String elementName) {
        String ret = a.getValue(namespace, elementName);
        if (ret == null) {
            ret = a.getValue(elementName);
        }
        return ret;
    }

    private void setProperty(String key, String value) {
        if (key != null && value != null) {
            LOG.debug("Setting propery.  key=" + key + " value=" + value);
            beProperties.put(key, value);
        }
    }

    private void inheritProperties(Hashtable inheritableProperties) {

        LOG.debug("Setting inherited properties...");
        Iterator it = inheritableProperties.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            if (!beProperties.containsKey(key)) {
                setProperty(key, (String) inheritableProperties.get(key));
            }
        }
    }

    private void validateProperties() throws GeneralException {

        LOG.debug("Validating properties...");
        if (!beProperties.containsKey(CALL_BASIC_AUTH)) {
            setProperty(CALL_BASIC_AUTH, "false");
        }
        if (!beProperties.containsKey(CALL_SSL)) {
            setProperty(CALL_SSL, "false");
        }
        if (!beProperties.containsKey(CALLBACK_BASIC_AUTH)) {
            setProperty(CALLBACK_BASIC_AUTH, "false");
        }
        if (!beProperties.containsKey(CALLBACK_SSL)) {
            setProperty(CALLBACK_SSL, "false");
        }
        if (beProperties.get(CALL_BASIC_AUTH).equals("true")) {
            if (!beProperties.containsKey(CALL_USERNAME)) {
                throw new GeneralException("BackendSecurityDeserializer: "
                        + "callBasicAuth is set to true, but callUsername is missing"
                        + "for role of " + tmp_role);
            }
            if (!beProperties.containsKey(CALL_PASSWORD)) {
                throw new GeneralException("BackendSecurityDeserializer: "
                        + "callBasicAuth is set to true, but callPassword is missing"
                        + "for role of " + tmp_role);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        LOG.debug("BackendSecurityDeserializer start main()...");
        BackendSecurityDeserializer bds =
                new BackendSecurityDeserializer("UTF-8", false);
        BackendSecuritySpec beSS = bds.deserialize(args[0]);

        // Let's see all the stuff...
        Set allRoleKeys = beSS.listRoleKeys();
        Iterator iterRoles = allRoleKeys.iterator();
        while (iterRoles.hasNext()) {
            String roleKey = (String) iterRoles.next();
            LOG.debug("************ ROLEKEY = " + roleKey);
            // let's see all the properties for this role...    
            Hashtable roleProperties = beSS.getSecuritySpec(roleKey);
            Iterator iterProps = roleProperties.keySet().iterator();
            while (iterProps.hasNext()) {
                String propKey = (String) iterProps.next();
                String propValue = (String) roleProperties.get(propKey);
                LOG.debug(propKey + "=" + propValue);
            }
        }
    }
}
