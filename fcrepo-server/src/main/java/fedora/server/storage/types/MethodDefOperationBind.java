/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.types;

/**
 * @author Sandy Payette
 */
public class MethodDefOperationBind
        extends MethodDef {

    public static final String HTTP_MESSAGE_PROTOCOL = "HTTP";

    public static final String SOAP_MESSAGE_PROTOCOL = "SOAP";

    public String protocolType = null;

    public String serviceBindingAddress = null;

    public String operationLocation = null;

    public String operationURL = null;

    public String[] dsBindingKeys = new String[0];

    /**
     * Possible response MIME types.
     */
    public String[] outputMIMETypes = new String[0];

    public MethodDefOperationBind() {
    }

}
