/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage.types;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilder;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.utilities.ReadableByteArrayOutputStream;
import org.fcrepo.utilities.ReadableCharArrayWriter;
import org.fcrepo.utilities.XmlTransformUtility;
import org.fcrepo.utilities.xml.XercesXmlSerializers;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @author Sandy Payette
 */
public class DatastreamXMLMetadata
        extends Datastream {

    // techMD (technical metadata),
    // sourceMD (analog/digital source metadata),
    // rightsMD (intellectual property rights metadata),
    // digiprovMD (digital provenance metadata).
    // dmdSec (descriptive metadata).

    /** Technical XML metadata */
    public final static int TECHNICAL = 1;

    /** Source XML metatdata */
    public final static int SOURCE = 2;

    /** Rights XML metatdata */
    public final static int RIGHTS = 3;

    /** Digital provenance XML metadata */
    public final static int DIGIPROV = 4;

    /** Descriptive XML metadata */
    public final static int DESCRIPTIVE = 5;

    // FIXME:xml datastream contents are held in memory...this could be expensive.
    public byte[] xmlContent;

    /**
     * The class of XML metadata (TECHNICAL, SOURCE, RIGHTS, DIGIPROV, or
     * DESCRIPTIVE)
     */
    public int DSMDClass = 0;

    private final String m_encoding;

    public DatastreamXMLMetadata() {
        m_encoding = "UTF-8";
    }

    public DatastreamXMLMetadata(String encoding) {
        m_encoding = encoding;
    }

    @Override
    public Datastream copy() {
        DatastreamXMLMetadata ds = new DatastreamXMLMetadata(m_encoding);
        copy(ds);
        if (xmlContent != null) {
            ds.xmlContent = new byte[xmlContent.length];
            System.arraycopy(xmlContent, 0, ds.xmlContent, 0, xmlContent.length);
        }
        ds.DSMDClass = DSMDClass;
        return ds;
    }

    @Override
    public InputStream getContentStream() {
        return new ByteArrayInputStream(xmlContent);
    }

    @Override
    public InputStream getContentStream(Context context) {
        return new ByteArrayInputStream(xmlContent);
    }

    @Override
    public InputStream getContentStreamForChecksum() {
        BufferedReader br;
        try {
            ReadableCharArrayWriter out =
                new ReadableCharArrayWriter(xmlContent.length + (xmlContent.length /4));
            DocumentBuilder builder = XmlTransformUtility.borrowDocumentBuilder();
            try {
                Document doc = builder.parse(new ByteArrayInputStream(xmlContent));
                XercesXmlSerializers.writeXmlNoSpace(doc, m_encoding, out);
                out.close();
            } finally {
                XmlTransformUtility.returnDocumentBuilder(builder);
            }

            br =
                    new BufferedReader(out.toReader());
            String line;
            ReadableByteArrayOutputStream bytes =
                    new ReadableByteArrayOutputStream(out.length());
            PrintWriter outStream = new PrintWriter(
                    new OutputStreamWriter(bytes, Charset.forName(m_encoding)));
            while ((line = br.readLine()) != null) {
                line = line.trim();
                outStream.append(line);
            }
            outStream.close();
            return bytes.toInputStream();
        } catch (UnsupportedEncodingException e) {
            return getContentStream();
        } catch (IOException e) {
            return getContentStream();
        } catch (SAXException e) {
            return getContentStream();
        }
    }

    public InputStream getContentStreamAsDocument()
            throws UnsupportedEncodingException {
        // *with* the <?xml version="1.0" encoding="m_encoding" ?> line
        String firstLine =
                "<?xml version=\"1.0\" encoding=\"" + m_encoding + "\" ?>\n";
        byte[] firstLineBytes = firstLine.getBytes(m_encoding);
        return new SequenceInputStream(new ByteArrayInputStream(firstLineBytes),
                new ByteArrayInputStream(xmlContent));
    }

    @Override
    public long getContentSize(Context ctx) throws StreamIOException {
        return xmlContent.length;
    }

    @Override
    public boolean isRepositoryManaged() {
        return true;
    }
}