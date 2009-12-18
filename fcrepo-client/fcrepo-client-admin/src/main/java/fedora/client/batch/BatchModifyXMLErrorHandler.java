/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.batch;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author Ross Wayland
 */
public class BatchModifyXMLErrorHandler
        implements ErrorHandler {

    public BatchModifyXMLErrorHandler() {
    }

    public void warning(SAXParseException e) throws SAXException {
        System.err.print("BatchModifyXMLErrorHandler detected SAX WARNING: ");
        printPubID(e);
        printMsg(e);
    }

    public void error(SAXParseException e) throws SAXException {
        System.err
                .print("BatchModifyXMLErrorHandler detected SAX ERROR.  Re-throwing SAXException.");
        throw new SAXException(formatParseExceptionMsg(e));
    }

    public void fatalError(SAXParseException e) throws SAXException {
        System.err
                .print("BatchModifyXMLErrorHandler detected SAX FATAL ERROR.  Re-throwing SAXException.");
        throw new SAXException(formatParseExceptionMsg(e));
    }

    private void printPubID(SAXParseException e) {
        if (e.getPublicId() != null) {
            System.err.print(e.getPublicId() + " ");
        }
        if (e.getLineNumber() != -1) {
            System.err.print("line: " + e.getLineNumber() + " ");
        }
    }

    private void printMsg(SAXParseException e) {
        System.err.println(e.getClass().getName()
                + " - "
                + (e.getMessage() == null ? "(no detail provided)" : e
                        .getMessage()));
    }

    private String formatParseExceptionMsg(SAXParseException spe) {
        String systemId = spe.getSystemId();
        if (systemId == null) {
            systemId = "null";
        }
        String info =
                "URI=" + systemId + " Line=" + spe.getLineNumber() + ": "
                        + spe.getMessage();
        return info;
    }
}
