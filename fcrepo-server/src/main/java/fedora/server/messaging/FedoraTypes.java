/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.messaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.XPath;

import fedora.common.Constants;

import fedora.utilities.XMLDocument;


/**
 * Utility class for retrieving the XML Schema Datatypes associated with
 * Fedora API methods.
 *
 * @author Edwin Shin
 * @since 3.0
 * @version $Id$
 */
public class FedoraTypes
        extends XMLDocument {
    /** Logger for this class. */
    private static Logger LOG =
            Logger.getLogger(FedoraTypes.class.getName());

    private final Map<String, String> method2datatype = new HashMap<String, String>();
    private final Map<String, String> response2parameter = new HashMap<String, String>();
    private final Map<String, String> ns2prefix = new HashMap<String, String>();

    public FedoraTypes() throws DocumentException, FileNotFoundException {
        this(getXsd());
    }
    public FedoraTypes(InputStream in) throws DocumentException {
        super(in);
        ns2prefix.put("xsd", Constants.XML_XSD.uri);
    }

    public String getDatatype(String method, String param) {
        String key = method + "." + param;

        if (!method2datatype.containsKey(key)) {
            String query = String.format("/xsd:schema/xsd:element[@name='%s']" +
                                      "/xsd:complexType/xsd:sequence" +
                                      "/xsd:element[@name='%s']/@type",
                                      method, param);
            XPath xpath = DocumentHelper.createXPath(query);
            xpath.setNamespaceURIs(ns2prefix);
            String datatype = xpath.valueOf(getDocument());
            if (datatype.equals("")) {
                datatype = null;
            }
            method2datatype.put(key, datatype);
        }
        return method2datatype.get(key);
    }

    public String getResponseParameter(String response) {
        if (!response2parameter.containsKey(response)) {
            String query = String.format("/xsd:schema/xsd:element[@name='%s']" +
                                  "/xsd:complexType/xsd:sequence" +
                                  "/xsd:element/@name",
                                  response);
            XPath xpath = DocumentHelper.createXPath(query);
            xpath.setNamespaceURIs(ns2prefix);
            String param = xpath.valueOf(getDocument());
            if (param.equals("")) {
                param = null;
            }
            response2parameter.put(response, param);
        }
        return response2parameter.get(response);
    }

    /**
     * Get fedora-types.xsd. First attempts to fetch the file from
     * FEDORA_HOME/server/xsd/fedora-types.xsd. Failing that, tries
     * src/main/resources/xsd/fedora-types.xsd (for the JUnit tests).
     *
     * @return fedora-types.xsd
     * @throws FileNotFoundException
     */
    private static InputStream getXsd() throws FileNotFoundException {
        String fedoraHome = Constants.FEDORA_HOME;
        FileInputStream xsd;
        try {
            xsd = new FileInputStream(new File(new File(fedoraHome),
                                               "server/xsd/fedora-types.xsd"));
        } catch (FileNotFoundException e) {
            LOG.warn(e.getMessage());
            xsd = new FileInputStream("server/src/main/resources/xsd/fedora-types.xsd");
        }
        return xsd;
    }
}
