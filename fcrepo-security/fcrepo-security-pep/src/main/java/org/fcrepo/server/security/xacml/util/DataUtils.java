
package org.fcrepo.server.security.xacml.util;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import org.w3c.dom.Document;

import org.fcrepo.utilities.ReadableByteArrayOutputStream;
import org.fcrepo.utilities.XmlTransformUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataUtils {

    private static final Logger logger =
            LoggerFactory.getLogger(DataUtils.class);

    public static void saveDocument(String filename, byte[] document)
            throws Exception {
        try {
            DocumentBuilderFactory documentBuilderFactory =
                    DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder =
                    XmlTransformUtility.borrowDocumentBuilder();

            Document doc = null;
            try {
                doc = docBuilder.parse(new ByteArrayInputStream(document));
            } finally {
                XmlTransformUtility.returnDocumentBuilder(docBuilder);
            }
            File file = new File(filename.trim());
            String data = format(doc);
            PrintWriter writer = new PrintWriter(file, "UTF-8");
            writer.print(data);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            logger.error("Unable to save file: " + filename, e);
            throw new Exception("Unable to save file: " + filename, e);
        }
    }

    public static void saveDocument(String filename, Document doc) {
        try {
            File file = new File(filename.trim());
            String data = format(doc);
            PrintWriter writer = new PrintWriter(file, "UTF-8");
            writer.print(data);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            logger.error("Unable to save file: " + filename, e);
        }
    }

    public static String format(Document doc) throws Exception {
        OutputFormat format = new OutputFormat(doc);
        format.setEncoding("UTF-8");
        format.setIndenting(true);
        format.setIndent(2);
        format.setOmitXMLDeclaration(true);

        ReadableByteArrayOutputStream out = new ReadableByteArrayOutputStream(8192);
        Writer output = new BufferedWriter(new OutputStreamWriter(out));

        XMLSerializer serializer = new XMLSerializer(output, format);
        serializer.serialize(doc);
        output.close();

        return out.getString(Charset.forName("UTF-8"));
    }
}
