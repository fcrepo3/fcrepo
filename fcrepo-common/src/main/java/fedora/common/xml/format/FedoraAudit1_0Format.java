/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.FedoraAuditNamespace;

/**
 * The Fedora Audit 1.0 XML format.
 * 
 * <pre>
 * Format URI        : info:fedora/fedora-system:format/xml.fedora.audit
 * Primary Namespace : info:fedora/fedora-system:def/audit#
 * XSD Schema URL    : http://www.fedora.info/definitions/1/0/fedora-auditing.xsd
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraAudit1_0Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final FedoraAudit1_0Format ONLY_INSTANCE =
            new FedoraAudit1_0Format();

    /**
     * Constructs the instance.
     */
    private FedoraAudit1_0Format() {
        super("info:fedora/fedora-system:format/xml.fedora.audit",
              FedoraAuditNamespace.getInstance(),
              "http://www.fedora.info/definitions/1/0/fedora-auditing.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraAudit1_0Format getInstance() {
        return ONLY_INSTANCE;
    }

}
