/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.xmlhelpers;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;

import fedora.server.journal.JournalConstants;

/**
 * An abstract base class that provides some useful methods for the XML writer
 * classes.
 * 
 * @author Jim Blake
 */
public class AbstractXmlWriter
        implements JournalConstants {

    private final XMLEventFactory factory = XMLEventFactory.newInstance();

    protected void putStartDocument(XMLEventWriter writer)
            throws XMLStreamException {
        writer.add(factory.createStartDocument(DOCUMENT_ENCODING,
                                               DOCUMENT_VERSION));
    }

    protected void putStartTag(XMLEventWriter writer, QName tagName)
            throws XMLStreamException {
        writer.add(factory.createStartElement(tagName, null, null));
    }

    protected void putAttribute(XMLEventWriter writer, QName name, String value)
            throws XMLStreamException {
        writer.add(factory.createAttribute(name, value));
    }

    protected void putAttributeIfNotNull(XMLEventWriter writer,
                                         QName attributeName,
                                         String value)
            throws XMLStreamException {
        if (value != null) {
            putAttribute(writer, attributeName, value);
        }
    }

    protected void putCharacters(XMLEventWriter writer, String chars)
            throws XMLStreamException {
        writer.add(factory.createCharacters(chars));
    }

    protected void putEndTag(XMLEventWriter writer, QName tagName)
            throws XMLStreamException {
        writer.add(factory.createEndElement(tagName, null));
    }

    protected void putEndDocument(XMLEventWriter writer)
            throws XMLStreamException {
        writer.add(factory.createEndDocument());
    }

}
