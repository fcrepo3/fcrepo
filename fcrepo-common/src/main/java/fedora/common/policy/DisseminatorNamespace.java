/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.policy;

import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.StringAttribute;

/**
 * The Fedora Disseminator XACML namespace.
 * 
 * <pre>
 * Namespace URI    : urn:fedora:names:fedora:2.1:resource:disseminator
 * </pre>
 */
@Deprecated
public class DisseminatorNamespace
        extends XacmlNamespace {

    // Properties
    public final XacmlName ID;

    public final XacmlName STATE;

    public final XacmlName METHOD;

    public final XacmlName AS_OF_DATETIME;

    public final XacmlName NEW_STATE;

    // Values

    private DisseminatorNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        ID = addName(new XacmlName(this, "id", StringAttribute.identifier));
        STATE =
                addName(new XacmlName(this, "state", StringAttribute.identifier));
        METHOD =
                addName(new XacmlName(this,
                                      "method",
                                      StringAttribute.identifier));
        AS_OF_DATETIME =
                addName(new XacmlName(this,
                                      "asOfDateTime",
                                      DateTimeAttribute.identifier));
        NEW_STATE =
                addName(new XacmlName(this,
                                      "newState",
                                      StringAttribute.identifier));
    }

    public static DisseminatorNamespace onlyInstance =
            new DisseminatorNamespace(ResourceNamespace.getInstance(),
                                      "disseminator");

    public static final DisseminatorNamespace getInstance() {
        return onlyInstance;
    }

}
