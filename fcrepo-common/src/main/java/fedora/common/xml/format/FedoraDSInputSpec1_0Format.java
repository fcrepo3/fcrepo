/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.FedoraBindingSpecNamespace;

/**
 * The Fedora Datastream Input Spec 1.0 XML format.
 * 
 * <pre>
 * Format URI        : info:fedora/fedora-system:FedoraDSInputSpec-1.0
 * Primary Namespace : http://fedora.comm.nsdlib.org/service/bindspec
 * XSD Schema URL    : http://www.fedora.info/definitions/1/0/fedoraBindingSpec.xsd
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraDSInputSpec1_0Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final FedoraDSInputSpec1_0Format ONLY_INSTANCE =
            new FedoraDSInputSpec1_0Format();

    /**
     * Constructs the instance.
     */
    private FedoraDSInputSpec1_0Format() {
        super("info:fedora/fedora-system:FedoraDSInputSpec-1.0",
              FedoraBindingSpecNamespace.getInstance(),
              "http://www.fedora.info/definitions/1/0/fedoraBindingSpec.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraDSInputSpec1_0Format getInstance() {
        return ONLY_INSTANCE;
    }

}
