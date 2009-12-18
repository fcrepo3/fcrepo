/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai;

/**
 * Signals that the value of the verb argument is not a legal OAI-PMH verb,
 * the verb argument is missing, or the verb argument is repeated.
 * 
 * @author Chris Wilper
 */
public class BadVerbException
        extends OAIException {

    private static final long serialVersionUID = 1L;

    public BadVerbException() {
        super("badVerb", null);
    }

    public BadVerbException(String message) {
        super("badVerb", message);
    }

}
