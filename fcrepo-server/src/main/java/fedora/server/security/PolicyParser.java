/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.ParsingException;
import com.sun.xacml.Policy;
import com.sun.xacml.PolicySet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import fedora.common.FaultException;

import fedora.server.errors.ValidationException;
import fedora.server.utilities.StreamUtility;

/**
 * A validating parser for XACML policies.
 * <p>
 * This class also provides a commandline XACML validation utility.
 * <p>
 * NOTE: Although instances may be re-used, this class is not thread-safe.
 * Use the <code>copy()</code> method to support concurrent parsing.
 */
public class PolicyParser {

    private final byte[] m_schemaBytes;

    private final Validator m_validator;

    private final DocumentBuilder m_domParser;

    /**
     * Creates an instance that will validate according to the given schema.
     *
     * @param schemaStream the XSD schema to use for schema validation
     * @throws IOException if the schema can't be read
     * @throws SAXException if the schema isn't valid
     */
    public PolicyParser(InputStream schemaStream)
            throws IOException, SAXException {
        this(StreamUtility.getBytes(schemaStream));
    }

    // actual constructor keeps schema bytes to enable cheap copying
    private PolicyParser(byte[] schemaBytes)
            throws SAXException {
        m_schemaBytes = schemaBytes;
        m_validator = createXSDValidator(new ByteArrayInputStream(m_schemaBytes));
        m_domParser = createDOMParser();
    }

    /**
     * Gets a new instance that uses the same schema as this one.
     *
     * @return a copy of this instance
     */
    public PolicyParser copy() {
        try {
            return new PolicyParser(m_schemaBytes);
        } catch (SAXException wontHappen) {
            throw new FaultException(wontHappen);
        }
    }

    /**
     * Parses the given policy and optionally schema validates it.
     *
     * @param policyStream
     *          the serialized XACML policy
     * @param validate
     *          whether to schema validate
     * @return the parsed policy.
     * @throws ValidationException
     *           if the given xml is not a valid policy. This will occur if it
     *           is not well-formed XML, its root element is not named
     *           <code>Policy</code> or <code>PolicySet</code>, it triggers
     *           a parse exception in the Sun libraries when constructing an
     *           <code>AbstractPolicy</code> from the DOM, or (if validation
     *           is true) it is not schema-valid.
     */
    public AbstractPolicy parse(InputStream policyStream,
                                boolean schemaValidate)
            throws ValidationException {

        // Parse; die if not well-formed
        Document doc = null;
        try {
            doc = m_domParser.parse(policyStream);
        } catch (Exception e) {
            throw new ValidationException("Policy invalid; malformed XML", e);
        }

        if (schemaValidate) {
            // XSD-validate; die if not schema-valid
            try {
                m_validator.validate(new DOMSource(doc));
            } catch (Exception e) {
                throw new ValidationException("Policy invalid; schema"
                                              + " validation failed", e);
            }
        }

        // Construct AbstractPolicy from doc; die if root isn't "Policy[Set]"
        Element root = doc.getDocumentElement();
        String rootName = root.getTagName();
        try {
            if (rootName.equals("Policy")) {
                return Policy.getInstance(root);
            } else if (rootName.equals("PolicySet")) {
                return PolicySet.getInstance(root);
            } else {
                throw new ValidationException("Policy invalid; root element is "
                                              + rootName + ", but should be "
                                              + "Policy or PolicySet");
            }
        } catch (ParsingException e) {
            throw new ValidationException("Policy invalid; failed parsing by "
                                          + "Sun XACML implementation", e);
        }
    }

    private static DocumentBuilder createDOMParser() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                public void error(SAXParseException e) throws SAXParseException { throw e; }
                public void fatalError(SAXParseException e) throws SAXParseException { throw e; }
                public void warning(SAXParseException e) throws SAXParseException { throw e; }
            });
            return builder;
        } catch (ParserConfigurationException e) {
            throw new FaultException(e);
        }
    }

    private static Validator createXSDValidator(InputStream schemaStream)
            throws SAXException {
        if (schemaStream == null) return null;
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(new StreamSource(schemaStream));
        return schema.newValidator();
    }

    /**
     * Command-line utility for validating XACML policies.
     * <p>
     * Accepts a single argument: the path to the policy instance to validate.
     * <p>
     * Also requires that the com.sun.xacml.PolicySchema system property points
     * to the XACML schema.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            fail("One argument required: /path/to/xacml-policy-to-validate.xml");
        }
        final String schemaPathProperty = "com.sun.xacml.PolicySchema";
        String schemaPath = System.getProperty(schemaPathProperty);
        if (schemaPath == null) {
            fail("System property " + schemaPathProperty + " (path to XACML "
                 + "schema) must be set. (e.g. -D" + schemaPathProperty
                 + "=/path/to/schema)");
        }
        try {
            InputStream instance = getStream(args[0]);
            PolicyParser parser = new PolicyParser(getStream(schemaPath));
            parser.parse(instance, true);
            System.out.println("Validation successful");
            System.exit(0);
        } catch (ValidationException e) {
            if (e.getCause() != null && e.getCause() instanceof SAXParseException) {
                fail(e.getCause().getMessage());
            } else {
                fail(e);
            }
        } catch (Exception e) {
            fail(e);
        }
    }

    private static InputStream getStream(String path) {
        try {
            return new FileInputStream(path);
        } catch (Exception e) {
            fail("File not found: " + path);
            return null;
        }
    }

    private static void fail(String message) {
        System.out.println("ERROR: " + message);
        System.out.println("Validation failed");
        System.exit(1);
    }

    private static void fail(Exception e) {
        e.printStackTrace();
        fail(e.getClass().getName() + ": See above for detail");
    }
}
