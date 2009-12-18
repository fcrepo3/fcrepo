/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security.servletfilters;

import org.xml.sax.SAXException;

public class FinishedParsingException
        extends SAXException {

    private static final long serialVersionUID = 1L;

    public FinishedParsingException(String message) {
        super(message);
    }

}
