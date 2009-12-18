/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import fedora.common.Constants;

import fedora.server.Module;
import fedora.server.Server;
import fedora.server.errors.BackendSecurityParserException;
import fedora.server.errors.ModuleInitializationException;

/**
 * A Module for accessing backend service security configuration information.
 * 
 * @author Ross Wayland
 */
public class DefaultBackendSecurity
        extends Module
        implements BackendSecurity {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(DefaultBackendSecurity.class.getName());

    public static BackendSecuritySpec beSS = null;

    private boolean m_validate = false;

    private String m_encoding = null;

    private static String m_beSecurityPath = null;

    /**
     * <p>
     * Creates a new DefaultBackendSecurity.
     * </p>
     * 
     * @param moduleParameters
     *        The name/value pair map of module parameters.
     * @param server
     *        The server instance.
     * @param role
     *        The module role name.
     * @throws ModuleInitializationException
     *         If initialization values are invalid or initialization fails for
     *         some other reason.
     */
    public DefaultBackendSecurity(Map moduleParameters,
                                  Server server,
                                  String role)
            throws ModuleInitializationException {
        super(moduleParameters, server, role);
    }

    /**
     * Post-Initializes the Module based on configuration parameters. The
     * implementation of this method is dependent on the schema used to define
     * the parameter names for the role of
     * <code>fedora.server.storage.DefaultBackendSecurity</code>.
     * 
     * @throws ModuleInitializationException
     *         If initialization values are invalid or initialization fails for
     *         some other reason.
     */
    @Override
    public void postInitModule() throws ModuleInitializationException {

        try {
            Server s_server = getServer();
            LOG.debug("DefaultBackendSecurity initialized");
            String fedoraHome = Constants.FEDORA_HOME;
            if (fedoraHome == null) {
                throw new ModuleInitializationException("[DefaultBackendSecurity] Module failed to initialize: "
                                                                + "FEDORA_HOME is undefined",
                                                        getRole());
            } else {
                m_beSecurityPath = fedoraHome + "/server/config/beSecurity.xml";
            }
            LOG.debug("m_beSecurityPath: " + m_beSecurityPath);

            String validate = getParameter("beSecurity_validation");
            if (validate != null) {
                if (!validate.equals("true") && !validate.equals("false")) {
                    LOG
                            .warn("Validation setting for backend "
                                    + "security configuration file must be either \"true\" or \"false\". "
                                    + "Value specified was: \"" + validate
                                    + "\". Validation is defaulted to "
                                    + "\"false\".");
                } else {
                    m_validate = new Boolean(validate).booleanValue();
                }
            } else {
                LOG
                        .warn("Validation setting for backend "
                                + "security configuration file was not specified. Validation is defaulted to "
                                + "\"false\".");
            }
            LOG.debug("beSecurity_validate: " + m_validate);

            m_encoding = getParameter("beSecurity_char_encoding");
            if (m_encoding == null) {
                m_encoding = "utf-8";
                LOG
                        .warn("Character encoding for backend "
                                + "security configuration file was not specified. Encoding defaulted to "
                                + "\"utf-8\".");
            }
            LOG.debug("beSecurity_char_encoding: " + m_encoding);

            // initialize static BackendSecuritySpec instance
            setBackendSecuritySpec();
            if (LOG.isDebugEnabled()) {
                Set roleList = beSS.listRoleKeys();
                Iterator iter = roleList.iterator();
                while (iter.hasNext()) {
                    LOG.debug("beSecurity ROLE: " + iter.next());
                }
            }

        } catch (Throwable th) {
            throw new ModuleInitializationException("[DefaultBackendSecurity] "
                    + "BackendSecurity "
                    + "could not be instantiated. The underlying error was a "
                    + th.getClass().getName() + "The message was \""
                    + th.getMessage() + "\".", getRole());
        }
    }

    /**
     * Parses the beSecurity configuration file.
     * 
     * @throws BackendSecurityParserException
     *         If an error occurs in attempting to parse the beSecurity
     *         configuration file.
     */
    public BackendSecuritySpec parseBeSecurity()
            throws BackendSecurityParserException {

        try {
            BackendSecurityDeserializer bsd =
                    new BackendSecurityDeserializer(m_encoding, m_validate);
            return bsd.deserialize(m_beSecurityPath);

        } catch (Throwable th) {
            throw new BackendSecurityParserException("[DefaultBackendSecurity] "
                    + "An error has occured in parsing the backend security "
                    + "configuration file located at \""
                    + m_beSecurityPath
                    + "\". "
                    + "The underlying error was a "
                    + th.getClass().getName()
                    + "The message was \""
                    + th.getMessage() + "\".");
        }
    }

    /**
     * Gets the static instance of BackendSecuritySpec.
     */
    public BackendSecuritySpec getBackendSecuritySpec() {
        return beSS;
    }

    /**
     * Initializes the static BackendSecuritySpec instance.
     * 
     * @throws BackendSecurityParserException
     *         If an error occurs in attempting to parse the beSecurity
     *         configuration file.
     */
    public void setBackendSecuritySpec() throws BackendSecurityParserException {
        beSS = parseBeSecurity();
    }

    /**
     * Re-initializes the static backendSecuritySpec instance by rereading the
     * beSecurity configurationfile. This method is used to refresh the
     * beSecurity configuration on the server when changes have been made to the
     * configuration file.
     * 
     * @throws BackendSecurityParserException
     *         If an error occurs in attempting to parse the beSecurity
     *         configuration file.
     */
    public BackendSecuritySpec reloadBeSecurity()
            throws BackendSecurityParserException {
        return parseBeSecurity();
    }

}
