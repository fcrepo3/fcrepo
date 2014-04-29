/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.security.xacml.pdp.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.server.security.xacml.pdp.MelcoePDPException;

/**
 * Various utility methods for managing policies
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class PolicyUtils {

    private static final Logger log =
            LoggerFactory.getLogger(PolicyUtils.class.getName());

    public PolicyUtils() {}


    /**
     * Read file and return contents as a string
     *
     * @param f File to read
     * @return
     * @throws PolicyStoreException
     */
    public String fileToString(File f)
        throws MelcoePDPException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] bytes = new byte[1024];

        try {
            FileInputStream fis = new FileInputStream(f);
            int count = fis.read(bytes);
            while (count > -1) {
                out.write(bytes, 0, count);
                count = fis.read(bytes);
            }
            fis.close();
        } catch (IOException e) {
            throw new MelcoePDPException("Error reading file: "
                    + f.getName(), e);
        }
        return out.toString();

    }

    public String getPolicyName(File policy) throws MelcoePDPException {
        InputStream is;
        String policyName;
        try {
            is = new FileInputStream(policy);
            Map<String, String> metadata = getDocumentMetadata(is);
            is.close();
            policyName = metadata.get("PolicyId");
        } catch (IOException e) {
            throw new MelcoePDPException(e.getMessage(), e);
        }
        return policyName;
    }

    public String getPolicyName(String policy) throws MelcoePDPException {
        Map<String, String> metadata;
        try {
            metadata = getDocumentMetadata(policy.getBytes("UTF-8"));
        } catch (IOException e) {
            throw new MelcoePDPException(e.getMessage(), e);
        }
        return metadata.get("PolicyId");
    }

    public Map<String, String> getDocumentMetadata(byte[] docData) {

        InputStream docIS = new ByteArrayInputStream(docData);
        return getDocumentMetadata(docIS);
    }


    /**
     * Obtains the metadata for the given document.
     *
     * @param docIS
     *        the document as an InputStream
     * @return the document metadata as a Map
     */
    public Map<String, String> getDocumentMetadata(InputStream docIS) {
        Map<String, String> metadata = new HashMap<String, String>();

        try {
            // Create instance of DocumentBuilderFactory
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            // Get the DocumentBuilder
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            // Create blank DOM Document and parse contents of input stream
            Document doc = docBuilder.parse(docIS);

            NodeList nodes = null;

            metadata.put("PolicyId", doc.getDocumentElement()
                    .getAttribute("PolicyId"));

            nodes = doc.getElementsByTagName("Subjects");
            if (nodes.getLength() == 0) {
                metadata.put("anySubject", "T");
            }

            nodes = doc.getElementsByTagName("Resources");
            if (nodes.getLength() == 0) {
                metadata.put("anyResource", "T");
            }

            nodes = doc.getElementsByTagName("Actions");
            if (nodes.getLength() == 0) {
                metadata.put("anyAction", "T");
            }

            nodes = doc.getElementsByTagName("Environments");
            if (nodes.getLength() == 0) {
                metadata.put("anyEnvironment", "T");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return metadata;
    }
}
