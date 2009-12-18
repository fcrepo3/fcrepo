/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The XLink XML namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.w3.org/1999/xlink
 * Preferred Prefix : xlink
 * </pre>
 * 
 * @author Chris Wilper
 */
public class XLinkNamespace
        extends XMLNamespace {

    //---
    // Attributes
    //---

    /** The <code>title</code> attribute. */
    public final QName TITLE;

    /** The <code>href</code> attribute. */
    public final QName HREF;

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final XLinkNamespace ONLY_INSTANCE = new XLinkNamespace();

    /**
     * Constructs the instance.
     */
    protected XLinkNamespace() {
        super("http://www.w3.org/1999/xlink", "xlink");

        // attributes
        TITLE = new QName(this, "title");
        HREF = new QName(this, "href");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static XLinkNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
