/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The Fedora Access XML namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.fedora.info/definitions/1/0/access/
 * Preferred Prefix : access
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraAccessNamespace
        extends XMLNamespace {

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final FedoraAccessNamespace ONLY_INSTANCE =
            new FedoraAccessNamespace();

    /**
     * Constructs the instance.
     */
    private FedoraAccessNamespace() {
        super("http://www.fedora.info/definitions/1/0/access/", "access");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraAccessNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
