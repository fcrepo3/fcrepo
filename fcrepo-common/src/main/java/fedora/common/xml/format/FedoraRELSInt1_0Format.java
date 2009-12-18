/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.XMLNamespace;

/**
 * The Fedora RELS-INT 1.0 Format.
 * 
 * <pre>
 * Format URI        : info:fedora/fedora-system:FedoraRELSInt-1.0
 * Primary Namespace : http://www.w3.org/1999/02/22-rdf-syntax-ns#
 * XSD Schema URL    : http://www.fedora.info/definitions/1/0/rels-int.xsd
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraRELSInt1_0Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final FedoraRELSInt1_0Format ONLY_INSTANCE =
            new FedoraRELSInt1_0Format();

    /**
     * Constructs the instance.
     */
    private FedoraRELSInt1_0Format() {
        super("info:fedora/fedora-system:FedoraRELSInt-1.0",
              new XMLNamespace("http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                               "rdf"),
              "http://www.fedora.info/definitions/1/0/rels-int.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraRELSInt1_0Format getInstance() {
        return ONLY_INSTANCE;
    }

}
