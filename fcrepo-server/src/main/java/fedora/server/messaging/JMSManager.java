/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.messaging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

import fedora.server.errors.MessagingException;

/**
  * The JMSManager class is a facade in that it provides a very simple interface
  * for using JMS. Whether a destination is a topic or queue is hidden as an
  * implementation detail.
  *
  * Adapted from code originally written by Eric J. Bruno.
  *
  * @author Eric J. Bruno
  * @author Edwin Shin
  * @author Bill Branan
  * @since 3.0
  * @version $Id$
  */
public class JMSManager {

    /** Logger for this class. */
    private static Logger LOG = Logger.getLogger(JMSManager.class.getName());

    /** Connection Factory Lookup Name */
    public static final String CONNECTION_FACTORY_NAME = "connection.factory.name";

    // Default connection factory name, used if no connection factory name is specified
    private final String defaultConnectionFactoryName = "ConnectionFactory";

    // JNDI related data
    protected Context jndi = null;

    protected Connection connection = null;

    protected boolean connected = false;

    // Destinations maintained in this hashtable
    protected Hashtable<String, JMSDestination> jmsDestinations =
            new Hashtable<String, JMSDestination>();

    // Durable topic consumers
    protected Map<String, MessageConsumer> durableSubscriptions =
            new HashMap<String, MessageConsumer>();

    private final Properties jndiProps;

    // Destination type determines the method by which messages are transferred
    public static enum DestinationType {
        Topic, Queue;
    }

    private DestinationType defaultDestinationType = DestinationType.Topic;

    /**
     * Creates a JMS manager using jndi properties to start a connection
     * to a JMS provider. If jndi properties are null an attempt will
     * be made to create a connection based on a <code>java:comp/env</code>
     * context
     *
     *
     * @param jndiProps
     * @throws MessagingException
     */
    public JMSManager(Properties jndiProps)
            throws MessagingException {
        this(jndiProps, null);
    }

    /**
     * Creates a JMS manager using jndi properties to start a connection
     * to a JMS provider. If jndi properties are null an attempt will
     * be made to create a connection based on a <code>java:comp/env</code>
     * context
     *
     * A connection must have a clientId in order to create durable
     * subscriptions. This clientId can either be set administratively
     * on the Connection object created in the JNDI store or by providing
     * a non-null value for the clientId parameter of this method.
     *
     * @param jndiProps
     * @param clientId
     * @throws MessagingException
     */
    public JMSManager(Properties jndiProps, String clientId)
            throws MessagingException {

        this.jndiProps = jndiProps;

        // Check properties
        if (jndiProps == null) {
            // Attempt a lookup to see if properties are provided by the container
            String providerUrl = null;
            try {
                providerUrl = (String) jndiLookup(Context.PROVIDER_URL);
            } catch (MessagingException me) {
                providerUrl = null;
            }
            if(providerUrl == null) {
                throw new MessagingException("JMS connection properties must either be "
                        + "provided by the container or by a non-null properties file "
                        + "containing, at minimum, a value for "
                        + Context.INITIAL_CONTEXT_FACTORY + " and "
                        + Context.PROVIDER_URL + ".");
            }
        } else {
            String icf = jndiProps.getProperty(Context.INITIAL_CONTEXT_FACTORY);
            if (icf == null) {
                throw new MessagingException("A value for "
                        + Context.INITIAL_CONTEXT_FACTORY
                        + " must be included in the JNDI properties.");
            }

            String pUrl = jndiProps.getProperty(Context.PROVIDER_URL);
            if (pUrl == null) {
                throw new MessagingException("A value for "
                        + Context.PROVIDER_URL
                        + "must be included in the JNDI properties.");
            }
        }

        connectToJMS(clientId);
    }

    // /////////////////////////////////////////////////////////////////////////
    // Public API methods

    /**
     * Creates a Destination. This is a convenience method which is the
     * same as calling:
     * <code>createDestination(name, type, false, Session.AUTO_ACKNOWLEDGE)</code>
     */
    public Destination createDestination(String name, DestinationType type)
            throws MessagingException {
        return this.createDestination(name, type, false, Session.AUTO_ACKNOWLEDGE);
    }

