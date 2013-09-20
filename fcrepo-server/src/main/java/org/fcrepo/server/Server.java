/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.fcrepo.common.Constants;
import org.fcrepo.common.FaultException;
import org.fcrepo.common.MalformedPIDException;
import org.fcrepo.common.PID;
import org.fcrepo.common.http.WebClientConfiguration;
import org.fcrepo.server.config.DatastoreConfiguration;
import org.fcrepo.server.config.ModuleConfiguration;
import org.fcrepo.server.config.Parameter;
import org.fcrepo.server.config.ServerConfiguration;
import org.fcrepo.server.config.ServerConfigurationParser;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.MalformedPidException;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.errors.ModuleShutdownException;
import org.fcrepo.server.errors.ServerInitializationException;
import org.fcrepo.server.errors.ServerShutdownException;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.resourceIndex.ModelBasedTripleGenerator;
import org.fcrepo.server.security.Authorization;
import org.fcrepo.server.utilities.status.ServerState;
import org.fcrepo.server.utilities.status.ServerStatusFile;
import org.fcrepo.utilities.DateUtility;
import org.fcrepo.utilities.XmlTransformUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * The starting point for working with a Fedora repository. This class handles
 * loading, starting, and stopping modules (the module lifecycle), and provides
 * access to core constants.
 *
 * @author Chris Wilper
 */
