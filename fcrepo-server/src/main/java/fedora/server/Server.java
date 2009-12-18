/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.text.MessageFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.xml.sax.SAXException;

import fedora.common.Constants;
import fedora.common.FaultException;
import fedora.common.MalformedPIDException;
import fedora.common.PID;

import fedora.server.config.ServerConfiguration;
import fedora.server.config.ServerConfigurationParser;
import fedora.server.errors.GeneralException;
import fedora.server.errors.MalformedPidException;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ModuleShutdownException;
import fedora.server.errors.ServerInitializationException;
import fedora.server.errors.ServerShutdownException;
import fedora.server.errors.authorization.AuthzException;
import fedora.server.security.Authorization;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.status.ServerState;
import fedora.server.utilities.status.ServerStatusFile;

import fedora.utilities.Log4J;

/**
 * The starting point for working with a Fedora repository. This class handles
 * loading, starting, and stopping modules (the module lifecycle), and provides
 * access to core constants.
 * 
 * @author Chris Wilper
 */
public abstract class Server
        extends Pluggable {

    public static final boolean USE_CACHE = true;

    public static final boolean USE_DEFINITIVE_STORE = false;

    public static final boolean GLOBAL_CHOICE = false;

    /** Logger for this class. */
    private static Logger LOG;

    /**
     * The ResourceBundle that provides access to constants from
     * fedora/server/resources/Server.properties.
     */
    private static ResourceBundle s_const =
            ResourceBundle.getBundle("fedora.server.resources.Server");

    /** The version of this release. */
    public static String VERSION = s_const.getString("version");

    /** The build date of this release. */
    public static String BUILD_DATE = s_const.getString("buildDate");

    /** The build number of this release. */
    public static String BUILD_NUMBER = s_const.getString("buildNumber");

    /** The name of the property that tells the server where it's based. */
    public static String HOME_PROPERTY = s_const.getString("home.property");

    /** The internal XML storage format for digital objects */
    public static String STORAGE_FORMAT = s_const.getString("format.storage");

    /** The directory where server configuration is stored, relative to home. */
    public static String CONFIG_DIR = s_const.getString("config.dir");

    /**
     * The startup log file. This file will include all log messages regardless
     * of their <code>Level</code>.
     */
    public static String LOG_STARTUP_FILE =
            s_const.getString("log.startup.file");

    /** The configuration filename. */
    public static String CONFIG_FILE = s_const.getString("config.file");

    /** The directory where server extensions are stored, relative to home. */
    public static String EXTENSION_DIR = s_const.getString("extension.dir");

    /** The directory where server executables are stored, relative to home. */
    public static String BIN_DIR = s_const.getString("bin.dir");

    /**
     * The prefix to all fedora-defined namespaces for this version.
     * 0={version.major}, 1={version.minor}
     */
    public static String NAMESPACE_PREFIX =
            MessageFormat.format(s_const.getString("namespace.prefix"),
                                 new Object[] {"1", "0"}); // so config namespace uses 1/0/

    /** The configuration file elements' namespace. 0={namespace.prefix} */
    public static String CONFIG_NAMESPACE =
            MessageFormat.format(s_const.getString("config.namespace"),
                                 new Object[] {NAMESPACE_PREFIX});

    /** The configuration file root element's name. */
    public static String CONFIG_ELEMENT_ROOT =
            s_const.getString("config.element.root");

    /** The configuration file comment element's name. */
    public static String CONFIG_ELEMENT_COMMENT =
            s_const.getString("config.element.comment");

    /** The configuration file datastore element's name. */
    public static String CONFIG_ELEMENT_DATASTORE =
            s_const.getString("config.element.datastore");

    /** The configuration file module element's name. */
    public static String CONFIG_ELEMENT_MODULE =
            s_const.getString("config.element.module");

    /** The configuration file param element's name. */
    public static String CONFIG_ELEMENT_PARAM =
            s_const.getString("config.element.param");

    /**
     * The configuration file's class-specifying attribute for server and module
     * elements.
     */
    public static String CONFIG_ATTRIBUTE_CLASS =
            s_const.getString("config.attribute.class");

    /**
     * The configuration file's role-specifying attribute for module elements.
     */
    public static String CONFIG_ATTRIBUTE_ROLE =
            s_const.getString("config.attribute.role");

    /** The configuration file param element's name attribute. */
    public static String CONFIG_ATTRIBUTE_NAME =
            s_const.getString("config.attribute.name");

    /** The configuration file param element's value attribute. */
    public static String CONFIG_ATTRIBUTE_VALUE =
            s_const.getString("config.attribute.value");

    /** The configuration file datastore element's id attribute. */
    public static String CONFIG_ATTRIBUTE_ID =
            s_const.getString("config.attribute.id");

    /** The required server constructor's first parameter's class. */
    public static String SERVER_CONSTRUCTOR_PARAM1_CLASS =
            s_const.getString("server.constructor.param1.class");

    /** The required server constructor's second parameter's class. */
    public static String SERVER_CONSTRUCTOR_PARAM2_CLASS =
            s_const.getString("server.constructor.param2.class");

    /** The required module constructor's first parameter's class. */
    public static String MODULE_CONSTRUCTOR_PARAM1_CLASS =
            s_const.getString("module.constructor.param1.class");

    /** The required module constructor's second parameter's class. */
    public static String MODULE_CONSTRUCTOR_PARAM2_CLASS =
            s_const.getString("module.constructor.param2.class");

    /** The required module constructor's third parameter's class. */
    public static String MODULE_CONSTRUCTOR_PARAM3_CLASS =
            s_const.getString("module.constructor.param3.class");

    /** The name of the default Server implementation class */
    public static String DEFAULT_SERVER_CLASS =
            s_const.getString("default.server.class");

    /** Indicates that an XML parser could not be found. */
    public static String INIT_XMLPARSER_SEVERE_MISSING =
            s_const.getString("init.xmlparser.severe.missing");

    /**
     * Indicates that the config file could not be read. 0=config file full
     * path, 1=additional info from underlying exception
     */
    public static String INIT_CONFIG_SEVERE_UNREADABLE =
            s_const.getString("init.config.severe.unreadable");

    /**
     * Indicates that the config file has malformed XML. 0=config file full
     * path, 1=additional info from underlying exception
     */
    public static String INIT_CONFIG_SEVERE_MALFORMEDXML =
            s_const.getString("init.config.severe.malformedxml");

    /**
     * Indicates that the config file has a mis-named root element. 0=config
     * file full path, 1={config.element.root}, 2=actual root element name
     */
    public static String INIT_CONFIG_SEVERE_BADROOTELEMENT =
            s_const.getString("init.config.severe.badrootelement");

    /**
     * Indicates that an invalid element was found in the configuration xml.
     * 1=the invalid element's name
     */
    public static String INIT_CONFIG_SEVERE_BADELEMENT =
            s_const.getString("init.config.severe.badelement");

    /**
     * Indicates that a CONFIG_ELEMENT_DATASTORE didn't specify the required
     * CONFIG_ATTRIBUTE_ID. 0={config.element.datastore},
     * 1={config.attribute.id}
     */
    public static String INIT_CONFIG_SEVERE_NOIDGIVEN =
            MessageFormat.format(s_const
                    .getString("init.config.severe.noidgiven"), new Object[] {
                    CONFIG_ELEMENT_DATASTORE, CONFIG_ATTRIBUTE_ID});

    /**
     * Indicates that the config file's element's namespace does not match
     * {config.namespace}. 0=config file full path, 1={config.namespace}
     */
    public static String INIT_CONFIG_SEVERE_BADNAMESPACE =
            s_const.getString("init.config.severe.badnamespace");

    /**
     * Indicates that a module element in the server configuration did not
     * specify a role, but should. 0={config.element.module},
     * 1={config.attribute.role}
     */
    public static String INIT_CONFIG_SEVERE_NOROLEGIVEN =
            MessageFormat.format(s_const
                    .getString("init.config.severe.norolegiven"), new Object[] {
                    CONFIG_ELEMENT_MODULE, CONFIG_ATTRIBUTE_ROLE});

    /**
     * Indicates that a module element in the server configuration did not
     * specify an implementing class, but should. 0={config.element.module},
     * 1={config.attribute.class}
     */
    public static String INIT_CONFIG_SEVERE_NOCLASSGIVEN =
            MessageFormat
                    .format(s_const
                                    .getString("init.config.severe.noclassgiven"),
                            new Object[] {CONFIG_ELEMENT_MODULE,
                                    CONFIG_ATTRIBUTE_CLASS});

    /**
     * Indicates that an attribute of an element was assigned the same value as
     * a previously specified element's attribute, and that this constitutes a
     * disallowed reassignment. 0=the common element, 1=the common attribute's
     * name, 2=the common attribute's value.
     */
    public static String INIT_CONFIG_SEVERE_REASSIGNMENT =
            s_const.getString("init.config.severe.reassignment");

    /**
     * Indicates that a parameter element in the config file is missing a
     * required element. 0={config.element.param}, 1={config.attribute.name},
     * 2={config.attribute.value}
     */
    public static String INIT_CONFIG_SEVERE_INCOMPLETEPARAM =
            MessageFormat
                    .format(s_const
                                    .getString("init.config.severe.incompleteparam"),
                            new Object[] {CONFIG_ELEMENT_PARAM,
                                    CONFIG_ATTRIBUTE_NAME,
                                    CONFIG_ATTRIBUTE_VALUE});

    /**
     * Tells which config element is being looked at in order to load its
     * parameters into memory. 0=name of element being examined,
     * 1=distinguishing attribute (name=&quot;value&quot;), or empty string if
     * no distinguishing attribute.
     */
    public static String INIT_CONFIG_CONFIG_EXAMININGELEMENT =
            s_const.getString("init.config.config.examiningelement");

    /**
     * Tells the name and value of a parameter loaded from the config file.
     * 0=param name, 1=param value
     */
    public static String INIT_CONFIG_CONFIG_PARAMETERIS =
            s_const.getString("init.config.config.parameteris");

    /**
     * Indicates that the server class could not be found. 0=server class
     * specified in config root element
     */
    public static String INIT_SERVER_SEVERE_CLASSNOTFOUND =
            s_const.getString("init.server.severe.classnotfound");

    /**
     * Indicates that the server class couldn't be accessed due to security
     * misconfiguration. 0=server class specified in config root element
     */
    public static String INIT_SERVER_SEVERE_ILLEGALACCESS =
            s_const.getString("init.server.severe.illegalaccess");

    /**
     * Indicates that the server class constructor was invoked improperly due to
     * programmer error. 0=server class specified in config root element
     */
    public static String INIT_SERVER_SEVERE_BADARGS =
            s_const.getString("init.server.severe.badargs");

    /**
     * Indicates that the server class doesn't have a constructor matching
     * Server(NodeList, File), but needs one. 0=server class specified in config
     * root element.
     */
    public static String INIT_SERVER_SEVERE_MISSINGCONSTRUCTOR =
            s_const.getString("init.server.severe.missingconstructor");

    /**
     * Indicates that a module role required to be fulfilled by this server was
     * not fulfilled because the configuration did not specify a module with
     * that role. 0=the role
     */
    public static String INIT_SERVER_SEVERE_UNFULFILLEDROLE =
            s_const.getString("init.server.severe.unfulfilledrole");

    public static String INIT_MODULE_SEVERE_UNFULFILLEDROLE =
            s_const.getString("init.module.severe.unfulfilledrole");

    /**
     * Indicates that the server class was abstract, but shouldn't be. 0=server
     * class specified in config root element
     */
    public static String INIT_SERVER_SEVERE_ISABSTRACT =
            s_const.getString("init.server.severe.isabstract");

    /**
     * Indicates that the module class could not be found. 0=module class
     * specified in config
     */
    public static String INIT_MODULE_SEVERE_CLASSNOTFOUND =
            s_const.getString("init.module.severe.classnotfound");

    /**
     * Indicates that the module class couldn't be accessed due to security
     * misconfiguration. 0=module class specified in config
     */
    public static String INIT_MODULE_SEVERE_ILLEGALACCESS =
            s_const.getString("init.module.severe.illegalaccess");

    /**
     * Indicates that the module class constructor was invoked improperly due to
     * programmer error. 0=module class specified in config
     */
    public static String INIT_MODULE_SEVERE_BADARGS =
            s_const.getString("init.module.severe.badargs");

    /**
     * Indicates that the module class doesn't have a constructor matching
     * Module(Map, Server, String), but needs one. 0=module class specified in
     * config
     */
    public static String INIT_MODULE_SEVERE_MISSINGCONSTRUCTOR =
            s_const.getString("init.module.severe.missingconstructor");

    /**
     * Indicates that the module class was abstract, but shouldn't be. 0=module
     * class specified in config
     */
    public static String INIT_MODULE_SEVERE_ISABSTRACT =
            s_const.getString("init.module.severe.isabstract");

    /**
     * Indicates that the startup log could not be written to its usual place
     * for some reason, and that we're falling back to stderr. 0=usual place,
     * 1=exception message
     */
    public static String INIT_LOG_WARNING_CANTWRITESTARTUPLOG =
            s_const.getString("init.log.warning.cantwritestartuplog");

    /**
     * The server-wide default locale, obtained via <code>getLocale()</code>.
     */
    private static Locale s_locale;

    /**
     * Holds an instance of a <code>Server</code> for each distinct
     * <code>File</code> given as a parameter to <code>getInstance(...)</code>
     */
    protected static Map<File, Server> s_instances = new HashMap<File, Server>();

    /**
     * The server's home directory.
     */
    private File m_homeDir;

    /**
     * Datastore configurations initialized from the server config file.
     */
    private Map<String, DatastoreConfig> m_datastoreConfigs;

    /**
     * Modules that have been loaded.
     */
    private Map<String, Module> m_loadedModules;

    /**
     * Is the server running?
     */
    private boolean m_initialized;

    /**
     * The server status File.
     */
    private ServerStatusFile m_statusFile;

    /**
     * What server profile should be used?
     */
    private static String s_serverProfile =
            System.getProperty("fedora.serverProfile");

    /**
     * Initializes the Server based on configuration.
     * <p>
     * </p>
     * Reads and schema-validates the configuration items in the given DOM
     * <code>NodeList</code>, validates required server params, initializes
     * the <code>Server</code>, then initializes each module, validating its
     * required params, then verifies that the server's required module roles
     * have been met.
     * 
     * @param rootConfigElement
     *        The root <code>Element</code> of configuration.
     * @param homeDir
     *        The home directory of fedora, used to interpret relative paths
     *        used in configuration.
     * @throws ServerInitializationException
     *         If there was an error starting the server.
     * @throws ModuleInitializationException
     *         If there was an error starting a module.
     */
    protected Server(Element rootConfigElement, File homeDir)
            throws ServerInitializationException, ModuleInitializationException {
        try {
            m_initialized = false;
            m_loadedModules = new HashMap<String, Module>();
            m_homeDir = new File(homeDir, "server");

            m_statusFile = new ServerStatusFile(m_homeDir);

            File logDir = new File(m_homeDir, "logs");
            if (!logDir.exists()) {
                logDir.mkdir(); // try to create dir if doesn't exist
            }
            File configFile =
                    new File(m_homeDir + File.separator + CONFIG_DIR
                            + File.separator + CONFIG_FILE);
            LOG.info("Server home is " + m_homeDir.toString());
            if (s_serverProfile == null) {
                LOG
                        .debug("fedora.serverProfile property not set... will always "
                                + "use param 'value' attributes from configuration for param values.");
            } else {
                LOG.debug("fedora.serverProfile property was '"
                        + s_serverProfile + "'... will use param '"
                        + s_serverProfile + "value' attributes from "
                        + "configuration for param values, falling back to "
                        + "'value' attributes where unspecified.");
            }
            LOG.debug("Loading and validating configuration file \""
                    + configFile + "\"");

            // do the parsing and validation of configuration
            Map serverParams = loadParameters(rootConfigElement, "");

            // get the module and datastore info, remove the holding element,
            // and set the server params so they can be seen via getParameter()
            ArrayList mdInfo = (ArrayList) serverParams.get(null);
            HashMap moduleParams = (HashMap) mdInfo.get(0);
            HashMap moduleClassNames = (HashMap) mdInfo.get(1);
            HashMap datastoreParams = (HashMap) mdInfo.get(2);
            serverParams.remove(null);
            setParameters(serverParams);

            // ensure server's module roles are met
            String[] reqRoles = getRequiredModuleRoles();
            for (String element : reqRoles) {
                if (moduleParams.get(element) == null) {
                    throw new ServerInitializationException(MessageFormat
                            .format(INIT_SERVER_SEVERE_UNFULFILLEDROLE,
                                    new Object[] {element}));
                }
            }

            // initialize the server
            m_statusFile.append(ServerState.STARTING, "Initializing Server");
            initServer();

            // create the datastore configs and set the instance variable
            // so they can be seen with getDatastoreConfig(...)
            Iterator dspi = datastoreParams.keySet().iterator();
            m_datastoreConfigs = new HashMap();
            while (dspi.hasNext()) {
                String id = (String) dspi.next();
                m_datastoreConfigs
                        .put(id, new DatastoreConfig((HashMap) datastoreParams
                                .get(id)));
            }

            // initialize each module
            m_statusFile.append(ServerState.STARTING, "Initializing Modules");
            Iterator mRoles = moduleParams.keySet().iterator();
            while (mRoles.hasNext()) {
                String role = (String) mRoles.next();
                String className = (String) moduleClassNames.get(role);
                LOG.info("Initializing " + className);
                try {
                    Class moduleClass = Class.forName(className);
                    Class param1Class =
                            Class.forName(MODULE_CONSTRUCTOR_PARAM1_CLASS);
                    Class param2Class =
                            Class.forName(MODULE_CONSTRUCTOR_PARAM2_CLASS);
                    Class param3Class =
                            Class.forName(MODULE_CONSTRUCTOR_PARAM3_CLASS);
                    LOG.debug("Getting constructor " + className + "("
                            + MODULE_CONSTRUCTOR_PARAM1_CLASS + ","
                            + MODULE_CONSTRUCTOR_PARAM2_CLASS + ","
                            + MODULE_CONSTRUCTOR_PARAM3_CLASS + ")");
                    Constructor moduleConstructor =
                            moduleClass.getConstructor(new Class[] {
                                    param1Class, param2Class, param3Class});
                    Module inst =
                            (Module) moduleConstructor
                                    .newInstance(new Object[] {
                                            moduleParams.get(role),
                                            (Server) this, role});
                    m_loadedModules.put(role, inst);
                } catch (ClassNotFoundException cnfe) {
                    throw new ModuleInitializationException(MessageFormat
                            .format(INIT_MODULE_SEVERE_CLASSNOTFOUND,
                                    new Object[] {className}), role);
                } catch (IllegalAccessException iae) {
                    // improbable
                    throw new ModuleInitializationException(MessageFormat
                            .format(INIT_MODULE_SEVERE_ILLEGALACCESS,
                                    new Object[] {className}), role);
                } catch (IllegalArgumentException iae) {
                    // improbable
                    throw new ModuleInitializationException(MessageFormat
                            .format(INIT_MODULE_SEVERE_BADARGS,
                                    new Object[] {className}), role);
                } catch (InstantiationException ie) {
                    throw new ModuleInitializationException(MessageFormat
                            .format(INIT_MODULE_SEVERE_MISSINGCONSTRUCTOR,
                                    new Object[] {className}), role);
                } catch (NoSuchMethodException nsme) {
                    throw new ModuleInitializationException(MessageFormat
                            .format(INIT_MODULE_SEVERE_ISABSTRACT,
                                    new Object[] {className}), role);
                } catch (InvocationTargetException ite) {
                    // throw the constructor's thrown exception, if any
                    try {
                        throw ite.getCause(); // as of java 1.4
                    } catch (ModuleInitializationException mie) {
                        throw mie;
                    } catch (Throwable t) {
                        // a runtime error..shouldn't happen, but if it does...
                        StringBuffer s = new StringBuffer();
                        s.append(t.getClass().getName());
                        s.append(": ");
                        for (int i = 0; i < t.getStackTrace().length; i++) {
                            s.append(t.getStackTrace()[i] + "\n");
                        }
                        throw new ModuleInitializationException(s.toString(),
                                                                role);
                    }
                }

            }

            // Do postInitModule for all Modules, verifying beforehand that
            // the required module roles (dependencies) have been fulfilled
            // for that module.
            m_statusFile.append(ServerState.STARTING,
                                "Post-Initializing Modules");
            mRoles = moduleParams.keySet().iterator();
            while (mRoles.hasNext()) {
                String r = (String) mRoles.next();
                Module m = getModule(r);
                LOG.info("Post-Initializing " + m.getClass().getName());
                reqRoles = m.getRequiredModuleRoles();
                LOG.debug("verifying dependencies have been loaded...");
                for (String element : reqRoles) {
                    if (getModule(element) == null) {
                        throw new ModuleInitializationException(MessageFormat
                                .format(INIT_MODULE_SEVERE_UNFULFILLEDROLE,
                                        new Object[] {element}), r);
                    }
                }
                LOG.debug(reqRoles.length + " dependencies, all loaded, ok.");
                m.postInitModule();
            }

            // Do postInitServer for the Server instance
            LOG.debug("Post-initializing server");
            postInitServer();

            // flag that we're done initting
            LOG.info("Server startup complete");
            m_initialized = true;
        } catch (ServerInitializationException sie) {
            // these are caught and rethrown for two reasons:
            // 1) so they can be logged in the startup log, and
            // 2) so an attempt can be made to free resources tied up thus far
            //    via shutdown()
            LOG.fatal("Server failed to initialize", sie);
            try {
                shutdown(null);
            } catch (Throwable th) {
                LOG.warn("Error shutting down server after failed startup", th);
            }
            throw sie;
        } catch (ModuleInitializationException mie) {
            LOG.fatal("Module (" + mie.getRole() + ") failed to initialize",
                      mie);
            try {
                shutdown(null);
            } catch (Throwable th) {
                LOG.warn("Error shutting down server after failed startup", th);
            }
            throw mie;
        } catch (Throwable th) {
            String msg = "Fatal error while starting server";
            LOG.fatal(msg, th);
            try {
                shutdown(null);
            } catch (Throwable oth) {
                LOG
                        .warn("Error shutting down server after failed startup",
                              oth);
            }
            throw new RuntimeException(msg, th);
        }
    }

    protected boolean overrideModuleRole(String moduleRole) {
        return false;
    }

    protected String overrideModuleClass(String moduleClass) {
        return null;
    }

    /**
     * Configures Log4J using FEDORA_HOME/config/log4j.properties.
     */
    protected static void configureLog4J(String extension)
            throws ServerInitializationException {

        File fedoraHome = new File(Constants.FEDORA_HOME);
        File serverDir = new File(fedoraHome, "server");
        File logDir = new File(serverDir, "logs");
        logDir.mkdirs();
        
        Map<String, String> options = new HashMap<String, String>();
        options.put("logDir", logDir.getPath());
        options.put("extension", extension);

        File propFile = new File(serverDir, "config/log4j.properties");
        
        try {
            Log4J.initFromPropFile(propFile, options);
        } catch (Exception e) {
            throw new ServerInitializationException("Error initializing from "
                    + "log4j configuration file: " + propFile.getPath(), e);
        }

        LOG = Logger.getLogger(Server.class.getName());
    }

    /**
     * Builds and returns a <code>Map</code> of parameter name-value pairs
     * defined as children of the given <code>Element</code>, according to
     * the server configuration schema.
     * <p>
     * </p>
     * If the given element is a CONFIG_ELEMENT_ROOT, this method will return
     * (along with the server's parameter name-value pairs) a <code>null</code>-keyed
     * value, which is an <code>ArrayList</code> of three <code>HashMap</code>
     * objects. The first will contain the name-value pair HashMaps of each of
     * the CONFIG_ELEMENT_MODULE elements found (in a <code>HashMap</code>
     * keyed by <i>role</i>), the second will contain a <code>HashMap</code>
     * mapping module <i>role</i>s to implementation classnames, and the third
     * will contain the the name-value pair <code>HashMaps</code> of each of
     * the CONFIG_ELEMENT_DATASTORE elements found (keyed by
     * CONFIG_ATTRIBUTE_ID).
     * 
     * @param element
     *        The element containing the name-value pair defintions.
     * @param dAttribute
     *        The name of the attribute of the <code>Element</code> whose
     *        value will distinguish this element from others that may occur in
     *        the <code>Document</code>. If there is no distinguishing
     *        attribute, this should be an empty string.
     */
    private final Map loadParameters(Element element, String dAttribute)
            throws ServerInitializationException {
        Map params = new HashMap();
        if (element.getLocalName().equals(CONFIG_ELEMENT_ROOT)) {
            ArrayList moduleAndDatastreamInfo = new ArrayList(3);
            moduleAndDatastreamInfo.add(new HashMap());
            moduleAndDatastreamInfo.add(new HashMap());
            moduleAndDatastreamInfo.add(new HashMap());
            params.put(null, moduleAndDatastreamInfo);
        }
        LOG.debug(MessageFormat.format(INIT_CONFIG_CONFIG_EXAMININGELEMENT,
                                       new Object[] {element.getLocalName(),
                                               dAttribute}));
        for (int i = 0; i < element.getChildNodes().getLength(); i++) {
            Node n = element.getChildNodes().item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (n.getLocalName().equals(CONFIG_ELEMENT_PARAM)) {
                    // if name-value pair, save in the HashMap
                    NamedNodeMap attrs = n.getAttributes();
                    Node nameNode =
                            attrs.getNamedItemNS(CONFIG_NAMESPACE,
                                                 CONFIG_ATTRIBUTE_NAME);
                    if (nameNode == null) {
                        nameNode = attrs.getNamedItem(CONFIG_ATTRIBUTE_NAME);
                    }
                    Node valueNode = null;
                    if (s_serverProfile != null) {
                        valueNode =
                                attrs.getNamedItemNS(CONFIG_NAMESPACE,
                                                     s_serverProfile + "value");
                        if (valueNode == null) {
                            valueNode =
                                    attrs.getNamedItem(s_serverProfile
                                            + "value");
                        }
                    }
                    if (valueNode == null) {
                        valueNode =
                                attrs.getNamedItemNS(CONFIG_NAMESPACE,
                                                     CONFIG_ATTRIBUTE_VALUE);
                        if (valueNode == null) {
                            valueNode =
                                    attrs.getNamedItem(CONFIG_ATTRIBUTE_VALUE);
                        }
                        if (nameNode == null || valueNode == null) {
                            throw new ServerInitializationException(INIT_CONFIG_SEVERE_INCOMPLETEPARAM);
                        }
                    }
                    if (nameNode.getNodeValue().equals("")
                            || valueNode.getNodeValue().equals("")) {
                        throw new ServerInitializationException(MessageFormat
                                .format(INIT_CONFIG_SEVERE_INCOMPLETEPARAM,
                                        new Object[] {CONFIG_ELEMENT_PARAM,
                                                CONFIG_ATTRIBUTE_NAME,
                                                CONFIG_ATTRIBUTE_VALUE}));
                    }
                    if (params.get(nameNode.getNodeValue()) != null) {
                        throw new ServerInitializationException(MessageFormat
                                .format(INIT_CONFIG_SEVERE_REASSIGNMENT,
                                        new Object[] {CONFIG_ELEMENT_PARAM,
                                                CONFIG_ATTRIBUTE_NAME,
                                                nameNode.getNodeValue()}));
                    }
                    params.put(nameNode.getNodeValue(), valueNode
                            .getNodeValue());
                    LOG.debug(MessageFormat
                            .format(INIT_CONFIG_CONFIG_PARAMETERIS,
                                    new Object[] {nameNode.getNodeValue(),
                                            valueNode.getNodeValue()}));
                } else if (!n.getLocalName().equals(CONFIG_ELEMENT_COMMENT)) {
                    if (element.getLocalName().equals(CONFIG_ELEMENT_ROOT)) {
                        if (n.getLocalName().equals(CONFIG_ELEMENT_MODULE)) {
                            NamedNodeMap attrs = n.getAttributes();
                            Node roleNode =
                                    attrs.getNamedItemNS(CONFIG_NAMESPACE,
                                                         CONFIG_ATTRIBUTE_ROLE);
                            if (roleNode == null) {
                                roleNode =
                                        attrs
                                                .getNamedItem(CONFIG_ATTRIBUTE_ROLE);
                                if (roleNode == null) {
                                    throw new ServerInitializationException(INIT_CONFIG_SEVERE_NOROLEGIVEN);
                                }
                            }
                            String moduleRole = roleNode.getNodeValue();
                            if (moduleRole.equals("")) {
                                throw new ServerInitializationException(INIT_CONFIG_SEVERE_NOROLEGIVEN);
                            }
                            if (overrideModuleRole(moduleRole)) {
                                continue;
                            }
                            HashMap moduleImplHash =
                                    (HashMap) ((ArrayList) params.get(null))
                                            .get(1);
                            if (moduleImplHash.get(moduleRole) != null) {
                                throw new ServerInitializationException(MessageFormat
                                        .format(INIT_CONFIG_SEVERE_REASSIGNMENT,
                                                new Object[] {
                                                        CONFIG_ELEMENT_MODULE,
                                                        CONFIG_ATTRIBUTE_ROLE,
                                                        moduleRole}));
                            }
                            Node classNode =
                                    attrs
                                            .getNamedItemNS(CONFIG_NAMESPACE,
                                                            CONFIG_ATTRIBUTE_CLASS);
                            if (classNode == null) {
                                classNode =
                                        attrs
                                                .getNamedItem(CONFIG_ATTRIBUTE_CLASS);
                                if (classNode == null) {
                                    throw new ServerInitializationException(INIT_CONFIG_SEVERE_NOCLASSGIVEN);
                                }
                            }
                            String moduleClass = classNode.getNodeValue();
                            if (overrideModuleClass(moduleClass) != null) {
                                moduleClass = overrideModuleClass(moduleClass);
                            }
                            if (moduleClass.equals("")) {
                                throw new ServerInitializationException(INIT_CONFIG_SEVERE_NOCLASSGIVEN);
                            }
                            moduleImplHash.put(moduleRole, moduleClass);
                            ((HashMap) ((ArrayList) params.get(null)).get(0))
                                    .put(moduleRole,
                                         loadParameters((Element) n,
                                                        CONFIG_ATTRIBUTE_ROLE
                                                                + "=\""
                                                                + moduleRole
                                                                + "\""));
                        } else if (n.getLocalName()
                                .equals(CONFIG_ELEMENT_DATASTORE)) {
                            NamedNodeMap attrs = n.getAttributes();
                            Node idNode =
                                    attrs.getNamedItemNS(CONFIG_NAMESPACE,
                                                         CONFIG_ATTRIBUTE_ID);
                            if (idNode == null) {
                                idNode =
                                        attrs.getNamedItem(CONFIG_ATTRIBUTE_ID);
                                if (idNode == null) {
                                    throw new ServerInitializationException(INIT_CONFIG_SEVERE_NOIDGIVEN);
                                }
                            }
                            String dConfigId = idNode.getNodeValue();
                            if (dConfigId.equals("")) {
                                throw new ServerInitializationException(INIT_CONFIG_SEVERE_NOIDGIVEN);
                            }
                            HashMap dParamHash =
                                    (HashMap) ((ArrayList) params.get(null))
                                            .get(2);
                            if (dParamHash.get(dConfigId) != null) {
                                throw new ServerInitializationException(MessageFormat
                                        .format(INIT_CONFIG_SEVERE_REASSIGNMENT,
                                                new Object[] {
                                                        CONFIG_ELEMENT_DATASTORE,
                                                        CONFIG_ATTRIBUTE_ID,
                                                        dConfigId}));
                            }
                            dParamHash.put(dConfigId,
                                           loadParameters((Element) n,
                                                          CONFIG_ATTRIBUTE_ID
                                                                  + "=\""
                                                                  + dConfigId
                                                                  + "\""));
                        } else {
                            throw new ServerInitializationException(MessageFormat
                                    .format(INIT_CONFIG_SEVERE_BADELEMENT,
                                            new Object[] {n.getLocalName()}));
                        }
                    }
                }

            } // else { // ignore non-Element nodes }
        }
        return params;
    }

    /**
     * Tells whether the server (and loaded modules) have initialized.
     * <p>
     * </p>
     * This is useful for threaded <code>Modules</code> that need to wait
     * until all initialization has occurred before doing something.
     * 
     * @return whether initialization has completed.
     */
    public final boolean hasInitialized() {
        return m_initialized;
    }

    /**
     * Get the status file for the server. Important messages pertaining to
     * startup and shutdown go here.
     */
    public ServerStatusFile getStatusFile() {
        return m_statusFile;
    }

    public final static boolean hasInstance(File homeDir) {
        return s_instances.get(homeDir) != null;
    }

    public final String status(Context context) throws AuthzException {
        ((Authorization) getModule("fedora.server.security.Authorization"))
                .enforceServerStatus(context);
        return "RUNNING";
    }

    public final static Server getInstance(File homeDir, boolean okToStart)
            throws ServerInitializationException, ModuleInitializationException {
        if (okToStart) {
            return getInstance(homeDir);
        } else {
            Server instance = (Server) s_instances.get(homeDir);
            if (instance == null) {
                throw new ServerInitializationException("The Fedora server is not yet running.");
            } else {
                return instance;
            }
        }

    }

    /**
     * Provides an instance of the server specified in the configuration file at
     * homeDir/CONFIG_DIR/CONFIG_FILE, or DEFAULT_SERVER_CLASS if unspecified.
     * 
     * @param homeDir
     *        The base directory for the server.
     * @return The instance.
     * @throws ServerInitializationException
     *         If there was an error starting the server.
     * @throws ModuleInitializationException
     *         If there was an error starting a module.
     */
    public final static synchronized Server getInstance(File homeDir)
            throws ServerInitializationException, ModuleInitializationException {
        // return an instance if already in memory
        Server instance = (Server) s_instances.get(homeDir);
        if (instance != null) {
            return instance;
        }

        configureLog4J(".log");
        LOG.info("Starting up server");

        // else instantiate a new one given the class provided in the
        // root element in the config file and return it
        File configFile = null;
        try {
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            configFile =
                    new File(homeDir + File.separator + "server"
                            + File.separator + CONFIG_DIR + File.separator
                            + CONFIG_FILE);
            // suck it in
            Element rootElement =
                    builder.parse(configFile).getDocumentElement();
            // ensure root element name ok
            if (!rootElement.getLocalName().equals(CONFIG_ELEMENT_ROOT)) {
                throw new ServerInitializationException(MessageFormat
                        .format(INIT_CONFIG_SEVERE_BADROOTELEMENT,
                                new Object[] {configFile, CONFIG_ELEMENT_ROOT,
                                        rootElement.getLocalName()}));
            }
            // ensure namespace specified properly
            if (!rootElement.getNamespaceURI().equals(CONFIG_NAMESPACE)) {
                throw new ServerInitializationException(MessageFormat
                        .format(INIT_CONFIG_SEVERE_BADNAMESPACE, new Object[] {
                                configFile, CONFIG_NAMESPACE}));
            }
            // select <server class="THIS_PART"> .. </server>
            String className = rootElement.getAttribute(CONFIG_ATTRIBUTE_CLASS);
            if (className.equals("")) {
                className =
                        rootElement.getAttributeNS(CONFIG_NAMESPACE,
                                                   CONFIG_ATTRIBUTE_CLASS);
                if (className.equals("")) {
                    className = DEFAULT_SERVER_CLASS;
                }
            }
            try {
                Class serverClass = Class.forName(className);
                Class param1Class =
                        Class.forName(SERVER_CONSTRUCTOR_PARAM1_CLASS);
                Class param2Class =
                        Class.forName(SERVER_CONSTRUCTOR_PARAM2_CLASS);
                Constructor serverConstructor =
                        serverClass.getConstructor(new Class[] {param1Class,
                                param2Class});
                Server inst =
                        (Server) serverConstructor.newInstance(new Object[] {
                                rootElement, homeDir});
                s_instances.put(homeDir, inst);
                return inst;
            } catch (ClassNotFoundException cnfe) {
                throw new ServerInitializationException(MessageFormat
                        .format(INIT_SERVER_SEVERE_CLASSNOTFOUND,
                                new Object[] {className}));
            } catch (IllegalAccessException iae) {
                // improbable
                throw new ServerInitializationException(MessageFormat
                        .format(INIT_SERVER_SEVERE_ILLEGALACCESS,
                                new Object[] {className}));
            } catch (IllegalArgumentException iae) {
                // improbable
                throw new ServerInitializationException(MessageFormat
                        .format(INIT_SERVER_SEVERE_BADARGS,
                                new Object[] {className}));
            } catch (InstantiationException ie) {
                throw new ServerInitializationException(MessageFormat
                        .format(INIT_SERVER_SEVERE_MISSINGCONSTRUCTOR,
                                new Object[] {className}));
            } catch (NoSuchMethodException nsme) {
                throw new ServerInitializationException(MessageFormat
                        .format(INIT_SERVER_SEVERE_ISABSTRACT,
                                new Object[] {className}));
            } catch (InvocationTargetException ite) {
                // throw the constructor's thrown exception, if any
                try {
                    throw ite.getCause(); // as of java 1.4
                } catch (ServerInitializationException sie) {
                    throw sie;
                } catch (ModuleInitializationException mie) {
                    throw mie;
                } catch (Throwable t) {
                    // a runtime error..shouldn't happen, but if it does...
                    StringBuffer s = new StringBuffer();
                    s.append(t.getClass().getName());
                    s.append(": ");
                    for (int i = 0; i < t.getStackTrace().length; i++) {
                        s.append(t.getStackTrace()[i] + "\n");
                    }
                    throw new ServerInitializationException(s.toString());
                }
            }
        } catch (ParserConfigurationException pce) {
            throw new ServerInitializationException(INIT_XMLPARSER_SEVERE_MISSING);
        } catch (FactoryConfigurationError fce) {
            throw new ServerInitializationException(INIT_XMLPARSER_SEVERE_MISSING);
        } catch (IOException ioe) {
            throw new ServerInitializationException(MessageFormat
                    .format(INIT_CONFIG_SEVERE_UNREADABLE, new Object[] {
                            configFile, ioe.getMessage()}));
        } catch (IllegalArgumentException iae) {
            throw new ServerInitializationException(MessageFormat
                    .format(INIT_CONFIG_SEVERE_UNREADABLE, new Object[] {
                            configFile, iae.getMessage()}));
        } catch (SAXException saxe) {
            throw new ServerInitializationException(MessageFormat
                    .format(INIT_CONFIG_SEVERE_MALFORMEDXML, new Object[] {
                            configFile, saxe.getMessage()}));
        }
    }

    /**
     * Gets the server's home directory.
     * 
     * @return The directory.
     */
    public final File getHomeDir() {
        return m_homeDir;
    }

    /**
     * Gets a loaded <code>Module</code>.
     * 
     * @param role
     *        The role of the <code>Module</code> to get.
     * @return The <code>Module</code>, <code>null</code> if not found.
     */
    public final Module getModule(String role) {
        return (Module) m_loadedModules.get(role);
    }

    /**
     * Gets a <code>DatastoreConfig</code>.
     * 
     * @param id
     *        The id as given in the server configuration.
     * @return The <code>DatastoreConfig</code>, <code>null</code> if not
     *         found.
     */
    public final DatastoreConfig getDatastoreConfig(String id) {
        return (DatastoreConfig) m_datastoreConfigs.get(id);
    }

    public Iterator<String> datastoreConfigIds() {
        return m_datastoreConfigs.keySet().iterator();
    }

    /**
     * Gets an <code>Iterator</code> over the roles that have been loaded.
     * 
     * @return (<code>String</code>s) The roles.
     */
    public final Iterator<String> loadedModuleRoles() {
        return m_loadedModules.keySet().iterator();
    }

    /**
     * Performs any server start-up tasks particular to this type of Server.
     * <p>
     * </p>
     * This is guaranteed to be run before any modules are loaded. The default
     * implementation does nothing.
     * 
     * @throws ServerInitializationException
     *         If a severe server startup-related error occurred.
     */
    protected void initServer() throws ServerInitializationException {
        if (1 == 2) {
            throw new ServerInitializationException(null);
        }
    }

    /**
     * Second stage of Server initialization.
     * <p>
     * </p>
     * This is guaranteed to be run after all Modules have been loaded and all
     * module initialization (initModule() and postInitModule()) has taken
     * place. The default implementation does nothing.
     * 
     * @throws ServerInitializationException
     *         If a severe server startup-related error occurred.
     */
    protected void postInitServer() throws ServerInitializationException {
        if (1 == 2) {
            throw new ServerInitializationException(null);
        }
    }

    /**
     * Performs shutdown tasks for the modules and the server.
     * <p>
     * </p>
     * All loaded modules' shutdownModule() methods are called, then
     * shutdownServer is called.
     * <p>
     * </p>
     * <h3>How to Ensure Clean Server Shutdown</h3>
     * <p>
     * </p>
     * After having used a <code>Server</code> instance, if you know your
     * program is the only client of the <code>Server</code> in the VM
     * instance, you should make an explicit call to this method so that you can
     * catch and handle its exceptions properly. If you are usure or know that
     * there may be at least one other client of the <code>Server</code> in
     * the VM instance, you should call <code>System.runFinalization()</code>
     * after ensuring you no longer have a reference. In this case, if there is
     * no other reference to the object in the VM, finalization will be called
     * (but you will be unable to catch <code>ShutdownException</code>
     * variants, if thrown).
     * <p>
     * </p>
     * Right before this is finished, the instance is removed from the server
     * instances map.
     * 
     * @throws ServerShutdownException
     *         If a severe server shutdown-related error occurred.
     *         USER_REPRESENTED = addName(new XacmlName(this,
     *         "subjectRepresented"));
     * @throws ModuleShutdownException
     *         If a severe module shutdown-related error occurred.
     */
    public final void shutdown(Context context) throws ServerShutdownException,
            ModuleShutdownException, AuthzException {
        Iterator roleIterator = loadedModuleRoles();
        LOG.info("Shutting down server");
        ModuleShutdownException mse = null;
        while (roleIterator.hasNext()) {
            Module m = getModule((String) roleIterator.next());
            String mName = m.getClass().getName();
            try {
                LOG.info("Shutting down " + mName);
                m.shutdownModule();
            } catch (ModuleShutdownException e) {
                LOG.warn("Error shutting down module " + mName, e);
                mse = e;
            }
        }
        shutdownServer();
        LOG.info("Server shutdown complete");
        s_instances.remove(getHomeDir());
        if (mse != null) {
            throw mse;
        }
    }

    /**
     * Performs shutdown tasks for the server itself. This should be written so
     * that system resources are always freed, regardless of whether there is an
     * error. If an error occurs, it should be thrown as a
     * <code>ServerShutdownException</code> after attempts to free every
     * resource have been made.
     * 
     * @throws ServerShutdownException
     *         If a severe server shutdown-related error occurred.
     */
    protected void shutdownServer() throws ServerShutdownException {
        if (1 == 2) {
            throw new ServerShutdownException(null);
        }
    }

    /**
     * Calls <code>shutdown()</code> when finalization occurs.
     * 
     * @throws ServerShutdownException
     *         If a severe server shutdown-related error occurred.
     * @throws ModuleShutdownException
     *         If a severe module shutdown-related error occurred.
     */
    public final void finalize() throws ServerShutdownException,
            ModuleShutdownException {
        shutdownServer();
    }

    public final static Locale getLocale() {
        if (s_locale == null) {
            String language = System.getProperty("locale.language");
            String country = System.getProperty("locale.country");
            String variant = System.getProperty("locale.variant");
            if (language != null && country != null) {
                if (variant != null) {
                    s_locale = new Locale(language, country, variant);
                } else {
                    s_locale = new Locale(language, country);
                }
            } else {
                s_locale = Locale.getDefault();
            }
        }
        return s_locale;
    }

    public String getConfigSummary() {
        int i;
        StringBuffer out = new StringBuffer();
        out.append("[ Fedora Server Configuration Summary ]\n\n");
        out.append("Server class     : " + this.getClass().getName() + "\n");
        out.append("Required modules : ");
        String padding = "                   ";
        String[] roles = getRequiredModuleRoles();
        if (roles.length == 0) {
            out.append("<none>\n");
        } else {
            for (i = 0; i < roles.length; i++) {
                if (i > 0) {
                    out.append(padding);
                }
                out.append(roles[i] + "\n");
            }
        }
        out.append("Parameters       : ");
        Iterator iter = parameterNames();
        i = 0;
        while (iter.hasNext()) {
            String name = (String) iter.next();
            String value = getParameter(name);
            if (i > 0) {
                out.append(padding);
            }
            out.append(name + "=" + value + "\n");
            i++;
        }
        if (i == 0) {
            out.append("<none>\n");
        }

        iter = loadedModuleRoles();
        while (iter.hasNext()) {
            String role = (String) iter.next();
            out.append("\nLoaded Module : " + role + "\n");
            Module module = getModule(role);
            out.append("Class         : " + module.getClass().getName() + "\n");
            out.append("Dependencies  : "
                    + module.getRequiredModuleRoles().length + "\n");
            for (i = 0; i < module.getRequiredModuleRoles().length; i++) {
                out.append("Dependency    : "
                        + module.getRequiredModuleRoles()[i] + "\n");
            }
            out.append("Parameters    : ");
            padding = "                ";
            i = 0;
            Iterator iter2 = module.parameterNames();
            while (iter2.hasNext()) {
                String name = (String) iter2.next();
                String value = module.getParameter(name);
                if (i > 0) {
                    out.append(padding);
                }
                out.append(name + "=" + value + "\n");
                i++;
            }
            if (i == 0) {
                out.append("<none>\n");
            }
        }

        iter = datastoreConfigIds();
        while (iter.hasNext()) {
            String id = (String) iter.next();
            out.append("\nDatastore Cfg : " + id + "\n");
            out.append("Parameters    : ");
            padding = "                ";
            i = 0;
            Iterator iter2 =
                    ((DatastoreConfig) getDatastoreConfig(id)).parameterNames();
            while (iter2.hasNext()) {
                String name = (String) iter2.next();
                String value =
                        ((DatastoreConfig) getDatastoreConfig(id))
                                .getParameter(name);
                if (i > 0) {
                    out.append(padding);
                }
                out.append(name + "=" + value + "\n");
                i++;
            }
            if (i == 0) {
                out.append("<none>\n");
            }
        }

        return out.toString();
    }

    // Wraps PID constructor, throwing a ServerException instead
    public static PID getPID(String pidString) throws MalformedPidException {
        try {
            return new PID(pidString);
        } catch (MalformedPIDException e) {
            throw new MalformedPidException(e.getMessage());
        }
    }

    // Wraps PID.fromFilename, throwing a ServerException instead
    public static PID pidFromFilename(String filename)
            throws MalformedPidException {
        try {
            return PID.fromFilename(filename);
        } catch (MalformedPIDException e) {
            throw new MalformedPidException(e.getMessage());
        }
    }

    /**
     * Get the current date from the context. If the context doesn't specify a
     * value for the current date, or the specified value cannot be parsed as an
     * ISO8601 date string, a GeneralException will be thrown.
     */
    public static Date getCurrentDate(Context context) throws GeneralException {

        String propName = Constants.ENVIRONMENT.CURRENT_DATE_TIME.uri;
        String dateTimeValue = context.getEnvironmentValue(propName);
        if (dateTimeValue == null) {
            throw new GeneralException("Missing value for environment "
                    + "context attribute: " + propName);
        }

        Date currentDate = DateUtility.convertStringToDate(dateTimeValue);
        if (currentDate == null) {
            throw new GeneralException("Unparsable dateTime string: '"
                    + dateTimeValue + "'");
        } else {
            return currentDate;
        }
    }
   
    /**
     * Gets the server configuration.
     * 
     * @return the server configuration.
     */
    public static ServerConfiguration getConfig() {
        try {
            InputStream fcfg = new FileInputStream(
                    new File(Constants.FEDORA_HOME,
                             "server/config/fedora.fcfg"));
            ServerConfigurationParser parser =
                new ServerConfigurationParser(fcfg);
            return parser.parse(); 
        } catch (IOException e) {
            throw new FaultException("Error loading server configuration",
                                     e);
        }
    }

}
