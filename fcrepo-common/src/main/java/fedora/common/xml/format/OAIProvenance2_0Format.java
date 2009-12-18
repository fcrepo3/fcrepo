/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.OAIProvenanceNamespace;

/**
 * The OAI Provenance 2.0 XML format.
 * 
 * <pre>
 * Format URI        : http://www.openarchives.org/OAI/2.0/provenance
 * Primary Namespace : http://www.openarchives.org/OAI/2.0/provenance
 * XSD Schema URL    : http://www.openarchives.org/OAI/2.0/provenance.xsd
 * </pre>
 * 
 * @author Chris Wilper
 */
public class OAIProvenance2_0Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final OAIProvenance2_0Format ONLY_INSTANCE =
            new OAIProvenance2_0Format();

    /**
     * Constructs the instance.
     */
    private OAIProvenance2_0Format() {
        super("http://www.openarchives.org/OAI/2.0/provenance",
              OAIProvenanceNamespace.getInstance(),
              "http://www.openarchives.org/OAI/2.0/provenance.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static OAIProvenance2_0Format getInstance() {
        return ONLY_INSTANCE;
    }

}
