/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public abstract class XMLDocument {

    private Document document;

    public XMLDocument() {
    }

    public XMLDocument(InputStream inputDoc)
            throws DocumentException {
        SAXReader saxReader = new SAXReader();
        document = saxReader.read(inputDoc);
    }

    public Document getDocument() {
        return document;
    }

    public void write(String location) throws IOException {
        write(new FileWriter(location));
    }

    public void write(Writer writer) throws IOException {
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter output = new XMLWriter(writer, format);
        output.write(document);
        output.close();
    }
}
