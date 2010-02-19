/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.validation;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sandy Payette
 */
public class DOValidatorXMLErrorHandler
        implements ErrorHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(DOValidatorXMLErrorHandler.class);

    public DOValidatorXMLErrorHandler() {
    }

    public void warning(SAXParseException e) throws SAXException {
        logger.warn("SAX WARNING (publicId=" + e.getPublicId() + ")", e);
    }

    public void error(SAXParseException e) throws SAXException {
        logger.error("SAX ERROR (publicId=" + e.getPublicId() + ")", e);
        throw new SAXException(formatParseExceptionMsg(e));
    }

    public void fatalError(SAXParseException e) throws SAXException {
        logger.error("SAX FATAL ERROR (publicId=" + e.getPublicId() + ")", e);
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
