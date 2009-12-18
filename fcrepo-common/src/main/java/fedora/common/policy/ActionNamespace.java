/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.policy;

import com.sun.xacml.attr.StringAttribute;

/**
 * The Fedora Action XACML namespace.
 *
 * <pre>
 * Namespace URI    : urn:fedora:names:fedora:2.1:action
 * </pre>
 */
public class ActionNamespace
        extends XacmlNamespace {

    // Properties
    public final XacmlName ID;

    public final XacmlName API;

    public final XacmlName CONTEXT_ID;

    // Values of API
    public final XacmlName APIM;

    public final XacmlName APIA;

    // Values of ID
    public final XacmlName ADD_DATASTREAM;

    public final XacmlName EXPORT;

    public final XacmlName GET_DATASTREAM;

    public final XacmlName GET_DATASTREAM_HISTORY;

    public final XacmlName GET_DATASTREAMS;

    public final XacmlName GET_NEXT_PID;

    public final XacmlName GET_OBJECT_XML;

    public final XacmlName INGEST;

    public final XacmlName MODIFY_DATASTREAM_BY_REFERENCE;

    public final XacmlName MODIFY_DATASTREAM_BY_VALUE;

    public final XacmlName MODIFY_OBJECT;

    public final XacmlName PURGE_OBJECT;

    public final XacmlName PURGE_DATASTREAM;

    public final XacmlName SET_DATASTREAM_STATE;

    public final XacmlName DESCRIBE_REPOSITORY;

    public final XacmlName FIND_OBJECTS;

    public final XacmlName RI_FIND_OBJECTS;

    public final XacmlName GET_DATASTREAM_DISSEMINATION;

    public final XacmlName GET_DISSEMINATION;

    public final XacmlName GET_OBJECT_HISTORY;

    public final XacmlName GET_OBJECT_PROFILE;

    public final XacmlName LIST_DATASTREAMS;

    public final XacmlName LIST_METHODS;

    public final XacmlName LIST_OBJECT_IN_FIELD_SEARCH_RESULTS;

    public final XacmlName LIST_OBJECT_IN_RESOURCE_INDEX_RESULTS;

    public final XacmlName SERVER_STATUS;

    public final XacmlName OAI;

    public final XacmlName UPLOAD;

    public final XacmlName INTERNAL_DSSTATE;

    public final XacmlName RESOLVE_DATASTREAM;

    public final XacmlName RELOAD_POLICIES;

    public final XacmlName SET_DATASTREAM_VERSIONABLE;

    public final XacmlName COMPARE_DATASTREAM_CHECKSUM;

    public final XacmlName GET_RELATIONSHIPS;

    public final XacmlName ADD_RELATIONSHIP;

    public final XacmlName PURGE_RELATIONSHIP;

    public final XacmlName RETRIEVE_FILE;

    private ActionNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        API = addName(new XacmlName(this, "api", StringAttribute.identifier));
        APIM = addName(new XacmlName(this, "api-m"));
        APIA = addName(new XacmlName(this, "api-a"));

        ID = addName(new XacmlName(this, "id", StringAttribute.identifier));
        // derived from respective Java methods in Access.java or Management.java
        ADD_DATASTREAM = addName(new XacmlName(this, "id-addDatastream"));
        EXPORT = addName(new XacmlName(this, "id-export"));
        GET_DATASTREAM = addName(new XacmlName(this, "id-getDatastream"));
        GET_DATASTREAM_HISTORY =
                addName(new XacmlName(this, "id-getDatastreamHistory"));
        GET_DATASTREAMS = addName(new XacmlName(this, "id-getDatastreams"));
        GET_NEXT_PID = addName(new XacmlName(this, "id-getNextPid"));
        GET_OBJECT_XML = addName(new XacmlName(this, "id-getObjectXML"));
        INGEST = addName(new XacmlName(this, "id-ingest"));
        MODIFY_DATASTREAM_BY_REFERENCE =
                addName(new XacmlName(this, "id-modifyDatastreamByReference"));
        MODIFY_DATASTREAM_BY_VALUE =
                addName(new XacmlName(this, "id-modifyDatastreamByValue"));
        MODIFY_OBJECT = addName(new XacmlName(this, "id-modifyObject"));
        PURGE_OBJECT = addName(new XacmlName(this, "id-purgeObject"));
        PURGE_DATASTREAM = addName(new XacmlName(this, "id-purgeDatastream"));
        SET_DATASTREAM_STATE =
                addName(new XacmlName(this, "id-setDatastreamState"));
        SET_DATASTREAM_VERSIONABLE =
                addName(new XacmlName(this, "id-setDatastreamVersionable"));
        COMPARE_DATASTREAM_CHECKSUM =
                addName(new XacmlName(this, "id-compareDatastreamChecksum"));
        DESCRIBE_REPOSITORY =
                addName(new XacmlName(this, "id-describeRepository"));
        FIND_OBJECTS = addName(new XacmlName(this, "id-findObjects"));
        RI_FIND_OBJECTS = addName(new XacmlName(this, "id-riFindObjects"));
        GET_DATASTREAM_DISSEMINATION =
                addName(new XacmlName(this, "id-getDatastreamDissemination"));
        GET_DISSEMINATION = addName(new XacmlName(this, "id-getDissemination"));
        GET_OBJECT_HISTORY =
                addName(new XacmlName(this, "id-getObjectHistory"));
        GET_OBJECT_PROFILE =
                addName(new XacmlName(this, "id-getObjectProfile"));
        LIST_DATASTREAMS = addName(new XacmlName(this, "id-listDatastreams"));
        LIST_METHODS = addName(new XacmlName(this, "id-listMethods"));
        LIST_OBJECT_IN_FIELD_SEARCH_RESULTS =
                addName(new XacmlName(this, "id-listObjectInFieldSearchResults"));
        LIST_OBJECT_IN_RESOURCE_INDEX_RESULTS =
                addName(new XacmlName(this,
                                      "id-listObjectInResourceIndexResults"));
        SERVER_STATUS = addName(new XacmlName(this, "id-serverStatus"));
        OAI = addName(new XacmlName(this, "id-oai"));
        UPLOAD = addName(new XacmlName(this, "id-upload"));
        INTERNAL_DSSTATE = addName(new XacmlName(this, "id-dsstate"));
        RESOLVE_DATASTREAM =
                addName(new XacmlName(this, "id-resolveDatastream"));
        RELOAD_POLICIES = addName(new XacmlName(this, "id-reloadPolicies"));
        GET_RELATIONSHIPS = addName(new XacmlName(this, "id-getRelationships"));
        ADD_RELATIONSHIP = addName(new XacmlName(this, "id-addRelationship"));
        PURGE_RELATIONSHIP =
                addName(new XacmlName(this, "id-purgeRelationship"));
        CONTEXT_ID =
                addName(new XacmlName(this,
                                      "contextId",
                                      StringAttribute.identifier)); //internal callback support
        RETRIEVE_FILE =
            addName(new XacmlName(this, "id-retrieveFile"));
        // Values of CONTEXT_ID are sequential numerals, hence not enumerated here.
    }

    public static ActionNamespace onlyInstance =
            new ActionNamespace(Release2_1Namespace.getInstance(), "action");

    public static final ActionNamespace getInstance() {
        return onlyInstance;
    }

}
