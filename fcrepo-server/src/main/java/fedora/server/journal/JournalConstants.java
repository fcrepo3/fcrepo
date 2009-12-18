/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal;

import javax.xml.namespace.QName;

/**
 * A collection of String constants and QName constants used by the Journaler
 * and its associated classes.
 * 
 * @author Jim Blake
 */
public interface JournalConstants {

    // Prefix prepended to parameter names when loaded as system properties.
    String SYSTEM_PROPERTY_PREFIX = "fedora.journal.";

    // Names of server parameters.
    String PARAMETER_JOURNAL_RECOVERY_LOG_CLASSNAME =
            "journalRecoveryLogClassname";

    String PARAMETER_RECOVERY_LOG_FILENAME = "recoveryLogFilename";

    String PARAMETER_RECOVERY_LOG_LEVEL = "recoveryLogLevel";

    String PARAMETER_JOURNAL_READER_CLASSNAME = "journalReaderClassname";

    String PARAMETER_JOURNAL_WRITER_CLASSNAME = "journalWriterClassname";

    String PARAMETER_JOURNAL_MODE = "journalMode";

    String PARAMETER_IGNORE_HASH = "continueOnHashError";

    String PARAMETER_JOURNAL_FILENAME_PREFIX = "journalFilenamePrefix";

    String PARAMETER_JOURNAL_FILE_SIZE_LIMIT = "journalFileSizeLimit";

    String PARAMETER_JOURNAL_FILE_AGE_LIMIT = "journalFileAgeLimit";

    // Acceptable values for server parameters.
    String VALUE_TRUE = "true";

    String VALUE_FALSE = "false";

    String VALUE_JOURNAL_MODE_NORMAL = "normal";

    String VALUE_JOURNAL_MODE_RECOVER = "recover";

    String VALUE_RECOVERY_LOG_LEVEL_HIGH = "high";

    String VALUE_RECOVERY_LOG_LEVEL_MEDIUM = "medium";

    String VALUE_RECOVERY_LOG_LEVEL_LOW = "low";

    // Default values for server parameters.
    String DEFAULT_FILENAME_PREFIX = "fedoraJournal";

    String DEFAULT_SIZE_LIMIT = "5M";

    String DEFAULT_AGE_LIMIT = "1D";

    // Strings for the XML document header of the Journal file
    String DOCUMENT_ENCODING = "UTF-8";

    String DOCUMENT_VERSION = "1.0";

    // Names for the XML tags in the Journal file
    QName QNAME_TAG_ARGUMENT = QName.valueOf("argument");

    QName QNAME_TAG_ARRAYELEMENT = QName.valueOf("element");

    QName QNAME_TAG_CONTEXT = QName.valueOf("context");

    QName QNAME_TAG_DS_BINDING = QName.valueOf("dsBinding");

    QName QNAME_TAG_DS_BINDING_MAP = QName.valueOf("dsBindingMap");

    QName QNAME_TAG_JOURNAL = QName.valueOf("FedoraJournal");

    QName QNAME_TAG_JOURNAL_ENTRY = QName.valueOf("JournalEntry");

    QName QNAME_TAG_MULTI_VALUE_MAP = QName.valueOf("multimap");

    QName QNAME_TAG_MULTI_VALUE_MAP_KEY = QName.valueOf("multimapkey");

    QName QNAME_TAG_MULTI_VALUE_MAP_VALUE = QName.valueOf("multimapvalue");

    QName QNAME_TAG_NOOP = QName.valueOf("noOp");

    QName QNAME_TAG_NOW = QName.valueOf("now");

    QName QNAME_TAG_PASSWORD = QName.valueOf("password");

    // Names for the XML attributes in the Journal file
    QName QNAME_ATTR_BIND_KEY_NAME = QName.valueOf("bindKeyName");

    QName QNAME_ATTR_BIND_LABEL = QName.valueOf("bindLabel");

    QName QNAME_ATTR_CLIENT_IP = QName.valueOf("clientIpAddress");

    QName QNAME_ATTR_DATASTREAM_ID = QName.valueOf("datastreamId");

    QName QNAME_ATTR_DS_BIND_MAP_ID = QName.valueOf("dsBindMapId");

    QName QNAME_ATTR_DS_BIND_MAP_LABEL = QName.valueOf("dsBindMapLabel");

    QName QNAME_ATTR_DS_BIND_MECHANISM_PID =
            QName.valueOf("dsBindMechanismPid");

    QName QNAME_ATTR_LOGIN_ID = QName.valueOf("loginId");

    QName QNAME_ATTR_METHOD = QName.valueOf("method");

    QName QNAME_ATTR_NAME = QName.valueOf("name");

    QName QNAME_ATTR_PASSWORD_TYPE = QName.valueOf("type");

    QName QNAME_ATTR_REPOSITORY_HASH = QName.valueOf("repositoryHash");

    QName QNAME_ATTR_SEQ_NO = QName.valueOf("seqNo");

    QName QNAME_ATTR_STATE = QName.valueOf("state");

    QName QNAME_ATTR_TIMESTAMP = QName.valueOf("timestamp");

    QName QNAME_ATTR_TYPE = QName.valueOf("type");

    QName QNAME_ATTR_USERID = QName.valueOf("userId");

    // Names of the management methods that are written to the Journal
    String METHOD_INGEST = "ingest";

