/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The XML Schema Instance XML namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.w3.org/2005/Atom
 * Preferred Prefix : atom
 * </pre>
 * 
 * @author Edwin Shin
 * @since 3.0
 * @version $Id$
 */
public class AtomNamespace
        extends XMLNamespace {

    //---
    // Attributes
    //---


    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final AtomNamespace ONLY_INSTANCE = new AtomNamespace();

    /**
     * Constructs the instance.
     */
    private AtomNamespace() {
        super("http://www.w3.org/2005/Atom", "atom");

        // attributes

    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static AtomNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
