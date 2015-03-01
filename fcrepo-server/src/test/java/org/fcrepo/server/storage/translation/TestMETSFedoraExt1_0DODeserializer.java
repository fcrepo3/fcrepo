/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.storage.translation;

import java.io.InputStream;
import java.util.concurrent.Callable;

import org.junit.Test;
import org.fcrepo.server.storage.translation.METSFedoraExt1_0DODeserializer;
import org.fcrepo.server.storage.translation.METSFedoraExt1_0DOSerializer;
import org.fcrepo.server.storage.types.BasicDigitalObject;
import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.utilities.ReadableByteArrayOutputStream;

/**
 * Unit tests for METSFedoraExt1_0DODeserializer.
 *
 * @author Chris Wilper
 */
public class TestMETSFedoraExt1_0DODeserializer
        extends TestMETSFedoraExtDODeserializer {

    public TestMETSFedoraExt1_0DODeserializer() {
        // superclass sets protected fields
        // m_deserializer and m_serializer as given below
        super(new METSFedoraExt1_0DODeserializer(translationUtility()),
              new METSFedoraExt1_0DOSerializer(translationUtility()));
    }

    //---
    // Tests
    //---
    @Test
    public void testConcurrentDeserialization() throws Exception {
        InputStream[] streams = getTestStreams();
        
        Callable<?>[] callables = new Callable[streams.length];
        int i = 0;
        for (InputStream stream: streams) {
            callables[i++] = new DeserializerCallable(m_deserializer, stream);
        }
        runConcurrent(callables); 
    }

    private InputStream[] getTestStreams() throws Exception {
        DODeserializer deser = new FOXML1_1DODeserializer(translationUtility());
        InputStream[] streams = new InputStream[]{
            getTranslatedTestStream("ecm/dataobject1.xml", deser, m_serializer),
            getTranslatedTestStream("ecm/dataobject2.xml", deser, m_serializer),
            getTranslatedTestStream("ecm/dataobject3.xml", deser, m_serializer),
        };
        return streams;
    }
    private InputStream getTranslatedTestStream(String src, DODeserializer deser, DOSerializer ser) throws Exception {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(src);
        DigitalObject obj = new BasicDigitalObject();
        deser.deserialize(in, obj, "UTF-8", DOTranslationUtility.DESERIALIZE_INSTANCE);
        ReadableByteArrayOutputStream out = new ReadableByteArrayOutputStream();
        ser.serialize(obj, out, "UTF-8", DOTranslationUtility.SERIALIZE_EXPORT_ARCHIVE);
        return out.toInputStream();
    }
    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestMETSFedoraExt1_0DODeserializer.class);
    }
}
