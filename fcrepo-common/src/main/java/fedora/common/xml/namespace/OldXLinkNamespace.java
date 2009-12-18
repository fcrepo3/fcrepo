/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The XLink XML namespace used in old versions of the METS format.
 * <p>
 * <em>NOTE: This is not the published xlink namespace, and is provided
 * here only for use with the "METS Fedora Extension 1.0" format, which
 * was originally derived from from METS 1.3 and was used in Fedora releases
 * prior to 3.0.</em>
 * </p>
 * 
 * <pre>
 * Namespace URI    : http://www.w3.org/TR/xlink
 * Preferred Prefix : xlink
 * </pre>
 * 
 * @author Chris Wilper
 */
public class OldXLinkNamespace
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
    private static final OldXLinkNamespace ONLY_INSTANCE =
            new OldXLinkNamespace();

    /**
     * Constructs the instance.
     */
    protected OldXLinkNamespace() {
        super("http://www.w3.org/TR/xlink", "xlink");

        // attributes
        TITLE = new QName(this, "title");
        HREF = new QName(this, "href");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static OldXLinkNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
