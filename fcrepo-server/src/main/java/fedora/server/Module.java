/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server;

import java.util.Map;

import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ModuleShutdownException;

/**
 * Base class for Fedora server modules.
 * </p>
 * <p>
 * A <code>Module</code> is a singleton object of a Fedora <code>Server</code>
 * instance with a simple lifecycle, supported by the <code>initModule()</code>
 * and <code>shutdownModule()</code> methods, which are automatically called
 * during server startup and shutdown, respectively.
 * </p>
 * <p>
 * Modules are configured via "param" elements inside module elements in the
 * configuration file. An instance of each module specified in the configuration
 * file is automatically created at startup and is available via the
 * <code>getModule(String)</code> instance method of the <code>Server</code>
 * class.
 * </p>
 * 
 * @author Chris Wilper
 */
public abstract class Module
        extends Pluggable {

    private final String m_role;

    private final Server m_server;

    /**
     * Creates and initializes the Module.
     * <p>
     * </p>
     * When the server is starting up, this is invoked as part of the
     * initialization process.
     * 
     * @param moduleParameters
     *        A pre-loaded Map of name-value pairs comprising the intended
     *        configuration of this Module.
     * @param server
     *        The <code>Server</code> instance.
     * @param role
     *        The role this module fulfills, a java class name.
     * @throws ModuleInitializationException
     *         If initilization values are invalid or initialization fails for
     *         some other reason.
     */
    public Module(Map<String, String> moduleParameters,
                  Server server,
                  String role)
            throws ModuleInitializationException {
        super(moduleParameters);
        m_role = role;
        m_server = server;
        initModule();
    }

    /**
     * Gets the <code>Server</code> instance to which this <code>Module</code>
     * belongs.
     * 
     * @return The <code>Server</code> instance.
     */
    public Server getServer() {
        return m_server;
    }

    /**
     * Gets the role this module fulfills, as given in the constructor.
     * <p>
     * </p>
     * <i>Role</i> is the name of the class or interface that this concrete
     * <code>Module</code> extends or implements.
     * 
     * @return String The role.
     */
    public final String getRole() {
        return m_role;
    }

    /**
     * Initializes the Module based on configuration parameters.
     * 
     * @throws ModuleInitializationException
     *         If initialization values are invalid or initialization fails for
     *         some other reason.
     */
    public void initModule() throws ModuleInitializationException {
        if (1 == 2) {
            throw new ModuleInitializationException(null, null);
        }
    }

    /**
     * Second stage of Module initialization. This is guaranteed to run after
     * all Modules' initModule() methods have run.
     * 
     * @throws ModuleInitializationException
     *         If initialization values are invalid or initialization fails for
     *         some other reason.
     */
    public void postInitModule() throws ModuleInitializationException {
        if (1 == 2) {
            throw new ModuleInitializationException(null, null);
        }
    }

    /**
     * Frees system resources allocated by this Module.
     * 
     * @throws ModuleShutdownException
     *         If there is a problem freeing system resources. Note that if
     *         there is a problem, it won't end up aborting the shutdown
     *         process. Therefore, this method should do everything possible to
     *         recover from exceptional situations before throwing an exception.
     */
    public void shutdownModule() throws ModuleShutdownException {
        if (1 == 2) {
            throw new ModuleShutdownException(null, null);
        }
    }
}
