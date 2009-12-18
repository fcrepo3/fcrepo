/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The Fedora Configuration XML namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.fedora.info/definitions/1/0/config/
 * Preferred Prefix : fcfg
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraFCFGNamespace
        extends XMLNamespace {

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final FedoraFCFGNamespace ONLY_INSTANCE =
            new FedoraFCFGNamespace();

    /**
     * Constructs the instance.
     */
    private FedoraFCFGNamespace() {
        super("http://www.fedora.info/definitions/1/0/config/", "fcfg");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraFCFGNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
