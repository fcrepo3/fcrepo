/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.OAIIdentifierNamespace;

/**
 * The OAI Identifier 2.0 XML format.
 * 
 * <pre>
 * Format URI        : http://www.openarchives.org/OAI/2.0/oai-identifier
 * Primary Namespace : http://www.openarchives.org/OAI/2.0/oai-identifier
 * XSD Schema URL    : http://www.openarchives.org/OAI/2.0/oai-identifier.xsd
 * </pre>
 * 
 * @author Chris Wilper
 */
public class OAIIdentifier2_0Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final OAIIdentifier2_0Format ONLY_INSTANCE =
            new OAIIdentifier2_0Format();

    /**
     * Constructs the instance.
     */
    private OAIIdentifier2_0Format() {
        super("http://www.openarchives.org/OAI/2.0/oai-identifier",
              OAIIdentifierNamespace.getInstance(),
              "http://www.openarchives.org/OAI/2.0/oai-identifier.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static OAIIdentifier2_0Format getInstance() {
        return ONLY_INSTANCE;
    }

}