    /**
     * Creates a Destination if the Destination has not already been created.
     *
     * @param name - the name of the destination to create
     * @param type - the destination type (topic or queue)
     * @param fTransacted - determines whether the session will maintain transactions
     * @param ackMode - determines the session acknowledgment mode
     * @throws MessagingException
     */
    public Destination createDestination(String name,
                                  DestinationType type,
                                  boolean fTransacted,
                                  int ackMode) throws MessagingException {
        // If the destination already exists, just return it
        JMSDestination jmsDest = jmsDestinations.get(name);
        if (jmsDest != null) {
            return jmsDest.destination;
        }

        // Create the new destination and store it
        Session session;
        try {
            session = connection.createSession(fTransacted, ackMode);
        } catch (JMSException e) {
            throw new MessagingException(e.getMessage(), e);
        }

        // Look up the destination otherwise create it
        Destination destination = null;
        try {
            destination = (Destination) jndiLookup(name);
        } catch (MessagingException me) {
            LOG.debug("JNDI lookup for destination " + name + " failed. "
                    + "Destination must be created.");
            destination = null;
        }
        if (destination == null) {
            // Create a topic or queue as specified
            try {
                if (type.equals(DestinationType.Queue)) {
                    LOG.debug("setupDestination() - creating Queue" + name);
                    destination = session.createQueue(name);
                } else {
                    LOG.debug("setupDestination() - creating Topic " + name);
                    destination = session.createTopic(name);
                }
            } catch (JMSException e) {
                throw new MessagingException(e.getMessage(), e);
            }
        }

        jmsDest = new JMSDestination(destination, session, null, null);

        jmsDestinations.put(name, jmsDest);

        return destination;
    }

    /**
     * Convenience method for synchronous listen with no message selector
     * @see JMSManager#listen(String, String)
     */
    public Message listen(String destName) throws MessagingException {
        return listen(destName, "");
    }

