/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signals that one or more user-supplied method parameters do not validate
 * against the method parameter definitions in the associated Behavior 
 * Mechanism object.
 * 
 * @author Ross Wayland
 */
public class InvalidUserParmException
        extends DisseminationException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an InvalidUserParmException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public InvalidUserParmException(String message) {
        super(message);
    }

}
