/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal;

import fedora.server.Server;
import fedora.server.errors.ServerException;
import fedora.server.management.ManagementDelegate;
import fedora.server.storage.DOManager;

/**
 * Wrap a Server in an object that implements an interface, so it can be passed
 * to the JournalWorker classes and their dependents.
 * <p>
 * It's also easy to mock, for unit tests.
 * 
 * @author Jim Blake
 */
public class ServerWrapper
        implements ServerInterface {

    private final Server server;

    public ServerWrapper(Server server) {
        this.server = server;
    }

    public boolean hasInitialized() {
        return server.hasInitialized();
    }

    public ManagementDelegate getManagementDelegate() {
        return (ManagementDelegate) server
                .getModule("fedora.server.management.ManagementDelegate");
    }

    public String getRepositoryHash() throws ServerException {
        DOManager doManager =
                (DOManager) server.getModule("fedora.server.storage.DOManager");
        return doManager.getRepositoryHash();
    }

}
