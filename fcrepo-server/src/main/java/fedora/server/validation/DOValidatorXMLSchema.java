/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URI;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import fedora.common.Constants;

import fedora.server.errors.GeneralException;
import fedora.server.errors.ObjectValidityException;

/**
 * XML Schema validation for Digital Objects.
 * 
 * @author Sandy Payette
 */
public class DOValidatorXMLSchema
        implements Constants, EntityResolver {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(DOValidatorXMLSchema.class.getName());

    /** Constants used for JAXP 1.2 */
    private static final String JAXP_SCHEMA_LANGUAGE =
            "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    private URI schemaURI = null;

    public DOValidatorXMLSchema(String schemaPath)
            throws GeneralException {
        try {
            schemaURI = (new File(schemaPath)).toURI();
        } catch (Exception e) {
            LOG.error("Error constructing validator", e);
            throw new GeneralException(e.getMessage());
        }
    }

    public void validate(File objectAsFile) throws ObjectValidityException,
            GeneralException {
        try {
            validate(new InputSource(new FileInputStream(objectAsFile)));
        } catch (IOException e) {
            String msg =
                    "DOValidatorXMLSchema returned error.\n"
                            + "The underlying exception was a "
                            + e.getClass().getName() + ".\n"
                            + "The message was " + "\"" + e.getMessage() + "\"";
            throw new GeneralException(msg);
        }
    }

    public void validate(InputStream objectAsStream)
            throws ObjectValidityException, GeneralException {
        validate(new InputSource(objectAsStream));
    }

    private void validate(InputSource objectAsSource)
            throws ObjectValidityException, GeneralException {
        InputSource doXML = objectAsSource;
        try {
            // XMLSchema validation via SAX parser
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setValidating(true);
            SAXParser sp = spf.newSAXParser();
            sp.setProperty(JAXP_SCHEMA_LANGUAGE, XML_XSD.uri);

            // JAXP property for schema location
            sp
                    .setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource",
                                 schemaURI.toString());

            XMLReader xmlreader = sp.getXMLReader();
            xmlreader.setErrorHandler(new DOValidatorXMLErrorHandler());
            xmlreader.setEntityResolver(this);
            xmlreader.parse(doXML);
        } catch (ParserConfigurationException e) {
            String msg =
                    "DOValidatorXMLSchema returned parser error.\n"
                            + "The underlying exception was a "
                            + e.getClass().getName() + ".\n"
                            + "The message was " + "\"" + e.getMessage() + "\"";
            throw new GeneralException(msg, e);
        } catch (SAXException e) {
            String msg =
                    "DOValidatorXMLSchema returned validation exception.\n"
                            + "The underlying exception was a "
                            + e.getClass().getName() + ".\n"
                            + "The message was " + "\"" + e.getMessage() + "\"";
            throw new ObjectValidityException(msg, e);
        } catch (Exception e) {
            String msg =
                    "DOValidatorXMLSchema returned error.\n"
                            + "The underlying error was a "
                            + e.getClass().getName() + ".\n"
                            + "The message was " + "\"" + e.getMessage() + "\"";
            throw new GeneralException(msg, e);
        }
    }

    /**
     * Resolve the entity if it's referring to a local schema. Otherwise, return
     * an empty InputSource. This behavior is required in order to ensure that
     * Xerces never attempts to load external schemas specified with
     * xsi:schemaLocation. It is not enough that we specify
     * processContents="skip" in our own schema.
     */
    public InputSource resolveEntity(String publicId, String systemId) {
        if (systemId != null && systemId.startsWith("file:")) {
            return null;
        } else {
            return new InputSource();
        }
    }
}
