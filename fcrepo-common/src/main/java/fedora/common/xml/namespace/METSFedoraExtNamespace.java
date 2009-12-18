/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The METS Fedora Extension XML namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.loc.gov/METS/
 * Preferred Prefix : METS
 * </pre>
 * 
 * @author Chris Wilper
 */
public class METSFedoraExtNamespace
        extends METSNamespace {

    //---
    // Attributes
    //---

    /** The <code>EXT_VERSION</code> attribute. */
    public final QName EXT_VERSION;

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final METSFedoraExtNamespace ONLY_INSTANCE =
            new METSFedoraExtNamespace();

    /**
     * Constructs the instance.
     */
    private METSFedoraExtNamespace() {

        // attributes
        EXT_VERSION = new QName(this, "EXT_VERSION");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static METSFedoraExtNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
