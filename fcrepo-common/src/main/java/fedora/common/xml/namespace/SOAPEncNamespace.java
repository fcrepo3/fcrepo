/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The SOAP Encoding XML Namespace.
 * 
 * <pre>
 * Namespace URI    : http://schemas.xmlsoap.org/wsdl/soap/encoding
 * Preferred Prefix : soapenc
 * </pre>
 * 
 * @author Chris Wilper
 */
public class SOAPEncNamespace
        extends XMLNamespace {

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final SOAPEncNamespace ONLY_INSTANCE =
            new SOAPEncNamespace();

    /**
     * Constructs the instance.
     */
    protected SOAPEncNamespace() {
        super("http://schemas.xmlsoap.org/wsdl/soap/encoding", "soapenc");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static SOAPEncNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
