/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.journal.xmlhelpers;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javanet.staxutils.IndentingXMLEventWriter;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import junit.framework.TestCase;

import org.fcrepo.server.MultiValueMap;
import org.fcrepo.server.journal.entry.JournalEntryContext;
import org.fcrepo.server.journal.helpers.JournalHelper;


public class TestContextXmlWriterAndReader
        extends TestCase {

    private StringWriter xmlStringWriter;

    private XMLEventWriter xmlWriter;

    public TestContextXmlWriterAndReader(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        xmlStringWriter = new StringWriter();

        xmlWriter = createXmlWriter();
    }

    public void testBasicWriteAndRead() throws Exception {
        JournalEntryContext context1 = new JournalEntryContext();
        context1.setPassword("SuperSecret");
        context1.setNoOp(true);
        context1.setEnvironmentAttributes(createMap(new URI("envAttr"), "envValue"));
        context1.setSubjectAttributes(createMap(new String[][] {
                {"subAttr1", "subValue1"}, {"subAttr2", "subValue2"}}));
        context1.setActionAttributes(createMap(new URI("ActionAttribute"), "ActionValue"));
        context1.setRecoveryAttributes(createMap(
                new URI("recoveryAttribute"),
                new String[] {"recoveryValue", "recoveryValue2"}));

        ContextXmlWriter contextWriter = new ContextXmlWriter();
        contextWriter.writeContext(context1, xmlWriter);

        String xmlString = xmlStringWriter.toString();
        XMLEventReader xmlReader = createXmlReaderAndPosition(xmlString);

        ContextXmlReader contextReader = new ContextXmlReader();
        JournalEntryContext context2 = contextReader.readContext(xmlReader);

        assertContextsAreEqual(context1, context2);

    }

    public void testReadObsoletePasswordCipherType() throws Exception {
        JournalEntryContext context1 = new JournalEntryContext();
        context1.setPassword("ShoopShoop");
        context1.setNoOp(true);

        String xmlString =
                "<context>\n" + "  <password>ShoopShoop</password>\n"
                        + "  <noOp>true</noOp>\n" + "  <now>"
                        + JournalHelper.formatDate(context1.now()) + "</now>\n"
                        + "  <multimap name=\"environment\"></multimap>\n"
                        + "  <multimap name=\"subject\"></multimap>\n"
                        + "  <multimap name=\"action\"></multimap>\n"
                        + "  <multimap name=\"resource\"></multimap>\n"
                        + "  <multimap name=\"recovery\"></multimap>\n"
                        + "</context>\n";

        XMLEventReader xmlReader = createXmlReaderAndPosition(xmlString);

        ContextXmlReader contextReader = new ContextXmlReader();
        JournalEntryContext context2 = contextReader.readContext(xmlReader);

        assertContextsAreEqual(context1, context2);
    }

    private XMLEventReader createXmlReaderAndPosition(String xmlString)
            throws FactoryConfigurationError, XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader xmlReader =
                factory.createXMLEventReader(new StringReader(xmlString));
        advanceToContext(xmlReader);
        return xmlReader;
    }

    private XMLEventWriter createXmlWriter() throws FactoryConfigurationError,
            XMLStreamException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        return new IndentingXMLEventWriter(factory
                .createXMLEventWriter(xmlStringWriter));
    }

    private <T> MultiValueMap<T> createMap(T key, String value) {
        return createMap(key, new String[]{value});
    }

    private <T> MultiValueMap<T> createMap(T key, String[] value) {
        MultiValueMap<T> map = new MultiValueMap<T>();
        map.set(key, value);
        return map;
    }

    private MultiValueMap<String> createMap(String[][] key_value) {
        MultiValueMap<String> map = new MultiValueMap<String>();
        for (String[] pair: key_value){
            map.set(pair[0], pair[1]);
        }
        return map;
    }


    private void advanceToContext(XMLEventReader reader)
            throws XMLStreamException {
        XMLEvent event = reader.peek();
        if (event.isStartDocument()) {
            reader.nextEvent();
        } else {
            fail("Document does not begin with a 'StartDocument' event.");
        }
    }

    private void assertContextsAreEqual(JournalEntryContext context1,
                                        JournalEntryContext context2) {
        assertEquals(context1.getPassword(), context2.getPassword());
        assertEquals(context1.getNoOp(), context2.getNoOp());
        assertEquals(context1.now(), context2.now());
        assertEqualMultiMaps(context1.getEnvironmentAttributes(), context2
                .getEnvironmentAttributes());
        assertEqualMultiMaps(context1.getSubjectAttributes(), context2
                .getSubjectAttributes());
        assertEqualMultiMaps(context1.getActionAttributes(), context2
                .getActionAttributes());
        assertEqualMultiMaps(context1.getResourceAttributes(), context2
                .getResourceAttributes());
        assertEqualMultiMaps(context1.getRecoveryAttributes(), context2
                .getRecoveryAttributes());
    }

    private <T> void assertEqualMultiMaps(MultiValueMap<T> map1, MultiValueMap<T> map2) {
        Iterator<T> names1 = map1.names();
        Iterator<T> names2 = map2.names();
        while (names1.hasNext() && names2.hasNext()) {
            T name1 = names1.next();
            T name2 = names2.next();
            assertEquals(name1, name2);
            String[] values1 = map1.getStringArray(name1);
            String[] values2 = map1.getStringArray(name2);
            assertEqualSets(new HashSet<String>(Arrays.asList(values1)),
                            new HashSet<String>(Arrays.asList(values2)));
        }
    }

    private void assertEqualSets(Set<String> set1, Set<String> set2) {
        assertEquals(set1, set2);
    }

}
