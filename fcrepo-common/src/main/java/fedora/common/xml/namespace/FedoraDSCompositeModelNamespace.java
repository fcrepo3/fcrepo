/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The Fedora DS Composite Model XML namespace.
 * 
 * <pre>
 * Namespace URI    : info:fedora/fedora-system:def/dsCompositeModel#
 * Preferred Prefix : dscm
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraDSCompositeModelNamespace
        extends XMLNamespace {

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final FedoraDSCompositeModelNamespace ONLY_INSTANCE =
            new FedoraDSCompositeModelNamespace();

    /**
     * Constructs the instance.
     */
    private FedoraDSCompositeModelNamespace() {
        super("info:fedora/fedora-system:def/dsCompositeModel#", "dscm");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraDSCompositeModelNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