    /**
     * This is a synchronous listen. The caller will block until a message is
     * received for the given destination.
     * Messages will be filtered based on the provided message selector.
     *
     * @param destName
     *        the Destination to listen on
     * @param messageSelector
     *        selection criteria for filtering messages
     * @return the Message received for the given Destination
     * @throws Exception
     */
    public Message listen(String destName, String messageSelector) throws MessagingException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("listen() - Synchronous listen on destination " + destName);
        }

        JMSDestination jmsDest = getJMSDestination(destName);

        // Setup the consumer and block until a
        // message arrives for this destination
        //
        return setupSynchConsumer(jmsDest, messageSelector, 0);
    }

    /**
     * Convenience method for synchronous listen with no message selector
     * @see JMSManager#listen(Destination, String)
     */
    public Message listen(Destination dest) throws MessagingException {
        return listen(dest, "");
    }

    /**
     * This is a synchronous listen. The caller will block until a message is
     * received for the given destination.
     * Messages will be filtered based on the provided message selector.
     *
     * @param dest
     *        the Destination to listen on
     * @param messageSelector
     *        selection criteria for filtering messages
     * @return the Message received for the given Destination
     * @throws Exception
     */
    public Message listen(Destination dest, String messageSelector) throws MessagingException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("listen() - Synchronous listen on destination " + dest);
        }
        try {
            Session s =
                    connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer c = s.createConsumer(dest, messageSelector);
            Message msg = c.receive();
            s.close();
            return msg;
        } catch (JMSException e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    /**
     * Convenience method for synchronous listen with no message selector
     * @see JMSManager#listen(String, String, int)
     */
    public Message listen(String destName, int timeout)
    throws MessagingException {
        return listen(destName, "", timeout);
    }

    /**
     * This is a synchronous listen. The caller will block until a message is
     * received for the given destination OR the timeout value (in milliseconds)
     * has been reached.
     * Messages will be filtered based on the provided message selector.
     *
     * @param destName
     *        the Destination to listen on
     * @param messageSelector
     *        selection criteria for filtering messages
     * @param timeout
     *        time in milliseconds before timing out
     * @return the Message received for the given Destination
     * @throws Exception
     */
    public Message listen(String destName, String messageSelector, int timeout)
            throws MessagingException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("listen() - Synchronous listen on destination "
                      + destName + " with timeout " + timeout);
        }

        JMSDestination jmsDest = getJMSDestination(destName);

        // Setup the consumer and block until a
        // message arrives for this destination
        //
        return setupSynchConsumer(jmsDest, messageSelector, timeout);
    }

    /**
     * Convenience method for asynchronous listen with no message selector
     * @see JMSManager#listen(String, String, MessageListener)
     */
    public void listen(String destName, MessageListener callback)
            throws MessagingException {
        listen(destName, "", callback);
    }

    /**
     * This is an asynchronous listen. The caller provides a JMS callback
     * interface reference, and any messages received for the given destination
     * are provided through the onMessage() callback method
     * Messages will be filtered based on the provided message selector.
     */
    public void listen(String destName, String messageSelector, MessageListener callback)
            throws MessagingException {
        JMSDestination jmsDest = getJMSDestination(destName);

        // Set the caller as a topic subscriber or queue receiver as appropriate
        setupAsynchConsumer(jmsDest, messageSelector, callback);

        if (LOG.isDebugEnabled()) {
            LOG.debug("listen() - Asynchronous listen on destination " + destName);
        }
    }

    /**
     * This is an asynchronous listen. The caller provides a JMS callback
     * interface reference, and any messages received for the given destination
     * are provided through the onMessage() callback method.
     */
    public void listen(Destination dest, MessageListener callback)
            throws MessagingException {
        listen(dest, "", callback);
    }

    /**
     * This is an asynchronous listen. The caller provides a JMS callback
     * interface reference, and any messages received for the given destination
     * are provided through the onMessage() callback method.
     * Messages will be filtered based on the provided message selector.
     */
    public void listen(Destination dest, String messageSelector, MessageListener callback)
            throws MessagingException {
        try {
            Session s =
                    connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer c = s.createConsumer(dest, messageSelector);
            c.setMessageListener(callback);
        } catch (JMSException e) {
            throw new MessagingException(e.getMessage(), e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("listen() - Asynchronous listen on destination " + dest);
        }
    }

    /**
     * @see JMSManager#listenDurable(Topic, MessageListener)
     */
    public String listenDurable(String topic, MessageListener callback)
            throws MessagingException {
        return listenDurable(topic, callback, null);
    }

    /**
     * @see JMSManager#listenDurable(Topic, MessageListener, String)
     */
    public String listenDurable(String topicName,
                                MessageListener callback,
                                String subscriptionName)
            throws MessagingException {
        createDestination(topicName, DestinationType.Topic);
        Topic topic = (Topic) getDestination(topicName);
        return listenDurable(topic, "", callback, subscriptionName);
    }

    /**
     * This is a convenience method to allow a durable subscription to be
     * created using the topic as the subscription name. Calling this
     * method is the same as calling
     * <code>listenDurable(topic, "", callback, null)</code>
     *
     * @see JMSManager#listenDurable(Topic, MessageListener, String)
     */
    public String listenDurable(Topic topic, MessageListener callback)
            throws MessagingException {
        return listenDurable(topic, "", callback, null);
    }

    /**
     * This is an asynchronous and durable listen. The caller provides a JMS
     * callback interface reference, and any messages received for the given
     * destination are provided through the onMessage() callback method. If the
     * listener becomes unavailable the JMS provider will store messages
     * received on the given topic until the listener reestablishes a connection
     * and will then deliver those messages.
     * Messages will be filtered based on the provided message selector.
     *
     * @param topic
     *        the topic on which to listen
     * @param messageSelector
     *        selection criteria for filtering messages
     * @param callback
     *        the listener to call when a message is received
     * @param subscriptionName
     *        the name of the subscription
     * @return The subscription name which can be used to stop, unsubscribe,
     *         and reconnect to this topic
     * @throws MessagingException
     */
    public String listenDurable(Topic topic,
                                String messageSelector,
                                MessageListener callback,
                                String subscriptionName)
            throws MessagingException {
        try {
            String clientId = connection.getClientID();
            if (clientId == null) {
                throw new MessagingException("A non-null client ID must be provided upon "
                        + "creation of a JMSManager in order to create a JMS connection "
                        + "capable of creating durable subscriptions.");
            }
            if (subscriptionName == null) {
                subscriptionName = topic.getTopicName();
            }

            Session s =
                    connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer c =
                    s.createDurableSubscriber(topic, subscriptionName, messageSelector, false);
            c.setMessageListener(callback);
            durableSubscriptions.put(subscriptionName, c);
        } catch (JMSException e) {
            throw new MessagingException(e.getMessage(), e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("listen() - Asynchronous durable listen on topic " + topic);
        }

        return subscriptionName;
    }

    /**
     * Allows the caller to send a Message object to a named destination
     */
    public void send(String destName, Message msg) throws MessagingException {
        JMSDestination jmsDest = getJMSDestination(destName);

        // Make sure we have a message producer created for this destination
        setupProducer(jmsDest);

        // Send the message for this destination
        try {
            jmsDest.producer.send(msg);
        } catch (JMSException e) {
            throw new MessagingException(e.getMessage(), e);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("send() - message sent to destination " + destName);
        }
    }

    /**
     * Allows the caller to send a Message object to a destination
     */
    public void send(Destination dest, Message msg) throws MessagingException {
        try {
            Session s =
                    connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer p = s.createProducer(dest);
            p.send(msg);
            s.close();
        } catch (JMSException e) {
            throw new MessagingException(e.getMessage(), e);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("send() - message sent to destination " + dest);
        }
    }

    /**
     * Allows the caller to send a Serializable object to a destination
     */
    public void send(String destName, Serializable obj)
            throws MessagingException {
        JMSDestination jmsDest = getJMSDestination(destName);

        // Make sure we have a message producer created for this destination
        setupProducer(jmsDest);

        // Send the message for this destination
        try {
            Message msg = createJMSMessage(obj, jmsDest.session);
            jmsDest.producer.send(msg);
        } catch (JMSException e) {
            throw new MessagingException(e.getMessage(), e);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("send() - message sent to destination " + destName);
        }
    }

    /**
     * Allows the caller to send text to a destination
     */
    public void send(String destName, String messageText)
            throws MessagingException {
        this.send(destName, (Serializable) messageText);
    }

    /**
     * Stops producers and consumers on a given destination.
     * This has no effect on durable subscriptions.
     *
     * @param destName
     * @throws MessagingException
     */
    public void stop(String destName) throws MessagingException {
        try {
            // Look for an existing destination for the given destination
            //
            JMSDestination jmsDest =
                    jmsDestinations.get(destName);
            if (jmsDest != null) {
                // Close out all JMS related state
                //
                if (jmsDest.producer != null) {
                    jmsDest.producer.close();
                    LOG.debug("Closed producer for " + destName);
                }
                if (jmsDest.consumer != null) {
                    jmsDest.consumer.close();
                    LOG.debug("Closed consumer for " + destName);
                }
                if (jmsDest.session != null) {
                    jmsDest.session.close();
                    LOG.debug("Closed session for " + destName);
                }

                jmsDest.destination = null;
                jmsDest.session = null;
                jmsDest.producer = null;
                jmsDest.consumer = null;

                // Remove the JMS client entry
                //
                jmsDestinations.remove(destName);
                jmsDest = null;
            }
        } catch (JMSException e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    /**
     * Stops a durable message consumer. Note that this is not
     * the same as unsubscribing. When a durable message consumer is
     * restarted all messages received since it was stopped will be
     * delivered.
     *
     * @param subscriptionName - the name of the subscription
     * @throws MessagingException
     */
    public void stopDurable(String subscriptionName) throws MessagingException {
        try {
            MessageConsumer durableSubscriber =
                    durableSubscriptions.get(subscriptionName);
            if (durableSubscriber != null) {
                durableSubscriber.close();
            }
        } catch (JMSException jmse) {
            throw new MessagingException("Exception encountered attempting to "
                    + "stop durable subscription with name: "
                    + subscriptionName + ". Exception message: "
                    + jmse.getMessage(), jmse);
        }
    }

    /**
     * Removes the durable subscription with the given name.
     *
     * @param subscriptionName - name of the durable subscription
     * @throws MessagingException
     */
    public void unsubscribeDurable(String subscriptionName)
            throws MessagingException {
        try {
            Session session =
                connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer consumer = durableSubscriptions.get(subscriptionName);
            if(consumer != null) {
                consumer.close();
            }
            session.unsubscribe(subscriptionName);
        } catch (JMSException jmse) {
            String errMsg = "Unable to unsubscribe from subscription with name: "
                          + subscriptionName + " due to exception: "
                          + jmse.getMessage();
            LOG.debug(errMsg, jmse);
            throw new MessagingException(errMsg, jmse);
        }
    }

    /**
     * Removes all durable topic subscriptions created using
     * this JMSManager instance
     *
     * @throws MessagingException
     */
    public void unsubscribeAllDurable() throws MessagingException {
        for (String name : durableSubscriptions.keySet()) {
            unsubscribeDurable(name);
        }
    }

    public void close() throws MessagingException {
        try {
            // Closing a connection also closes all sessions, producers,
            // and consumers established over that connection
            connection.stop();
            connection.close();
            connected = false;
            LOG.debug("Connection closed.");
        } catch (JMSException e) {
            LOG.debug("Error closing Connection.");
            throw new MessagingException(e.getMessage(), e);
        }
    }

    public MapMessage createMapMessage(String destName)
            throws MessagingException {
        JMSDestination jmsDest = getJMSDestination(destName);
        try {
            return jmsDest.session.createMapMessage();
        } catch (JMSException e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    public TextMessage createTextMessage(String destName, String text)
            throws MessagingException {
        JMSDestination jmsDest = getJMSDestination(destName);
        try {
            TextMessage message = jmsDest.session.createTextMessage();
            message.setText(text);
            return message;
        } catch (JMSException e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    public ObjectMessage createObjectMessage(String destName,
                                             Serializable object)
            throws MessagingException {
        JMSDestination jmsDest = getJMSDestination(destName);
        try {
            ObjectMessage message = jmsDest.session.createObjectMessage();
            message.setObject(object);
            return message;
        } catch (JMSException e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    public BytesMessage createBytesMessage(String destName)
            throws MessagingException {
        JMSDestination jmsDest = getJMSDestination(destName);
        try {
            return jmsDest.session.createBytesMessage();
        } catch (JMSException e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    /**
     * @param destName
     * @return the Session object for the specified destination name.
     * @throws Exception
     */
    public Session getSession(String destName) throws MessagingException {
        JMSDestination jmsDest = getJMSDestination(destName);
        return jmsDest.session;
    }

    /**
     * Gets the named Destination if it has been created.
     *
     * @param destName
     * @return the Destination object for the specified destination
     *         name or null if the destination does not exist
     * @throws Exception
     */
    public Destination getDestination(String destName)
            throws MessagingException {
        Destination destination = null;
        JMSDestination jmsDest = getJMSDestination(destName);
        if(jmsDest != null) {
            destination = jmsDest.destination;
        }
        return destination;
    }

    /**
     * Provides a listing of the currently available destinations
     * @return destination list
     */
    public List<Destination> getDestinations() {
        List<Destination> destinations = new ArrayList<Destination>();
        Iterator<JMSDestination> destinationIterator =
            jmsDestinations.values().iterator();
        while(destinationIterator.hasNext()) {
            destinations.add(destinationIterator.next().destination);
        }
        return destinations;
    }

    /**
     * @param destName
     * @return the MessageProducer object for the specified destination name
     * @throws Exception
     */
    public MessageProducer getProducer(String destName)
            throws MessagingException {
        JMSDestination jmsDest = getJMSDestination(destName);
        return jmsDest.producer;
    }

    /**
     * @param destName
     * @return the MessageConsumer object for the specified destination name
     * @throws Exception
     */
    public MessageConsumer getConsumer(String destName)
            throws MessagingException {
        JMSDestination jmsDest = getJMSDestination(destName);
        return jmsDest.consumer;
    }

    /**
     * @return the default DestinationType
     */
    public DestinationType getDefaultDestinationType() {
        return defaultDestinationType;
    }

    /**
     * Sets the default DestinationType
     *
     * @param defaultDestinationType
     */
    public void setDefaultDestinationType(DestinationType defaultDestinationType) {
        this.defaultDestinationType = defaultDestinationType;
    }

    // /////////////////////////////////////////////////////////////////////////
    // Internal worker methods

    protected void connectToJMS(String clientId) throws MessagingException {
        // Check to see if already connected
        //
        if (connected == true) return;

        try {
            // Get a JMS Connection
            //
            connection = getConnection();
            if(clientId != null) {
                connection.setClientID(clientId);
            }
            connection.start();
            connected = true;
            LOG.debug("connectToJMS - connected");
        } catch (JMSException e) {
            connected = false;
            LOG.error("JMSManager.connectToJMS - Exception occurred:");
            throw new MessagingException(e.getMessage(), e);
        }
    }

    protected JMSDestination getJMSDestination(String name)
            throws MessagingException {
        // Look for an existing Destination for the given name
        //
        JMSDestination jmsDest = jmsDestinations.get(name);

        // If not found, create it now
        //
        if (jmsDest == null) {
            this.createDestination(name, defaultDestinationType);
            jmsDest = jmsDestinations.get(name);
        }

        return jmsDest;
    }

    protected void setupProducer(JMSDestination jmsDest)
            throws MessagingException {
        if (jmsDest.producer != null) return;
        try {
            jmsDest.producer =
                    jmsDest.session.createProducer(jmsDest.destination);
        } catch (JMSException e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    protected void setupAsynchConsumer(JMSDestination jmsDest,
                                       String messageSelector,
                                       MessageListener callback)
            throws MessagingException {
        try {
            if (jmsDest.consumer == null) {
                jmsDest.consumer =
                        jmsDest.session.createConsumer(jmsDest.destination,
                                                       messageSelector);
            }

            jmsDest.consumer.setMessageListener(callback);
        } catch (JMSException e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    protected Message setupSynchConsumer(JMSDestination jmsDest,
                                         String messageSelector,
                                         int timeout)
            throws MessagingException {
        try {
            if (jmsDest.consumer == null) {
                jmsDest.consumer =
                        jmsDest.session.createConsumer(jmsDest.destination,
                                                       messageSelector);
            }

            if (timeout > 0)
                return jmsDest.consumer.receive(timeout);
            else
                return jmsDest.consumer.receive();
        } catch (JMSException e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    protected Message createJMSMessage(Serializable obj, Session session)
            throws JMSException {
        if (obj instanceof String) {
            TextMessage textMsg = session.createTextMessage();
            textMsg.setText((String) obj);
            return textMsg;
        } else {
            ObjectMessage objMsg = session.createObjectMessage();
            objMsg.setObject(obj);
            return objMsg;
        }
    }

    protected Connection getConnection() throws MessagingException {
        ConnectionFactory connectionFactory;
        if (jndiProps != null) {
            String connectionFactoryName =
                    jndiProps.getProperty(CONNECTION_FACTORY_NAME);
            if(connectionFactoryName == null || connectionFactoryName.equals("")) {
                connectionFactoryName = defaultConnectionFactoryName;
            }
            connectionFactory =
                    (ConnectionFactory) jndiLookup(connectionFactoryName);
        } else {
            throw new MessagingException("Unable to create JMS connection "
                    + "because JNDI properties were not initialized.");
        }

        try {
            connection = connectionFactory.createConnection();
        } catch (JMSException e) {
            throw new MessagingException(e.getMessage(), e);
        }
        return connection;
    }

    protected Object jndiLookup(String name) throws MessagingException {
        if (jndi == null) {
            jndi = getContext();
        }

        try {
            return jndi.lookup(name);
        } catch (NamingException e) {
            throw new MessagingException("jndiLookup(" + name + ") failed: "
                                         + e.getMessage(), e);
        }
    }

    protected Context getContext() throws MessagingException {
        try {
            InitialContext initCtx;
            if (jndiProps != null) {
                return new InitialContext(jndiProps);
            }

            initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");

            if (LOG.isDebugEnabled()) {
                LOG.debug("InitalContext properties:");
                LOG.debug("----------------");

                Hashtable<?, ?> props = initCtx.getEnvironment();
                Set<?> keys = props.keySet();
                for (Object key : keys) {
                    LOG.debug(key.toString() + "=" + props.get(key));
                }

                LOG.debug("java:comp/env context properties:");
                LOG.debug("----------------");
                props = envCtx.getEnvironment();
                keys = props.keySet();

                for (Object key : keys) {
                    LOG.debug(key.toString() + "=" + props.get(key));
                }
                LOG.debug("----------------");
            }
            return envCtx;
        } catch (Exception e) {
            LOG.error("getContext() failed with: " + e.getMessage());
            throw new MessagingException(e.getMessage(), e);
        }
    }

    /**
     * Nested class to encapsulate JMS Destination objects
     */
    class JMSDestination {

        Destination destination = null;

        Session session = null;

        MessageProducer producer = null;

        MessageConsumer consumer = null;

        public JMSDestination(Destination destination,
                              Session session,
                              MessageProducer producer,
                              MessageConsumer consumer) {
            this.destination = destination;
            this.session = session;
            this.producer = producer;
            this.consumer = consumer;
        }
    }
}
