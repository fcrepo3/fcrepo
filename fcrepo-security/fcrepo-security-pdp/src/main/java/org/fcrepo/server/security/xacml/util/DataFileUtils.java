
package org.fcrepo.server.security.xacml.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.fcrepo.utilities.ReadableByteArrayOutputStream;
import org.fcrepo.utilities.ReadableCharArrayWriter;
import org.fcrepo.utilities.XmlTransformUtility;
import org.fcrepo.utilities.xml.ProprietaryXmlSerializers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class DataFileUtils {

    private static final Logger logger =
            LoggerFactory.getLogger(DataFileUtils.class);

    public static Document getDocumentFromFile(File file) throws Exception {
        byte[] document = loadFile(file);
        DocumentBuilder docBuilder =
                XmlTransformUtility.borrowDocumentBuilder();

        Document doc = null;
        try {
            doc = docBuilder.parse(new ByteArrayInputStream(document));
        } finally {
            XmlTransformUtility.returnDocumentBuilder(docBuilder);
        }

        return doc;
    }

    public static byte[] loadFile(String filename) throws Exception {
        File file = new File(filename.trim());
        return loadFile(file);
    }

    public static byte[] loadFile(File file) throws Exception {
        if (!file.exists() || !file.canRead()) {
            String message = "Cannot read file: " + file.getCanonicalPath();
            logger.error(message);
            throw new Exception(message);
        }

        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        int len = 0;
        byte[] buf = new byte[1024];
        while ((len = fis.read(buf)) >= 0) {
            data.write(buf, 0, len);
        }

        IOUtils.closeQuietly(fis);

        return data.toByteArray();
    }

    public static void saveDocument(String filename, byte[] document)
            throws Exception {
        try {
            DocumentBuilder docBuilder =
                    XmlTransformUtility.borrowDocumentBuilder();

            Document doc = null;
            try {
                doc = docBuilder.parse(new ByteArrayInputStream(document));
            } finally {
                XmlTransformUtility.returnDocumentBuilder(docBuilder);
            }
            saveDocument(filename, doc);
        } catch (Exception e) {
            String message = "Unable to save file: " + filename;
            logger.error(message,e);
            throw new Exception(message, e);
        }
    }

    public static void saveDocument(String filename, Document doc)
            throws Exception {
        try {
            File file = new File(filename.trim());
            String data = format(doc);
            PrintWriter writer = new PrintWriter(file, "UTF-8");
            writer.print(data);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            String message = "Unable to save file: " + filename;
            logger.error(message,e);
            throw new Exception(message, e);
        }
    }

    public static String format(Document doc) {
        try {
            ReadableCharArrayWriter out = new ReadableCharArrayWriter();
            Writer output = new BufferedWriter(out);

            ProprietaryXmlSerializers.writePrettyPrint(doc, output);
            output.close();

            return out.getString();
        } catch (Exception e) {
            logger.error("Failed to format document.", e);
        }

        return null;
    }

    public static String format(byte[] document) throws Exception {
        DocumentBuilder builder = XmlTransformUtility.borrowDocumentBuilder();

        Document doc = null;
        try {
            doc = builder.parse(new ByteArrayInputStream(document));
        } finally {
            XmlTransformUtility.returnDocumentBuilder(builder);
        }

        return format(doc);
    }

    public static byte[] fedoraXMLHashFormat(byte[] data) throws Exception {
        ReadableCharArrayWriter writer = new ReadableCharArrayWriter();

        DocumentBuilder builder = XmlTransformUtility.borrowDocumentBuilder();
        try {
            Document doc = builder.parse(new ByteArrayInputStream(data));
            ProprietaryXmlSerializers.writeXmlNoSpace(doc, "UTF-8", writer);
            writer.close();
        } finally {
            XmlTransformUtility.returnDocumentBuilder(builder);
        }

        BufferedReader br =
                new BufferedReader(writer.toReader());
        String line = null;
        ReadableByteArrayOutputStream outStream = new ReadableByteArrayOutputStream();
        OutputStreamWriter sb = new OutputStreamWriter(outStream, "UTF-8");
        while ((line = br.readLine()) != null) {
            line = line.trim();
            sb.write(line);
        }
        sb.close();

        return outStream.toByteArray();
    }

    public static String getHash(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hash = digest.digest(data);

        String hexHash = byte2hex(hash);

        return hexHash;
    }

    /**
     * Converts a hash into its hexadecimal string representation.
     *
     * @param bytes
     *        the byte array to convert
     * @return the hexadecimal string representation
     */
    private static String byte2hex(byte[] bytes) {
        char[] hexChars =
                {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b',
                        'c', 'd', 'e', 'f'};

        StringBuffer sb = new StringBuffer();
        for (byte b : bytes) {
            sb.append(hexChars[b >> 4 & 0xf]);
            sb.append(hexChars[b & 0xf]);
        }

        return new String(sb);
    }
}
