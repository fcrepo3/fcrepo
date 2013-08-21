/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.fcrepo.common.Constants;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.ObjectValidityException;
import org.fcrepo.server.storage.types.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;



/**
 * XML Schema validation for Digital Objects.
 *
 * @author Sandy Payette
 */
public class DOValidatorXMLSchema
        implements Constants, EntityResolver {

    private static final Logger logger =
            LoggerFactory.getLogger(DOValidatorXMLSchema.class);
    
    
    private final Schema m_schema;
    
    public DOValidatorXMLSchema(Schema schema) {
        m_schema = schema;
    }

    public DOValidatorXMLSchema(String schemaPath)
            throws GeneralException {
        try {
            File schemaFile = new File(schemaPath);
            SchemaFactory schemaFactory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            m_schema = schemaFactory.newSchema(schemaFile);
        } catch (Exception e) {
            logger.error("Error constructing validator", e);
            throw new GeneralException(e.getMessage());
        }
    }

    public void validate(File objectAsFile) throws ObjectValidityException,
            GeneralException {
        try {
            validate(new StreamSource(new FileInputStream(objectAsFile)));
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
        validate(new StreamSource(objectAsStream));
    }

    private void validate(StreamSource objectAsSource)
            throws ObjectValidityException, GeneralException {
        StreamSource doXML = objectAsSource;
        try {
            // XMLSchema validation via SAX parser
            
            Validator xsv = m_schema.newValidator();
            xsv.setErrorHandler(new DOValidatorXMLErrorHandler());

            xsv.validate(doXML);
        }  catch (SAXException e) {
            String msg =
                    "DOValidatorXMLSchema returned validation exception.\n"
                            + "The underlying exception was a "
                            + e.getClass().getName() + ".\n"
                            + "The message was " + "\"" + e.getMessage() + "\"";
            Validation validation = new Validation("unknown");
            List<String> problems = new ArrayList<String>();
            problems.add(msg);
            validation.setObjectProblems(problems);
            throw new ObjectValidityException(msg, validation, e);
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
