/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities.rebuild;

import java.io.File;
import java.io.IOException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.text.MessageFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;

import org.xml.sax.SAXException;

import fedora.server.Server;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ServerInitializationException;

/**
 * @author Robert Haschart
 */
public class RebuildServer
        extends Server {

    /**
     * @param rootConfigElement
     * @param homeDir
     * @throws ServerInitializationException
     * @throws ModuleInitializationException
     */
    public RebuildServer(Element rootConfigElement, File homeDir)
            throws ServerInitializationException, ModuleInitializationException {
        super(rootConfigElement, homeDir);
    }

    @Override
    protected boolean overrideModuleRole(String moduleRole) {
        if (moduleRole.indexOf("Authorization") != -1
                || moduleRole.indexOf("Access") != -1
                || moduleRole.indexOf("OAIProvider") != -1
                || moduleRole.indexOf("Management") != -1
                || moduleRole.indexOf("ResourceIndex") != -1) {
            return true;
        }
        return false;
    }

    @Override
    protected String overrideModuleClass(String moduleClass) {
        if (moduleClass.endsWith("DOManager")) {
            return "fedora.server.utilities.rebuild.RebuildDOManager";
        }
        return null;
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
    public final static Server getRebuildInstance(File homeDir)
            throws ServerInitializationException, ModuleInitializationException {
        configureLog4J("-rebuild.log");
        // instantiate a new special purpose server for rebuilding 
        // SQL databases given the class provided in the root element
        // in the config file and return it
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
            String className = "fedora.server.utilities.rebuild.RebuildServer";
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
                    s.append(":[z] ");
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

    @Override
    protected void initServer() throws ServerInitializationException {
    }

}