    String METHOD_MODIFY_OBJECT = "modifyObject";

    String METHOD_PURGE_OBJECT = "purgeObject";

    String METHOD_ADD_DATASTREAM = "addDatastream";

    String METHOD_MODIFY_DATASTREAM_BY_REFERENCE =
            "modifyDatastreamByReference";

    String METHOD_MODIFY_DATASTREAM_BY_VALUE = "modifyDatastreamByValue";

    String METHOD_SET_DATASTREAM_STATE = "setDatastreamState";

    String METHOD_SET_DATASTREAM_VERSIONABLE = "setDatastreamVersionable";

    String METHOD_PURGE_DATASTREAM = "purgeDatastream";

    String METHOD_ADD_DISSEMINATOR = "addDisseminator";

    String METHOD_MODIFY_DISSEMINATOR = "modifyDisseminator";

    String METHOD_SET_DISSEMINATOR_STATE = "setDisseminatorState";

    String METHOD_PURGE_DISSEMINATOR = "purgeDisseminator";

    String METHOD_PUT_TEMP_STREAM = "putTempStream";

    String METHOD_GET_NEXT_PID = "getNextPID";

    String METHOD_ADD_RELATIONSHIP = "addRelationship";

    String METHOD_PURGE_RELATIONSHIP = "purgeRelationship";

    // Types of arguments to the management methods, as written to the Journal
    String ARGUMENT_TYPE_STRING = "string";

    String ARGUMENT_TYPE_STRINGARRAY = "stringarray";

    String ARGUMENT_TYPE_INTEGER = "integer";

    String ARGUMENT_TYPE_BOOLEAN = "boolean";

    String ARGUMENT_TYPE_DATE = "date";

    String ARGUMENT_TYPE_STREAM = "stream";

    String ARGUMENT_TYPE_BINDING_MAP = "bindingMap";

    String ARGUMENT_TYPE_NULL = "null";

    // Names of arguments to the management methods, as written to the Journal
    String ARGUMENT_NAME_ALT_IDS = "altIds";

    String ARGUMENT_NAME_CHECKSUM = "checksum";

    String ARGUMENT_NAME_CHECKSUM_TYPE = "checksumType";

    String ARGUMENT_NAME_CONTEXT = "context";

    String ARGUMENT_NAME_CONTROL_GROUP = "controlGroup";

    String ARGUMENT_NAME_DISSEMINATOR_ID = "disseminatorID";

    String ARGUMENT_NAME_DISSEMINATOR_LABEL = "disseminatorLabel";

    String ARGUMENT_NAME_DISSEMINATOR_STATE = "disseminatorState";

    String ARGUMENT_NAME_DS_CONTENT = "dsContent";

    String ARGUMENT_NAME_DS_ID = "dsId";

    String ARGUMENT_NAME_DS_LABEL = "dsLabel";

    String ARGUMENT_NAME_DS_LOCATION = "dsLocation";

    String ARGUMENT_NAME_DS_STATE = "dsState";

    String ARGUMENT_NAME_ENCODING = "encoding";

    String ARGUMENT_NAME_START_DATE = "startDT";

    String ARGUMENT_NAME_END_DATE = "endDT";

    String ARGUMENT_NAME_FORCE = "force";

    String ARGUMENT_NAME_FORMAT = "format";

    String ARGUMENT_NAME_FORMAT_URI = "formatUri";

    String ARGUMENT_NAME_IN = "in";

    String ARGUMENT_NAME_LABEL = "label";

    String ARGUMENT_NAME_LOCATION = "location";

    String ARGUMENT_NAME_LOG_MESSAGE = "message";

    String ARGUMENT_NAME_MIME_TYPE = "mimeType";

    String ARGUMENT_NAME_NAMESPACE = "namespace";

    String ARGUMENT_NAME_NEW_PID = "newPid";

    String ARGUMENT_NAME_NUM_PIDS = "numPids";

    String ARGUMENT_NAME_OWNERID = "ownerId";

    String ARGUMENT_NAME_PID = "pid";

    String ARGUMENT_NAME_SERIALIZATION = "serialization";

    String ARGUMENT_NAME_STATE = "state";

    String ARGUMENT_NAME_VERSIONABLE = "versionable";

    String ARGUMENT_NAME_VERSION_DATE = "versionDate";

    String ARGUMENT_NAME_RELATIONSHIP = "relationship";

    String ARGUMENT_NAME_OBJECT = "object";

    String ARGUMENT_NAME_IS_LITERAL = "isLiteral";

    String ARGUMENT_NAME_DATATYPE = "datatype";

    // Names of maps in the Context object, as written to the Journal
    String CONTEXT_MAPNAME_ACTION = "action";

    String CONTEXT_MAPNAME_ENVIRONMENT = "environment";

    String CONTEXT_MAPNAME_RESOURCE = "resource";

    String CONTEXT_MAPNAME_RECOVERY = "recovery";

    String CONTEXT_MAPNAME_SUBJECT = "subject";

    // Format of time stamps used in the Journal and in the recovery log
    String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    // Format of time stamps used in names of Journal files and recover logs.
    String FORMAT_JOURNAL_FILENAME_TIMESTAMP = "yyyyMMdd.HHmmss.SSS";

    // Type code for the latest and greatest password cipher.
    String PASSWORD_CIPHER_TYPE = "1";

}
