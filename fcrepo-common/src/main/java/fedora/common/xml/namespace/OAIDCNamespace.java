/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The OAI DC XML namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.openarchives.org/OAI/2.0/oai_dc/
 * Preferred Prefix : oai_dc
 * </pre>
 * 
 * @author Chris Wilper
 */
public class OAIDCNamespace
        extends XMLNamespace {

    //---
    // Elements
    //---

    /** The <code>dc</code> element. */
    public final QName DC;

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final OAIDCNamespace ONLY_INSTANCE = new OAIDCNamespace();

    /**
     * Constructs the instance.
     */
    private OAIDCNamespace() {
        super("http://www.openarchives.org/OAI/2.0/oai_dc/", "oai_dc");

        // elements
        DC = new QName(this, "dc");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static OAIDCNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
