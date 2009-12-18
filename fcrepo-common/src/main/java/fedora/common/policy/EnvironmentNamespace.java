/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.policy;

import com.sun.xacml.attr.DateAttribute;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.TimeAttribute;

/**
 * The Fedora Environment XACML namespace.
 * 
 * <pre>
 * Namespace URI    : urn:fedora:names:fedora:2.1:environment
 * </pre>
 */
public class EnvironmentNamespace
        extends XacmlNamespace {

    public final XacmlName CURRENT_DATE_TIME;

    public final XacmlName CURRENT_DATE;

    public final XacmlName CURRENT_TIME;

    private EnvironmentNamespace() {
        super(Release2_1Namespace.getInstance(), "environment");
        CURRENT_DATE =
                addName(new XacmlName(this,
                                      "currentDate",
                                      DateAttribute.identifier));
        CURRENT_DATE_TIME =
                addName(new XacmlName(this,
                                      "currentDateTime",
                                      DateTimeAttribute.identifier));
        CURRENT_TIME =
                addName(new XacmlName(this,
                                      "currentTime",
                                      TimeAttribute.identifier));
    }

    public static EnvironmentNamespace onlyInstance =
            new EnvironmentNamespace();
    static {
        onlyInstance.addNamespace(HttpRequestNamespace.getInstance());
    }

    public static final EnvironmentNamespace getInstance() {
        return onlyInstance;
    }

}
