/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The SOAP XML Namespace.
 * 
 * <pre>
 * Namespace URI    : http://schemas.xmlsoap.org/wsdl/soap
 * Preferred Prefix : soap
 * </pre>
 * 
 * @author Chris Wilper
 */
public class SOAPNamespace
        extends XMLNamespace {

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final SOAPNamespace ONLY_INSTANCE = new SOAPNamespace();

    /**
     * Constructs the instance.
     */
    protected SOAPNamespace() {
        super("http://schemas.xmlsoap.org/wsdl/soap", "soap");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static SOAPNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
