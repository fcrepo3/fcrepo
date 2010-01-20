/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.journal;

import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.management.ManagementDelegate;


/**
 * Pass this to the constructors of the JournalWorker classes and their
 * dependents instead of passing a Server.
 * <p>
 * This makes it much easier to write unit tests, since I don't need to create a
 * Server instance.
 * 
 * @author Jim Blake
 */
public interface ServerInterface {

    ManagementDelegate getManagementDelegate();

    String getRepositoryHash() throws ServerException;

    boolean hasInitialized();

}
