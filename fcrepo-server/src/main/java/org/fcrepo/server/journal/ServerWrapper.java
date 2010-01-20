/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.journal;

import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.management.ManagementDelegate;
import org.fcrepo.server.storage.DOManager;


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
                .getModule("org.fcrepo.server.management.ManagementDelegate");
    }

    public String getRepositoryHash() throws ServerException {
        DOManager doManager =
                (DOManager) server.getModule("org.fcrepo.server.storage.DOManager");
        return doManager.getRepositoryHash();
    }

}
