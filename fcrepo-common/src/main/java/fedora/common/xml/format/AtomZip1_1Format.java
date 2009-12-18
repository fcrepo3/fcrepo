/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.xml.format;

import fedora.common.xml.namespace.AtomNamespace;

/**
 * The Atom 1.1 XML format.
 * 
 * <pre>
 * Format URI        : info:fedora/fedora-system:ATOMZip-1.1
 * Primary Namespace : http://www.w3.org/2005/Atom
 * XSD Schema URL    : http://www.kbcafe.com/rss/atom.xsd.xml
 * </pre>
 * 
 * @author Edwin Shin
 */
public class AtomZip1_1Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final AtomZip1_1Format ONLY_INSTANCE = new AtomZip1_1Format();

    /**
     * Constructs the instance.
     */
    private AtomZip1_1Format() {
        super("info:fedora/fedora-system:ATOMZip-1.1",
              AtomNamespace.getInstance(),
              "http://www.kbcafe.com/rss/atom.xsd.xml");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static AtomZip1_1Format getInstance() {
        return ONLY_INSTANCE;
    }

}
