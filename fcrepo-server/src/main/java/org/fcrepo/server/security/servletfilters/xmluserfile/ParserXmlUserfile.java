/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.servletfilters.xmluserfile;

import java.io.IOException;
import java.io.InputStream;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.fcrepo.server.security.servletfilters.FinishedParsingException;
import org.fcrepo.utilities.XmlTransformUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ParserXmlUserfile
        extends DefaultHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(ParserXmlUserfile.class);

    private final InputStream m_xmlStream;

    public ParserXmlUserfile(InputStream xmlStream)
            throws IOException {
        logger.debug("Initializing XMLUserfile parser");

        m_xmlStream = xmlStream;
    }

    private String username = null;

    private String password = null;

    private Boolean authenticated = null;

    private Map<String, Set<String>> namedAttributes = null;

    private String attributeName = null;

    private Set<String> attributeValues = null;

    public final Boolean getAuthenticated() {
        return authenticated;
    }

    public final Map<String, Set<String>> getNamedAttributes() {
        return namedAttributes;
    }

    private StringBuffer value = null;

    private boolean inValue = false;

    private boolean foundUser = false;

    @Override
    public void startElement(String uri,
                             String localName,
                             String qName,
                             Attributes a) throws SAXException {
        if (localName.equals("users")) {
            logger.debug("<users> foundUser==" + foundUser);
        } else if (localName.equals("user")) {
            logger.debug("<user> foundUser==" + foundUser);
            logger.debug("<<user>> this node username==" + a.getValue("name")
                    + " password==" + a.getValue("password"));
            if (username.equals(a.getValue("name"))) {
                foundUser = true;
                authenticated =
                        Boolean.valueOf(password != null
                                && password.equals(a.getValue("password")));
            }
        } else if (localName.equals("attribute")) {
            logger.debug("<attribute> foundUser==" + foundUser);
            if (foundUser) {
                attributeName = a.getValue("name");
                attributeValues = new HashSet<String>();
                logger.debug("attributeName==" + attributeName);
            }
        } else if (localName.equals("value")) {
            logger.debug("<value> foundUser==" + foundUser);
            inValue = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (localName.equals("users")) {
            logger.debug("</users> foundUser==" + foundUser);
            authenticated = Boolean.FALSE;
        } else if (localName.equals("user")) {
            logger.debug("</user> foundUser==" + foundUser);
            if (foundUser) {
                logger.debug("at </user> (quick audit)");
                logger.debug("authenticated=={}", authenticated);
                logger.debug("namedAttributes n=={}", namedAttributes.size());
                throw new FinishedParsingException("");
            }
        } else if (localName.equals("attribute")) {
            logger.debug("</attribute> foundUser==" + foundUser);
            if (foundUser) {
                logger.debug("set n=={}", attributeValues.size());
                namedAttributes.put(attributeName, attributeValues);
                logger.debug("just added values for {}", attributeName);
            }
            attributeName = null;
            attributeValues = null;
        } else if (localName.equals("value")) {
            logger.debug("</value> foundUser=={}", foundUser);
            if (foundUser) {
                attributeValues.add(value.toString());
                logger.debug("just added {}", value);
            }
            logger.debug("quick audit of value string =={}", value);
            value.setLength(0);
            inValue = false;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (inValue && foundUser && value != null) {
            value.append(ch, start, length);
            logger.debug("characters called start=={} length=={}",
                    start, length);
        }
    }

    public void parse(String username, String password) throws IOException,
            FinishedParsingException {
        this.username = username;
        this.password = password;
        try {
            value = new StringBuffer();
            authenticated = null;
            namedAttributes = new Hashtable<String, Set<String>>();
            foundUser = false;
            XmlTransformUtility.parseWithoutValidating(m_xmlStream, this);
        } catch (FinishedParsingException fpe) {
            throw fpe;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new IOException("Error parsing XML: " + e.getMessage());
        }
    }

}
