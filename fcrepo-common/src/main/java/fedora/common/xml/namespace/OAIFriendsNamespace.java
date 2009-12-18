/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The OAI Friends XML namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.openarchives.org/OAI/2.0/friends/
 * Preferred Prefix : friends
 * </pre>
 * 
 * @author Chris Wilper
 */
public class OAIFriendsNamespace
        extends XMLNamespace {

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final OAIFriendsNamespace ONLY_INSTANCE =
            new OAIFriendsNamespace();

    /**
     * Constructs the instance.
     */
    private OAIFriendsNamespace() {
        super("http://www.openarchives.org/OAI/2.0/friends/", "friends");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static OAIFriendsNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
