/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The OAI Provenance XML namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.openarchives.org/OAI/2.0/provenance
 * Preferred Prefix : provenance
 * </pre>
 * 
 * @author Chris Wilper
 */
public class OAIProvenanceNamespace
        extends XMLNamespace {

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final OAIProvenanceNamespace ONLY_INSTANCE =
            new OAIProvenanceNamespace();

    /**
     * Constructs the instance.
     */
    private OAIProvenanceNamespace() {
        super("http://www.openarchives.org/OAI/2.0/provenance", "provenance");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static OAIProvenanceNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
