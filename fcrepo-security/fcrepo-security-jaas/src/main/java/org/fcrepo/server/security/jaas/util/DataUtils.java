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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.fcrepo.utilities.ReadableByteArrayOutputStream;
import org.fcrepo.utilities.ReadableCharArrayWriter;
import org.fcrepo.utilities.XmlTransformUtility;
import org.fcrepo.utilities.xml.ProprietaryXmlSerializers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

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
        ReadableCharArrayWriter out = new ReadableCharArrayWriter();
        Writer output = new BufferedWriter(out);

        ProprietaryXmlSerializers.writePrettyPrint(doc, output);
        output.close();

        return out.getString();
    }

    public static String format(byte[] data) throws Exception {
        Document doc =
            XmlTransformUtility.parseNamespaceAware(new ByteArrayInputStream(data));

        return format(doc);
    }

    public static InputStream fedoraXMLHashFormat(byte[] data) throws Exception {
        ReadableCharArrayWriter writer = new ReadableCharArrayWriter();

        Document doc =
            XmlTransformUtility.parseNamespaceAware(new ByteArrayInputStream(data));
        ProprietaryXmlSerializers.writeXmlNoSpace(doc, "UTF-8", writer);
        writer.close();

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

        return outStream.toInputStream();
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

        StringBuffer sb = new StringBuffer(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(hexChars[b >> 4 & 0xf]);
            sb.append(hexChars[b & 0xf]);
        }

        return new String(sb);
    }
}
