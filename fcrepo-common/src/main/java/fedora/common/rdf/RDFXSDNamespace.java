/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.rdf;

/**
 * The XSD RDF namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.w3.org/2001/XMLSchema#
 * Preferred Prefix : xsd
 * </pre>
 * 
 * <p>
 * <em><b>NOTE:</b> This is subtly different from the XML XSD namespace, in
 * that its URI ends with a <code>#</code>.</em>
 * See <a href="http://www.w3.org/2001/tag/group/track/issues/6">
 * http://www.w3.org/2001/tag/group/track/issues/6</a> for more information on
 * why this is necessary.
 * </p>
 * 
 * @author Chris Wilper
 */
public class RDFXSDNamespace
        extends RDFNamespace {

    private static final long serialVersionUID = 1L;

    public final RDFName DATE_TIME;

    public final RDFName INT;

    public final RDFName LONG;

    public final RDFName FLOAT;

    public final RDFName DOUBLE;

    public final RDFName STRING;

    public RDFXSDNamespace() {

        uri = "http://www.w3.org/2001/XMLSchema#";
        prefix = "xsd";

        DATE_TIME = new RDFName(this, "dateTime");
        INT = new RDFName(this, "int");
        LONG = new RDFName(this, "long");
        FLOAT = new RDFName(this, "float");
        DOUBLE = new RDFName(this, "double");
        STRING = new RDFName(this, "string");
    }

}
