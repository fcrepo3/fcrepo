/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The WSDL XML Namespace.
 * 
 * <pre>
 * Namespace URI    : http://schemas.xmlsoap.org/wsdl/
 * Preferred Prefix : wsdl
 * </pre>
 * 
 * @author Chris Wilper
 */
public class WSDLNamespace
        extends XMLNamespace {

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final WSDLNamespace ONLY_INSTANCE = new WSDLNamespace();

    /**
     * Constructs the instance.
     */
    protected WSDLNamespace() {
        super("http://schemas.xmlsoap.org/wsdl/", "wsdl");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static WSDLNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
