/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.fcrepo.server.errors.ValidationException;
import org.fcrepo.utilities.XmlTransformUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.jboss.security.xacml.sunxacml.AbstractPolicy;
import org.jboss.security.xacml.sunxacml.ParsingException;
import org.jboss.security.xacml.sunxacml.Policy;
import org.jboss.security.xacml.sunxacml.PolicySet;



/**
 * A validating parser for XACML policies.
 * <p>
 * This class also provides a commandline XACML validation utility.
 * <p>
 * NOTE: Although instances may be re-used, this class is not thread-safe.
 * Use the <code>copy()</code> method to support concurrent parsing.
 */
public class PolicyParser {
    private static final Logger logger = LoggerFactory.getLogger(PolicyParser.class);
    private static final String W3C_XML_SCHEMA_NS_URI = "http://www.w3.org/2001/XMLSchema";
    
    // Neither of these factories are thread-safe, so access is synchronized in methods below
    private static final SchemaFactory SCHEMA_FACTORY = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);

    private static final ErrorHandler THROW_ALL = new ThrowAllErrorHandler();
    
    private final SoftReferenceObjectPool<Validator> m_validators;

    /**
     * Creates an instance that will validate according to the given schema.
     *
     * @param schemaStream the XSD schema to use for schema validation
     * @throws IOException if the schema can't be read
     * @throws SAXException if the schema isn't valid
     */
    public PolicyParser(InputStream schemaStream)
            throws IOException, SAXException {
        this(getSchema(schemaStream));
    }

    // actual constructor keeps schema (which is thread safe) for cheap copying
    private PolicyParser(Schema schema)
            throws SAXException {
        m_validators = new SoftReferenceObjectPool<Validator>(new PoolableValidatorFactory(schema));
    }

    private PolicyParser(SoftReferenceObjectPool<Validator> validators) {
        m_validators = validators;
    }
    /**
     * Schema Factory is not thread safe
     * @return
     */
    private static Schema getSchema(InputStream schemaStream) throws SAXException {
        Schema result;
        synchronized(SCHEMA_FACTORY){
            result = SCHEMA_FACTORY.newSchema(new StreamSource(schemaStream));
        }
        return result;
    }

    /**
     * Gets a new instance that uses the same schema as this one.
     *
     * @return a copy of this instance
     */
    public PolicyParser copy() {
        return new PolicyParser(m_validators);
    }

    /**
     * Parses the given policy and optionally schema validates it.
     *
     * @param policyStream
     *          the serialized XACML policy
     * @param schemaValidate
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
        DocumentBuilder domParser = null;
        try {
            domParser = XmlTransformUtility.borrowDocumentBuilder();
            domParser.setErrorHandler(THROW_ALL);
            doc = domParser.parse(policyStream);
        } catch (Exception e) {
            throw new ValidationException("Policy invalid; malformed XML", e);
        } finally {
            if (domParser != null) {
                XmlTransformUtility.returnDocumentBuilder(domParser);
            }
        }

        if (schemaValidate) {
            // XSD-validate; die if not schema-valid
            Validator validator = null;
            try {
                validator = m_validators.borrowObject();
                validator.validate(new DOMSource(doc));
            } catch (Exception e) {
                throw new ValidationException("Policy invalid; schema"
                                              + " validation failed", e);
            } finally {
                if (validator != null) try {
                    m_validators.returnObject(validator);
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
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
/**
 * This class is a workaround to some shift in the behavior of anonymous inner classes
 *
 */
    public static class ThrowAllErrorHandler implements ErrorHandler {
        public void error(SAXParseException e) throws SAXParseException { throw e; }
        public void fatalError(SAXParseException e) throws SAXParseException { throw e; }
        public void warning(SAXParseException e) throws SAXParseException { throw e; }
    }

    public static class PoolableValidatorFactory implements PoolableObjectFactory<Validator> {
        private final Schema m_schema;
        public PoolableValidatorFactory(Schema schema) {
            m_schema = schema;
        }
        @Override
        public Validator makeObject() throws Exception {
            return m_schema.newValidator();
        }
        @Override
        public void destroyObject(Validator obj) throws Exception {
            // no op
        }
        @Override
        public boolean validateObject(Validator obj) {
            // no op
            return true;
        }
        @Override
        public void activateObject(Validator obj) throws Exception {
            obj.reset();
        }
        @Override
        public void passivateObject(Validator obj) throws Exception {
            // no op
        }
    }
}
