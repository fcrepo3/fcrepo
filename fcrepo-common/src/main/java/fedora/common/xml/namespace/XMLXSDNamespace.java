/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The XSD XML namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.w3.org/2001/XMLSchema
 * Preferred Prefix : xsd
 * </pre>
 * 
 * <p>
 * <em><b>NOTE:</b> This is subtly different from the RDF XSD namespace, in
 * that its URI does not end with a <code>#</code>.</em>
 * See <a href="http://www.w3.org/2001/tag/group/track/issues/6">
 * http://www.w3.org/2001/tag/group/track/issues/6</a> for more information on
 * why this is necessary.
 * </p>
 * 
 * @author Chris Wilper
 */
public class XMLXSDNamespace
        extends XMLNamespace {

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final XMLXSDNamespace ONLY_INSTANCE = new XMLXSDNamespace();

    /**
     * Constructs the instance.
     */
    private XMLXSDNamespace() {
        super("http://www.w3.org/2001/XMLSchema", "xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static XMLXSDNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
