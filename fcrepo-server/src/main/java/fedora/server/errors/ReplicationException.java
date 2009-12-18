/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signals a problem during replication.
 * 
 * @author Paul Charlton
 */
public class ReplicationException
        extends ServerException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a ReplicationException.
     * 
     * @param msg
     *        Description of the exception.
     */
    public ReplicationException(String msg) {
        super(null, msg, null, null, null);
    }
}
