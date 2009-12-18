/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai;

/**
 * An exception occuring as a result of an OAI-PMH request.
 * 
 * @author Chris Wilper
 */
public abstract class OAIException
        extends Exception {

    private final String m_code;

    protected OAIException(String code, String message) {
        super(message);
        m_code = code;
    }

    public String getCode() {
        return m_code;
    }

}
