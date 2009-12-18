/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.FedoraDSCompositeModelNamespace;

/**
 * The Fedora DS Composite Model 1.0 XML format.
 * 
 * <pre>
 * Format URI        : info:fedora/fedora-system:FedoraDSCompositeModel-1.0
 * Primary Namespace : info:fedora/fedora-system:def/dsCompositeModel#
 * XSD Schema URL    : http://www.fedora.info/definitions/1/0/dsCompositeModel.xsd
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraDSCompositeModel1_0Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final FedoraDSCompositeModel1_0Format ONLY_INSTANCE =
            new FedoraDSCompositeModel1_0Format();

    /**
     * Constructs the instance.
     */
    private FedoraDSCompositeModel1_0Format() {
        super("info:fedora/fedora-system:FedoraDSCompositeModel-1.0",
              FedoraDSCompositeModelNamespace.getInstance(),
              "http://www.fedora.info/definitions/1/0/dsCompositeModel.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraDSCompositeModel1_0Format getInstance() {
        return ONLY_INSTANCE;
    }

}
