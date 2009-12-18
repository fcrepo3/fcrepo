/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities.install;

/**
 * Signals that installation failed.
 */
public class InstallationFailedException
        extends Exception {

    private static final long serialVersionUID = 1L;

    public InstallationFailedException(String msg) {
        super(msg);
    }

    public InstallationFailedException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
