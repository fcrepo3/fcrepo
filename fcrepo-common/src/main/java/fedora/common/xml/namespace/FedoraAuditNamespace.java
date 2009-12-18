/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The Fedora Audit XML namespace.
 * 
 * <pre>
 * Namespace URI    : info:fedora/fedora-system:def/audit#
 * Preferred Prefix : audit
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraAuditNamespace
        extends XMLNamespace {

    //---
    // Elements
    //---

    /** The <code>action</code> element. */
    public final QName ACTION;

    /** The <code>auditTrail</code> element. */
    public final QName AUDIT_TRAIL;

    /** The <code>componentID</code> element. */
    public final QName COMPONENT_ID;

    /** The <code>date</code> element. */
    public final QName DATE;

    /** The <code>justification</code> element. */
    public final QName JUSTIFICATION;

    /** The <code>process</code> element. */
    public final QName PROCESS;

    /** The <code>record</code> element. */
    public final QName RECORD;

    /** The <code>responsibility</code> element. */
    public final QName RESPONSIBILITY;

    //---
    // Attributes
    //---

    /** The <code>ID</code> attribute. */
    public final QName ID;

    /** The <code>type</code> attribute. */
    public final QName TYPE;

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final FedoraAuditNamespace ONLY_INSTANCE =
            new FedoraAuditNamespace();

    /**
     * Constructs the instance.
     */
    private FedoraAuditNamespace() {
        super("info:fedora/fedora-system:def/audit#", "audit");

        // elements
        ACTION = new QName(this, "action");
        AUDIT_TRAIL = new QName(this, "auditTrail");
        COMPONENT_ID = new QName(this, "componentID");
        DATE = new QName(this, "date");
        JUSTIFICATION = new QName(this, "justification");
        PROCESS = new QName(this, "process");
        RECORD = new QName(this, "record");
        RESPONSIBILITY = new QName(this, "responsibility");

        // attributes
        ID = new QName(this, "ID");
        TYPE = new QName(this, "type");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraAuditNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
