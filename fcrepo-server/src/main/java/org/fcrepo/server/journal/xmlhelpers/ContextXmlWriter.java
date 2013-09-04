/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.journal.xmlhelpers;

import java.util.Iterator;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;

import org.fcrepo.server.Context;
import org.fcrepo.server.MultiValueMap;
import org.fcrepo.server.journal.entry.JournalEntryContext;
import org.fcrepo.server.journal.helpers.JournalHelper;
import org.fcrepo.server.journal.helpers.PasswordCipher;


/**
 * Write an entire Context object to the Journal file.
 * 
 * @author Jim Blake
 */
public class ContextXmlWriter
        extends AbstractXmlWriter {

    public void writeContext(JournalEntryContext context, XMLEventWriter writer)
            throws XMLStreamException {
        putStartTag(writer, QNAME_TAG_CONTEXT);

        writeContextPassword(context, writer);
        writeContextNoOp(context, writer);
        writeContextNow(context, writer);

        writeMultiMap(writer, CONTEXT_MAPNAME_ENVIRONMENT, context
                .getEnvironmentAttributes());
        writeMultiMap(writer, CONTEXT_MAPNAME_SUBJECT, context
                .getSubjectAttributes());
        writeMultiMap(writer, CONTEXT_MAPNAME_ACTION, context
                .getActionAttributes());
        writeMultiMap(writer, CONTEXT_MAPNAME_RESOURCE, context
                .getResourceAttributes());
        writeMultiMap(writer, CONTEXT_MAPNAME_RECOVERY, context
                .getRecoveryAttributes());

        putEndTag(writer, QNAME_TAG_CONTEXT);
    }

    private void writeContextPassword(Context context, XMLEventWriter writer)
            throws XMLStreamException {
        String password = context.getPassword();
        if (password == null) {
            password = "";
        }

        putStartTag(writer, QNAME_TAG_PASSWORD);
        putAttribute(writer, QNAME_ATTR_PASSWORD_TYPE, PASSWORD_CIPHER_TYPE);
        putCharacters(writer, encipherPassword(context, password));
        putEndTag(writer, QNAME_TAG_PASSWORD);
    }

    private String encipherPassword(Context context, String password) {
        String key = JournalHelper.formatDate(context.now());
        return PasswordCipher.encipher(key, password);
    }

    private void writeContextNoOp(Context context, XMLEventWriter writer)
            throws XMLStreamException {
        putStartTag(writer, QNAME_TAG_NOOP);
        putCharacters(writer, String.valueOf(context.getNoOp()));
        putEndTag(writer, QNAME_TAG_NOOP);
    }

    private void writeContextNow(Context context, XMLEventWriter writer)
            throws XMLStreamException {
        putStartTag(writer, QNAME_TAG_NOW);
        putCharacters(writer, JournalHelper.formatDate(context.now()));
        putEndTag(writer, QNAME_TAG_NOW);
    }

    private <T> void writeMultiMap(XMLEventWriter writer,
                               String mapName,
                               MultiValueMap<T> map) throws XMLStreamException {
        putStartTag(writer, QNAME_TAG_MULTI_VALUE_MAP);
        putAttribute(writer, QNAME_ATTR_NAME, mapName);
        for (Iterator<T> attributes = map.names(); attributes.hasNext();) {
            T attribute = attributes.next();
            putStartTag(writer, QNAME_TAG_MULTI_VALUE_MAP_KEY);
            putAttribute(writer, QNAME_ATTR_NAME, attribute.toString());
            String[] values = map.getStringArray(attribute);
            for (String element : values) {
                putStartTag(writer, QNAME_TAG_MULTI_VALUE_MAP_VALUE);
                putCharacters(writer, element);
                putEndTag(writer, QNAME_TAG_MULTI_VALUE_MAP_VALUE);
            }
            putEndTag(writer, QNAME_TAG_MULTI_VALUE_MAP_KEY);
        }
        putEndTag(writer, QNAME_TAG_MULTI_VALUE_MAP);
    }

}
