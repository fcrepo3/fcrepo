/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.OAIFriendsNamespace;

/**
 * The OAI Friends 2.0 XML format.
 * 
 * <pre>
 * Format URI        : http://www.openarchives.org/OAI/2.0/friends/
 * Primary Namespace : http://www.openarchives.org/OAI/2.0/friends/
 * XSD Schema URL    : http://www.openarchives.org/OAI/2.0/friends.xsd
 * </pre>
 * 
 * @author Chris Wilper
 */
public class OAIFriends2_0Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final OAIFriends2_0Format ONLY_INSTANCE =
            new OAIFriends2_0Format();

    /**
     * Constructs the instance.
     */
    private OAIFriends2_0Format() {
        super("http://www.openarchives.org/OAI/2.0/friends/",
              OAIFriendsNamespace.getInstance(),
              "http://www.openarchives.org/OAI/2.0/friends.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static OAIFriends2_0Format getInstance() {
        return ONLY_INSTANCE;
    }

}
