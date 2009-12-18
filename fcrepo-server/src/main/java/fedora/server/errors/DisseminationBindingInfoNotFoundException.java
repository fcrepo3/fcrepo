/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signals that an instance of DisseminationBindingInfo could not be found 
 * or was null.
 * 
 * @author Ross Wayland
 */
public class DisseminationBindingInfoNotFoundException
        extends StorageException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a DisseminationBindingInfoNotFoundException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public DisseminationBindingInfoNotFoundException(String message) {
        super(message);
    }
}
