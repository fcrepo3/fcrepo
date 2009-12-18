/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.FedoraBatchModifyNamespace;

/**
 * The Fedora Batch Modify 1.1 XML format.
 * 
 * <pre>
 * Format URI        : info:fedora/fedora-system:FedoraBatchModify-1.1
 * Primary Namespace : http://www.fedora.info/definitions/ 
 * XSD Schema URL    : http://www.fedora.info/definitions/1/0/api/batchModify-1.1.xsd 
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraBatchModify1_1Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final FedoraBatchModify1_1Format ONLY_INSTANCE =
            new FedoraBatchModify1_1Format();

    /**
     * Constructs the instance.
     */
    private FedoraBatchModify1_1Format() {
        super("info:fedora/fedora-system:FedoraBatchModify-1.1",
              FedoraBatchModifyNamespace.getInstance(),
              "http://www.fedora.info/definitions/1/0/api/batchModify-1.1.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraBatchModify1_1Format getInstance() {
        return ONLY_INSTANCE;
    }

}
