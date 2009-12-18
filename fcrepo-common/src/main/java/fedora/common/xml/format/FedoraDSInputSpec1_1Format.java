/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.xml.format;

import fedora.common.xml.namespace.FedoraBindingSpecNamespace;

/**
 * The Fedora Datastream Input Spec 1.1 XML format.
 * 
 * <pre>
 * Format URI        : info:fedora/fedora-system:FedoraDSInputSpec-1.1
 * Primary Namespace : http://fedora.comm.nsdlib.org/service/bindspec
 * XSD Schema URL    : http://www.fedora.info/definitions/1/0/fedoraBindingSpec-1.1.xsd
 * </pre>
 * 
 * @author Edwin Shin
 * @since 3.1
 * @version $Id$
 */
public class FedoraDSInputSpec1_1Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final FedoraDSInputSpec1_1Format ONLY_INSTANCE =
            new FedoraDSInputSpec1_1Format();

    /**
     * Constructs the instance.
     */
    private FedoraDSInputSpec1_1Format() {
        super("info:fedora/fedora-system:FedoraDSInputSpec-1.1",
              FedoraBindingSpecNamespace.getInstance(),
              "http://www.fedora.info/definitions/1/0/fedoraBindingSpec-1.1.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraDSInputSpec1_1Format getInstance() {
        return ONLY_INSTANCE;
    }

}
