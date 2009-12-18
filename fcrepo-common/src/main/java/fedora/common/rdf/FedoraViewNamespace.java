/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.rdf;

/**
 * The Fedora View RDF namespace.
 * 
 * <pre>
 * Namespace URI    : info:fedora/fedora-system:def/view#
 * Preferred Prefix : fedora-view
 * </pre>
 * 
 * @author Chris Wilper
 * @version $Id$
 */
public class FedoraViewNamespace
        extends RDFNamespace {

    private static final long serialVersionUID = 2L;

    // Properties

    /**
     * Deprecated as of Fedora 3.0. No replacement.
     */
    @Deprecated
    public final RDFName HAS_DATASTREAM;

    public final RDFName DISSEMINATES;

    public final RDFName DISSEMINATION_TYPE;

    public final RDFName IS_VOLATILE;

    public final RDFName LAST_MODIFIED_DATE;

    public final RDFName MIME_TYPE;
    
    public final RDFName VERSION;

    public FedoraViewNamespace() {

        uri = "info:fedora/fedora-system:def/view#";
        prefix = "fedora-view";

        // Properties
        HAS_DATASTREAM = new RDFName(this, "hasDatastream");
        DISSEMINATES = new RDFName(this, "disseminates");
        DISSEMINATION_TYPE = new RDFName(this, "disseminationType");
        IS_VOLATILE = new RDFName(this, "isVolatile");
        LAST_MODIFIED_DATE = new RDFName(this, "lastModifiedDate");
        MIME_TYPE = new RDFName(this, "mimeType");
        VERSION = new RDFName(this, "version");
    }

}
