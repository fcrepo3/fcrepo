/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.FOXMLNamespace;

/**
 * The FOXML 1.0 XML format.
 * 
 * <pre>
 * Format URI        : info:fedora/fedora-system:FOXML-1.0
 * Primary Namespace : info:fedora/fedora-system:def/foxml#
 * XSD Schema URL    : http://www.fedora.info/definitions/1/0/foxml1-0.xsd
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FOXML1_0Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final FOXML1_0Format ONLY_INSTANCE = new FOXML1_0Format();

    /**
     * Constructs the instance.
     */
    private FOXML1_0Format() {
        super("info:fedora/fedora-system:FOXML-1.0",
              FOXMLNamespace.getInstance(),
              "http://www.fedora.info/definitions/1/0/foxml1-0.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FOXML1_0Format getInstance() {
        return ONLY_INSTANCE;
    }

}
