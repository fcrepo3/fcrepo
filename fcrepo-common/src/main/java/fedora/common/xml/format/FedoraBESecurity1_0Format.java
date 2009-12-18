/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.FedoraBESecurityNamespace;

/**
 * The Fedora BE Security 1.0 XML format.
 * 
 * <pre>
 * Format URI        : info:fedora/fedora-system:FedoraBESecurity-1.0
 * Primary Namespace : info:fedora/fedora-system:def/beSecurity#
 * XSD Schema URL    : http://www.fedora.info/definitions/1/0/api/beSecurity.xsd
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraBESecurity1_0Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final FedoraBESecurity1_0Format ONLY_INSTANCE =
            new FedoraBESecurity1_0Format();

    /**
     * Constructs the instance.
     */
    private FedoraBESecurity1_0Format() {
        super("info:fedora/fedora-system:FedoraBESecurity-1.0",
              FedoraBESecurityNamespace.getInstance(),
              "http://www.fedora.info/definitions/1/0/api/beSecurity.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraBESecurity1_0Format getInstance() {
        return ONLY_INSTANCE;
    }

}
