/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.FedoraAccessNamespace;

/**
 * The Fedora Repository Description 1.0 XML format.
 * 
 * <pre>
 * Format URI        : info:fedora/fedora-system:FedoraRepositoryDesc-1.0
 * Primary Namespace : http://www.fedora.info/definitions/1/0/access/
 * XSD Schema URL    : http://www.fedora.info/definitions/1/0/fedoraRepository.xsd
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraRepositoryDesc1_0Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final FedoraRepositoryDesc1_0Format ONLY_INSTANCE =
            new FedoraRepositoryDesc1_0Format();

    /**
     * Constructs the instance.
     */
    private FedoraRepositoryDesc1_0Format() {
        super("info:fedora/fedora-system:FedoraRepositoryDesc-1.0",
              FedoraAccessNamespace.getInstance(),
              "http://www.fedora.info/definitions/1/0/fedoraRepository.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraRepositoryDesc1_0Format getInstance() {
        return ONLY_INSTANCE;
    }

}
