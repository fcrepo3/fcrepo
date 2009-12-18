/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.types;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

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
            for (int i = 0; i < xmlContent.length; i++) {
                ds.xmlContent[i] = xmlContent[i];
            }
        }
        ds.DSMDClass = DSMDClass;
        return ds;
    }

    @Override
    public InputStream getContentStream() {
        return new ByteArrayInputStream(xmlContent);
    }

    @Override
    public InputStream getContentStreamForChecksum() {
        BufferedReader br;
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            OutputFormat fmt = new OutputFormat("XML", "UTF-8", false);
            fmt.setIndent(0);
            fmt.setLineWidth(0);
            fmt.setPreserveSpace(false);
            XMLSerializer ser = new XMLSerializer(outStream, fmt);
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent));
            ser.serialize(doc);

            br =
                    new BufferedReader(new InputStreamReader(new ByteArrayInputStream(outStream
                                                                     .toByteArray()),
                                                             m_encoding));
            String line;
            StringBuffer buf = new StringBuffer();
            while ((line = br.readLine()) != null) {
                line = line.trim();
                buf = buf.append(line);
            }
            String bufStr = buf.toString();
            return new ByteArrayInputStream(bufStr.getBytes(m_encoding));
        } catch (UnsupportedEncodingException e) {
            return getContentStream();
        } catch (IOException e) {
            return getContentStream();
        } catch (ParserConfigurationException e) {
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
        byte[] out = new byte[xmlContent.length + firstLineBytes.length];
        for (int i = 0; i < firstLineBytes.length; i++) {
            out[i] = firstLineBytes[i];
        }
        for (int i = firstLineBytes.length; i < firstLineBytes.length
                + xmlContent.length; i++) {
            out[i] = xmlContent[i - firstLineBytes.length];
        }
        return new ByteArrayInputStream(out);
    }
}
