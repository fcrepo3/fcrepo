/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.rdf;

import org.mulgara.query.rdf.Mulgara;

/**
 * The Mulgara RDF namespace.
 * 
 * <pre>
 * Namespace URI    : http://mulgara.org/mulgara#
 * Preferred Prefix : mulgara
 * </pre>
 * 
 * @see org.mulgara.query.rdf.Mulgara
 * @see org.mulgara.query.SpecialPredicates
 * @see org.mulgara.resolver.xsd.XSDResolverFactory
 * @author Edwin Shin
 */
public class MulgaraNamespace
        extends RDFNamespace {

    private static final long serialVersionUID = 1L;

    // Properties
    public final RDFName AFTER;

    public final RDFName BEFORE;

    public final RDFName GT;

    public final RDFName LT;

    public final RDFName IS;

    public final RDFName NOT_OCCURS;

    public final RDFName OCCURS;

    public final RDFName OCCURS_LESS_THAN;

    public final RDFName OCCURS_MORE_THAN;

    public MulgaraNamespace() {

        uri = Mulgara.NAMESPACE; // http://mulgara.org/mulgara#
        prefix = "mulgara";

        // Properties
        AFTER = new RDFName(this, "after");
        BEFORE = new RDFName(this, "before");
        GT = new RDFName(this, "gt");
        LT = new RDFName(this, "lt");

        IS = new RDFName(this, "is");
        NOT_OCCURS = new RDFName(this, "notOccurs");
        OCCURS = new RDFName(this, "occurs");
        OCCURS_LESS_THAN = new RDFName(this, "occursLessThan");
        OCCURS_MORE_THAN = new RDFName(this, "occursMoreThan");
    }

}
