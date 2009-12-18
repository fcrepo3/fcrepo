/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.batch;

import java.io.PrintStream;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author Ross Wayland
 */
public class BatchModifyValidatorErrorHandler
        implements ErrorHandler {

    protected static int errorCount = 0;

    private static PrintStream out;

    public BatchModifyValidatorErrorHandler(PrintStream out) {
        BatchModifyValidatorErrorHandler.out = out;
        BatchModifyValidatorErrorHandler.errorCount = 0;
    }

    public void warning(SAXParseException e) throws SAXException {
        System.err
                .println("BatchModifyValidatorErrorHandler detected SAX WARNING: ");
        logError(e);
    }

    public void error(SAXParseException e) throws SAXException {
        System.err
                .println("BatchModifyValidatorErrorHandler detected SAX ERROR: ");
        logError(e);
    }

    public void fatalError(SAXParseException e) throws SAXException {
        System.err
                .print("BatchModifyValidatorErrorHandler detected SAX FATAL ERROR:");
        logError(e);
    }

    private static void printPubID(SAXParseException e) {
        if (e.getPublicId() != null) {
            System.err.print(e.getPublicId() + " ");
            out.println("    " + e.getPublicId() + " ");
        }
        if (e.getLineNumber() != -1) {
            System.err
                    .println("[modify-directives-file] Parsing error at line: "
                            + e.getLineNumber() + " column: "
                            + e.getColumnNumber() + " ");
            out.println("    [modify-directives-file] Parsing error at line: "
                    + e.getLineNumber() + " column: " + e.getColumnNumber()
                    + " ");
        }
    }

    private static void printMsg(SAXParseException e) {
        System.err.println(e.getClass().getName()
                + " - "
                + (e.getMessage() == null ? "(no detail provided)" : e
                        .getMessage()));
        out.println("    "
                + e.getClass().getName()
                + " - "
                + (e.getMessage() == null ? "(no detail provided)" : e
                        .getMessage()));
    }

    private static void logError(SAXParseException e) {
        out.println("  <parser-error>");
        printPubID(e);
        printMsg(e);
        out.println("  </parser-error>");
        errorCount++;
    }

}
