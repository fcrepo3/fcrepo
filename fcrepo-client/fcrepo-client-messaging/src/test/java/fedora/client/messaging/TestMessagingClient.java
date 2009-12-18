/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.messaging;

import java.util.Properties;

import javax.naming.Context;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.jms.Topic;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;

import fedora.server.errors.MessagingException;
import fedora.server.messaging.JMSManager;

/**
 * Tests the messaging client
 *
 * @author Bill Branan
 */
public class TestMessagingClient
        extends TestCase
        implements MessagingListener {

    private static final String TOPIC_NAME = "messageTopic";
    private static final String TOPIC = "fedora.test.topic";
    private static final String QUEUE_NAME = "messageQueue";
    private static final String QUEUE = "fedora.test.queue";

    private int messageCount = 0;
    private int messageTimeout = 5000; // Maximum number of milliseconds to wait for a message
    private Message currentMessage = null;
    private String currentClientId = null;
    private Properties properties = null;

    private String messageText =
            "This is a message sent as part of a junit test";
    private String propertyName = "testProperty";
    private String propertyValue = "testProperty value";

    public void setUp() throws Exception {
        properties = new Properties();
        properties.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                               "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        properties.setProperty(Context.PROVIDER_URL,
                               "vm://localhost?broker.useShutdownHook=false&broker.persistent=true");
        properties.setProperty(JMSManager.CONNECTION_FACTORY_NAME,
                               "ConnectionFactory");
        properties.setProperty("topic." + TOPIC_NAME, TOPIC);
        properties.setProperty("queue." + QUEUE_NAME, QUEUE);
    }

    public void testMessagingClientTopic() throws Exception {

        String clientId = "0";
        MessagingClient messagingClient =
                new JmsMessagingClient(clientId, this, properties, false);

        messagingClient.start();
        sendMessage(TOPIC_NAME);
        checkMessage(clientId, TOPIC);
        messagingClient.stop(true);
    }

    public void testMessagingClientDurableTopic() throws Exception {

        String clientId = "1";
        MessagingClient messagingClient =
                new JmsMessagingClient(clientId, this, properties, true);

        // Establish that the client can start and receive messages
        messagingClient.start();
        sendMessage(TOPIC_NAME);
        checkMessage(clientId, TOPIC);
        messagingClient.stop(false);

        // Check to see if messages are received in a durable fashion
        sendMessage(TOPIC_NAME);
        messagingClient.start();
        checkMessage(clientId, TOPIC);
        messagingClient.stop(true);

        // Make sure durable subscriptions were closed
        sendMessage(TOPIC_NAME);
        messagingClient.start();
        checkNoMessages();
        messagingClient.stop(true);
    }

    public void testMessagingClientMultipleTopics() throws Exception {

        String clientId = "2";
        String topicName = "additionalTopic";
        String topic = "fedora.test.additional";
        properties.setProperty("topic." + topicName, topic);
        MessagingClient messagingClient =
                new JmsMessagingClient(clientId, this, properties, true);

        messagingClient.start();
        sendMessage(TOPIC_NAME);
        checkMessage(clientId, TOPIC);
        sendMessage(topicName);
        checkMessage(clientId, topic);
        messagingClient.stop(true);
    }

    public void testMessagingClientQueue() throws Exception {

        String clientId = "3";
        MessagingClient messagingClient =
                new JmsMessagingClient(clientId, this, properties, false);

        messagingClient.start();
        sendMessage(QUEUE_NAME);
        checkMessage(clientId, QUEUE);
        messagingClient.stop(true);
    }

    public void testInvalidProperties() throws Exception {
        // Null properties
        try {
            new JmsMessagingClient("4", this, null, false);
            fail("Creating a Messagingient with null properties " +
                 "should throw an exception");
        } catch(MessagingException me) {
            assertTrue(me.getMessage().contains("Connection properties may not be null"));
        }

        // Missing all properties
        properties = new Properties();

        try {
            new JmsMessagingClient("5", this, properties, false);
            fail("Creating a Messaging Client with no properties " +
                 "should throw an exception");
        } catch(MessagingException me) {
            assertTrue(me.getMessage().contains("Propery values"));
            assertTrue(me.getMessage().contains("must be provided"));
        }

        // Missing connection factory property
        properties = new Properties();
        properties.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                               "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        properties.setProperty(Context.PROVIDER_URL,
                               "vm://localhost?broker.useShutdownHook=false&broker.persistent=false");

        try {
            new JmsMessagingClient("6", this, properties, false);
            fail("Creating a Messaging Client with no connection factory " +
                 "property should throw an exception");
        } catch(MessagingException me) {
            assertTrue(me.getMessage().contains("Propery values"));
            assertTrue(me.getMessage().contains("must be provided"));
        }

        // Missing provider url property
        properties = new Properties();
        properties.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                               "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        properties.setProperty(JMSManager.CONNECTION_FACTORY_NAME,
                               "ConnectionFactory");

        try {
            new JmsMessagingClient("7", this, properties, false);
            fail("Creating a Messaging Client with no provider url " +
                 "property should throw an exception");
        } catch(MessagingException me) {
            assertTrue(me.getMessage().contains("Propery values"));
            assertTrue(me.getMessage().contains("must be provided"));
        }

        // Missing initial context factory property
        properties = new Properties();
        properties.setProperty(Context.PROVIDER_URL,
                               "vm://localhost?broker.useShutdownHook=false&broker.persistent=false");
        properties.setProperty(JMSManager.CONNECTION_FACTORY_NAME,
                               "ConnectionFactory");

        try {
            new JmsMessagingClient("8", this, properties, false);
            fail("Creating a Messaging Client with no initial context factory " +
                 "property should throw an exception");
        } catch(MessagingException me) {
            assertTrue(me.getMessage().contains("Propery values"));
            assertTrue(me.getMessage().contains("must be provided"));
        }
    }

    public void testMessageSelectors() throws Exception {

        String clientId = "9";
        // Selector to include test message
        String messageSelector = propertyName + " LIKE 'test%'";
        MessagingClient messagingClient =
                new JmsMessagingClient(clientId, this, properties,
                                       messageSelector, false);
        messagingClient.start();
        sendMessage(TOPIC_NAME);
        checkMessage(clientId, TOPIC);
        messagingClient.stop(true);

        clientId = "10";
        // Selector to omit test message
        messageSelector = propertyName + " LIKE 'testing%'";
        messagingClient =
                new JmsMessagingClient(clientId, this, properties,
                                       messageSelector, false);
        messagingClient.start();
        sendMessage(TOPIC_NAME);
        checkNoMessages();
        messagingClient.stop(true);
    }

    public void testAsynchronousStart() throws Exception {
        String clientId = "11";
        JmsMessagingClient messagingClient =
                new JmsMessagingClient(clientId, this, properties, false);

        messagingClient.start(false);
        long startTime = System.currentTimeMillis();
        long maxWaitTime = 60000;
        while(!messagingClient.isConnected()) {
            // Don't wait forever
            if(System.currentTimeMillis() - startTime > maxWaitTime) {
                fail("Messaging client did not connect in " +
                     maxWaitTime/1000 + " seconds.");
            }
        }
        sendMessage(TOPIC_NAME);
        checkMessage(clientId, TOPIC);
        messagingClient.stop(true);
    }

    private void sendMessage(String jndiName) throws Exception {
        JMSManager jmsManager = new JMSManager(properties);
        TextMessage message = jmsManager.createTextMessage(jndiName, messageText);
        message.setStringProperty(propertyName, propertyValue);
        jmsManager.send(jndiName, message);
        jmsManager.stop(jndiName);
        jmsManager.close();
    }

    /**
     * Waits for a message and checks to see if it is valid.
     */
    private void checkMessage(String clientId, String destination) throws JMSException {
        long startTime = System.currentTimeMillis();

        while (true) { // Wait for the message
            if (messageCount > 0) {
                assertNotNull(currentMessage);
                if(currentMessage instanceof TextMessage) {
                    assertEquals(messageText, ((TextMessage)currentMessage).getText());
                } else {
                    fail("Text Message expected.");
                }

                assertEquals(clientId, currentClientId);

                Destination messageDestination = currentMessage.getJMSDestination();
                if (messageDestination instanceof Topic) {
                    String topic = ((Topic) messageDestination).getTopicName();
                    assertEquals(topic, destination);
                } else if (messageDestination instanceof Queue) {
                    String queue = ((Queue) messageDestination).getQueueName();
                    assertEquals(queue, destination);
                }

                String propertyTest =
                    currentMessage.getStringProperty(propertyName);
                assertEquals(propertyValue, propertyTest);
                break;
            } else { // Check for timeout
                long currentTime = System.currentTimeMillis();
                if (currentTime > (startTime + messageTimeout)) {
                    fail("Timeout reached waiting for message.");
                    break;
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        messageCount = 0;
        currentMessage = null;
        currentClientId = null;
    }

    /**
     * Waits for a message to make sure none come through.
     */
    private void checkNoMessages() {
        long startTime = System.currentTimeMillis();

        while (true) { // Wait for the message
            if (messageCount > 0) {
                fail("No messagess should be received during this test.");
                break;
            } else { // Check for timeout
                long currentTime = System.currentTimeMillis();
                if (currentTime > (startTime + messageTimeout)) {
                    break;
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        messageCount = 0;
        currentMessage = null;
        currentClientId = null;
    }

    public void onMessage(String clientId, Message message) {
        messageCount++;
        currentMessage = message;
        currentClientId = clientId;
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestMessagingClient.class);
    }
}
