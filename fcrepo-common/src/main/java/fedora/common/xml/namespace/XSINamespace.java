/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The XML Schema Instance XML namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.w3.org/2001/XMLSchema-instance
 * Preferred Prefix : xsi
 * </pre>
 * 
 * @author Chris Wilper
 */
public class XSINamespace
        extends XMLNamespace {

    //---
    // Attributes
    //---

    /** The <code>schemaLocation</code> attribute. */
    public final QName SCHEMA_LOCATION;

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final XSINamespace ONLY_INSTANCE = new XSINamespace();

    /**
     * Constructs the instance.
     */
    private XSINamespace() {
        super("http://www.w3.org/2001/XMLSchema-instance", "xsi");

        // attributes
        SCHEMA_LOCATION = new QName(this, "schemaLocation");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static XSINamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
