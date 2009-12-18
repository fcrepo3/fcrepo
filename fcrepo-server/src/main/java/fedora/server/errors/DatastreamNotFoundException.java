/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signals that a datastream could not be found.
 * 
 * @author Chris Wilper
 */
public class DatastreamNotFoundException
        extends StorageException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a DatastreamNotFoundException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public DatastreamNotFoundException(String message) {
        super(message);
    }

}
