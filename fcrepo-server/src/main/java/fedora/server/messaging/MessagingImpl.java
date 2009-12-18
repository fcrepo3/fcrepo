/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.messaging;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;

import fedora.common.Constants;

import fedora.server.Server;
import fedora.server.errors.MessagingException;
import fedora.server.management.Management;

/**
 * The default, JMS implementation of Messaging.
 *
 * @author Edwin Shin
 * @since 3.0
 * @version $Id$
 */
public class MessagingImpl implements Messaging {
    /** Logger for this class. */
    private static Logger LOG =
            Logger.getLogger(MessagingImpl.class.getName());

    private final Map<String, List<String>> mdMap;
    private final JMSManager jmsMgr;
    private final String fedoraBaseUrl;
    private final static String messageFormat = Constants.ATOM_APIM1_0.uri;

    /**
     * Required JNDI Properties:
     * <ul>
     *   <li>{@link javax.naming.Context#INITIAL_CONTEXT_FACTORY INITIAL_CONTEXT_FACTORY}</li>
     *   <li>{@link javax.naming.Context#PROVIDER_URL PROVIDER_URL}</li>
     * </ul>
     *
     * Optional JNDI Properties:
     * <ul>
     *   <li>{@link JMSManager#CONNECTION_FACTORY_NAME CONNECTION_FACTORY_NAME}</li>
     * </ul>
     *
     * @param fedoraBaseUrl e.g. http://localhost:8080/fedora
     * @param mdMap a <code>Map</code> of {@link Messaging#MessageType} to
     * Destinations.
     * @param jndiProps the JNDI configuration properties.
     * @throws MessagingException
     */
    public MessagingImpl(String fedoraBaseUrl, Map<String, List<String>> mdMap, Properties jndiProps) throws MessagingException {
        this(fedoraBaseUrl, mdMap, new JMSManager(jndiProps));
    }

    public MessagingImpl(String fedoraBaseUrl, Map<String, List<String>> mdMap, JMSManager jmsMgr) {
        this.fedoraBaseUrl = fedoraBaseUrl;
        this.mdMap = mdMap;
        this.jmsMgr = jmsMgr;
    }

    public void send(String destName, FedoraMessage message)
            throws MessagingException {
        jmsMgr.send(destName, message.toString());
    }

    public void send(String destName, FedoraMethod method, FedoraMessage message)
            throws MessagingException {
        TextMessage jmsMessage = jmsMgr.createTextMessage(destName, message.toString());
        try {
            jmsMessage.setStringProperty("methodName", method.getName());
            if(method.getPID() != null) {
                jmsMessage.setStringProperty("pid", method.getPID().toString());
            }
        } catch(JMSException jmse) {
            throw new MessagingException("Unable to set message properties.", jmse);
        }
        jmsMgr.send(destName, jmsMessage);
    }

    /**
     * Send a message to each of the destinations configured for each
     * {@link Messaging#MessageType}. Currently, only
     * {@link FedoraMethod}s that represent
     * {@link fedora.server.Management} methods are supported.
     * {@inheritDoc}
     */
    public void send(FedoraMethod method) throws MessagingException {
        if (Management.class == method.getMethod().getDeclaringClass()) {

            APIMMessage message = new AtomAPIMMessage(method, fedoraBaseUrl, Server.VERSION, messageFormat);

            String methodName = method.getName();
            if (methodName.startsWith("ingest")
                    || methodName.startsWith("add")
                    || methodName.startsWith("modify")
                    || methodName.startsWith("purge")
                    || methodName.startsWith("set")) {
                for (String destName : mdMap.get(MessageType.apimUpdate.toString())) {
                    send(destName, method, message);
                }
            } else {
                for (String destName : mdMap.get(MessageType.apimAccess.toString())) {
                    send(destName, method, message);
                }
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Silently dropping non-Management method: " + method.getName());
            }
        }
    }

    public void close() throws MessagingException {
        if (jmsMgr != null) {
            jmsMgr.close();
        }
    }
}
