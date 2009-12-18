/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.messaging;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;

import javax.jms.Session;

import org.apache.log4j.Logger;

import fedora.common.Constants;

import fedora.server.DatastoreConfig;
import fedora.server.Module;
import fedora.server.Server;
import fedora.server.errors.MessagingException;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ModuleShutdownException;
import fedora.server.messaging.JMSManager.DestinationType;
import fedora.server.utilities.ServerUtility;

/**
 * Fedora's <code>Messaging</code> as a configurable module.
 * 
 * @author Edwin Shin
 * @version $Id$
 */
public class MessagingModule
        extends Module
        implements Messaging {

    /** Logger for this class. */
    private static Logger LOG =
            Logger.getLogger(MessagingModule.class.getName());

    private Messaging msg;

    private JMSManager jmsMgr;

    private static final String ACTIVEMQ_PREFIX = 
            "org.apache.activemq.default.directory.prefix";
    
    public MessagingModule(Map<String, String> moduleParameters,
                           Server server,
                           String role)
            throws ModuleInitializationException {
        super(moduleParameters, server, role);
    }

    public void initModule() throws ModuleInitializationException {

        if (!enabled()) {
            LOG.info("Messaging Module is disabled.");
            return;
        }
        
        // Sets the location of the activemq-data directory
        // Property is ignored if the messaging provider is not ActiveMQ
        if (System.getProperty(ACTIVEMQ_PREFIX) == null) {
            System.setProperty(ACTIVEMQ_PREFIX, 
                               new File(Constants.FEDORA_HOME, "data").getPath() 
                               + File.separator);
        }
        
        Properties jndiProps = getJNDISettings();

        try {
            jmsMgr = new JMSManager(jndiProps);
        } catch (Exception e) {
            throw new ModuleInitializationException(e.getMessage(), getRole());
        }

        try {
            String fedoraBaseUrl = ServerUtility.getBaseURL("http");
            msg =
                    new MessagingImpl(fedoraBaseUrl,
                                      createDestinations(),
                                      jmsMgr);
        } catch (Exception e) {
            throw new ModuleInitializationException("Error connecting to JMS ",
                                                    getRole(),
                                                    e);
        }
    }

    public void postInitModule() throws ModuleInitializationException {

    }

    public void shutdownModule() throws ModuleShutdownException {
        if(enabled()) {
            try {
                close();
            } catch (MessagingException e) {
                throw new ModuleShutdownException(e.getMessage(), getRole(), e);
            }
        }
    }

    public void send(String destName, FedoraMessage message)
            throws MessagingException {
        msg.send(destName, message);
    }

    public void send(FedoraMethod method) throws MessagingException {
        msg.send(method);
    }

    private Properties getJNDISettings() {

        String contextFactory = getParameter(Context.INITIAL_CONTEXT_FACTORY);
        String providerURL = getParameter(Context.PROVIDER_URL);
        String connectionFactory =
                getParameter(JMSManager.CONNECTION_FACTORY_NAME);

        if (providerURL == null || providerURL.length() == 0) {
            providerURL = "vm:(broker:(tcp://localhost:61616))";
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using default provider url: " + providerURL);
            }
        }

        if (connectionFactory == null || connectionFactory.length() == 0) {
            connectionFactory = "ConnectionFactory";
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using default connection factory name: " + connectionFactory);
            }
        }

        if (contextFactory == null || contextFactory.length() == 0) {
            contextFactory =
                    "org.apache.activemq.jndi.ActiveMQInitialContextFactory";
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using default initial context factory: " + contextFactory);
            }
        } else if (contextFactory.equalsIgnoreCase("container")) {
            // assume jndi information is provided via the container
            return null;
        }

        Properties props = new Properties();
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
        props.setProperty(Context.PROVIDER_URL, providerURL);
        props
                .setProperty(JMSManager.CONNECTION_FACTORY_NAME,
                             connectionFactory);

        return props;
    }

    /**
     * @return a <code>Map</code>ping of message type to destinations
     * @throws ModuleInitializationException
     */
    private Map<String, List<String>> createDestinations()
            throws ModuleInitializationException {
        Map<String, List<String>> mdMap = new HashMap<String, List<String>>();
        for (MessageType type : MessageType.values()) {
            mdMap.put(type.toString(), new ArrayList<String>());
        }

        Iterator<String> parameters = parameterNames();
        String param;
        while (parameters.hasNext()) {
            param = parameters.next();
            if (param.startsWith("datastore")) {
                DatastoreConfig dsConfig = getDatastore(param);
                String[] msgTypes =
                        dsConfig.getParameter("messageTypes").split(" ");
                for (String msgType : msgTypes) {
                    if (!mdMap.containsKey(msgType)) {
                        throw new ModuleInitializationException(msgType
                                + " is not a supported MessageType.", getRole());
                    }
                }

                String destName = dsConfig.getParameter("name");
                String type = dsConfig.getParameter("type");
                boolean transacted =
                        Boolean.parseBoolean(dsConfig
                                .getParameter("transacted"));
                String ackMode = dsConfig.getParameter("ackMode");

                DestinationType destType = DestinationType.Topic;
                if (type.equalsIgnoreCase("queue")) {
                    destType = DestinationType.Queue;
                }

                int destAckMode = Session.AUTO_ACKNOWLEDGE;

                if (ackMode != null && ackMode.length() > 0) {
                    try {
                        destAckMode = Integer.parseInt(ackMode);
                    } catch (NumberFormatException e) {
                        throw new ModuleInitializationException("ackMode must be a number",
                                                                getRole());
                    }
                }

                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String
                                .format("createDestination(%s, %s, %s, %s)",
                                        destName,
                                        destType,
                                        transacted,
                                        destAckMode));
                    }
                    jmsMgr.createDestination(destName,
                                             destType,
                                             transacted,
                                             destAckMode);
                } catch (Exception e) {
                    throw new ModuleInitializationException(e.getMessage(),
                                                            getRole());
                }

                for (String msgType : msgTypes) {
                    mdMap.get(msgType).add(destName);
                }
            }
        }
        return mdMap;
    }

    private DatastoreConfig getDatastore(String name)
            throws ModuleInitializationException {
        String value = getParameter(name);
        if (value == null || value.length() == 0) {
            throw new ModuleInitializationException(name + " parameter "
                    + "is required", getRole());
        }
        DatastoreConfig dsConfig = getServer().getDatastoreConfig(value);
        if (dsConfig == null) {
            throw new ModuleInitializationException(value + " datastore "
                    + "configuration is missing.", getRole());
        }
        return dsConfig;
    }
    
    // Check to see if messaging is enabled
    private boolean enabled() {
        String enabled = getParameter("enabled");
        return (enabled != null && enabled.equalsIgnoreCase("true"));
    }
    

    public void close() throws MessagingException {
        if (msg != null) {
            msg.close();
            msg = null;
        }
    }
}
