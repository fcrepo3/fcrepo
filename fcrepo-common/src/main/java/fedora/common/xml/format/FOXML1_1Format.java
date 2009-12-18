/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.FOXMLNamespace;

/**
 * The FOXML 1.1 XML format.
 * 
 * <pre>
 * Format URI        : info:fedora/fedora-system:FOXML-1.1
 * Primary Namespace : info:fedora/fedora-system:def/foxml#
 * XSD Schema URL    : http://www.fedora.info/definitions/1/0/foxml1-1.xsd
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FOXML1_1Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final FOXML1_1Format ONLY_INSTANCE = new FOXML1_1Format();

    /**
     * Constructs the instance.
     */
    private FOXML1_1Format() {
        super("info:fedora/fedora-system:FOXML-1.1",
              FOXMLNamespace.getInstance(),
              "http://www.fedora.info/definitions/1/0/foxml1-1.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FOXML1_1Format getInstance() {
        return ONLY_INSTANCE;
    }

}
