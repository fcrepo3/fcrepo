package org.fcrepo.utilities.xml;

import java.io.IOException;
import java.io.Writer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

@Deprecated
@SuppressWarnings("restriction")
public abstract class SunXmlSerializers {

    /**
     * Serialize the dom Document with no preserved space between elements,
     * but without indenting, line wrapping, omission of XML declaration, or
     * omission of doctype
     * @param doc
     * @param encoding
     * @param out
     * @throws IOException
     */
    public static void writeXmlNoSpace(Document doc, String encoding, Writer out)
        throws IOException {
        XMLSerializer ser = new XMLSerializer(out, getXmlNoSpace(encoding));
        
        ser.serialize(doc);
        out.close();
    }
    
    /**
     * method: "XML"
     * charset: "UTF-8"
     * indenting: TRUE
     * indent-width: 2
     * line-width: 80
     * preserve-space: FALSE
     * omit-XML-declaration: FALSE
     * omit-DOCTYPE: TRUE
     * @param ele
     * @param out
     * @throws IOException
     */
    public static void writeConsoleNoDocType(Document ele, Writer out)
    throws IOException {
        XMLSerializer serializer =
                new XMLSerializer(out, CONSOLE_NO_DOCTYPE);
        serializer.serialize(ele);
    }
    
    private static OutputFormat CONSOLE_NO_DOCTYPE =
            getConsoleNoDocType();

    // this method only makes sense with OutputStreams
    // with writers, the encoding to bytes happens downstream
    // so this method really just serializes with these defaults:
    /**
     * method: "XML"
     * charset: "UTF-8"
     * indenting: FALSE
     * indent-width: 0
     * line-width: 0
     * preserve-space: FALSE
     * omit-XML-declaration: FALSE
     * omit-DOCTYPE: FALSE
     * @param ele
     * @param out
     * @throws IOException
     */
    public static void writeXmlToUTF8(Element ele, Writer out) 
            throws IOException {
        XMLSerializer serializer =
                new XMLSerializer(out, XML_TO_UTF8);
        serializer.serialize(ele);
    }
    
    private static OutputFormat XML_TO_UTF8 =
            new OutputFormat("XML", "UTF-8", false);

    /**
     * This method is used in object conversion.
     * None of the standard formats --
     * FOXML, METS, ATOM -- have DOCTYPE declarations,
     * so the inability of XSLT to propagate that
     * information is probably irrelevant. However, the
     * line wrapping issue remains.
     * 
     * method: "XML"
     * charset: "UTF-8"
     * indenting: TRUE
     * indent-width: 2
     * line-width: 80
     * preserve-space: FALSE
     * omit-XML-declaration: FALSE
     * omit-DOCTYPE: FALSE
     * @param ele
     * @param out
     * @throws IOException
     */
    public static void writeConsoleWithDocType(Document ele, Writer out)
    throws IOException {
        XMLSerializer serializer =
                new XMLSerializer(out, CONSOLE_WITH_DOCTYPE);
        serializer.serialize(ele);
    }
    
    private static OutputFormat CONSOLE_WITH_DOCTYPE =
            getConsoleWithDocType();

    public static void writeMgmtNoDecl(Document ele, Writer out)
    throws IOException {
        XMLSerializer serializer = new XMLSerializer(out, MGMT_NO_DECL);
        serializer.serialize(ele);
    }

    private static OutputFormat MGMT_NO_DECL =
            getMgmtNoDecl();
    
    public static void writeMgmtWithDecl(Document ele, Writer out)
    throws IOException {
        XMLSerializer serializer = new XMLSerializer(out, MGMT_WITH_DECL);
        serializer.serialize(ele);
    }

    private static OutputFormat MGMT_WITH_DECL = getMgmtWithDecl();
    
    public static void writePrettyPrint(Document ele, Writer out)
    throws IOException {
        XMLSerializer serializer = new XMLSerializer(out, PRETTY_PRINT);
        serializer.serialize(ele);
    }

    private static OutputFormat PRETTY_PRINT = getPrettyPrint();

    public static void writePrettyPrintWithDecl(Document ele, Writer out)
    throws IOException {
        XMLSerializer serializer = new XMLSerializer(out, PRETTY_PRINT_WITH_DECL);
        serializer.serialize(ele);
    }

    public static void writePrettyPrintWithDecl(Node ele, Writer out)
    throws IOException {
        XMLSerializer serializer = new XMLSerializer(out, PRETTY_PRINT_WITH_DECL);
        serializer.serialize(ele);
    }

    private static OutputFormat PRETTY_PRINT_WITH_DECL =
        getPrettyPrintWithDecl();

    private static OutputFormat getXmlNoSpace(String encoding) {
        OutputFormat fmt = new OutputFormat("XML", encoding, false);
        // indent == 0 means add no indenting
        fmt.setIndent(0);
        // default line width is 72, but only applies when indenting
        fmt.setLineWidth(0);
        fmt.setPreserveSpace(false);
        return fmt;
    }

    private static OutputFormat getConsoleNoDocType() {
        OutputFormat fmt = new OutputFormat("XML", "UTF-8", true);
        fmt.setIndent(2);
        fmt.setLineWidth(80);
        fmt.setPreserveSpace(false);
        // default is false
        fmt.setOmitXMLDeclaration(false);
        fmt.setOmitDocumentType(true);
        return fmt;
    }

    private static OutputFormat getConsoleWithDocType() {
        OutputFormat fmt = new OutputFormat("XML", "UTF-8", true);
        fmt.setIndent(2);
        fmt.setLineWidth(80);
        fmt.setPreserveSpace(false);
        // default is false
        fmt.setOmitXMLDeclaration(false);
        // default is false
        fmt.setOmitDocumentType(false);
        return fmt;
    }

    private static OutputFormat getMgmtNoDecl() {
        OutputFormat fmt = new OutputFormat("XML", "UTF-8", true);
        fmt.setIndent(2);
        fmt.setLineWidth(120);
        fmt.setPreserveSpace(false);
        fmt.setOmitXMLDeclaration(true);
        fmt.setOmitDocumentType(true);
        return fmt;
    }

    private static OutputFormat getMgmtWithDecl() {
        OutputFormat fmt = new OutputFormat("XML", "UTF-8", true);
        fmt.setIndent(2);
        fmt.setLineWidth(120);
        fmt.setPreserveSpace(false);
        fmt.setOmitXMLDeclaration(false);
        fmt.setOmitDocumentType(true);
        return fmt;
    }

    private static OutputFormat getPrettyPrint() {
        OutputFormat fmt = new OutputFormat("XML", "UTF-8", true);
        fmt.setEncoding("UTF-8");
        fmt.setIndenting(true);
        fmt.setIndent(2);
        fmt.setOmitXMLDeclaration(true);
        return fmt;
    }

    private static OutputFormat getPrettyPrintWithDecl() {
        OutputFormat fmt = new OutputFormat("XML", "UTF-8", true);
        fmt.setEncoding("UTF-8");
        fmt.setIndenting(true);
        fmt.setIndent(2);
        fmt.setOmitXMLDeclaration(false);
        return fmt;
    }
}
