/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.journal;

import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.errors.ModuleShutdownException;
import org.fcrepo.server.management.Management;
import org.fcrepo.server.management.ManagementDelegate;


/**
 * A common interface for the <code>JournalConsumer</code> and
 * <code>JournalCreator</code> classes. These classes form the implementation
 * layer between the <code>Journaler</code> and the
 * <code>ManagementDelegate</code>.
 * 
 * @author Jim Blake
 */
public interface JournalWorker
        extends Management {

    /**
     * Called by the Journaler during post-initialization, with a reference to
     * the ManagementDelegate module.
     */
    public void setManagementDelegate(ManagementDelegate delegate)
            throws ModuleInitializationException;

    /**
     * Called when the Journaler module receives a shutdown() from the server.
     */
    public void shutdown() throws ModuleShutdownException;
}
