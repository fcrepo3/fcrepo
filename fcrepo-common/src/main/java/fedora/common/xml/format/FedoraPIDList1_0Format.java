/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.FedoraAccessNamespace;

/**
 * The Fedora PID List 1.0 XML format.
 * 
 * <pre>
 * Format URI        : info:fedora/fedora-system:FedoraPIDList-1.0
 * Primary Namespace : http://www.fedora.info/definitions/1/0/management/
 * XSD Schema URL    : http://www.fedora.info/definitions/1/0/getNextPIDInfo.xsd
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraPIDList1_0Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final FedoraPIDList1_0Format ONLY_INSTANCE =
            new FedoraPIDList1_0Format();

    /**
     * Constructs the instance.
     */
    private FedoraPIDList1_0Format() {
        super("info:fedora/fedora-system:FedoraPIDList-1.0",
              FedoraAccessNamespace.getInstance(),
              "http://www.fedora.info/definitions/1/0/objectHistory.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraPIDList1_0Format getInstance() {
        return ONLY_INSTANCE;
    }

}
