/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server;

import java.util.Map;

/**
 * A holder of configuration name-value pairs for a datastore.
 *
 * <p>A datastore is a system for retrieving and storing information. This
 * class is a convenient placeholder for the configuration values of such a 
 * system.
 *
 * <p>Configuration values for datastores are set in the server configuration
 * file. (see fedora-config.xsd)
 * 
 * @author Chris Wilper
 */
public class DatastoreConfig
        extends Parameterized {

    /**
     * Creates and initializes the <code>DatastoreConfig</code>.
     * 
     * <p>When the server is starting up, this is invoked as part of the
     * initialization process.
     * 
     * @param parameters
     *        A pre-loaded Map of name-value pairs comprising the intended
     *        configuration for the datastore.
     */
    public DatastoreConfig(Map parameters) {
        super(parameters);
    }

}
