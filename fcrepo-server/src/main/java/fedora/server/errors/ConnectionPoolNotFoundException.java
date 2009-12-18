/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signals a database ConnectionPool could not be found.
 * 
 * @author Ross Wayland
 */
public class ConnectionPoolNotFoundException
        extends StorageException {

    private static final long serialVersionUID = 1L;

    public ConnectionPoolNotFoundException(String message) {
        super(message);
    }

}
