/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The WSDL MIME XML Namespace.
 * 
 * <pre>
 * Namespace URI    : http://schemas.xmlsoap.org/wsdl/mime/
 * Preferred Prefix : mime
 * </pre>
 * 
 * @author Chris Wilper
 */
public class WSDLMIMENamespace
        extends XMLNamespace {

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final WSDLMIMENamespace ONLY_INSTANCE =
            new WSDLMIMENamespace();

    /**
     * Constructs the instance.
     */
    protected WSDLMIMENamespace() {
        super("http://schemas.xmlsoap.org/wsdl/mime/", "mime");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static WSDLMIMENamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
