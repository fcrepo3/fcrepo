/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.XACMLPolicyNamespace;

/**
 * The XACML Policy 1.0 XML format.
 * 
 * <pre>
 * Format URI        : urn:oasis:names:tc:xacml:1.0:policy
 * Primary Namespace : urn:oasis:names:tc:xacml:1.0:policy
 * XSD Schema URL    : http://www.oasis-open.org/committees/xacml/repository/cs-xacml-schema-policy-01.xsd
 * </pre>
 * 
 * @author Chris Wilper
 */
public class XACMLPolicy1_0Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final XACMLPolicy1_0Format ONLY_INSTANCE =
            new XACMLPolicy1_0Format();

    /**
     * Constructs the instance.
     */
    private XACMLPolicy1_0Format() {
        super("urn:oasis:names:tc:xacml:1.0:policy",
              XACMLPolicyNamespace.getInstance(),
              "http://www.oasis-open.org/committees/xacml/repository/cs-xacml-schema-policy-01.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static XACMLPolicy1_0Format getInstance() {
        return ONLY_INSTANCE;
    }

}
