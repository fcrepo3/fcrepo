/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.xmlhelpers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import fedora.server.MultiValueMap;
import fedora.server.journal.JournalException;
import fedora.server.journal.entry.JournalEntryContext;
import fedora.server.journal.helpers.JournalHelper;
import fedora.server.journal.helpers.PasswordCipher;

/**
 * Reads a Context tag from the journal file, and assembles a
 * JournalEntryContext from it.
 * 
 * @author Jim Blake
 */
public class ContextXmlReader
        extends AbstractXmlReader {

    private String passwordType;

    /**
     * Read the context tax and populate a JournalEntryContext object.
     */
    public JournalEntryContext readContext(XMLEventReader reader)
            throws JournalException, XMLStreamException {
        JournalEntryContext context = new JournalEntryContext();

        XMLEvent event = reader.nextTag();
        if (!isStartTagEvent(event, QNAME_TAG_CONTEXT)) {
            throw getNotStartTagException(QNAME_TAG_CONTEXT, event);
        }

        context.setPassword(readContextPassword(reader));
        context.setNoOp(readContextNoOp(reader));
        context.setNow(readContextNow(reader));
        context
                .setEnvironmentAttributes(readMultiMap(reader,
                                                       CONTEXT_MAPNAME_ENVIRONMENT));
        context.setSubjectAttributes(readMultiMap(reader,
                                                  CONTEXT_MAPNAME_SUBJECT));
        context
                .setActionAttributes(readMultiMap(reader,
                                                  CONTEXT_MAPNAME_ACTION));
        context.setResourceAttributes(readMultiMap(reader,
                                                   CONTEXT_MAPNAME_RESOURCE));
        context.setRecoveryAttributes(readMultiMap(reader,
                                                   CONTEXT_MAPNAME_RECOVERY));

        event = reader.nextTag();
        if (!isEndTagEvent(event, QNAME_TAG_CONTEXT)) {
            throw getNotEndTagException(QNAME_TAG_CONTEXT, event);
        }

        decipherPassword(context);

        return context;
    }

    /**
     * Read the context password from XML. Note: While doing this, fetch the
     * password type, and store it to use when deciphering the password. Not the
     * cleanest structure, perhaps, but it will serve for now.
     */
    private String readContextPassword(XMLEventReader reader)
            throws JournalException, XMLStreamException {
        XMLEvent startTag = readStartTag(reader, QNAME_TAG_PASSWORD);
        passwordType =
                getOptionalAttributeValue(startTag.asStartElement(),
                                          QNAME_ATTR_PASSWORD_TYPE);
        return readCharactersUntilEndTag(reader, QNAME_TAG_PASSWORD);
    }

    /**
     * Read the context no-op flag from XML.
     */
    private boolean readContextNoOp(XMLEventReader reader)
            throws XMLStreamException, JournalException {
        readStartTag(reader, QNAME_TAG_NOOP);
        String value = readCharactersUntilEndTag(reader, QNAME_TAG_NOOP);
        return Boolean.valueOf(value).booleanValue();
    }

    /**
     * Read the context date from XML.
     */
    private Date readContextNow(XMLEventReader reader)
            throws XMLStreamException, JournalException {
        readStartTag(reader, QNAME_TAG_NOW);
        String value = readCharactersUntilEndTag(reader, QNAME_TAG_NOW);
        return JournalHelper.parseDate(value);
    }

    /**
     * Read a multi-map, with its nested tags.
     */
    private MultiValueMap readMultiMap(XMLEventReader reader, String mapName)
            throws JournalException, XMLStreamException {
        MultiValueMap map = new MultiValueMap();

        // must start with a multi-map tag
        XMLEvent event = reader.nextTag();
        if (!isStartTagEvent(event, QNAME_TAG_MULTI_VALUE_MAP)) {
            throw getNotStartTagException(QNAME_TAG_MULTI_VALUE_MAP, event);
        }

        // the map name must match the expected name
        String value =
                getRequiredAttributeValue(event.asStartElement(),
                                          QNAME_ATTR_NAME);
        if (!mapName.equals(value)) {
            throw new JournalException("Expecting a '" + mapName
                    + "' multi-map, but found a '" + value
                    + "' multi-map instead");
        }

        // populate the map
        readMultiMapKeys(reader, map);

        return map;
    }

    /**
     * Read through the keys of the multi-map, adding to the map as we go.
     */
    private void readMultiMapKeys(XMLEventReader reader, MultiValueMap map)
            throws XMLStreamException, JournalException {
        while (true) {
            XMLEvent event2 = reader.nextTag();
            if (isStartTagEvent(event2, QNAME_TAG_MULTI_VALUE_MAP_KEY)) {
                // if we find a key tag, get the name
                String key =
                        getRequiredAttributeValue(event2.asStartElement(),
                                                  QNAME_ATTR_NAME);
                // read as many values as we find.
                String[] values = readMultiMapValuesForKey(reader);
                // store in the map
                storeInMultiMap(map, key, values);
            } else if (isEndTagEvent(event2, QNAME_TAG_MULTI_VALUE_MAP)) {
                break;
            } else {
                throw getNotNextMemberOrEndOfGroupException(QNAME_TAG_MULTI_VALUE_MAP,
                                                            QNAME_TAG_MULTI_VALUE_MAP_KEY,
                                                            event2);
            }
        }
    }

    /**
     * Read the list of values for one key of the multi-map.
     */
    private String[] readMultiMapValuesForKey(XMLEventReader reader)
            throws XMLStreamException, JournalException {
        List<String> values = new ArrayList<String>();
        while (true) {
            XMLEvent event = reader.nextTag();
            if (isStartTagEvent(event, QNAME_TAG_MULTI_VALUE_MAP_VALUE)) {
                values
                        .add(readCharactersUntilEndTag(reader,
                                                       QNAME_TAG_MULTI_VALUE_MAP_VALUE));
            } else if (isEndTagEvent(event, QNAME_TAG_MULTI_VALUE_MAP_KEY)) {
                return values.toArray(new String[values.size()]);
            } else {
                throw getNotNextMemberOrEndOfGroupException(QNAME_TAG_MULTI_VALUE_MAP_KEY,
                                                            QNAME_TAG_MULTI_VALUE_MAP_VALUE,
                                                            event);
            }
        }
    }

    /**
     * This method is just to guard against the totally bogus Exception
     * declaration in MultiValueMap.set()
     */
    private void storeInMultiMap(MultiValueMap map, String key, String[] values)
            throws JournalException {
        try {
            map.set(key, values);
        } catch (Exception e) {
            // totally bogus Exception here.
            throw new JournalException(e);
        }
    }

    /**
     * The password as read was not correct. It needs to be deciphered.
     */
    private void decipherPassword(JournalEntryContext context) {
        String key = JournalHelper.formatDate(context.now());
        String passwordCipher = context.getPassword();
        String clearPassword =
                PasswordCipher.decipher(key, passwordCipher, passwordType);
        context.setPassword(clearPassword);
    }

}
