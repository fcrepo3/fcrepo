/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.policy;

import com.sun.xacml.attr.DateTimeAttribute;

/**
 * The Fedora Resource XACML namespace.
 * 
 * <pre>
 * Namespace URI    : urn:fedora:names:fedora:2.1:resource
 * </pre>
 */
public class ResourceNamespace
        extends XacmlNamespace {

    // Properties
    public final XacmlName AS_OF_DATETIME;

    public final XacmlName TICKET_ISSUED_DATETIME;

    private ResourceNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        AS_OF_DATETIME =
                new XacmlName(this,
                              "asOfDateTime",
                              DateTimeAttribute.identifier);
        TICKET_ISSUED_DATETIME =
                addName(new XacmlName(this,
                                      "ticketIssuedDateTime",
                                      DateTimeAttribute.identifier));

    }

    public static ResourceNamespace onlyInstance =
            new ResourceNamespace(Release2_1Namespace.getInstance(), "resource");

    static {
        init();
    }

    @SuppressWarnings("deprecation")
    private static void init() {
        onlyInstance.addNamespace(ObjectNamespace.getInstance());
        onlyInstance.addNamespace(DatastreamNamespace.getInstance());
        onlyInstance.addNamespace(DisseminatorNamespace.getInstance());
        onlyInstance.addNamespace(ServiceDefinitionNamespace.getInstance());
        onlyInstance.addNamespace(ServiceDeploymentNamespace.getInstance());
    }

    public static final ResourceNamespace getInstance() {
        return onlyInstance;
    }

}
