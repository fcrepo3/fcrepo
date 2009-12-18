/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.METSFedoraExtNamespace;

/**
 * The METS Fedora Extension 1.1 XML format.
 * 
 * <pre>
 * Format URI        : info:fedora/fedora-system:METSFedoraExt-1.1
 * Primary Namespace : http://www.loc.gov/METS/
 * XSD Schema URL    : http://www.fedora.info/definitions/1/0/mets-fedora-ext1-1.xsd
 * </pre>
 * 
 * @author Chris Wilper
 */
public class METSFedoraExt1_1Format
        extends XMLFormat {

    /** The only instance of this class. */
    private static final METSFedoraExt1_1Format ONLY_INSTANCE =
            new METSFedoraExt1_1Format();

    /**
     * Constructs the instance.
     */
    private METSFedoraExt1_1Format() {
        super("info:fedora/fedora-system:METSFedoraExt-1.1",
              METSFedoraExtNamespace.getInstance(),
              "http://www.fedora.info/definitions/1/0/mets-fedora-ext1-1.xsd");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static METSFedoraExt1_1Format getInstance() {
        return ONLY_INSTANCE;
    }

}
