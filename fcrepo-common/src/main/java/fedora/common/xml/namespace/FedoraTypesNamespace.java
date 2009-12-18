/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The Fedora Types XML namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.fedora.info/definitions/1/0/types/
 * Preferred Prefix : types
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraTypesNamespace
        extends XMLNamespace {

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final FedoraTypesNamespace ONLY_INSTANCE =
            new FedoraTypesNamespace();

    /**
     * Constructs the instance.
     */
    private FedoraTypesNamespace() {
        super("http://www.fedora.info/definitions/1/0/types/", "types");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraTypesNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
