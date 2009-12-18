/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.service;

/**
 * A data structure for holding WSDL HTTP binding information for an operation.
 * 
 * @author Sandy Payette
 */
public class HTTPOperation
        extends AbstractOperation {

    /**
     * operationLocation: a relative URI for the operation. The URI is
     * ultimately combined with the URI in the http:address element to (see Port
     * object) form the full URI for the HTTP request.
     */
    public String operationLocation;

    /**
     * inputBinding:
     */
    public HTTPOperationInOut inputBinding;

    /**
     * outputBinding:
     */
    public HTTPOperationInOut outputBinding;
}
