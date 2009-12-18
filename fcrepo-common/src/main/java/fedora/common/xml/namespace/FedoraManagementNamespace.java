/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The Fedora Management XML namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.fedora.info/definitions/1/0/management/
 * Preferred Prefix : management
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraManagementNamespace
        extends XMLNamespace {

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final FedoraManagementNamespace ONLY_INSTANCE =
            new FedoraManagementNamespace();

    /**
     * Constructs the instance.
     */
    private FedoraManagementNamespace() {
        super("http://www.fedora.info/definitions/1/0/management/",
              "management");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraManagementNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
