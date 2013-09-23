
package org.fcrepo.server.security.xacml.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.fcrepo.utilities.ReadableCharArrayWriter;
import org.fcrepo.utilities.XmlTransformUtility;
import org.fcrepo.utilities.xml.XercesXmlSerializers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

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
            PrintWriter writer = new PrintWriter(file, "UTF-8");
            format(doc, writer);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            logger.error("Unable to save file: " + filename, e);
        }
    }

    public static String format(Document doc) throws Exception {

        ReadableCharArrayWriter out = new ReadableCharArrayWriter(8192);
        format(doc, out);
        out.close();

        return out.getString();
    }
    
    private static void format(Document doc, Writer out) throws Exception {
        XercesXmlSerializers.writePrettyPrint(doc, out);
    }
}
