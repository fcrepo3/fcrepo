/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.validation;

import org.apache.log4j.Logger;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author Sandy Payette
 */
public class DOValidatorXMLErrorHandler
        implements ErrorHandler {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(DOValidatorXMLErrorHandler.class.getName());

    public DOValidatorXMLErrorHandler() {
    }

    public void warning(SAXParseException e) throws SAXException {
        LOG.warn("SAX WARNING (publicId=" + e.getPublicId() + ")", e);
    }

    public void error(SAXParseException e) throws SAXException {
        LOG.error("SAX ERROR (publicId=" + e.getPublicId() + ")", e);
        throw new SAXException(formatParseExceptionMsg(e));
    }

    public void fatalError(SAXParseException e) throws SAXException {
        LOG.error("SAX FATAL ERROR (publicId=" + e.getPublicId() + ")", e);
        throw new SAXException(formatParseExceptionMsg(e));
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
