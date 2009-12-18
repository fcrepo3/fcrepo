/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The XACML Policy Namespace
 * 
 * <pre>
 * Namespace URI    : urn:oasis:names:tc:xacml:1.0:policy
 * Preferred Prefix : xacml
 * </pre>
 * 
 * @author Chris Wilper
 */
public class XACMLPolicyNamespace
        extends XMLNamespace {

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final XACMLPolicyNamespace ONLY_INSTANCE =
            new XACMLPolicyNamespace();

    /**
     * Constructs the instance.
     */
    protected XACMLPolicyNamespace() {
        super("urn:oasis:names:tc:xacml:1.0:policy", "xacml");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static XACMLPolicyNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
