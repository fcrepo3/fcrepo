package org.fcrepo.utilities.xml;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;

import junit.framework.JUnit4TestAdapter;

import org.fcrepo.utilities.XmlTransformUtility;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class TestXmlSerializers {

    private static final String DC_SRC_PATH =
            "/org/fcrepo/utilities/xml/dc_long_desc_src.xml";

    private static final String DEEP_SRC_PATH =
            "/org/fcrepo/utilities/xml/deep_nest_long_desc_src.xml";

    private static final String WSDL_SRC_PATH =
            "/org/fcrepo/utilities/xml/wsdl_src.xml";

    private static final String METS_SRC_PATH =
            "/org/fcrepo/utilities/xml/mets_src.xml";

    private Document dcDoc;

    private Document deepDoc;

    private Document wsdlDoc;
    
    // the METS source is serialized inconsistently because
    // of a bug in the Sun-internal serializers: Comments
    // outside of the document are moved to the end of the
    // serialization. This appears to be fixed in Xerces, but
    // obviously means output will not be equal in these circumstances.
    @SuppressWarnings("unused")
    private Document metsDoc;
    
    @Before
    public void parseDocs() throws Exception {
        InputStream docSrc;
        docSrc = getClass().getResourceAsStream(DC_SRC_PATH);
        dcDoc = XmlTransformUtility.parseNamespaceAware(docSrc);
        docSrc = getClass().getResourceAsStream(DEEP_SRC_PATH);
        deepDoc = XmlTransformUtility.parseNamespaceAware(docSrc);
        docSrc = getClass().getResourceAsStream(WSDL_SRC_PATH);
        wsdlDoc = XmlTransformUtility.parseNamespaceAware(docSrc);
        docSrc = getClass().getResourceAsStream(METS_SRC_PATH);
        metsDoc = XmlTransformUtility.parseNamespaceAware(docSrc);
    }
    
    @SuppressWarnings("deprecation")
    private void testWriteConsoleNoDocType(Document doc) throws Exception {
        StringWriter sout = new StringWriter();
        SunXmlSerializers.writeConsoleNoDocType(doc, sout);
        String proprietary = sout.toString();
        sout = new StringWriter();
        XercesXmlSerializers.writeConsoleNoDocType(doc, sout);
        String standard = sout.toString();
        if (!proprietary.equals(standard)) {
            System.out.println("<<<<");
            System.out.println(proprietary);
            System.out.println(">>>>");
            System.out.println(standard);
        }
        assertEquals(proprietary, standard);
    }
    
    @Test
    public void testWriteConsoleNoDocType() throws Exception {
        testWriteConsoleNoDocType(wsdlDoc);
        testWriteConsoleNoDocType(dcDoc);
        testWriteConsoleNoDocType(deepDoc);
    }

    @SuppressWarnings("deprecation")
    private void testWriteMgmtNoDecl(Document doc) throws Exception {
        StringWriter sout = new StringWriter();
        SunXmlSerializers.writeMgmtNoDecl(doc, sout);
        String proprietary = sout.toString();
        sout = new StringWriter();
        XercesXmlSerializers.writeMgmtNoDecl(doc, sout);
        String standard = sout.toString();
        if (!proprietary.equals(standard)) {
            System.out.println("<<<<");
            System.out.println(proprietary);
            System.out.println(">>>>");
            System.out.println(standard);
        }
        assertEquals(proprietary, standard);
    }
    
    @Test
    public void testWriteMgmtNoDecl() throws Exception {
        testWriteMgmtNoDecl(wsdlDoc);
        testWriteMgmtNoDecl(dcDoc);
        testWriteMgmtNoDecl(deepDoc);
    }

    @SuppressWarnings("deprecation")
    private void testWriteXmlNoSpace(Document doc) throws Exception {
        StringWriter sout = new StringWriter();
        SunXmlSerializers.writeXmlNoSpace(doc, "UTF-8", sout);
        String proprietary = sout.toString();
        sout = new StringWriter();
        XercesXmlSerializers.writeXmlNoSpace(doc, "UTF-8", sout);
        String standard = sout.toString();
        if (!proprietary.equals(standard)) {
            System.out.println("<<<<");
            System.out.println(proprietary);
            System.out.println(">>>>");
            System.out.println(standard);
        }
        assertEquals(proprietary, standard);
    }
    
    @Test
    public void testWriteXmlNoSpace() throws Exception {
        testWriteXmlNoSpace(wsdlDoc);
        testWriteXmlNoSpace(dcDoc);
        testWriteXmlNoSpace(deepDoc);
    }
    
    @Test
    public void testTransformer() throws Exception {
        Transformer t = XmlTransformUtility.getTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestXmlSerializers.class);
    }

}
