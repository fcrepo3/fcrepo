/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities.install;

/**
 * Signals that the user has intentionally cancelled installation.
 */
public class InstallationCancelledException
        extends Exception {

    private static final long serialVersionUID = 1L;

    public InstallationCancelledException() {
    }

    public InstallationCancelledException(String msg) {
        super(msg);
    }

}
