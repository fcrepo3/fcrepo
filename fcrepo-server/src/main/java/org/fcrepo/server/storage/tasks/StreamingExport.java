package org.fcrepo.server.storage.tasks;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.translation.DOTranslator;
import org.fcrepo.server.storage.types.DigitalObject;


public class StreamingExport implements StreamingOutput {
    final DOTranslator m_translator;
    final DigitalObject m_object;
    final String m_exportFormat;
    final String m_encoding;
    final int m_transContext;
    public StreamingExport(DOTranslator translator, DigitalObject object,
            String exportFormat, String encoding, int transContext) {
        m_translator = translator;
        m_object = object;
        m_exportFormat = exportFormat;
        m_encoding = encoding;
        m_transContext = transContext;
    }

    @Override
    public void write(OutputStream output) throws IOException,
            WebApplicationException {
        try {
            m_translator.serialize(m_object, output,
                m_exportFormat, m_encoding, m_transContext);
        } catch (ServerException e) {
            throw new WebApplicationException(e);
        }
    }
}
