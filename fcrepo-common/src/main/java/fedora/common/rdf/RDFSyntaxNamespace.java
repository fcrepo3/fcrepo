/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.rdf;

/**
 * The RDF Syntax RDF namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.w3.org/1999/02/22-rdf-syntax-ns#
 * Preferred Prefix : rdf
 * </pre>
 * 
 * @author Chris Wilper
 */
public class RDFSyntaxNamespace
        extends RDFNamespace {

    private static final long serialVersionUID = 1L;

    public final RDFName TYPE;

    public final String prefix;

    public RDFSyntaxNamespace() {

        uri = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        prefix = "rdf";

        TYPE = new RDFName(this, "type");
    }

}
