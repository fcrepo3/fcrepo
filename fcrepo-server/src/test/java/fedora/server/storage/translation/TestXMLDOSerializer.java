/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.custommonkey.xmlunit.XMLUnit;

import org.junit.Test;

import org.w3c.dom.Document;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.StreamIOException;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.DigitalObject;

import static fedora.common.Models.FEDORA_OBJECT_3_0;
import static fedora.common.Models.SERVICE_DEFINITION_3_0;
import static fedora.common.Models.SERVICE_DEPLOYMENT_3_0;

import static fedora.server.storage.translation.DOTranslationUtility.SERIALIZE_EXPORT_ARCHIVE;
import static fedora.server.storage.translation.DOTranslationUtility.SERIALIZE_EXPORT_MIGRATE;
import static fedora.server.storage.translation.DOTranslationUtility.SERIALIZE_EXPORT_PUBLIC;
import static fedora.server.storage.translation.DOTranslationUtility.SERIALIZE_STORAGE_INTERNAL;

/**
 * Common unit tests and utility methods for XML-based serializers.
 *
 * @author Chris Wilper
 */
public abstract class TestXMLDOSerializer
        extends TranslationTest {

    /** The serializer to test. */
    protected final DOSerializer m_serializer;

    TestXMLDOSerializer(DOSerializer serializer) {
        m_serializer = serializer;
    }

    //---
    // Tests
    //---

    @Test
    public void testSerializeSimpleDataObject() {
        doSerializeAllOrFail(createTestObject(FEDORA_OBJECT_3_0));
    }

    @Test
    public void testSerializeSimpleSDepObject() {
        doSerializeAllOrFail(createTestObject(SERVICE_DEPLOYMENT_3_0));
    }

    @Test
    public void testSerializeSimpleSDefObject() {
        doSerializeAllOrFail(createTestObject(SERVICE_DEFINITION_3_0));
    }

    @Test
    public void testInlineXMLEncoding() throws Exception {
        final String TAG = "test";
        final String OPEN = "<" + TAG + ">";
        final String CLOSE = "</" + TAG + ">";
        char[] unicodeContent = new char[1365];

        StringBuilder payload =
                new StringBuilder(unicodeContent.length + OPEN.length()
                        + CLOSE.length());

        for (int i = 0; i < unicodeContent.length; i++) {
            unicodeContent[i] = '\u0e57'; // Thai digit 7
        }

        payload.append(OPEN);
        payload.append(unicodeContent);
        payload.append(CLOSE);

        DigitalObject obj = createTestObject(FEDORA_OBJECT_3_0);
        final String dsID1 = "DS1";

        /* Populate the object with a test datastream and serialize */
        DatastreamXMLMetadata ds1 = createXDatastream(dsID1);
        ds1.xmlContent = payload.toString().getBytes("UTF-8");
        obj.addDatastreamVersion(ds1, true);

        String serializedContent =
                doSerialize(obj, SERIALIZE_STORAGE_INTERNAL)
                        .getElementsByTagName(TAG).item(0).getFirstChild()
                        .getNodeValue();

        assertTrue("UTF-8 chars are not serialized properly!",
                   new String(unicodeContent).equals(serializedContent));
    }

    //---
    // Instance helpers
    //---

    protected void doSerializeAllOrFail(DigitalObject obj) {
        doSerializeOrFail(obj, SERIALIZE_EXPORT_ARCHIVE);
        doSerializeOrFail(obj, SERIALIZE_EXPORT_MIGRATE);
        doSerializeOrFail(obj, SERIALIZE_EXPORT_PUBLIC);
        doSerializeOrFail(obj, SERIALIZE_STORAGE_INTERNAL);
    }

    protected Document doSerializeOrFail(DigitalObject obj) {
        return doSerializeOrFail(obj, SERIALIZE_STORAGE_INTERNAL);
    }

    /**
     * Serialize the object, failing the test if an exception is thrown.
     */
    protected Document doSerializeOrFail(DigitalObject obj, int transContext) {
        Document result = null;
        try {
            result = doSerialize(obj, transContext);
        } catch (ObjectIntegrityException e) {
            e.printStackTrace();
            fail("Serializer threw ObjectIntegrityException");
        } catch (SAXException e) {
            e.printStackTrace();
            fail("Serialized XML was not well-formed");
        }
        return result;
    }

    /**
     * Serialize the object, failing the test only if obviously incorrect
     * behavior occurs.
     *
     * @throws ObjectIntegrityException
     *         if the serializer fails due to same.
     * @throws SAXException
     *         if the result XML is not well-formed.
     */
    protected Document doSerialize(DigitalObject obj, int transContext)
            throws ObjectIntegrityException, SAXException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            m_serializer.serialize(obj, out, "UTF-8", transContext);
        } catch (StreamIOException e) {
            fail("Serializer threw StreamIOException");
        } catch (UnsupportedEncodingException e) {
            fail("Serializer doesn't support UTF-8!?");
        }

        InputStream in = new ByteArrayInputStream(out.toByteArray());
        try {
            return XMLUnit.buildControlDocument(new InputSource(in));
        } catch (SAXException notWellFormed) {
            throw notWellFormed;
        } catch (IOException wontHappen) {
            throw new Error(wontHappen);
        }
    }

}
