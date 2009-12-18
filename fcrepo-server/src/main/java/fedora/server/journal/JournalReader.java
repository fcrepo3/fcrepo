/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.log4j.Logger;

import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ServerException;
import fedora.server.journal.entry.ConsumerJournalEntry;
import fedora.server.journal.entry.JournalEntryContext;
import fedora.server.journal.helpers.DecodingBase64OutputStream;
import fedora.server.journal.helpers.JournalHelper;
import fedora.server.journal.recoverylog.JournalRecoveryLog;
import fedora.server.journal.xmlhelpers.AbstractXmlReader;
import fedora.server.journal.xmlhelpers.ContextXmlReader;

/**
 * The abstract base for all JournalReader classes.
 * <p>
 * Each child class is responsible for providing an XMLEventReader that is
 * positioned at the beginning of a JournalEntry tag. This class will read the
 * entry and leave the XMLEventReader positioned after the corresponding closing
 * tag.
 * 
 * @author Jim Blake
 */
public abstract class JournalReader
        extends AbstractXmlReader
        implements JournalConstants {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(JournalReader.class.getName());

    protected final Map<String, String> parameters;

    protected final String role;

    protected final JournalRecoveryLog recoveryLog;

    protected final ServerInterface server;

    private boolean ignoreHashErrors;

    /**
     * Create an instance of the proper JournalReader child class, as determined
     * by the server parameters.
     */
    public static JournalReader getInstance(Map<String, String> parameters,
                                            String role,
                                            JournalRecoveryLog recoveryLog,
                                            ServerInterface server)
            throws ModuleInitializationException {

        try {
            Object journalReader =
                    JournalHelper
                            .createInstanceAccordingToParameter(PARAMETER_JOURNAL_READER_CLASSNAME,
                                                                new Class[] {
                                                                        Map.class,
                                                                        String.class,
                                                                        JournalRecoveryLog.class,
                                                                        ServerInterface.class},
                                                                new Object[] {
                                                                        parameters,
                                                                        role,
                                                                        recoveryLog,
                                                                        server},
                                                                parameters);
            LOG.info("JournalReader is " + journalReader.toString());
            return (JournalReader) journalReader;
        } catch (JournalException e) {
            String msg = "Can't create JournalReader";
            LOG.error(msg, e);
            throw new ModuleInitializationException(msg, role, e);
        }
    }

    /**
     * Concrete sub-classes must implement this constructor.
     */
    protected JournalReader(Map<String, String> parameters,
                            String role,
                            JournalRecoveryLog recoveryLog,
                            ServerInterface server)
            throws JournalException {
        this.parameters = parameters;
        this.role = role;
        this.recoveryLog = recoveryLog;
        this.server = server;
        parseParameters();
    }

    private void parseParameters() throws JournalException {
        String ignore = parameters.get(PARAMETER_IGNORE_HASH);
        if (ignore == null) {
            ignoreHashErrors = false;
        } else if (ignore.equals(VALUE_FALSE)) {
            ignoreHashErrors = false;
        } else if (ignore.equals(VALUE_TRUE)) {
            ignoreHashErrors = true;
        } else {
            throw new JournalException("'" + PARAMETER_IGNORE_HASH
                    + "' parameter must be '" + VALUE_FALSE + "'(default) or '"
                    + VALUE_TRUE + "'");
        }
    }

    /**
     * Concrete sub-classes should probably synchronize this method, since it
     * can be called either from the JournalConsumerThread or from the Server.
     */
    public abstract void shutdown() throws JournalException;

    /**
     * Concrete sub-classes should insure that their XMLEventReader is
     * positioned at the beginning of a JournalEntry, and call
     * {@link #readJournalEntry(XMLEventReader)}. It is likely that this method
     * should be synchronized also, since it could be called from
     * JournalConsumerThread when the server calls {@link #shutdown()}
     */
    public abstract ConsumerJournalEntry readJournalEntry()
            throws JournalException, XMLStreamException;

    /**
     * Compare the repository hash from the journal file with the current hash
     * obtained from the server. If they do not match, either throw an exception
     * or simply log it, depending on the parameters. This method must not be
     * called before the server has completed initialization. That's the only
     * way we can be confident that the DOManager is present, and ready to
     * create the repository has that we will compare to.
     */
    protected void checkRepositoryHash(String hash) throws JournalException {
        if (!server.hasInitialized()) {
            throw new IllegalStateException("The repository has is not available until "
                    + "the server is fully initialized.");
        }

        JournalException hashException = null;

        if (hash == null) {
            hashException =
                    new JournalException("'" + QNAME_TAG_JOURNAL
                            + "' tag must have a '"
                            + QNAME_ATTR_REPOSITORY_HASH + "' attribute.");
        } else {
            try {
                String currentHash = server.getRepositoryHash();
                if (hash.equals(currentHash)) {
                    recoveryLog.log("Validated repository hash: '" + hash
                            + "'.");
                } else {
                    hashException =
                            new JournalException("'"
                                    + QNAME_ATTR_REPOSITORY_HASH
                                    + "' attribute in '"
                                    + QNAME_TAG_JOURNAL
                                    + "' tag does not match current repository hash: '"
                                    + hash + "' vs. '" + currentHash + "'.");
                }
            } catch (ServerException e) {
                hashException = new JournalException(e);
            }
        }

        if (hashException != null) {
            if (ignoreHashErrors) {
                recoveryLog.log("WARNING: " + hashException.getMessage());
            } else {
                throw hashException;
            }
        }

    }

    /**
     * Read a JournalEntry from the journal, to produce a
     * <code>ConsumerJournalEntry</code> instance. Concrete sub-classes should
     * insure that the XMLEventReader is positioned at the beginning of a
     * JournalEntry before calling this method.
     */
    protected ConsumerJournalEntry readJournalEntry(XMLEventReader reader)
            throws JournalException, XMLStreamException {
        StartElement startTag = getJournalEntryStartTag(reader);
        String methodName =
                getRequiredAttributeValue(startTag, QNAME_ATTR_METHOD);

        JournalEntryContext context =
                new ContextXmlReader().readContext(reader);
        ConsumerJournalEntry cje =
                new ConsumerJournalEntry(methodName, context);

        readArguments(reader, cje);

        return cje;
    }

    /**
     * Get the next event and complain if it isn't a JournalEntry start tag.
     */
    private StartElement getJournalEntryStartTag(XMLEventReader reader)
            throws XMLStreamException, JournalException {
        XMLEvent event = reader.nextTag();
        if (!isStartTagEvent(event, QNAME_TAG_JOURNAL_ENTRY)) {
            throw getNotStartTagException(QNAME_TAG_JOURNAL_ENTRY, event);
        }
        return event.asStartElement();
    }

    /**
     * Read arguments and add them to the event, until we hit the end tag for
     * the event.
     */
    private void readArguments(XMLEventReader reader, ConsumerJournalEntry cje)
            throws XMLStreamException, JournalException {
        while (true) {
            XMLEvent nextTag = reader.nextTag();
            if (isStartTagEvent(nextTag, QNAME_TAG_ARGUMENT)) {
                readArgument(nextTag, reader, cje);
            } else if (isEndTagEvent(nextTag, QNAME_TAG_JOURNAL_ENTRY)) {
                return;
            } else {
                throw getNotNextMemberOrEndOfGroupException(QNAME_TAG_JOURNAL_ENTRY,
                                                            QNAME_TAG_ARGUMENT,
                                                            nextTag);
            }
        }
    }

    private void readArgument(XMLEvent nextTag,
                              XMLEventReader reader,
                              ConsumerJournalEntry journalEntry)
            throws JournalException, XMLStreamException {
        StartElement element = nextTag.asStartElement();

        String argName = getRequiredAttributeValue(element, QNAME_ATTR_NAME);
        String argType = getRequiredAttributeValue(element, QNAME_ATTR_TYPE);

        if (ARGUMENT_TYPE_NULL.equals(argType)) {
            readNullArgument(reader, journalEntry, argName);
        } else if (ARGUMENT_TYPE_STRING.equals(argType)) {
            readStringArgument(reader, journalEntry, argName);
        } else if (ARGUMENT_TYPE_STRINGARRAY.equals(argType)) {
            readStringArrayArgument(reader, journalEntry, argName);
        } else if (ARGUMENT_TYPE_INTEGER.equals(argType)) {
            readIntegerArgument(reader, journalEntry, argName);
        } else if (ARGUMENT_TYPE_BOOLEAN.equals(argType)) {
            readBooleanArgument(reader, journalEntry, argName);
        } else if (ARGUMENT_TYPE_DATE.equals(argType)) {
            readDateArgument(reader, journalEntry, argName);
        } else if (ARGUMENT_TYPE_STREAM.equals(argType)) {
            readStreamArgument(reader, journalEntry, argName);
        } else {
            throw new JournalException("Unknown argument type: name='"
                    + argName + "', type='" + argType + "'");
        }
    }

    private void readStringArgument(XMLEventReader reader,
                                    ConsumerJournalEntry journalEntry,
                                    String name) throws XMLStreamException,
            JournalException {
        String value =
                readCharactersUntilEndOfArgument(reader,
                                                 QNAME_TAG_ARGUMENT,
                                                 journalEntry.getMethodName(),
                                                 name,
                                                 ARGUMENT_TYPE_STRING);
        journalEntry.addArgument(name, value);
    }

    private void readStringArrayArgument(XMLEventReader reader,
                                         ConsumerJournalEntry journalEntry,
                                         String name)
            throws XMLStreamException, JournalException {
        List<String> values = new ArrayList<String>();
        while (true) {
            XMLEvent event = reader.nextTag();
            if (isStartTagEvent(event, QNAME_TAG_ARRAYELEMENT)) {
                values
                        .add(readCharactersUntilEndOfArgument(reader,
                                                              QNAME_TAG_ARRAYELEMENT,
                                                              journalEntry
                                                                      .getMethodName(),
                                                              name,
                                                              ARGUMENT_TYPE_STRINGARRAY));
            } else if (isEndTagEvent(event, QNAME_TAG_ARGUMENT)) {
                break;
            } else {
                throw getUnexpectedEventInArgumentException(name,
                                                            ARGUMENT_TYPE_STRINGARRAY,
                                                            journalEntry
                                                                    .getMethodName(),
                                                            event);
            }
        }
        Object[] valuesArray = values.toArray(new String[values.size()]);
        journalEntry.addArgument(name, valuesArray);
    }

    private void readIntegerArgument(XMLEventReader reader,
                                     ConsumerJournalEntry journalEntry,
                                     String name) throws XMLStreamException,
            JournalException {
        XMLEvent chars = reader.nextEvent();
        if (!chars.isCharacters()) {
            throw getUnexpectedEventInArgumentException(name,
                                                        ARGUMENT_TYPE_INTEGER,
                                                        journalEntry
                                                                .getMethodName(),
                                                        chars);
        }

        Integer integerValue = Integer.valueOf(chars.asCharacters().getData());

        XMLEvent endTag = reader.nextEvent();
        if (isEndTagEvent(endTag, QNAME_TAG_ARGUMENT)) {
            journalEntry.addArgument(name, integerValue);
        } else {
            throw getUnexpectedEventInArgumentException(name,
                                                        ARGUMENT_TYPE_INTEGER,
                                                        journalEntry
                                                                .getMethodName(),
                                                        endTag);
        }
    }

    private void readBooleanArgument(XMLEventReader reader,
                                     ConsumerJournalEntry journalEntry,
                                     String name) throws XMLStreamException,
            JournalException {
        XMLEvent chars = reader.nextEvent();
        if (!chars.isCharacters()) {
            throw getUnexpectedEventInArgumentException(name,
                                                        ARGUMENT_TYPE_BOOLEAN,
                                                        journalEntry
                                                                .getMethodName(),
                                                        chars);
        }

        Boolean booleanValue = Boolean.valueOf(chars.asCharacters().getData());

        XMLEvent endTag = reader.nextEvent();
        if (isEndTagEvent(endTag, QNAME_TAG_ARGUMENT)) {
            journalEntry.addArgument(name, booleanValue);
        } else {
            throw getUnexpectedEventInArgumentException(name,
                                                        ARGUMENT_TYPE_BOOLEAN,
                                                        journalEntry
                                                                .getMethodName(),
                                                        endTag);
        }
    }

    private void readDateArgument(XMLEventReader reader,
                                  ConsumerJournalEntry journalEntry,
                                  String name) throws XMLStreamException,
            JournalException {
        XMLEvent chars = reader.nextEvent();
        if (!chars.isCharacters()) {
            throw getUnexpectedEventInArgumentException(name,
                                                        ARGUMENT_TYPE_BOOLEAN,
                                                        journalEntry
                                                                .getMethodName(),
                                                        chars);
        }

        Date dateValue =
                JournalHelper.parseDate(chars.asCharacters().getData());

        XMLEvent endTag = reader.nextEvent();
        if (isEndTagEvent(endTag, QNAME_TAG_ARGUMENT)) {
            journalEntry.addArgument(name, dateValue);
        } else {
            throw getUnexpectedEventInArgumentException(name,
                                                        ARGUMENT_TYPE_DATE,
                                                        journalEntry
                                                                .getMethodName(),
                                                        endTag);
        }
    }

    /**
     * An InputStream argument appears as a Base64-encoded String. It must be
     * decoded and written to a temp file, so it can be presented to the
     * management method as an InputStream again.
     */
    private void readStreamArgument(XMLEventReader reader,
                                    ConsumerJournalEntry journalEntry,
                                    String name) throws XMLStreamException,
            JournalException {
        try {
            File tempFile = JournalHelper.createTempFile();
            DecodingBase64OutputStream decoder =
                    new DecodingBase64OutputStream(new FileOutputStream(tempFile));

            while (true) {
                XMLEvent event = reader.nextEvent();
                if (event.isCharacters()) {
                    decoder.write(event.asCharacters().getData());
                } else if (isEndTagEvent(event, QNAME_TAG_ARGUMENT)) {
                    break;
                } else {
                    throw getUnexpectedEventInArgumentException(name,
                                                                ARGUMENT_TYPE_STREAM,
                                                                journalEntry
                                                                        .getMethodName(),
                                                                event);
                }
            }
            decoder.close();
            journalEntry.addArgument(name, tempFile);
        } catch (IOException e) {
            throw new JournalException("failed to write stream argument to temp file",
                                       e);
        }
    }

    private void readNullArgument(XMLEventReader reader,
                                  ConsumerJournalEntry journalEntry,
                                  String name) throws XMLStreamException,
            JournalException {
        XMLEvent endTag = reader.nextTag();
        if (!isEndTagEvent(endTag, QNAME_TAG_ARGUMENT)) {
            throw getUnexpectedEventInArgumentException(name,
                                                        ARGUMENT_TYPE_NULL,
                                                        journalEntry
                                                                .getMethodName(),
                                                        endTag);
        }
    }

    /**
     * Loop through a series of character events, accumulating the data into a
     * String. The character events should be terminated by an EndTagEvent with
     * the expected tag name.
     */
    private String readCharactersUntilEndOfArgument(XMLEventReader reader,
                                                    QName tagName,
                                                    String methodName,
                                                    String argumentName,
                                                    String argumentType)
            throws XMLStreamException, JournalException {
        StringBuffer stringValue = new StringBuffer();
        while (true) {
            XMLEvent event = reader.nextEvent();
            if (event.isCharacters()) {
                stringValue.append(event.asCharacters().getData());
            } else if (isEndTagEvent(event, tagName)) {
                break;
            } else {
                throw getUnexpectedEventInArgumentException(argumentName,
                                                            argumentType,
                                                            methodName,
                                                            event);
            }
        }
        return stringValue.toString();
    }

    /**
     * If we encounter an unexpected event when reading the a method argument,
     * create an exception with all of the pertinent information.
     */
    private JournalException getUnexpectedEventInArgumentException(String name,
                                                                   String argumentType,
                                                                   String methodName,
                                                                   XMLEvent event) {
        return new JournalException("Unexpected event while processing '"
                + name + "' argument (type = '" + argumentType + "') for '"
                + methodName + "' method call: event is '" + event + "'");
    }

}
