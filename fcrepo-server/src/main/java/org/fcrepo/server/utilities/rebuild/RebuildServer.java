/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.utilities.rebuild;

import java.io.File;
import java.io.IOException;

import java.text.MessageFormat;

import org.trippi.TriplestoreConnector;

import org.w3c.dom.Element;

import org.fcrepo.server.Server;
import org.fcrepo.server.config.DatastoreConfiguration;
import org.fcrepo.server.config.ModuleConfiguration;
import org.fcrepo.server.config.ServerConfiguration;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.errors.ServerInitializationException;
import org.fcrepo.server.resourceIndex.ResourceIndex;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;


/**
 * @author Robert Haschart
 */
public class RebuildServer
        extends Server {

    /**
     * Default Rebuilders that the rebuild utility knows about.
     */
    public static String[] REBUILDERS =
        new String[] {"org.fcrepo.server.resourceIndex.ResourceIndexRebuilder",
    "org.fcrepo.server.utilities.rebuild.SQLRebuilder","org.fcrepo.server.security.xacml.pdp.data.PolicyIndexRebuilder"};

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

    protected RebuildServer(File homeDir)
    throws ServerInitializationException, ModuleInitializationException {
        super(homeDir);
    }

    @Override
    protected void registerBeanDefinitions()
            throws ServerInitializationException{
        ServerConfiguration sc = getConfig();
        ModuleConfiguration rim = sc.getModuleConfiguration(ResourceIndex.class.getName());
        if (rim != null){
            String ds = rim.getParameter("datastore");
            if (ds != null){
                DatastoreConfiguration dsConfig = sc.getDatastoreConfiguration(ds);
                if (dsConfig != null){
                    try{
                        String name = TriplestoreConnector.class.getName();
                        GenericBeanDefinition beanDefinition = Server.getTriplestoreConnectorBeanDefinition(dsConfig);
                        beanDefinition.setAttribute("name", name);
                        beanDefinition.setAttribute("id", name);
                        registerBeanDefinition(name, beanDefinition);
                    }
                    catch (IOException e){
                        throw new ServerInitializationException(e.toString(),e);
                    }
                }
            }
        }
        for (String rebuilder:REBUILDERS){
            try{
                ScannedGenericBeanDefinition beanDefinition = Server.getScannedBeanDefinition(rebuilder);
                beanDefinition.setAutowireCandidate(true);
                beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
                beanDefinition.setAttribute("id", rebuilder);
                beanDefinition.setAttribute("name", rebuilder);
                beanDefinition.setInitMethodName("init");
                beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
                beanDefinition.setDependencyCheck(AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);
                registerBeanDefinition(rebuilder, beanDefinition);
            }
            catch (IOException e){
                throw new ServerInitializationException(e.toString(),e);
            }
        }
    }

    @Override
    protected boolean overrideModuleRole(String moduleRole) {
        if ("org.fcrepo.server.security.Authorization".equals(moduleRole)
                || "org.fcrepo.server.access.Access".equals(moduleRole)
                || "org.fcrepo.server.access.DynamicAccess".equals(moduleRole)
                || "org.fcrepo.oai.OAIProvider".equals(moduleRole)
                || "org.fcrepo.oai.OAIProvider".equals(moduleRole)
                || "org.fcrepo.server.management.Management".equals(moduleRole)
                || "org.fcrepo.server.resourceIndex.ResourceIndex".equals(moduleRole)) {
            return true;
        }
        return false;
    }

    @Override
    protected String overrideModuleClass(String moduleClass) {
        if (moduleClass != null && moduleClass.endsWith("DOManager")) {
            return "org.fcrepo.server.utilities.rebuild.RebuildDOManager";
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
        // instantiate a new special purpose server for rebuilding
        // SQL databases given the class provided in the root element
        // in the config file and return it

            String className = "org.fcrepo.server.utilities.rebuild.RebuildServer";
            try {
                Server inst = new RebuildServer(homeDir);
                s_instances.put(homeDir, inst);
                inst.init();
                return inst;
            } catch (IllegalArgumentException iae) {
                // improbable
                throw new ServerInitializationException(MessageFormat
                        .format(INIT_SERVER_SEVERE_BADARGS,
                                new Object[] {className}));
            }
    }

}
