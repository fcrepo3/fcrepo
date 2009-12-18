/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import fedora.server.errors.UnsupportedTranslationException;
import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.DigitalObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for DOTranslatorImpl.
 *
 * @author Chris Wilper
 */
public class TestDOTranslatorImpl {

    private static final String TEST_PID = "test:pid";

    private static final String FORMAT_1 = "format1";

    private static final String FORMAT_2 = "format2";

    private static final String FORMAT_UNKNOWN = "formatUnknown";

    private DOTranslator m_trans;

    //---
    // Setup/Teardown
    //---

    @Before
    public void setUp() {
        Map<String, DOSerializer> serializers =
                new HashMap<String, DOSerializer>();
        serializers.put(FORMAT_1, new MockDOSerializer(FORMAT_1));
        serializers.put(FORMAT_2, new MockDOSerializer(FORMAT_2));
        Map<String, DODeserializer> deserializers =
                new HashMap<String, DODeserializer>();
        deserializers.put(FORMAT_1, new MockDODeserializer(FORMAT_1));
        deserializers.put(FORMAT_2, new MockDODeserializer(FORMAT_2));
        m_trans = new DOTranslatorImpl(serializers, deserializers);
    }

    //---
    // Tests
    //---

    @Test
    public void testDeserializeKnownFormats() {
        DigitalObject obj1 = null;
        DigitalObject obj2 = null;
        try {
            obj1 = doDeserialize(FORMAT_1);
            obj2 = doDeserialize(FORMAT_2);
        } catch (UnsupportedTranslationException e) {
            fail("Deserialization should have succeeded, but threw "
                    + "UnsupportedTranslationException");
        }
        assertEquals(TEST_PID, obj1.getPid());
        assertEquals(FORMAT_1, obj1.getLabel());
        assertEquals(TEST_PID, obj2.getPid());
        assertEquals(FORMAT_2, obj2.getLabel());
    }

    @Test
    public void testDeserializeUnknownFormat() {
        DigitalObject obj = null;
        try {
            obj = doDeserialize(FORMAT_UNKNOWN);
            fail("Deserialization should have failed with "
                    + "UnsupportedTranslationException");
        } catch (UnsupportedTranslationException e) {
            // expected
        }
    }

    @Test
    public void testSerializeKnownFormats() {
        try {
            String[] lines1 = doSerialize(FORMAT_1);
            String[] lines2 = doSerialize(FORMAT_2);
            assertEquals(2, lines1.length);
            assertEquals(FORMAT_1, lines1[0]);
            assertEquals(TEST_PID, lines1[1]);
            assertEquals(2, lines2.length);
            assertEquals(FORMAT_2, lines2[0]);
            assertEquals(TEST_PID, lines2[1]);
        } catch (UnsupportedTranslationException e) {
            fail("Serialization should have succeeded, but threw "
                    + "UnsupportedTranslationException");
        }
    }

    @Test
    public void testSerializeUnknownFormat() {
        try {
            doSerialize(FORMAT_UNKNOWN);
            fail("Serialization should have failed with "
                    + "UnsupportedTranslationException");
        } catch (UnsupportedTranslationException e) {
            // expected
        }
    }

    //---
    // Instance helpers
    //---

    private DigitalObject doDeserialize(String format)
            throws UnsupportedTranslationException {
        DigitalObject obj = new BasicDigitalObject();
        try {
            InputStream in = getInputStream(format + "\n" + TEST_PID + "\n");
            m_trans.deserialize(in, obj, format, "UTF-8", 0);
            return obj;
        } catch (UnsupportedTranslationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Test failure: " + e.getClass().getName());
        }
    }

    private String[] doSerialize(String format)
            throws UnsupportedTranslationException {
        DigitalObject obj = new BasicDigitalObject();
        obj.setPid(TEST_PID);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            m_trans.serialize(obj, out, format, "UTF-8", 0);
            String[] lines = new String(out.toByteArray(), "UTF-8").split("\n");
            for (int i = 0; i < lines.length; i++) {
                lines[i] = lines[i].replaceAll("\r", "");
            }
            return lines;
        } catch (UnsupportedTranslationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Test failure: " + e.getClass().getName());
        }
    }

    //---
    // Static helpers
    //---

    private static InputStream getInputStream(String value) {
        try {
            return new ByteArrayInputStream(value.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException wontHappen) {
            throw new Error(wontHappen);
        }
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestDOTranslatorImpl.class);
    }

}
