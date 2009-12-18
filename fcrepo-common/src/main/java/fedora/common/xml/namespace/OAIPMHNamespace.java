/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The OAI-PMH XML namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.openarchives.org/OAI/2.0/
 * Preferred Prefix : pmh
 * </pre>
 * 
 * @author Chris Wilper
 */
public class OAIPMHNamespace
        extends XMLNamespace {

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final OAIPMHNamespace ONLY_INSTANCE = new OAIPMHNamespace();

    /**
     * Constructs the instance.
     */
    private OAIPMHNamespace() {
        super("http://www.openarchives.org/OAI/2.0/", "pmh");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static OAIPMHNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
