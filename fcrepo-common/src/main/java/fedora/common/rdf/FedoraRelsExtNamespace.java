/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.rdf;

/**
 * The Fedora RELS-EXT RDF namespace.
 * 
 * <pre>
 * Namespace URI    : info:fedora/fedora-system:def/relations-external#
 * Preferred Prefix : rel
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraRelsExtNamespace
        extends RDFNamespace {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    // Properties
    public final RDFName IS_MEMBER_OF;

    // Values

    // Types

    public FedoraRelsExtNamespace() {

        uri = "info:fedora/fedora-system:def/relations-external#";
        prefix = "rel";

        // Properties
        IS_MEMBER_OF = new RDFName(this, "isMemberOf");

        // Values

        // Types

    }

}
