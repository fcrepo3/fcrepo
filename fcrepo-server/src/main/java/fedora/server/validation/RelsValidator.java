/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.validation;

import java.io.InputStream;

import java.net.URI;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.TimeZone;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.axis.types.NCName;

import org.apache.log4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fedora.common.Constants;
import fedora.common.FaultException;
import fedora.common.PID;

import fedora.server.errors.ValidationException;

/**
 * Validates the RDF/XML content of the RELS-EXT and RELS-INT datastreams.
 * <p>
 * The following restrictions are enforced:
 * <ul>
 * <li>The RDF must follow a prescribed RDF/XML authoring style where there is
 * ONE (RELS-EXT) or more (RELS-INT) subjects encoded as RDF &lt;Description&gt;
 * elements with RDF <code>about</code> attribute containing either a digital
 * object URI (RELS-EXT) or a datastream URI (RELS-INT). The sub-elements are
 * the relationship properties of the subject. Each relationship may refer to
 * any resource (identified by URI) via an RDF 'resource' attribute, or a
 * literal. Relationship assertions can be from the default Fedora relationship
 * ontology, or from other namespaces.
 * <ul>
 * <li>RELS-EXT example:
 *
 * <pre>&lt;rdf:Description about="info:fedora/demo:5"&gt;
 *   &lt;fedora:isMemberOfCollection resource="info:fedora/demo:100"/&gt;
 *   &lt;nsdl:isAugmentedBy resource="info:fedora/demo:333"/&gt;
 *   &lt;example:source resource="http://example.org/bsmith/article1.html"/&gt;
 *   &lt;example:primaryAuthor&gt;Bob Smith&lt;/example:primaryAuthor&gt;
 * &lt;/rdf:Description&gt;
 * </pre>
 * </li>
 * <li>RELS-INT example:
 *
 * <pre>&lt;rdf:Description about="info:fedora/demo:5/DS1"&gt;
 *   &lt;nsdl:isAugmentedBy resource="info:fedora/demo:333"/&gt;
 *   &lt;example:source resource="http://example.org/bsmith/article1.html"/&gt;
 *   &lt;example:primaryAuthor&gt;Bob Smith&lt;/example:primaryAuthor&gt;
 * &lt;/rdf:Description&gt;
 * </pre>
 * </li>
 * </ul>
 * </li>
 * <li>For RELS-EXT, there must be only ONE &lt;rdf:Description&gt; element.</li>
 * <li>There must be NO nesting of assertions. In terms of XML depth, the RDF
 * root element is considered depth of 0. Then, the &lt;rdf:Description&gt;
 * element must be at depth of 1, and the relationship properties must exist at
 * depth of 2. That's it.</li>
 * <li>For RELS-EXT, The RDF <code>about</code> attribute of the RDF
 * &lt;Description&gt; must be the URI of the digital object in which the
 * RELS-EXT datastream resides. This means that all relationships are FROM
 * "this" object to other objects.</li>
 * <li>For RELS-INT, the RDF <code>about</code> attribute(s) of the RDF
 * &lt;Description&gt; element(s) must be valid URIs of datastreams for the
 * digital object in which the RELS-EXT datastream resides. The datastreams do
 * not actually have to exist, but these URIs must be syntactically valid. This
 * means that all relationships are FROM datastreams in "this" object to other
 * objects.</li>
 * <li>If the target of the statement is a resource (identified by a URI), the
 * RDF <code>resource</code> attribute must specify a syntactically valid,
 * absolute URI.</li>
 * <li>For RELS-EXT, there must NOT be any assertion of properties from the DC
 * namespace.</li>
 * <li>There must NOT be any assertions of properties from the Fedora object
 * properties namespaces (model and view), with the following exceptions for
 * RELS-EXT only:
 *
 * <pre>
 * fedora-model:hasService
 * fedora-model:hasModel
 * fedora-model:isDeploymentOf
 * fedora-model:isContractorOf
 *        These assertions are allowed in the RELS-EXT datastream, but all
 *        others from the <code>fedora-model</code> and <code>fedora-view</code>
 *        namespaces are inferred from values expressed elsewhere in the
 *        digital object, and we do not want duplication.
 * </li>
 * </ul>
 *
 * @author Sandy Payette
 * @author Eddie Shin
 * @author Chris Wilper
 * @author Stephen Bayliss
 */
