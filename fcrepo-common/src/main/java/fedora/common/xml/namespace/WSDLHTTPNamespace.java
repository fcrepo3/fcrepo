/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The WSDL HTTP XML Namespace.
 * 
 * <pre>
 * Namespace URI    : http://schemas.xmlsoap.org/wsdl/http/
 * Preferred Prefix : http
 * </pre>
 * 
 * @author Chris Wilper
 */
public class WSDLHTTPNamespace
        extends XMLNamespace {

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final WSDLHTTPNamespace ONLY_INSTANCE =
            new WSDLHTTPNamespace();

    /**
     * Constructs the instance.
     */
    protected WSDLHTTPNamespace() {
        super("http://schemas.xmlsoap.org/wsdl/http/", "http");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static WSDLHTTPNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
