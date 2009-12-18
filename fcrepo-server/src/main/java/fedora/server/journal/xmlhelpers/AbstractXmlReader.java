/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.xmlhelpers;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import fedora.server.journal.JournalConstants;
import fedora.server.journal.JournalException;

/**
 * An abstract base class that provides some useful methods for the XML reader
 * classes.
 * 
 * @author Jim Blake
 */
public abstract class AbstractXmlReader
        implements JournalConstants {

    /**
     * Advance past any white space. Leave the reader positioned before the next
     * tag (or non-white-space event).
     */
    protected void advancePastWhitespace(XMLEventReader reader)
            throws XMLStreamException {
        XMLEvent next = reader.peek();
        while (next.isCharacters() && next.asCharacters().isWhiteSpace()) {
            reader.nextEvent();
            next = reader.peek();
        }
    }

    /**
     * Read the next event and complain if it is not the Start Tag that we
     * expected.
     */
    protected XMLEvent readStartTag(XMLEventReader reader, QName tagName)
            throws XMLStreamException, JournalException {
        XMLEvent event = reader.nextTag();
        if (!isStartTagEvent(event, tagName)) {
            throw getNotStartTagException(tagName, event);
        }
        return event;
    }

    /**
     * Test an event to see whether it is an start tag with the expected name.
     */
    protected boolean isStartTagEvent(XMLEvent event, QName tagName) {
        return event.isStartElement()
                && event.asStartElement().getName().equals(tagName);
    }

    /**
     * Test an event to see whether it is an end tag with the expected name.
     */
    protected boolean isEndTagEvent(XMLEvent event, QName tagName) {
        return event.isEndElement()
                && event.asEndElement().getName().equals(tagName);
    }

    /**
     * Get the value of a given attribute from this start tag, or complain if
     * it's not there.
     */
    protected String getRequiredAttributeValue(StartElement start,
                                               QName attributeName)
            throws JournalException {
        Attribute mapNameAttribute = start.getAttributeByName(attributeName);
        if (mapNameAttribute == null) {
            throw new JournalException("Start tag '" + start
                    + "' must contain a '" + attributeName + "' attribute.");
        }
        return mapNameAttribute.getValue();
    }

    /**
     * Get the value of a given attribute from this start tag, or null if the
     * attribute is not there.
     */
    protected String getOptionalAttributeValue(StartElement start,
                                               QName attributeName) {
        Attribute mapNameAttribute = start.getAttributeByName(attributeName);
        if (mapNameAttribute == null) {
            return null;
        } else {
            return mapNameAttribute.getValue();
        }
    }

    /**
     * Loop through a series of character events, accumulating the data into a
     * String. The character events should be terminated by an EndTagEvent with
     * the expected tag name.
     */
    protected String readCharactersUntilEndTag(XMLEventReader reader,
                                               QName tagName)
            throws XMLStreamException, JournalException {
        StringBuffer stringValue = new StringBuffer();
        while (true) {
            XMLEvent event = reader.nextEvent();
            if (event.isCharacters()) {
                stringValue.append(event.asCharacters().getData());
            } else if (isEndTagEvent(event, tagName)) {
                break;
            } else {
                throw getNotCharactersException(tagName, event);
            }
        }
        return stringValue.toString();
    }

    /**
     * Complain when we were expecting a start tag, and didn't find it.
     */
    protected JournalException getNotStartTagException(QName tagName,
                                                       XMLEvent event) {
        return new JournalException("Expecting '" + tagName
                + "' start tag, but event was '" + event + "'");

    }

    /**
     * Complain when we were expecting a end tag, and didn't find it.
     */
    protected JournalException getNotEndTagException(QName tagName,
                                                     XMLEvent event) {
        return new JournalException("Expecting '" + tagName
                + "' end tag, but event was '" + event + "'");

    }

    /**
     * If we encounter an unexpected event when reading the journal file, create
     * an exception with all of the pertinent information.
     */
    protected JournalException getNotCharactersException(QName tagName,
                                                         XMLEvent event) {
        return new JournalException("Expecting characters or '" + tagName
                + "' end tag, but event was '" + event + "'");
    }

    /**
     * While traversing a group of member tags, we expected either the start of
     * another member tag, or the end of the group.
     */
    protected JournalException getNotNextMemberOrEndOfGroupException(QName groupTagName,
                                                                     QName memberTagName,
                                                                     XMLEvent event) {
        return new JournalException("Expecting either '" + memberTagName
                + "' start tag, or '" + groupTagName
                + "' end tag, but event was '" + event + "'");
    }
}