public abstract class Server
        extends Pluggable implements ApplicationContextAware, BeanDefinitionRegistry, ListableBeanFactory {

    public static final boolean USE_CACHE = true;

    public static final boolean USE_DEFINITIVE_STORE = false;

    public static final boolean GLOBAL_CHOICE = false;

    private static final Logger logger =
            LoggerFactory.getLogger(Server.class);

    /**
     * The ResourceBundle that provides access to constants from
     * fedora/server/resources/Server.properties.
     */
    private static ResourceBundle s_const =
            ResourceBundle.getBundle("org.fcrepo.server.resources.Server");

    private static MetadataReaderFactory s_readerFactory = new SimpleMetadataReaderFactory();

    /** The version of this release. */
    public static String VERSION = s_const.getString("version");

    /** The build date of this release. */
    public static String BUILD_DATE = s_const.getString("buildDate");

    /** The name of the property that tells the server where it's based. */
    public static String HOME_PROPERTY = s_const.getString("home.property");

    /** The internal XML storage format for digital objects */
    public static String STORAGE_FORMAT = s_const.getString("format.storage");

    /** The directory where server configuration is stored, relative to home. */
    public static String CONFIG_DIR = s_const.getString("config.dir");

    /** The directory where Spring configurations are stored, relative to CONFIG_DIR. */
    public static String SPRING_DIR = s_const.getString("spring.dir");
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
     * The server's home directory; this is typically the 'server' subdirectory under $FEDORA_HOME.
     */
    private final File m_serverDir;

    private AbstractApplicationContext m_serverContext;
    private GenericApplicationContext m_moduleContext;

    /**
     * The server's directory for (temp) uploaded files
     */
    private final File m_uploadDir;

    /**
     * Datastore configurations initialized from the server config file.
     * Should now be handled by application context
     */
//    private Map<String, DatastoreConfig> m_datastoreConfigs;

    /**
     * Modules that have been loaded.
     */
    @Deprecated
    protected Map<String, Module> m_loadedModules;
    @Deprecated
    protected Set<String> m_loadedModuleRoles;

    /**
     * The server's configuration file
     */
    private final File m_configFile;
    /**
     * Is the server running?
     */
    private boolean m_initialized;

    /**
     * The server status File.
     */
    private final ServerStatusFile m_statusFile;

    /**
     * What server profile should be used?
     */
    private static String s_serverProfile =
            System.getProperty("fedora.serverProfile");

    /**
     * Web Client http connection configuration object
     */
    private WebClientConfiguration m_webClientConfig;

    /**
     * Initializes the Server from a Map of Strings (as per Module)
     */

    protected Server(Map<String,String> params, File homeDir)
        throws ServerInitializationException, ModuleInitializationException {

        setParameters(params);
        m_initialized = false;
        m_loadedModuleRoles = new HashSet<String>();
        m_serverDir = new File(homeDir, "server");
        m_uploadDir = new File(m_serverDir, "management/upload");
        try{
            m_statusFile = new ServerStatusFile(m_serverDir);
        } catch (Exception e) {
            throw new ServerInitializationException(e.toString());
        }
        File logDir = new File(m_serverDir, "logs");
        if (!logDir.exists()) {
            logDir.mkdir(); // try to create dir if doesn't exist
        }
        m_configFile = new File(m_serverDir + File.separator + CONFIG_DIR
                                + File.separator + CONFIG_FILE);
        Server.s_instances.put(homeDir, this);
    }

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
        this(Server.loadParameters(rootConfigElement, ""), homeDir);
    }

    public void init()  throws ServerInitializationException, ModuleInitializationException {
        logger.info("Registered server at {}", getHomeDir().getPath());
        try {
            if (m_serverContext == null) {
                m_serverContext = getDefaultContext();
            }
            m_moduleContext = new GenericApplicationContext(m_serverContext);
            registerBeanDefinition(CommonAnnotationBeanPostProcessor.class.getName(),
                                                   getScannedBeanDefinition(CommonAnnotationBeanPostProcessor.class.getName()));
            // Load bean definitions that should be override-able (ie, are new)

            loadSpringModules();

            // Default definition for ModelBasedTripleGenerator
            if (!knownBeanDefinition(ModelBasedTripleGenerator.class.getName())){
                ScannedGenericBeanDefinition tripleGen = getScannedBeanDefinition(ModelBasedTripleGenerator.class.getName());
                tripleGen.setScope(AbstractBeanDefinition.SCOPE_PROTOTYPE);
                registerBeanDefinition(ModelBasedTripleGenerator.class.getName(), tripleGen);
            }

            logger.info("Server home is " + m_serverDir.toString());
            if (s_serverProfile == null) {
                logger.debug("fedora.serverProfile property not set... will always "
                        + "use param 'value' attributes from configuration for param values.");
            } else {
                logger.debug("fedora.serverProfile property was '{}"
                        + "'... will use param '{}value' attributes from "
                        + "configuration for param values, falling back to "
                        + "'value' attributes where unspecified.", s_serverProfile, s_serverProfile);
            }
            logger.debug("Loading and validating configuration file \"{}\"",
                    m_configFile);

            // do the parsing and validation of configuration
            ServerConfiguration serverConfig = getConfig();
            List<DatastoreConfiguration> dsConfigs = serverConfig.getDatastoreConfigurations();
            List<ModuleConfiguration> moduleConfigs = serverConfig.getModuleConfigurations();

            // ensure server's module roles are met
            String[] reqRoles = getRequiredModuleRoles();
            for (String element : reqRoles) {
                if (!knownBeanDefinition(element) && serverConfig.getModuleConfiguration(element) == null) {
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
            m_statusFile.append(ServerState.STARTING, "Initializing datastore definitions");

            for (DatastoreConfiguration ds:dsConfigs) {
                String id = ds.getId();
//                m_datastoreConfigs
//                        .put(id, new DatastoreConfig((HashMap) datastoreParams
//                                .get(id)));
                logger.info("Loading fcfg datastore definitions for " + id);
                registerBeanDefinition(id, createDatastoreConfigurationBeanDefinition(id));
            }

            // Initialize the web client configuration
            initWebClientConfig();

            // initialize each module
            m_statusFile.append(ServerState.STARTING, "Loading Module Definitions");

            for (ModuleConfiguration mconfig:moduleConfigs) {
                String role = mconfig.getRole();
                String className = mconfig.getClassName();
                if (!knownBeanDefinition(role)){
                    logger.info("Loading bean definitions for {} impl class={}", className, role);
                    registerBeanDefinition(role,
                                           createModuleBeanDefinition(className, mconfig.getParameters(), role));
                } else {
                    logger.info("FCFG bean definitions for {} superceded by existing Spring bean definition", className);
                }
                if (!knownBeanDefinition(role+"Configuration")){
                    registerBeanDefinition(role+"Configuration",
                                           createModuleConfigurationBeanDefinition(role));
                }
            }
            registerBeanDefinitions();
            // initialize each module by getting Spring bean
            m_statusFile.append(ServerState.STARTING, "Initializing Modules");
            m_moduleContext.refresh(); // attach postprocessors to bean definitions
            String [] moduleNames = getBeanNamesForType(Module.class,false,true);
            for (String moduleName:moduleNames){
                getBean(moduleName);
            }


            // Do postInitModule for all Modules, verifying beforehand that
            // the required module roles (dependencies) have been fulfilled
            // for that module.
            m_statusFile.append(ServerState.STARTING,
                                "Post-Initializing Modules");
            for (String moduleName:moduleNames){
                Module m = getModule(moduleName);
                logger.info("Post-Initializing " + m.getClass().getName());
                reqRoles = m.getRequiredModuleRoles();
                logger.debug("verifying dependencies have been loaded...");
                for (String element : reqRoles) {
                    if (getModule(element) == null) {
                        throw new ModuleInitializationException(MessageFormat
                                .format(INIT_MODULE_SEVERE_UNFULFILLEDROLE,
                                        new Object[] {element}), moduleName);
                    }
                }
                logger.debug(reqRoles.length + " dependencies, all loaded, ok.");
                m.postInitModule();
            }

            // Do postInitServer for the Server instance
            logger.debug("Post-initializing server");
            postInitServer();

            // flag that we're done initting
            logger.info("Server startup complete");
            m_initialized = true;
        } catch (ServerInitializationException sie) {
            // these are caught and rethrown for two reasons:
            // 1) so they can be logged in the startup log, and
            // 2) so an attempt can be made to free resources tied up thus far
            //    via shutdown()
            logger.error("Server failed to initialize", sie);
            try {
                shutdown(null);
            } catch (Throwable th) {
                logger.warn("Error shutting down server after failed startup", th);
            }
            throw sie;
        } catch (ModuleInitializationException mie) {
            logger.error("Module (" + mie.getRole() + ") failed to initialize",
                      mie);
            try {
                shutdown(null);
            } catch (Throwable th) {
                logger.warn("Error shutting down server after failed startup", th);
            }
            throw mie;
        } catch (Throwable th) {
            String msg = "Fatal error while starting server";
            logger.error(msg, th);
            try {
                shutdown(null);
            } catch (Throwable oth) {
                logger.warn("Error shutting down server after failed startup",
                              oth);
            }
            throw new RuntimeException(msg, th);
        }
    }

    protected AbstractApplicationContext getDefaultContext() throws IOException {
        GenericApplicationContext appContext = new GenericApplicationContext();
        appContext.refresh(); // init event multicaster to avoid synch issue
        appContext.registerBeanDefinition(MODULE_CONSTRUCTOR_PARAM2_CLASS, getServerBeanDefinition());
        appContext.getBeanFactory().registerSingleton(MODULE_CONSTRUCTOR_PARAM2_CLASS, this);
        appContext.registerBeanDefinition(ServerConfiguration.class.getName(), getServerConfigurationBeanDefinition());
        ScannedGenericBeanDefinition moduleDef = getScannedBeanDefinition(Module.class.getName());
        moduleDef.setAbstract(true);
        moduleDef.setInitMethodName("initModule");
        moduleDef.setDestroyMethodName("shutdownModule");
        appContext.registerBeanDefinition(Module.class.getName(), moduleDef);
        return appContext;
    }

    /**
     * This constructor is a compatibility bridge to allow
     *  the getInstance factory method to be used by Spring contexts
     * @param homeDir
     * @throws ServerInitializationException
     * @throws ModuleInitializationException
     */
    protected Server(File homeDir)
            throws ServerInitializationException, ModuleInitializationException {
        this(getConfigElement(homeDir),homeDir);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        m_serverContext = (AbstractApplicationContext)applicationContext;
    }

    protected BeanDefinition getServerBeanDefinition(){
        GenericBeanDefinition result = new GenericBeanDefinition();
        result.setAutowireCandidate(true);
        result.setScope(BeanDefinition.SCOPE_SINGLETON);
        result.setBeanClass(this.getClass());
        result.setAttribute("id", MODULE_CONSTRUCTOR_PARAM2_CLASS);
        result.setAttribute("name", MODULE_CONSTRUCTOR_PARAM2_CLASS);
        return result;
    }

    /**
     * Provide a generic bean definition if the Server was not created by Spring
     * @return
     */
    protected static BeanDefinition getServerConfigurationBeanDefinition(){
        String className = ServerConfiguration.class.getName();
        GenericBeanDefinition result = new GenericBeanDefinition();
        result.setAutowireCandidate(true);
        result.setScope(BeanDefinition.SCOPE_SINGLETON);
        result.setBeanClass(Server.class);
        result.setFactoryMethodName("getConfig");
        result.setAttribute("id", className);
        result.setAttribute("name", className);
        return result;
    }

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        String beanClassName = overrideModuleClass(beanDefinition.getBeanClassName());
        if (beanClassName != null) beanDefinition.setBeanClassName(beanClassName);
        if (overrideModuleRole(beanName)){
            beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
            if (beanDefinition instanceof GenericBeanDefinition){
                ((GenericBeanDefinition)beanDefinition).setAbstract(true);
            }
        }
        m_moduleContext.registerBeanDefinition(beanName, beanDefinition);
    }

    /**
     * Register any implementation-specific bean definitions before the context is refreshed.
     * @throws ServerInitializationException
     */
    protected void registerBeanDefinitions() throws ServerInitializationException {
    }

    protected static ScannedGenericBeanDefinition getScannedBeanDefinition(String className)
        throws IOException {
        MetadataReader reader = s_readerFactory.getMetadataReader(className);
        ScannedGenericBeanDefinition beanDefinition = new ScannedGenericBeanDefinition(reader);
        return beanDefinition;
    }

    /**
     * Generates Spring Bean definitions for Fedora Modules.
     * Server param should be unnecessary if autowired.
     * @param className
     * @param server
     * @param params
     * @param role
     * @return
     */
    protected static GenericBeanDefinition createModuleBeanDefinition(String className, Map<String,String> params, String role)
        throws IOException {
        ScannedGenericBeanDefinition result = getScannedBeanDefinition(className);
        result.setParentName(Module.class.getName());
        result.setScope(BeanDefinition.SCOPE_SINGLETON);
        result.setAttribute("id", role);
        result.setAttribute("name", role);
        result.setAttribute("init-method", "initModule");
        result.setEnforceInitMethod(true);
        result.setAttribute("destroy-method", "shutdownModule");
        result.setEnforceDestroyMethod(true);

        ConstructorArgumentValues cArgs = new ConstructorArgumentValues();
        cArgs.addIndexedArgumentValue(0, params,MODULE_CONSTRUCTOR_PARAM1_CLASS);
        // one server bean in context
        BeanReference serverRef = new RuntimeBeanReference(MODULE_CONSTRUCTOR_PARAM2_CLASS);
        cArgs.addIndexedArgumentValue(1, serverRef);
        cArgs.addIndexedArgumentValue(2, role,MODULE_CONSTRUCTOR_PARAM3_CLASS);
        result.setConstructorArgumentValues(cArgs);
        return result;
    }

    protected static GenericBeanDefinition createModuleConfigurationBeanDefinition(String role){
        GenericBeanDefinition result = new GenericBeanDefinition();
        result.setScope(BeanDefinition.SCOPE_SINGLETON);
        result.setBeanClassName(ModuleConfiguration.class.getName());
        String name = role+"Configuration";
        result.setAttribute("id", name);
        result.setAttribute("name", name);
        result.setFactoryBeanName(ServerConfiguration.class.getName());
        result.setFactoryMethodName("getModuleConfiguration");
        ConstructorArgumentValues cArgs = new ConstructorArgumentValues();
        cArgs.addGenericArgumentValue(role);
        result.setConstructorArgumentValues(cArgs);
        return result;
    }

    protected static GenericBeanDefinition createDatastoreConfigurationBeanDefinition(String id){
        GenericBeanDefinition result = new GenericBeanDefinition();
        result.setScope(BeanDefinition.SCOPE_SINGLETON);
        result.setBeanClassName(DatastoreConfiguration.class.getName());
        result.setAttribute("id", id);
        result.setAttribute("name", id);
        result.setFactoryBeanName(ServerConfiguration.class.getName());
        result.setFactoryMethodName("getDatastoreConfiguration");
        ConstructorArgumentValues cArgs = new ConstructorArgumentValues();
        cArgs.addGenericArgumentValue(id);
        result.setConstructorArgumentValues(cArgs);
        return result;
    }

    /**
     *
     * @param DatastoreConfiguration tsDC : the datastore configuration intended for the triplestore connector
     * @return GenericBeanDefinition for the TriplestoreConnector
     * @throws ClassNotFoundException
     * @throws IOException
     */
    protected static GenericBeanDefinition getTriplestoreConnectorBeanDefinition(DatastoreConfiguration tsDC)
        throws IOException {
        String tsConnector = tsDC.getParameter("connectorClassName",Parameter.class).getValue();
        ScannedGenericBeanDefinition beanDefinition = getScannedBeanDefinition(tsConnector);
        beanDefinition.setAutowireCandidate(true);

        Iterator<Parameter> it;
        Parameter p;

        Map<String, String> tsTC = new HashMap<String, String>();
        it = tsDC.getParameters(Parameter.class).iterator();
        while (it.hasNext()) {
            p = it.next();
            tsTC.put(p.getName(), p.getValue(p.getIsFilePath()));
        }
        MutablePropertyValues propertyValues = new MutablePropertyValues();
        propertyValues.addPropertyValue("configuration", tsTC);
        beanDefinition.setPropertyValues(propertyValues);
        return beanDefinition;
    }


    private void loadSpringModules(){
        File springDir =
            new File(m_serverDir + File.separator + CONFIG_DIR
                     + File.separator + SPRING_DIR);
        if (springDir.exists() && springDir.isDirectory()){

            // load some Spring configs with an XmlBeanDefinitionReader
            XmlBeanDefinitionReader beanReader = new XmlBeanDefinitionReader(this);
            for(String path:springDir.list()){
                if (path.endsWith(".xml")){
                    File springConfig = new File(springDir,path);
                    logger.info("loading spring beans from {}", springConfig.getAbsolutePath());
                    FileSystemResource beanConfig = new FileSystemResource(springConfig);
                    int count = beanReader.loadBeanDefinitions(beanConfig);
                    if (count < 1){
                        logger.warn("Loaded " + Integer.toString(count) + " beans from " + springConfig.getAbsolutePath());
                    }
                }
            }
        }
    }

    @Override
    public Object getBean(String name)
        throws BeansException {
        return m_moduleContext.getBean(name);
    }

    @Override
    public boolean containsBean(String name)
    throws BeansException {
        return m_moduleContext.containsBean(name);
    }

    /**
     * Gets a loaded <code>Module</code>.
     *
     * @param role
     *        The role of the <code>Module</code> to get.
     * @return The <code>Module</code>, <code>null</code> if not found.
     */
    public final Module getModule(String role) {
        if(m_moduleContext.containsBean(role)){
            try{
                return m_moduleContext.getBean(role,Module.class);
            }
            catch(Throwable e){
                logger.warn(e.toString());
            }
        }
        return null;
    }

    protected boolean overrideModuleRole(String moduleRole) {
        return false;
    }

    protected String overrideModuleClass(String moduleClass) {
        return null;
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
     *        The element containing the name-value pair definitions.
     * @param dAttribute
     *        The name of the attribute of the <code>Element</code> whose
     *        value will distinguish this element from others that may occur in
     *        the <code>Document</code>. If there is no distinguishing
     *        attribute, this should be an empty string.
     */
    private static final Map<String,String> loadParameters(Element element, String dAttribute)
            throws ServerInitializationException {
        Map<String,String> params = new HashMap<String,String>();

        logger.debug(MessageFormat.format(INIT_CONFIG_CONFIG_EXAMININGELEMENT,
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
                    if (nameNode.getNodeValue().isEmpty()
                            || valueNode.getNodeValue().isEmpty()) {
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
                    logger.debug(MessageFormat
                            .format(INIT_CONFIG_CONFIG_PARAMETERIS,
                                    new Object[] {nameNode.getNodeValue(),
                                            valueNode.getNodeValue()}));

                } else if (!n.getLocalName().equals(CONFIG_ELEMENT_COMMENT)) {
                }

            }
        }
        params.remove(null);
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
        (getBean("org.fcrepo.server.security.Authorization", Authorization.class))
                .enforceServerStatus(context);
        return "RUNNING";
    }

    public final static Server getInstance(File homeDir, boolean okToStart)
            throws ServerInitializationException, ModuleInitializationException {
        if (okToStart) {
            return getInstance(homeDir);
        } else {
            Server instance = s_instances.get(homeDir);
            if (instance == null) {
                throw new ServerInitializationException("The Fedora server is not yet running.");
            } else {
                return instance;
            }
        }

    }

    public final static Element getConfigElement(File homeDir)
            throws ServerInitializationException {
        File configFile = null;
        try {
            configFile =
                    new File(homeDir + File.separator + "server"
                            + File.separator + CONFIG_DIR + File.separator
                            + CONFIG_FILE);
            // suck it in
            Element rootElement =
                    XmlTransformUtility.parseNamespaceAware(configFile).getDocumentElement();
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
            return rootElement;
        } catch (IOException ioe) {
            throw new ServerInitializationException(MessageFormat
                    .format(INIT_CONFIG_SEVERE_UNREADABLE, new Object[] {
                            configFile, ioe.getMessage()}));
        } catch (SAXException saxe) {
            throw new ServerInitializationException(MessageFormat
                    .format(INIT_CONFIG_SEVERE_MALFORMEDXML, new Object[] {
                            configFile, saxe.getMessage()}));
        } catch (Exception e) {
            throw new ServerInitializationException(MessageFormat
                    .format(INIT_XMLPARSER_SEVERE_MISSING, new Object[] {
                            configFile, e.getMessage()}));
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
        Server instance = s_instances.get(homeDir);
        if (instance != null) {
            return instance;
        }

        logger.info("Starting up server");

        // else instantiate a new one given the class provided in the
        // root element in the config file and return it
        File configFile = null;
        try {
            Element rootElement = getConfigElement(homeDir);
            // select <server class="THIS_PART"> .. </server>
            String className = rootElement.getAttribute(CONFIG_ATTRIBUTE_CLASS);
            if (className.isEmpty()) {
                className =
                        rootElement.getAttributeNS(CONFIG_NAMESPACE,
                                                   CONFIG_ATTRIBUTE_CLASS);
                if (className.isEmpty()) {
                    className = DEFAULT_SERVER_CLASS;
                }
            }
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Server> serverClass =
                        (Class<? extends Server>) Class.forName(className);
                Class<?> param1Class =
                        Class.forName(SERVER_CONSTRUCTOR_PARAM1_CLASS);
                Class<?> param2Class =
                        Class.forName(SERVER_CONSTRUCTOR_PARAM2_CLASS);
                Constructor<? extends Server> serverConstructor =
                        serverClass.getConstructor(param1Class,
                                param2Class);
                Server inst =
                        serverConstructor.newInstance(new Object[] {
                                rootElement, homeDir});
                inst.init();
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
        } catch (FactoryConfigurationError fce) {
            throw new ServerInitializationException(INIT_XMLPARSER_SEVERE_MISSING);
        } catch (IllegalArgumentException iae) {
            throw new ServerInitializationException(MessageFormat
                    .format(INIT_CONFIG_SEVERE_UNREADABLE, new Object[] {
                            configFile, iae.getMessage()}));
        }
    }

    /**
     * Gets the server's home directory; this is typically the 'server' subdirectory under $FEDORA_HOME.
     *
     * @return The directory.
     */
    public final File getHomeDir() {
        return m_serverDir;
    }

    /**
    * Gets the server's temp file upload directory.
    *
    * @return The directory.
    */
    public File getUploadDir() {
       return m_uploadDir;
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
        try{
            return m_moduleContext.getBean(id,DatastoreConfiguration.class);
        }
        catch (Throwable t){
            logger.info(t.getMessage(),t);
            return null;
        }
    }

    public Iterator<String> datastoreConfigIds() {
        String [] dsNames = m_moduleContext.getBeanNamesForType(DatastoreConfiguration.class,false,true);
        return Arrays.asList(dsNames).iterator();
    }

    /**
     * Gets an <code>Iterator</code> over the roles that have been loaded.
     *
     * @return (<code>String</code>s) The roles.
     */
    public final Iterator<String> loadedModuleRoles() {
        String [] names = m_moduleContext.getBeanNamesForType(Module.class,false,true);
        return Arrays.asList(names).iterator();
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
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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

        logger.info("Shutting down server");
        Throwable mse = null;
        try{
            m_moduleContext.close();
            m_moduleContext.destroy();
        }
        catch(Throwable e){
            logger.error("Shutdown error: " + e.toString(),e);
            mse = e;
        }

        shutdownServer();
        logger.info("Server shutdown complete");
        s_instances.remove(getHomeDir());

        if (mse != null) {
            throw new ServerShutdownException(mse.toString());
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
    @SuppressWarnings("unused")
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
    @Override
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
        Iterator<String> iter = parameterNames();
        i = 0;
        while (iter.hasNext()) {
            String name = iter.next();
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
            String role = iter.next();
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
            Iterator<String> iter2 = module.parameterNames();
            while (iter2.hasNext()) {
                String name = iter2.next();
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
            String id = iter.next();
            out.append("\nDatastore Cfg : " + id + "\n");
            out.append("Parameters    : ");
            padding = "                ";
            i = 0;
            Iterator<String> iter2 =
                    (getDatastoreConfig(id)).parameterNames();
            while (iter2.hasNext()) {
                String name = iter2.next();
                String value =
                        (getDatastoreConfig(id))
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

        URI propName = Constants.ENVIRONMENT.CURRENT_DATE_TIME.attributeId;
        String dateTimeValue = context.getEnvironmentValue(propName);
        if (dateTimeValue == null) {
            throw new GeneralException("Missing value for environment "
                    + "context attribute: " + propName);
        }

        try {
            return DateUtility.parseDateStrict(dateTimeValue);
        } catch (ParseException e) {
            throw new GeneralException(e.getMessage());
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

    /**
     * Initializes the web client http connection settings.
     */
    private void initWebClientConfig() {
        m_webClientConfig = new WebClientConfiguration();

        if (getParameter("httpClientTimeoutSecs") != null)
            m_webClientConfig.setTimeoutSecs(Integer.parseInt(getParameter("httpClientTimeoutSecs")));

        if (getParameter("httpClientSocketTimeoutSecs") != null)
            m_webClientConfig.setSockTimeoutSecs(Integer.parseInt(getParameter("httpClientSocketTimeoutSecs")));

        if (getParameter("httpClientMaxConnectionsPerHost") != null)
            m_webClientConfig.setMaxConnPerHost(Integer.parseInt(getParameter("httpClientMaxConnectionsPerHost")));

        if (getParameter("httpClientMaxTotalConnections") != null)
            m_webClientConfig.setMaxTotalConn(Integer.parseInt(getParameter("httpClientMaxTotalConnections")));

        if (getParameter("httpClientFollowRedirects") != null)
            m_webClientConfig.setFollowRedirects(Boolean.parseBoolean(getParameter("httpClientFollowRedirects")));

        if (getParameter("httpClientMaxFollowRedirects") != null)
            m_webClientConfig.setMaxRedirects(Integer.parseInt(getParameter("httpClientMaxFollowRedirects")));

        if (getParameter("httpClientUserAgent") != null)
            m_webClientConfig.setUserAgent(getParameter("httpClientUserAgent"));
    }

    /**
     * Gets the web client http connection configuration object.
     * @return the web client http connection configuration
     */
    public WebClientConfiguration getWebClientConfig() {
        return m_webClientConfig;
    }
    
    protected boolean knownBeanDefinition(String beanName) {
        return m_moduleContext.containsBeanDefinition(beanName)
               || m_moduleContext.getParent().containsBeanDefinition(beanName);
    }

    // Spring methods
    @Override
    public boolean containsBeanDefinition(String beanName) {
        return m_moduleContext.containsBeanDefinition(beanName);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName)
            throws NoSuchBeanDefinitionException {
        return m_moduleContext.getBeanDefinition(beanName);
    }

    @Override
    public int getBeanDefinitionCount() {
        return m_moduleContext.getBeanDefinitionCount();
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return m_moduleContext.getBeanDefinitionNames();
    }

    @Override
    public boolean isBeanNameInUse(String beanName) {
        return m_moduleContext.isBeanNameInUse(beanName);
    }

    @Override
    public void removeBeanDefinition(String beanName)
            throws NoSuchBeanDefinitionException {
        m_moduleContext.removeBeanDefinition(beanName);
    }

    @Override
    public String[] getAliases(String name) {
        return m_moduleContext.getAliases(name);
    }

    @Override
    public boolean isAlias(String beanName) {
        return m_moduleContext.isAlias(beanName);
    }

    @Override
    public void registerAlias(String beanName, String alias) {
        m_moduleContext.registerAlias(beanName, alias);
    }

    @Override
    public void removeAlias(String alias) {
        m_moduleContext.removeAlias(alias);
    }

    @Override
    public String[] getBeanNamesForType(@SuppressWarnings("rawtypes") Class type) {
        return m_moduleContext.getBeanNamesForType(type);
    }

    @Override
    public String[] getBeanNamesForType(@SuppressWarnings("rawtypes") Class type,
                                        boolean includeNonSingletons,
                                        boolean allowEagerInit) {
        return m_moduleContext.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
    }

    @Override
    public <T> Map<String,T> getBeansOfType(Class<T> type) throws BeansException {
        return m_moduleContext.getBeansOfType(type);
    }

    @Override
    public <T> Map<String,T> getBeansOfType(Class<T> type,
                              boolean includeNonSingletons,
                              boolean allowEagerInit) throws BeansException {
        return m_moduleContext.getBeansOfType(type, includeNonSingletons, allowEagerInit);
    }

    @Override
    public <T>T getBean(String name, Class <T> type)
            throws BeansException {
        return m_moduleContext.getBean(name, type);
    }

    @Override
    public <T>T getBean(Class <T> requiredType)
        throws BeansException {
        return m_moduleContext.getBean(requiredType);
    }

    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        return m_moduleContext.getBean(name, args);
    }

    @Override
    public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
        return m_moduleContext.getType(name);
    }

    @Override
    public boolean isPrototype(String name)
            throws NoSuchBeanDefinitionException {
        return m_moduleContext.isPrototype(name);
    }

    @Override
    public boolean isSingleton(String name)
            throws NoSuchBeanDefinitionException {
        return m_moduleContext.isSingleton(name);
    }

    @Override
    public boolean isTypeMatch(String name, @SuppressWarnings("rawtypes") Class targetType)
            throws NoSuchBeanDefinitionException {
        return m_moduleContext.isTypeMatch(name, targetType);
    }

    @Override
    public Map<String,Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
            throws BeansException {
        return m_moduleContext.getBeansWithAnnotation(annotationType);
    }

    @Override
    public <T extends Annotation> T findAnnotationOnBean(String beanName, Class<T> annotationType)
        {
        return m_moduleContext.findAnnotationOnBean(beanName, annotationType);
        }
}
