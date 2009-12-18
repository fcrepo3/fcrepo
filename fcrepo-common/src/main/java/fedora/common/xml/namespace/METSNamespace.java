/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The METS XML namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.loc.gov/METS/
 * Preferred Prefix : METS
 * </pre>
 * 
 * @author Chris Wilper
 */
public class METSNamespace
        extends XMLNamespace {

    //---
    // Elements
    //---

    /** The <code>amdSec</code> element. */
    public final QName AMD_SEC;

    /** The <code>behaviorSec</code> element. */
    public final QName BEHAVIOR_SEC;

    /** The <code>file</code> element. */
    public final QName FILE;

    /** The <code>fileGrp</code> element. */
    public final QName FILE_GRP;

    /** The <code>fileSec</code> element. */
    public final QName FILE_SEC;

    /** The <code>FLocat</code> element. */
    public final QName FLOCAT;

    /** The <code>mets</code> element. */
    public final QName METS;

    /** The <code>structMap</code> element. */
    public final QName STRUCT_MAP;

    //---
    // Attributes
    //---

    /** The <code>LABEL</code> attribute. */
    public final QName LABEL;

    /** The <code>OBJID</code> attribute. */
    public final QName OBJID;

    /** The <code>PROFILE</code> attribute. */
    public final QName PROFILE;

    /** The <code>TYPE</code> attribute. */
    public final QName TYPE;

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final METSNamespace ONLY_INSTANCE = new METSNamespace();

    /**
     * Constructs the instance.
     */
    protected METSNamespace() {
        super("http://www.loc.gov/METS/", "METS");

        // elements
        AMD_SEC = new QName(this, "amdSec");
        BEHAVIOR_SEC = new QName(this, "behaviorSec");
        FILE = new QName(this, "file");
        FILE_GRP = new QName(this, "fileGrp");
        FILE_SEC = new QName(this, "fileSec");
        FLOCAT = new QName(this, "FLocat");
        METS = new QName(this, "mets");
        STRUCT_MAP = new QName(this, "structMap");

        // attributes
        LABEL = new QName(this, "LABEL");
        OBJID = new QName(this, "OBJID");
        PROFILE = new QName(this, "PROFILE");
        TYPE = new QName(this, "TYPE");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static METSNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
