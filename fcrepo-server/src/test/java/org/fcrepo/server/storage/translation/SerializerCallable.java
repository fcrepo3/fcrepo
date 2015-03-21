package org.fcrepo.server.storage.translation;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Callable;

import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.utilities.ReadableByteArrayOutputStream;


public class SerializerCallable implements Callable<ByteArrayInputStream> {
    final DOSerializer m_ser;
    final DigitalObject m_obj;
    public SerializerCallable(DOSerializer ser, DigitalObject obj){
        m_ser = ser;
        m_obj = obj;
    }
    @Override
    public ByteArrayInputStream call() throws Exception {
        ReadableByteArrayOutputStream out = new ReadableByteArrayOutputStream();
        m_ser.serialize(m_obj, out, "UTF-8", DOTranslationUtility.SERIALIZE_EXPORT_ARCHIVE);
        return out.toInputStream();
    }
}
