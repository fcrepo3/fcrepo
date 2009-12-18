/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.FedoraAccessNamespace;

/**
 * The Fedora Object Profile 1.0 XML format.
 * 
 * <pre>
 * Format URI        : info:fedora/fedora-system:FedoraObjectProfile-1.0
 * Primary Namespace : http://www.fedora.info/definitions/1/0/access/
 * XSD Schema URL    : http://www.fedora.info/definitions/1/0/objectProfile.xsd
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraObjectProfile1_0Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final FedoraObjectProfile1_0Format ONLY_INSTANCE =
            new FedoraObjectProfile1_0Format();

    /**
     * Constructs the instance.
     */
    private FedoraObjectProfile1_0Format() {
        super("info:fedora/fedora-system:FedoraObjectProfile-1.0",
              FedoraAccessNamespace.getInstance(),
              "http://www.fedora.info/definitions/1/0/objectProfile.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraObjectProfile1_0Format getInstance() {
        return ONLY_INSTANCE;
    }

}
