package org.fcrepo.utilities.xml;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.io.StringWriter;

import org.fcrepo.utilities.XmlTransformUtility;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class TestXmlSerializers {

    private static final String WSDL_SRC_PATH =
            "/org/fcrepo/utilities/xml/wsdl_src.xml";
    
    private Document wsdlDoc;
    
    @Before
    public void parseDocs() throws Exception {
        InputStream wsdSrc =
                getClass().getResourceAsStream(WSDL_SRC_PATH);
        wsdlDoc = XmlTransformUtility.parseNamespaceAware(wsdSrc);
    }
    
    @Test
    public void testWriteConsoleNoDocType() throws Exception {
        StringWriter sout = new StringWriter();
        ProprietaryXmlSerializers.writeConsoleNoDocType(wsdlDoc, sout);
        String proprietary = sout.toString();
        sout = new StringWriter();
        StandardXmlSerializers.writeConsoleNoDocType(wsdlDoc, sout);
        String standard = sout.toString();
        assertEquals(proprietary, standard);
    }
}
