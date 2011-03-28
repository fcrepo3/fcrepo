/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.common.xml.format;

import org.fcrepo.common.xml.namespace.FedoraManagementNamespace;

/**
 * The Fedora Object Validation 1.0 XML format.
 * 
 * <pre>
 * Format URI        : info:fedora/fedora-system:FedoraObjectValidation-1.0
 * Primary Namespace : http://www.fedora.info/definitions/1/0/management/
 * XSD Schema URL    : http://www.fedora.info/definitions/1/0/validation.xsd
 * </pre>
 * 
 * @author Edwin Shin
 */
public class FedoraObjectValidation1_0Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final FedoraObjectValidation1_0Format ONLY_INSTANCE =
            new FedoraObjectValidation1_0Format();

    /**
     * Constructs the instance.
     */
    private FedoraObjectValidation1_0Format() {
        super("info:fedora/fedora-system:FedoraObjectValidation-1.0",
              FedoraManagementNamespace.getInstance(),
              "http://www.fedora.info/definitions/1/0/validation.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraObjectValidation1_0Format getInstance() {
        return ONLY_INSTANCE;
    }

}
