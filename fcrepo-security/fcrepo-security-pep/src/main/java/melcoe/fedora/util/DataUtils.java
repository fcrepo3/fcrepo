
package melcoe.fedora.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class DataUtils {

    private static final Logger log = Logger.getLogger(DataUtils.class);

    public static void saveDocument(String filename, byte[] document)
            throws Exception {
        try {
            DocumentBuilderFactory documentBuilderFactory =
                    DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder =
                    documentBuilderFactory.newDocumentBuilder();

            Document doc = docBuilder.parse(new ByteArrayInputStream(document));

            File file = new File(filename.trim());
            String data = format(doc);
            PrintWriter writer = new PrintWriter(file, "UTF-8");
            writer.print(data);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            log.fatal("Unable to save file: " + filename, e);
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
            log.fatal("Unable to save file: " + filename, e);
        }
    }

    public static String format(Document doc) throws Exception {
        OutputFormat format = new OutputFormat(doc);
        format.setEncoding("UTF-8");
        format.setIndenting(true);
        format.setIndent(2);
        format.setOmitXMLDeclaration(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer output = new OutputStreamWriter(out);

        XMLSerializer serializer = new XMLSerializer(output, format);
        serializer.serialize(doc);

        return new String(out.toByteArray(), "UTF-8");
    }
}
