/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The XMLNS XML namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.w3.org/2000/xmlns/ 
 * Preferred Prefix : xmlns
 * </pre>
 * 
 * @author Chris Wilper
 */
public class XMLNSNamespace
        extends XMLNamespace {

    //---
    // Attributes
    //---

    /** The <code>xmlns</code> attribute. */
    public final QName XMLNS;

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final XMLNSNamespace ONLY_INSTANCE = new XMLNSNamespace();

    /**
     * Constructs the instance.
     */
    private XMLNSNamespace() {
        super("http://www.w3.org/2000/xmlns/", "xmlns");

        // attributes
        XMLNS = new QName(this, "xmlns");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static XMLNSNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
