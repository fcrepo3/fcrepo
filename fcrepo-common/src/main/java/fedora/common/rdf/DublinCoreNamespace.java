/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.rdf;

/**
 * The Dublin Core RDF namespace.
 * 
 * <pre>
 * Namespace URI    : http://purl.org/dc/elements/1.1/
 * Preferred Prefix : dc
 * </pre>
 * 
 * @author Chris Wilper
 */
public class DublinCoreNamespace
        extends RDFNamespace {

    private static final long serialVersionUID = 1L;

    public final RDFName TITLE;

    public final RDFName CREATOR;

    public final RDFName SUBJECT;

    public final RDFName DESCRIPTION;

    public final RDFName PUBLISHER;

    public final RDFName CONTRIBUTOR;

    public final RDFName DATE;

    public final RDFName TYPE;

    public final RDFName FORMAT;

    public final RDFName IDENTIFIER;

    public final RDFName SOURCE;

    public final RDFName LANGUAGE;

    public final RDFName RELATION;

    public final RDFName COVERAGE;

    public final RDFName RIGHTS;

    public DublinCoreNamespace() {

        uri = "http://purl.org/dc/elements/1.1/";
        prefix = "dc";

        TITLE = new RDFName(this, "title");
        CREATOR = new RDFName(this, "creator");
        SUBJECT = new RDFName(this, "subject");
        DESCRIPTION = new RDFName(this, "description");
        PUBLISHER = new RDFName(this, "publisher");
        CONTRIBUTOR = new RDFName(this, "contributor");
        DATE = new RDFName(this, "date");
        TYPE = new RDFName(this, "type");
        FORMAT = new RDFName(this, "format");
        IDENTIFIER = new RDFName(this, "identifier");
        SOURCE = new RDFName(this, "source");
        LANGUAGE = new RDFName(this, "language");
        RELATION = new RDFName(this, "relation");
        COVERAGE = new RDFName(this, "coverage");
        RIGHTS = new RDFName(this, "rights");

    }

}
