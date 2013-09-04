/*
 * File: DataUtils.java
 *
 * Copyright 2009 Muradora
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.fcrepo.server.security.jaas.util;

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

import org.fcrepo.utilities.ReadableByteArrayOutputStream;
import org.fcrepo.utilities.XmlTransformUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class DataUtils {

    private static Logger logger = LoggerFactory.getLogger(DataUtils.class);

    public static Document getDocumentFromFile(File file) throws Exception {
        byte[] document = loadFile(file);
        return getDocumentFromBytes(document);
    }

    public static Document getDocumentFromBytes(byte[] data) throws Exception {
        Document doc =
            XmlTransformUtility.parseNamespaceAware(new ByteArrayInputStream(data));

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
        fis.close();
        return data.toByteArray();
    }

    public static void saveDocument(String filename, byte[] data)
            throws Exception {
        Document doc = null;
        try {
            doc =
                XmlTransformUtility.parseNamespaceAware(new ByteArrayInputStream(data));
        } catch (Exception e) {
            String message = "Unable to save file: " + filename;
            logger.error(message);
            e.printStackTrace();
            throw new Exception(message, e);
        }

        saveDocument(filename, doc);
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
            logger.error(message);
            e.printStackTrace();
            throw new Exception(message, e);
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

    public static String format(byte[] data) throws Exception {
        Document doc =
            XmlTransformUtility.parseNamespaceAware(new ByteArrayInputStream(data));

        OutputFormat format = new OutputFormat(doc);
        format.setEncoding("UTF-8");
        format.setIndenting(true);
        format.setIndent(2);
        format.setOmitXMLDeclaration(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer output = new BufferedWriter(new OutputStreamWriter(out));

        XMLSerializer serializer = new XMLSerializer(output, format);
        serializer.serialize(doc);
        output.close();

        return new String(out.toByteArray(), "UTF-8");
    }

    public static byte[] fedoraXMLHashFormat(byte[] data) throws Exception {
        OutputFormat format = new OutputFormat("XML", "UTF-8", false);
        format.setIndent(0);
        format.setLineWidth(0);
        format.setPreserveSpace(false);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        XMLSerializer serializer = new XMLSerializer(outStream, format);

        Document doc =
            XmlTransformUtility.parseNamespaceAware(new ByteArrayInputStream(data));
        serializer.serialize(doc);

        ByteArrayInputStream in =
                new ByteArrayInputStream(outStream.toByteArray());
        BufferedReader br =
                new BufferedReader(new InputStreamReader(in, "UTF-8"));
        String line = null;
        StringBuffer sb = new StringBuffer();
        while ((line = br.readLine()) != null) {
            line = line.trim();
            sb = sb.append(line);
        }

        return sb.toString().getBytes("UTF-8");
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
