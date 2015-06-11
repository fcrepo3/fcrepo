/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.utilities.rebuild;

import java.io.File;

import java.util.Map;

import org.fcrepo.server.config.ServerConfiguration;
import org.fcrepo.server.storage.types.DigitalObject;


/**
 * Interface for a class that rebuilds some aspect of the repository.
 *
 * <p>It is expected that clients of this interface will first call init, then
 * start, then addObject (possibly a series of times), then finish.
 *
 * @author Chris Wilper
 */
public interface Rebuilder {

    /**
     * Get a short phrase describing what the user can do with this rebuilder.
     */
    public String getAction();

    /**
     * Initialize the rebuilder, given the server configuration.
     *
     */
    public void setServerConfiguration(ServerConfiguration serverConfig);

    public void setServerDir(File serverBaseDir);

    public void init();

    public Map<String, String> getOptions()
            throws Exception;

    /**
     * Returns true is the server _must_ be shut down for this rebuilder to
     * safely operate.
     */
    public boolean shouldStopServer();

    /**
     * Validate the provided options and perform any necessary startup tasks.
     */
    public void start(Map<String, String> options) throws Exception;

    /**
     * Add the data of interest for the given object.
     */
    public void addObject(DigitalObject object) throws Exception;

    /**
     * Free up any system resources associated with rebuilding.
     */
    public void finish() throws Exception;

}