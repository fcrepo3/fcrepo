/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The OAI Identifier XML namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.openarchives.org/OAI/2.0/oai-identifier
 * Preferred Prefix : oai-identifier
 * </pre>
 * 
 * @author Chris Wilper
 */
public class OAIIdentifierNamespace
        extends XMLNamespace {

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final OAIIdentifierNamespace ONLY_INSTANCE =
            new OAIIdentifierNamespace();

    /**
     * Constructs the instance.
     */
    private OAIIdentifierNamespace() {
        super("http://www.openarchives.org/OAI/2.0/oai-identifier",
              "oai-identifier");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static OAIIdentifierNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
