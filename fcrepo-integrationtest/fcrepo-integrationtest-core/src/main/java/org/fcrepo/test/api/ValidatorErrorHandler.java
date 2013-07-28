package org.fcrepo.test.api;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

// error handler for validating parsing (see validate(String))
// collects errors and fatal errors in a stringBuilder
public class ValidatorErrorHandler
        implements ErrorHandler {

    private final StringBuilder m_errors;

    ValidatorErrorHandler(StringBuilder errors) {
        m_errors = errors;
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        m_errors.append(e.getMessage());
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        m_errors.append(e.getMessage());
    }

}