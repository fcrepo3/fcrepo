package org.fcrepo.server.storage.translation;

import java.io.InputStream;
import java.util.concurrent.Callable;

import org.fcrepo.server.storage.types.BasicDigitalObject;
import org.fcrepo.server.storage.types.DigitalObject;


public class DeserializerCallable implements Callable<DigitalObject> {
    final DODeserializer m_deser;
    final InputStream m_src;
    DeserializerCallable(DODeserializer deser, InputStream src) {
        m_deser = deser;
        m_src = src;
    }
    @Override
    public DigitalObject call() throws Exception {
        DigitalObject obj = new BasicDigitalObject();
        m_deser.deserialize(m_src, obj, "UTF-8", DOTranslationUtility.DESERIALIZE_INSTANCE);
        return obj;
    }
}