public class RelsValidator
        extends DefaultHandler
        implements Constants {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(RelsValidator.class.getName());

    // state variables
    private String m_doURI;

    private String m_dsId;

    private boolean m_rootRDFFound;

    // total number of description elements encountered
    private int m_descriptionCount;

    // if currently within a description element
    private boolean m_withinDescription;

    private int m_depth;

    private String m_literalType;

    private StringBuffer m_literalValue;

    // SAX parser
    private final SAXParser m_parser;

    private static final String RELS_EXT = "RELS-EXT";

    private static final String RELS_INT = "RELS-INT";

    public RelsValidator() {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            m_parser = spf.newSAXParser();
        } catch (Exception wontHappen) {
            throw new FaultException(wontHappen);
        }
    }

    public void validate(PID pid, String dsId, InputStream content)
            throws ValidationException {
        try {
            m_rootRDFFound = false;
            m_descriptionCount = 0;
            m_withinDescription = false;
            m_depth = 0;
            m_doURI = pid.toURI();
            if (!dsId.equals(RELS_EXT) && !dsId.equals(RELS_INT)) {
                throw new ValidationException("Relationships datastream ID must be RELS-EXT or RELS-INT ("
                        + dsId + ")");
            }
            m_dsId = dsId;
            m_parser.parse(content, this);
        } catch (Exception e) {
            throw new ValidationException(dsId + " validation failed: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public void startElement(String nsURI,
                             String localName,
                             String qName,
                             Attributes a) throws SAXException {

        if (nsURI.equals(RDF.uri) && localName.equalsIgnoreCase("RDF")) {
            m_rootRDFFound = true;
        } else if (m_rootRDFFound) {
            if (nsURI.equals(RDF.uri)
                    && localName.equalsIgnoreCase("Description")) {
                // are we not already within a Description element?
                if (!m_withinDescription) {
                    m_withinDescription = true;
                    m_descriptionCount++;
                    m_depth++;
                    if ((m_descriptionCount > 1) && m_dsId.equals(RELS_EXT)) {
                        throw new SAXException("RelsExtValidator:"
                                + " Only ONE RDF <Description> element is allowed"
                                + " in the RELS-EXT datastream.");
                    }
                    checkDepth(m_depth, qName);
                    checkAboutURI(grab(a, RDF.uri, "about"));

                } else {
                    throw new SAXException("RelsExtValidator:"
                            + " RDF <Description> elements may not be nested"
                            + " in the " + m_dsId + " datastream.");
                }
            } else if (m_withinDescription) {
                m_depth++;
                checkDepth(m_depth, qName);
                checkBadAssertion(nsURI, localName, qName);
                String resourceURI = grab(a, RDF.uri, "resource");
                if (resourceURI.length() > 0) {
                    checkResourceURI(resourceURI, qName);
                    m_literalType = null;
                    m_literalValue = null;
                } else {
                    if (nsURI.equals(MODEL.uri)) {
                        // if it's not a resource, the predicate cannot
                        // be fedora-model:hasService, hasModel,
                        // isContractor, or is DeploymentOf
                        if (localName.equals(MODEL.HAS_SERVICE.localName)
                                || localName.equals(MODEL.HAS_MODEL.localName)
                                || localName
                                        .equals(MODEL.IS_CONTRACTOR_OF.localName)
                                || localName
                                        .equals(MODEL.IS_DEPLOYMENT_OF.localName)) {
                            throw new SAXException("RelsExtValidator: "
                                    + "Target of " + qName + " statement "
                                    + "MUST be an rdf:resource");
                        }
                    }
                    String datatypeURI = grab(a, RDF.uri, "datatype");
                    if (datatypeURI.length() == 0) {
                        m_literalType = null;
                    } else {
                        m_literalType = datatypeURI;
                    }
                    m_literalValue = new StringBuffer();
                }
            } else {
                throw new SAXException("RelsExtValidator:"
                        + " Invalid element " + localName
                        + " found in the RELS-EXT datastream.\n"
                        + " Relationship assertions must be built"
                        + " upon an RDF <Description> element.");
            }
        } else {
            throw new SAXException("RelsExtValidator:"

            + " The 'RDF' root element was not found "
                    + " in the RELS-EXT datastream.\n"
                    + " Relationship metadata must be encoded using RDF/XML.");
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (m_literalValue != null) {
            m_literalValue.append(ch, start, length);
        }
    }

    @Override
    public void endElement(String nsURI, String localName, String qName)
            throws SAXException {
        if (m_rootRDFFound) {
            m_depth--;
        }
        if (nsURI.equals(RDF.uri) && localName.equalsIgnoreCase("Description")) {
            m_withinDescription = false;
        }
        if (m_literalType != null && m_literalValue != null) {
            checkTypedValue(m_literalType, m_literalValue.toString(), qName);
        }
        m_literalType = null;
        m_literalValue = null;
    }

    private static String grab(Attributes a,
                               String namespace,
                               String elementName) {
        String ret = a.getValue(namespace, elementName);
        if (ret == null) {
            ret = a.getValue(elementName);
        }
        // set null attribute value to empty string since it's
        // generally helpful in the code to avoid null pointer exception
        // when operations are performed on attributes values.
        if (ret == null) {
            ret = "";
        }
        return ret;
    }

    /**
     * checkDepth: checks that there is NO nesting of relationship assertions.
     * In terms of XML depth, the RDF root element is considered depth of 0.
     * Then, the RDF <Description> must be at depth of 1, and the relationship
     * properties must exist at depth of 2. That's it.
     *
     * @param depth
     *        the depth of the XML element being evaluated
     * @param qName
     *        the name of the relationship property being evaluated
     * @throws SAXException
     */
    private void checkDepth(int depth, String qName) throws SAXException {

        if (depth > 2) {
            throw new SAXException("RelsExtValidator:"
                    + " The RELS-EXT datastream has improper"
                    + " nesting in its relationship assertions.\n"
                    + " (The XML depth is " + depth
                    + " which must not exceed a depth of 2.\n"
                    + " The root <RDF> element should be level 0,"
                    + " the <Description> element should be level 1,"
                    + " and relationship elements should be level 2.)");
        }
    }

    /**
     * checkBadAssertion: checks that the DC and fedora-view namespace are not
     * being used in RELS-EXT, and that if fedora-model is used, the localName
     * is hasService, hasModel, isDeploymentOf, or isContractorOf. Also ensures
     * that fedora-model:hasContentModel is only used once.
     *
     * @param nsURI
     *        the namespace URI of the predicate being evaluated
     * @param localName
     *        the local name of the predicate being evaluated
     * @param qName
     *        the qualified name of the predicate being evaluated
     */
    private void checkBadAssertion(String nsURI, String localName, String qName)
            throws SAXException {

        if (m_dsId.equals(RELS_EXT)
                && (nsURI.equals(DC.uri) || nsURI.equals(OAI_DC.uri))) {
            throw new SAXException("RelsExtValidator:"
                    + " The RELS-EXT datastream has improper"
                    + " relationship assertion: " + qName + ".\n"
                    + " No Dublin Core assertions allowed"
                    + " in Fedora relationship metadata.");
        } else if (nsURI.equals(MODEL.uri)) {
            if ((m_dsId.equals(RELS_INT)
                    && !localName.equals(MODEL.DOWNLOAD_FILENAME.localName)                    )
                    ||
                    (m_dsId.equals(RELS_EXT)
                    && !localName.equals(MODEL.HAS_SERVICE.localName)
                    && !localName.equals(MODEL.IS_CONTRACTOR_OF.localName)
                    && !localName.equals(MODEL.HAS_MODEL.localName)
                    && !localName.equals(MODEL.IS_DEPLOYMENT_OF.localName)
                    )) {
                throw new SAXException("RelsExtValidator:"
                        + " Disallowed predicate in " + m_dsId + ": "
                        + qName
                        + "\n"
                        + " The only predicates from the fedora-model namespace"
                        + " allowed in RELS-EXT are "
                        + MODEL.HAS_SERVICE.localName + ", "
                        + MODEL.IS_CONTRACTOR_OF.localName + ", "
                        + MODEL.HAS_MODEL.localName + ", "
                        + MODEL.IS_DEPLOYMENT_OF.localName + ".  The only predicate allowed "
                        + "in RELS-INT is "
                        + MODEL.DOWNLOAD_FILENAME.localName +"."
                        );
            }
        } else if (nsURI.equals(VIEW.uri)) {
            throw new SAXException("RelsExtValidator:"
                    + " Disallowed predicate in RELS-EXT: " + qName + "\n"
                    + " The fedora-view namespace is reserved by Fedora.");
        }
    }

    /**
     * checkAboutURI: ensure that the RDF <Description> is about the digital
     * object that contains the RELS-EXT datastream, since the REL-EXT
     * datastream is only supposed to capture relationships about "this" digital
     * object.
     *
     * @param aboutURI
     *        the URI value of the RDF 'about' attribute
     * @throws SAXException
     */
    private void checkAboutURI(String aboutURI) throws SAXException {
        if (m_dsId.equals(RELS_EXT)) {
            if (!m_doURI.equals(aboutURI)) {
                throw new SAXException("RelsExtValidator:"
                        + " The RELS-EXT datastream refers to"
                        + " an improper URI in the 'about' attribute of the"
                        + " RDF <Description> element.\n"
                        + " The URI must be that of the digital object"
                        + " in which the RELS-EXT datastream resides" + " ("
                        + m_doURI + ").");
            }
        } else if (m_dsId.equals(RELS_INT)) {
            if (!aboutURI.startsWith(m_doURI + "/")) {
                throw new SAXException("RelsExtValidator:"
                        + " The RELS-INT datastream refers to"
                        + " an improper URI in the 'about' attribute of the"
                        + " RDF <Description> element.\n"
                        + " The URI must be a datastream in the digital object"
                        + " in which the RELS-INT datastream resides" + " ("
                        + m_doURI + ", " + aboutURI + ").");

            }
            String dsId = aboutURI.replace(m_doURI + "/", "");
            // datastream ID must be an XML NCName, implemented using axis NCName class
            if (dsId.length() > ValidationConstants.DATASTREAM_ID_MAXLEN
                    || dsId.length() < 1 || !NCName.isValid(dsId)) {
                throw new SAXException("RelsExtValidator:"
                        + " The RELS-INT datastream refers to"
                        + " an improper URI in the 'about' attribute of the"
                        + " RDF <Description> element.\n" + " " + dsId
                        + " is not a valid datastream ID");

            }

        }
    }

    /**
     * checkResourceURI: ensure that the target resource is a proper URI.
     *
     * @param resourceURI
     *        the URI value of the RDF 'resource' attribute
     * @param relName
     *        the name of the relationship property being evaluated
     * @throws SAXException
     */
    private void checkResourceURI(String resourceURI, String relName)
            throws SAXException {

        URI uri;
        try {
            uri = new URI(resourceURI);
        } catch (Exception e) {
            throw new SAXException("RelsExtValidator:"
                    + "Error in relationship '" + relName + "'."
                    + " The RDF 'resource' is not a valid URI.");
        }

        if (!uri.isAbsolute()) {
            throw new SAXException("RelsValidator:" + "Error in relationship '"
                    + relName + "'."
                    + " The specified RDF 'resource' is not an absolute URI.");
        }
    }

    /**
     * checkTypedValue: ensure that the datatype of a literal is one of the
     * supported types and that it's a valid value for that type.
     *
     * @param datatypeURI
     *        the URI value of the RDF 'datatype' attribute
     * @param value
     *        the value
     * @param relName
     *        the name of the property being evaluated
     * @throws SAXException
     */
    private void checkTypedValue(String datatypeURI,
                                 String value,
                                 String relName) throws SAXException {
        if (datatypeURI.equals(RDF_XSD.INT.uri)) {
            try {
                Integer.parseInt(value);
            } catch (Exception e) {
                throw new SAXException("RelsExtValidator:"
                        + " The value specified for " + relName
                        + " is not a valid 'int' value");
            }
        } else if (datatypeURI.equals(RDF_XSD.LONG.uri)) {
            try {
                Long.parseLong(value);
            } catch (Exception e) {
                throw new SAXException("RelsExtValidator:"
                        + " The value specified for " + relName
                        + " is not a valid 'long' value");
            }
        } else if (datatypeURI.equals(RDF_XSD.FLOAT.uri)) {
            try {
                Float.parseFloat(value);
            } catch (Exception e) {
                throw new SAXException("RelsExtValidator:"
                        + " The value specified for " + relName
                        + " is not a valid 'float' value");
            }
        } else if (datatypeURI.equals(RDF_XSD.DOUBLE.uri)) {
            try {
                Double.parseDouble(value);
            } catch (Exception e) {
                throw new SAXException("RelsExtValidator:"
                        + " The value specified for " + relName
                        + " is not a valid 'double' value");
            }
        } else if (datatypeURI.equals(RDF_XSD.DATE_TIME.uri)) {
            if (!isValidDateTime(value)) {
                throw new SAXException("RelsExtValidator:"
                        + " The value specified for " + relName
                        + " is not a valid 'dateTime' value.\n"
                        + "The following dateTime formats are allowed:\n"
                        + "  yyyy-MM-ddTHH:mm:ss\n"
                        + "  yyyy-MM-ddTHH:mm:ss.S\n"
                        + "  yyyy-MM-ddTHH:mm:ss.SS\n"
                        + "  yyyy-MM-ddTHH:mm:ss.SSS\n"
                        + "  yyyy-MM-ddTHH:mm:ss.SSSZ");
            }
        } else {
            throw new SAXException("RelsExtValidator:"
                    + " Error in relationship '"
                    + relName
                    + "'.\n"
                    + " The RELS-EXT datastream does not support the specified"
                    + " datatype.\n"
                    + "If specified, the RDF 'datatype' must be the URI of one of\n"
                    + "the following W3C XML Schema data types: int, long, float,\n"
                    + "double, or dateTime");
        }
    }

    /**
     * Tells whether the given string is a valid lexical representation of a
     * dateTime value. Passing this test will ensure successful indexing later.
     */
    private static boolean isValidDateTime(String lex) {
        SimpleDateFormat format = new SimpleDateFormat();

        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        int length = lex.length();
        if (lex.startsWith("-")) {
            length--;
        }
        if (lex.endsWith("Z")) {
            if (length == 20) {
                format.applyPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            } else if (length == 22) {
                format.applyPattern("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
            } else if (length == 23) {
                format.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SS'Z'");
            } else if (length == 24) {
                format.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            } else {
                LOG.warn("Not a valid dateTime: " + lex);
                return false;
            }
        } else {
            if (length == 19) {
                format.applyPattern("yyyy-MM-dd'T'HH:mm:ss");
            } else if (length == 21) {
                format.applyPattern("yyyy-MM-dd'T'HH:mm:ss.S");
            } else if (length == 22) {
                format.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SS");
            } else if (length == 23) {
                format.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
            } else {
                LOG.warn("Not a valid dateTime: " + lex);
                return false;
            }
        }
        try {
            format.parse(lex);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Validated dateTime: " + lex);
            }
            return true;
        } catch (ParseException e) {
            LOG.warn("Not a valid dateTime: " + lex);
            return false;
        }
    }
}
