/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The FOXML XML namespace.
 * 
 * <pre>
 * Namespace URI    : info:fedora/fedora-system:def/foxml#
 * Preferred Prefix : foxml
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FOXMLNamespace
        extends XMLNamespace {

    //---
    // Elements
    //---

    /** The <code>binaryContent</code> element. */
    public final QName BINARY_CONTENT;

    /** The <code>contentDigest</code> element. */
    public final QName CONTENT_DIGEST;

    /** The <code>contentLocation</code> element. */
    public final QName CONTENT_LOCATION;

    /** The <code>datastream</code> element. */
    public final QName DATASTREAM;

    /** The <code>datastreamBinding</code> element. */
    public final QName DATASTREAM_BINDING;

    /** The <code>datastreamVersion</code> element. */
    public final QName DATASTREAM_VERSION;

    /** The <code>digitalObject</code> element. */
    public final QName DIGITAL_OBJECT;

    /** The <code>disseminator</code> element. */
    public final QName DISSEMINATOR;

    /** The <code>disseminatorVersion</code> element. */
    public final QName DISSEMINATOR_VERSION;

    /** The <code>extproperty</code> element. */
    public final QName EXT_PROPERTY;

    /** The <code>objectProperties</code> element. */
    public final QName OBJECT_PROPERTIES;

    /** The <code>property</code> element. */
    public final QName PROPERTY;

    /** The <code>serviceInputMap</code> element. */
    public final QName SERVICE_INPUT_MAP;

    /** The <code>xmlContent</code> element. */
    public final QName XML_CONTENT;

    //---
    // Attributes
    //---

    /** The <code>ALT_IDS</code> attribute. */
    public final QName ALT_IDS;

    /** The <code>CONTROL_GROUP</code> attribute. */
    public final QName CONTROL_GROUP;

    /** The <code>CREATED</code> attribute. */
    public final QName CREATED;

    /** The <code>DATASTREAM_ID</code> attribute. */
    public final QName DATASTREAM_ID;

    /** The <code>DIGEST</code> attribute. */
    public final QName DIGEST;

    /** The <code>FEDORA_URI</code> attribute. */
    public final QName FEDORA_URI;

    /** The <code>FORMAT_URI</code> attribute. */
    public final QName FORMAT_URI;

    /** The <code>ID</code> attribute. */
    public final QName ID;

    /** The <code>KEY</code> attribute. */
    public final QName KEY;

    /** The <code>LABEL</code> attribute. */
    public final QName LABEL;

    /** The <code>MIMETYPE</code> attribute. */
    public final QName MIMETYPE;

    /** The <code>NAME</code> attribute. */
    public final QName NAME;

    /** The <code>ORDER</code> attribute. */
    public final QName ORDER;

    /** The <code>PID</code> attribute. */
    public final QName PID;

    /** The <code>REF</code> attribute. */
    public final QName REF;

    /** The <code>SIZE</code> attribute. */
    public final QName SIZE;

    /** The <code>STATE</code> attribute. */
    public final QName STATE;

    /** The <code>VALUE</code> attribute. */
    public final QName VALUE;

    /** The <code>VERSION</code> attribute. */
    public final QName VERSION;

    /** The <code>VERSIONABLE</code> attribute. */
    public final QName VERSIONABLE;

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final FOXMLNamespace ONLY_INSTANCE = new FOXMLNamespace();

    /**
     * Constructs the instance.
     */
    private FOXMLNamespace() {
        super("info:fedora/fedora-system:def/foxml#", "foxml");

        // elements
        BINARY_CONTENT = new QName(this, "binaryContent");
        CONTENT_DIGEST = new QName(this, "contentDigest");
        CONTENT_LOCATION = new QName(this, "contentLocation");
        DATASTREAM = new QName(this, "datastream");
        DATASTREAM_BINDING = new QName(this, "datastreamBinding");
        DATASTREAM_VERSION = new QName(this, "datastreamVersion");
        DIGITAL_OBJECT = new QName(this, "digitalObject");
        DISSEMINATOR = new QName(this, "disseminator");
        DISSEMINATOR_VERSION = new QName(this, "disseminatorVersion");
        EXT_PROPERTY = new QName(this, "extproperty");
        OBJECT_PROPERTIES = new QName(this, "objectProperties");
        PROPERTY = new QName(this, "property");
        SERVICE_INPUT_MAP = new QName(this, "serviceInputMap");
        XML_CONTENT = new QName(this, "xmlContent");

        // attributes
        ALT_IDS = new QName(this, "ALT_IDS");
        CONTROL_GROUP = new QName(this, "CONTROL_GROUP");
        CREATED = new QName(this, "CREATED");
        DATASTREAM_ID = new QName(this, "DATASTREAM_ID");
        DIGEST = new QName(this, "DIGEST");
        FEDORA_URI = new QName(this, "FEDORA_URI");
        FORMAT_URI = new QName(this, "FORMAT_URI");
        ID = new QName(this, "ID");
        KEY = new QName(this, "KEY");
        LABEL = new QName(this, "LABEL");
        MIMETYPE = new QName(this, "MIMETYPE");
        NAME = new QName(this, "NAME");
        ORDER = new QName(this, "ORDER");
        PID = new QName(this, "PID");
        REF = new QName(this, "REF");
        SIZE = new QName(this, "SIZE");
        STATE = new QName(this, "STATE");
        VALUE = new QName(this, "VALUE");
        VERSION = new QName(this, "VERSION");
        VERSIONABLE = new QName(this, "VERSIONABLE");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FOXMLNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
