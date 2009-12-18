/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.xml.format;

import fedora.common.xml.namespace.FOXMLNamespace;

/**
 * The Atom APIM 1.0 XML format.
 * 
 * <pre>
 * Format URI        : info:fedora/fedora-system:ATOM-APIM-1.0
 * Primary Namespace : http://www.w3.org/2005/Atom
 * XSD Schema URL    : http://www.kbcafe.com/rss/atom.xsd.xml
 * </pre>
 * 
 * @author Edwin Shin
 * @since 3.0
 * @version $Id$
 */
public class AtomApiM1_0Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final AtomApiM1_0Format ONLY_INSTANCE = new AtomApiM1_0Format();

    /**
     * Constructs the instance.
     */
    private AtomApiM1_0Format() {
        super("info:fedora/fedora-system:ATOM-APIM-1.0",
              FOXMLNamespace.getInstance(),
              "http://www.kbcafe.com/rss/atom.xsd.xml");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static AtomApiM1_0Format getInstance() {
        return ONLY_INSTANCE;
    }

}
