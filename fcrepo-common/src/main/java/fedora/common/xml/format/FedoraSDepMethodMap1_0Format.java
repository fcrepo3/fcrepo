/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.FedoraMethodMapNamespace;

/**
 * The Fedora Service Deployment Method Map 1.0 XML format.
 * 
 * <pre>
 * Format URI        : info:fedora/fedora-system:FedoraSDepMethodMap-1.0
 * Primary Namespace : http://fedora.comm.nsdlib.org/service/methodmap
 * XSD Schema URL    : http://www.fedora.info/definitions/1/0/fedoraSDepMethodMap.xsd
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraSDepMethodMap1_0Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final FedoraSDepMethodMap1_0Format ONLY_INSTANCE =
            new FedoraSDepMethodMap1_0Format();

    /**
     * Constructs the instance.
     */
    private FedoraSDepMethodMap1_0Format() {
        super("info:fedora/fedora-system:FedoraSDepMethodMap-1.0",
              FedoraMethodMapNamespace.getInstance(),
              "http://www.fedora.info/definitions/1/0/fedoraSDepMethodMap.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraSDepMethodMap1_0Format getInstance() {
        return ONLY_INSTANCE;
    }

}
