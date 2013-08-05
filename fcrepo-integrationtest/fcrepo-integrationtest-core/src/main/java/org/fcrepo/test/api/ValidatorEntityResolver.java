package org.fcrepo.test.api;

import java.io.FileInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

        
public class ValidatorEntityResolver extends DefaultHandler implements EntityResolver {
    
    private static Logger LOGGER = LoggerFactory.getLogger(ValidatorEntityResolver.class);
    
    public static String W3C = "http://www.w3.org/2001/03/";
    public static String FEDORA = "http://www.fedora.info/definitions/1/0/";
    public static String FEDORA_COMMONS = "http://www.fedora-commons.org/definitions/1/0/";
    public static String LOCAL_FEDORA = "http://localhost:8080/fedora/schema/";
    public static String DC = "http://dublincore.org/schemas/xmls/";
    public static String OAI = "http://www.openarchives.org/OAI/2.0/";
    private static final String SCHEMA_RESOURCE_PATH =
    System.getProperty("fcrepo-integrationtest-core.classes") != null ?
        System.getProperty("fcrepo-integrationtest-core.classes")
                        + "schema" : "src/test/resources/schema"; 
    @Override
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        String fname = systemId.substring(systemId.lastIndexOf('/') + 1);
        String sname = systemId.substring(0,systemId.lastIndexOf('/') + 1);
        LOGGER.debug("xmlns p: {} s: {}", publicId, systemId);
        if (W3C.equals(sname)) return inputsource("w3c/" + fname, publicId, systemId);
        if (FEDORA.equals(sname)) return inputsource("fedora/" + fname, publicId, systemId);
        if (FEDORA_COMMONS.equals(sname)) return inputsource("fedora/" + fname, publicId, systemId);
        if (LOCAL_FEDORA.equals(sname)) return inputsource("fedora/" + fname, publicId, systemId);
        if (DC.equals(sname)) return inputsource("dc/" + fname, publicId, systemId);
        if (OAI.equals(sname)) return inputsource("oai/" + fname, publicId, systemId);
        if ("".equals(sname))  return inputsource("fedora/" + fname, publicId, systemId);
        LOGGER.warn("FAILED to resolve p: " + publicId + " s: " + systemId);
        return null;
    }
    
    private InputSource inputsource(String path, String publicId, String systemId)
             throws IOException {
        FileInputStream fis = new FileInputStream(SCHEMA_RESOURCE_PATH + '/' + path);
        InputSource result =  new InputSource(fis);
        result.setEncoding("UTF-8");
        result.setPublicId(publicId);
        result.setSystemId(systemId);
        return result;
    }
    
}