/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The Fedora Batch Modify XML namespace.
 * 
 * <pre>
 * Namespace URI    : http://www.fedora.info/definitions/ 
 * Preferred Prefix : fbm
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraBatchModifyNamespace
        extends XMLNamespace {

    //---
    // Elements
    //---

    /** The <code>addDatastream</code> element. */
    public final QName ADD_DATASTREAM;

    /** The <code>addObject</code> element. */
    public final QName ADD_OBJECT;

    /** The <code>batchModify</code> element. */
    public final QName BATCH_MODIFY;

    /** The <code>compareDatastreamChecksum</code> element. */
    public final QName COMPARE_DATASTREAM_CHECKSUM;

    /** The <code>modifyDatastream</code> element. */
    public final QName MODIFY_DATASTREAM;

    /** The <code>modifyObject</code> element. */
    public final QName MODIFY_OBJECT;

    /** The <code>purgeDatastream</code> element. */
    public final QName PURGE_DATASTREAM;

    /** The <code>purgeObject</code> element. */
    public final QName PURGE_OBJECT;

    /** The <code>setDatastreamState</code> element. */
    public final QName SET_DATASTREAM_STATE;

    /** The <code>setDatastreamVersionable</code> element. */
    public final QName SET_DATASTREAM_VERSIONABLE;

    /** The <code>xmlData</code> element. */
    public final QName XML_DATA;

    //---
    // Attributes
    //---

    /** The <code>altIDs</code> attribute. */
    public final QName ALT_IDS;

    /** The <code>asOfDate</code> attribute. */
    public final QName AS_OF_DATE;

    /** The <code>checksum</code> attribute. */
    public final QName CHECKSUM;

    /** The <code>checksumType</code> attribute. */
    public final QName CHECKSUM_TYPE;

    /** The <code>contentModel</code> attribute. */
    public final QName CONTENT_MODEL;

    /** The <code>dsControlGroupType</code> attribute. */
    public final QName DS_CONTROL_GROUP_TYPE;

    /** The <code>dsID</code> attribute. */
    public final QName DS_ID;

    /** The <code>dsLabel</code> attribute. */
    public final QName DS_LABEL;

    /** The <code>dsLocation</code> attribute. */
    public final QName DS_LOCATION;

    /** The <code>dsMIME</code> attribute. */
    public final QName DS_MIME;

    /** The <code>dsState</code> attribute. */
    public final QName DS_STATE;

    /** The <code>endDate</code> attribute. */
    public final QName END_DATE;

    /** The <code>force</code> attribute. */
    public final QName FORCE;

    /** The <code>formatURI</code> attribute. */
    public final QName FORMAT_URI;

    /** The <code>label</code> attribute. */
    public final QName LABEL;

    /** The <code>logMessage</code> attribute. */
    public final QName LOG_MESSAGE;

    /** The <code>ownerId</code> attribute. */
    public final QName OWNER_ID;

    /** The <code>pid</code> attribute. */
    public final QName PID;

    /** The <code>state</code> attribute. */
    public final QName STATE;

    /** The <code>versionable</code> attribute. */
    public final QName VERSIONABLE;

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final FedoraBatchModifyNamespace ONLY_INSTANCE =
            new FedoraBatchModifyNamespace();

    /**
     * Constructs the instance.
     */
    private FedoraBatchModifyNamespace() {
        super("http://www.fedora.info/definitions/", "fbm");

        // elements
        ADD_DATASTREAM = new QName(this, "addDatastream");
        ADD_OBJECT = new QName(this, "addObject");
        BATCH_MODIFY = new QName(this, "batchModify");
        COMPARE_DATASTREAM_CHECKSUM =
                new QName(this, "compareDatastreamChecksum");
        MODIFY_DATASTREAM = new QName(this, "modifyDatastream");
        MODIFY_OBJECT = new QName(this, "modifyObject");
        PURGE_DATASTREAM = new QName(this, "purgeDatastream");
        PURGE_OBJECT = new QName(this, "purgeObject");
        SET_DATASTREAM_STATE = new QName(this, "setDatastreamState");
        SET_DATASTREAM_VERSIONABLE =
                new QName(this, "setDatastreamVersionable");
        XML_DATA = new QName(this, "xmlData");

        // attributes
        ALT_IDS = new QName(this, "altIDs");
        AS_OF_DATE = new QName(this, "asOfDate");
        CHECKSUM = new QName(this, "checksum");
        CHECKSUM_TYPE = new QName(this, "checksumType");
        CONTENT_MODEL = new QName(this, "contentModel");
        DS_CONTROL_GROUP_TYPE = new QName(this, "dsControlGroupType");
        DS_ID = new QName(this, "dsID");
        DS_LABEL = new QName(this, "dsLabel");
        DS_LOCATION = new QName(this, "dsLocation");
        DS_MIME = new QName(this, "dsMIME");
        DS_STATE = new QName(this, "dsState");
        END_DATE = new QName(this, "endDate");
        FORCE = new QName(this, "force");
        FORMAT_URI = new QName(this, "formatURI");
        LABEL = new QName(this, "label");
        LOG_MESSAGE = new QName(this, "logMessage");
        OWNER_ID = new QName(this, "ownerId");
        PID = new QName(this, "pid");
        STATE = new QName(this, "state");
        VERSIONABLE = new QName(this, "versionable");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraBatchModifyNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
