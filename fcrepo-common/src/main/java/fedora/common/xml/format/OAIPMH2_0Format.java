/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.OAIPMHNamespace;

/**
 * The OAI-PMH 2.0 XML format.
 * 
 * <pre>
 * Format URI        : http://www.openarchives.org/OAI/2.0/
 * Primary Namespace : http://www.openarchives.org/OAI/2.0/
 * XSD Schema URL    : http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd
 * </pre>
 * 
 * @author Chris Wilper
 */
public class OAIPMH2_0Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final OAIPMH2_0Format ONLY_INSTANCE = new OAIPMH2_0Format();

    /**
     * Constructs the instance.
     */
    private OAIPMH2_0Format() {
        super("http://www.openarchives.org/OAI/2.0/",
              OAIPMHNamespace.getInstance(),
              "http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static OAIPMH2_0Format getInstance() {
        return ONLY_INSTANCE;
    }

}
