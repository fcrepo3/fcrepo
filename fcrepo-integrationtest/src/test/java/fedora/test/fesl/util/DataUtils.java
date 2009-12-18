/*
 * File: DataUtils.java
 * 
 * Copyright 2007 Macquarie E-Learning Centre Of Excellence
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

package fedora.test.fesl.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

/**
 * Utility class for managing documents.
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public class DataUtils {

    private static final Logger log = Logger.getLogger(DataUtils.class);

    /**
     * Creates an XML document object from a file.
     * 
     * @param file
     *        the filename of the file to load.
     * @return the XML document object.
     * @throws Exception
     */
    public static Document getDocumentFromFile(String filename)
            throws Exception {
        File file = new File(filename);
        return getDocumentFromFile(file);
    }

    /**
     * Creates an XML document object from a file.
     * 
     * @param file
     *        the file to load.
     * @return the XML document object.
     * @throws Exception
     */
    public static Document getDocumentFromFile(File file) throws Exception {
        byte[] document = loadFile(file);
        DocumentBuilderFactory documentBuilderFactory =
                DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder =
                documentBuilderFactory.newDocumentBuilder();

        Document doc = docBuilder.parse(new ByteArrayInputStream(document));

        return doc;
    }

    /**
     * Creates an XML document object from a byte array.
     * 
     * @param file
     *        the file to load.
     * @return the XML document object.
     * @throws Exception
     */
    public static Document getDocumentFromBytes(byte[] data) throws Exception {
        DocumentBuilderFactory documentBuilderFactory =
                DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder =
                documentBuilderFactory.newDocumentBuilder();

        Document doc = docBuilder.parse(new ByteArrayInputStream(data));

        return doc;
    }

    /**
     * Loads a file into a byte array.
     * 
     * @param filename
     *        name of file to load.
     * @return byte array containing the data file.
     * @throws Exception
     */
    public static byte[] loadFile(String filename) throws Exception {
        File file = new File(filename.trim());
        return loadFile(file);
    }

    /**
     * Loads a file into a byte array.
     * 
     * @param File
     *        to load.
     * @return byte array containing the data file.
     * @throws Exception
     */
    public static byte[] loadFile(File file) throws Exception {
        if (!file.exists() || !file.canRead()) {
            String message = "Cannot read file: " + file.getCanonicalPath();
            log.error(message);
            throw new Exception(message);
        }

        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        int len = 0;
        byte[] buf = new byte[1024];
        while ((len = fis.read(buf)) >= 0) {
            data.write(buf, 0, len);
        }

        return data.toByteArray();
    }

    /**
     * Generates an MD5 checksum of a series of bytes.
     * 
     * @param data
     *        the byte array on which to compute the hash.
     * @return the MD5 hash.
     * @throws NoSuchAlgorithmException
     */
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
