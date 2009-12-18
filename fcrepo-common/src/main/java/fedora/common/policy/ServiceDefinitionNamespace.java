/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.policy;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.StringAttribute;

/**
 * The Fedora Service Definition XACML namespace.
 * 
 * <pre>
 * Namespace URI    : urn:fedora:names:fedora:2.1:resource:sdef
 * </pre>
 */
public class ServiceDefinitionNamespace
        extends XacmlNamespace {

    public final XacmlName PID;

    public final XacmlName NAMESPACE;

    public final XacmlName LOCATION;

    public final XacmlName STATE;

    private ServiceDefinitionNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        PID = addName(new XacmlName(this, "pid", StringAttribute.identifier));
        NAMESPACE =
                addName(new XacmlName(this,
                                      "namespace",
                                      StringAttribute.identifier));
        LOCATION =
                addName(new XacmlName(this,
                                      "location",
                                      AnyURIAttribute.identifier));
        STATE =
                addName(new XacmlName(this, "state", StringAttribute.identifier));
    }

    public static ServiceDefinitionNamespace onlyInstance =
            new ServiceDefinitionNamespace(ResourceNamespace.getInstance(),
                                           "sdef");

    public static final ServiceDefinitionNamespace getInstance() {
        return onlyInstance;
    }

}
