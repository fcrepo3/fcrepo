/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai;

/**
 * Signals that a request includes illegal arguments, is missing required
 * arguments, includes a repeated argument, or values for arguments have 
 * an illegal syntax.
 * 
 * This may occur while fulfilling any request.
 * 
 * @author Chris Wilper
 */
public class BadArgumentException
        extends OAIException {

    private static final long serialVersionUID = 1L;

    public BadArgumentException() {
        super("badArgument", null);
    }

    public BadArgumentException(String message) {
        super("badArgument", message);
    }

}
