/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.policy;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.BooleanAttribute;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.IntegerAttribute;
import com.sun.xacml.attr.StringAttribute;

/**
 * The Fedora Datastream XACML namespace.
 * 
 * <pre>
 * Namespace URI    : urn:fedora:names:fedora:2.1:resource:datastream
 * </pre>
 */
public class DatastreamNamespace
        extends XacmlNamespace {

    // Properties
    public final XacmlName ID;

    public final XacmlName STATE;

    public final XacmlName LOCATION_TYPE;

    public final XacmlName CONTROL_GROUP;

    public final XacmlName FORMAT_URI;

    public final XacmlName LOCATION;

    public final XacmlName CREATED_DATETIME;

    public final XacmlName INFO_TYPE;

    public final XacmlName MIME_TYPE;

    public final XacmlName CONTENT_LENGTH;

    public final XacmlName AS_OF_DATETIME;

    public final XacmlName NEW_STATE;

    public final XacmlName NEW_LOCATION;
    
    public final XacmlName FILE_URI;

    public final XacmlName NEW_CONTROL_GROUP;

    public final XacmlName NEW_FORMAT_URI;

    public final XacmlName NEW_MIME_TYPE;

    public final XacmlName NEW_VERSIONABLE;

    public final XacmlName CHECKSUM;

    public final XacmlName NEW_CHECKSUM;

    public final XacmlName CHECKSUM_TYPE;

    public final XacmlName NEW_CHECKSUM_TYPE;

    public final XacmlName ALT_IDS;

    public final XacmlName NEW_ALT_IDS;

    // Values

    private DatastreamNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        AS_OF_DATETIME =
                addName(new XacmlName(this,
                                      "asOfDateTime",
                                      DateTimeAttribute.identifier));
        CONTENT_LENGTH =
                addName(new XacmlName(this,
                                      "contentLength",
                                      IntegerAttribute.identifier));
        CONTROL_GROUP =
                addName(new XacmlName(this,
                                      "controlGroup",
                                      StringAttribute.identifier));
        NEW_CONTROL_GROUP =
                addName(new XacmlName(this,
                                      "newControlGroup",
                                      StringAttribute.identifier));
        CREATED_DATETIME =
                addName(new XacmlName(this,
                                      "createdDate",
                                      DateTimeAttribute.identifier));
        FORMAT_URI =
                addName(new XacmlName(this,
                                      "formatUri",
                                      AnyURIAttribute.identifier));
        NEW_FORMAT_URI =
                addName(new XacmlName(this,
                                      "newFormatUri",
                                      AnyURIAttribute.identifier));
        ID = addName(new XacmlName(this, "id", StringAttribute.identifier));
        INFO_TYPE =
                addName(new XacmlName(this,
                                      "infoType",
                                      StringAttribute.identifier));
        LOCATION =
                addName(new XacmlName(this,
                                      "location",
                                      AnyURIAttribute.identifier));
        NEW_LOCATION =
                addName(new XacmlName(this,
                                      "newLocation",
                                      AnyURIAttribute.identifier));
        FILE_URI =
            addName(new XacmlName(this,
                                  "fileUri",
                                  AnyURIAttribute.identifier));

        LOCATION_TYPE =
                addName(new XacmlName(this,
                                      "locationType",
                                      StringAttribute.identifier));
        MIME_TYPE =
                addName(new XacmlName(this,
                                      "mimeType",
                                      StringAttribute.identifier));
        NEW_MIME_TYPE =
                addName(new XacmlName(this,
                                      "newMimeType",
                                      StringAttribute.identifier));
        STATE =
                addName(new XacmlName(this, "state", StringAttribute.identifier));
        NEW_STATE =
                addName(new XacmlName(this,
                                      "newState",
                                      StringAttribute.identifier));
        NEW_VERSIONABLE =
                addName(new XacmlName(this,
                                      "newVersionable",
                                      BooleanAttribute.identifier));
        CHECKSUM =
                addName(new XacmlName(this,
                                      "checksum",
                                      StringAttribute.identifier));
        NEW_CHECKSUM =
                addName(new XacmlName(this,
                                      "newChecksum",
                                      StringAttribute.identifier));
        CHECKSUM_TYPE =
                addName(new XacmlName(this,
                                      "checksumType",
                                      StringAttribute.identifier));
        NEW_CHECKSUM_TYPE =
                addName(new XacmlName(this,
                                      "newChecksumType",
                                      StringAttribute.identifier));
        ALT_IDS =
                addName(new XacmlName(this,
                                      "altIds",
                                      StringAttribute.identifier));
        NEW_ALT_IDS =
                addName(new XacmlName(this,
                                      "newAltIds",
                                      StringAttribute.identifier));
    }

    public static DatastreamNamespace onlyInstance =
            new DatastreamNamespace(ResourceNamespace.getInstance(),
                                    "datastream");

    public static final DatastreamNamespace getInstance() {
        return onlyInstance;
    }

}
