/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.messaging;

import java.util.Properties;

import javax.naming.Context;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.apache.activemq.broker.BrokerService;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;

import fedora.server.errors.MessagingException;
import fedora.server.messaging.JMSManager.DestinationType;

/**
* Tests the JMSManager
*
* @author Edwin Shin
* @author Bill Branan
* @version $Id$
*/
public class JMSManagerTest extends TestCase implements MessageListener {

    private Properties properties;
    private final String messageText = "Message Text";
    private Message currentMessage = null;
    private int messageCount = 0;
    private final int timeout = 5000; // Maximum number of milliseconds to wait for a message

    @Override
    @Before
    public void setUp() throws Exception {
        properties = new Properties();
        properties.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                               "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        properties.setProperty(Context.PROVIDER_URL,
                               "vm://localhost");
        properties.setProperty(JMSManager.CONNECTION_FACTORY_NAME,
                               "ConnectionFactory");

        messageCount = 0;
        currentMessage = null;
    }

    @Test
    public void testVMMessage() throws Exception {
        String topic = "jmsmanager.test";
        properties.setProperty("topic." + topic, topic);
        JMSManager jmsMgr = new JMSManager(properties);
        jmsMgr.listen(topic, this);
        jmsMgr.send(topic, messageText);
        checkMessage(topic, DestinationType.Topic, messageText);
        jmsMgr.close();
    }

    @Ignore("Broker thread in test occationally fails to start")
    @Test
    public void testTCPMessage() throws Exception {
        /*
        String topic = "jmsmanager.test";
        String connectorUrl = "tcp://localhost:61616";
        JmsBroker broker = new JmsBroker(connectorUrl);
        broker.start();

        long start = System.currentTimeMillis();
        while(System.currentTimeMillis() < start + timeout) {
            // wait for broker to start
        }

        properties.setProperty(Context.PROVIDER_URL, connectorUrl);
        properties.setProperty("topic." + topic, topic);
        JMSManager jmsMgr = new JMSManager(properties);
        jmsMgr.listen(topic, this);
        jmsMgr.send(topic, messageText);
        checkMessage(topic, DestinationType.Topic, messageText);
        jmsMgr.close();

        broker.exit();

        start = System.currentTimeMillis();
        while(System.currentTimeMillis() < start + timeout) {
            // wait for broker to stop
        }
        */
    }

    @Test
    public void testCreateTopic() throws Exception {
        String topic = "jmsmanager.test";
        JMSManager jmsMgr = new JMSManager(properties);
        jmsMgr.createDestination(topic, DestinationType.Topic);
        jmsMgr.listen(topic, this);
        jmsMgr.send(topic, messageText);
        checkMessage(topic, DestinationType.Topic, messageText);
        jmsMgr.close();
    }

    @Test
    public void testCreateQueue() throws Exception {
        String queue = "jmsmanager";
        JMSManager jmsMgr = new JMSManager(properties);
        jmsMgr.createDestination(queue, DestinationType.Queue);
        jmsMgr.listen(queue, this);
        jmsMgr.send(queue, messageText);
        checkMessage(queue, DestinationType.Queue, messageText);
        jmsMgr.close();
    }

    @Test
    public void testSendToDestination() throws Exception {
        String topic = "jmsmanager.test";
        JMSManager jmsMgr = new JMSManager(properties);
        Destination destination =
            jmsMgr.createDestination(topic, DestinationType.Topic);
        TextMessage textMessage = jmsMgr.createTextMessage(topic, messageText);
        jmsMgr.listen(destination, this);
        jmsMgr.send(destination, textMessage);
        checkMessage(topic, DestinationType.Topic, messageText);
        jmsMgr.close();
    }

    @Test
    public void testMessageSelectors() throws Exception {
        String topic = "jmsmanager.test";
        JMSManager jmsMgr = new JMSManager(properties);
        jmsMgr.createDestination(topic, DestinationType.Topic);

        String messageSelector = "jmsProperty IN ('selectMe')";
        jmsMgr.listen(topic, messageSelector, this);

        TextMessage textMessage = jmsMgr.createTextMessage(topic, messageText);
        textMessage.setStringProperty("jmsProperty", "selectMe");
        jmsMgr.send(topic, textMessage);
        checkMessage(topic, DestinationType.Topic, messageText);

        textMessage = jmsMgr.createTextMessage(topic, messageText);
        textMessage.setStringProperty("jmsProperty", "doNotSelectMe");
        jmsMgr.send(topic, textMessage);
        checkNoMessage();

        jmsMgr.close();
    }

    @Test
    public void testDurableSubscription() throws Exception {
        // Connect and ensure that a message can be received
        String topic = "jmsmanager.test.durable";
        JMSManager jmsMgr = new JMSManager(properties, "clientId1");
        jmsMgr.listenDurable(topic, this);
        jmsMgr.send(topic, messageText);
        checkMessage(topic, DestinationType.Topic, messageText);

        // Check for listener durability
        String message2 = "Message Number 2";
        jmsMgr.stopDurable(topic);
        jmsMgr.send(topic, message2);
        checkNoMessage();
        jmsMgr.listenDurable(topic, this);
        checkMessage(topic, DestinationType.Topic, message2);

        // Check unsubscribe
        String message3 = "Message Number 3";
        jmsMgr.unsubscribeDurable(topic);
        jmsMgr.send(topic, message3);
        checkNoMessage();
        jmsMgr.close();
    }

    @Test
    public void testSendMessages() throws Exception {
        String topic = "jmsmanager.test";
        JMSManager jmsMgr = new JMSManager(properties);
        jmsMgr.createDestination(topic, DestinationType.Topic);
        jmsMgr.listen(topic, this);

        TextMessage textMessage = jmsMgr.createTextMessage(topic, messageText);
        jmsMgr.send(topic, textMessage);
        checkMessage(topic, DestinationType.Topic, messageText);

        BytesMessage bytesMessage = jmsMgr.createBytesMessage(topic);
        bytesMessage.writeBytes(messageText.getBytes());
        jmsMgr.send(topic, bytesMessage);
        checkMessage(topic, DestinationType.Topic, null);

        ObjectMessage objectMessage = jmsMgr.createObjectMessage(topic, messageText);
        jmsMgr.send(topic, objectMessage);
        checkMessage(topic, DestinationType.Topic, null);

        MapMessage mapMessage = jmsMgr.createMapMessage(topic);
        mapMessage.setString("key", messageText);
        jmsMgr.send(topic, mapMessage);
        checkMessage(topic, DestinationType.Topic, null);

        StringBuffer serializableObj = new StringBuffer(messageText);
        jmsMgr.send(topic, serializableObj);
        checkMessage(topic, DestinationType.Topic, null);

        jmsMgr.close();
    }

    @Test
    public void testMessageVolume() throws Exception {
        String topic = "jmsmanager.test";
        JMSManager jmsMgr = new JMSManager(properties);
        jmsMgr.createDestination(topic, DestinationType.Topic);
        jmsMgr.listen(topic, this);

        int sentMessages = 0;
        for(int i=0; i<timeout; i++) {
            jmsMgr.send(topic, messageText);
            ++sentMessages;
        }

        long startTime = System.currentTimeMillis();
        boolean timeExpired = false;
        while (messageCount < sentMessages) {
            if (timeExpired) {
                fail("Sent " + sentMessages + " messages but only received "
                        + messageCount + " messages");
            }
            if (System.currentTimeMillis() > (startTime + timeout)) {
                timeExpired = true;
            }
        }

        jmsMgr.close();
    }

    @Test
    public void testInvalidProperties() throws Exception {
        // Null properties
        try {
            new JMSManager(null);
            fail("Creating a JMSManager with null properties " +
                 "should throw an exception");
        } catch(MessagingException expected) {
            assertTrue(expected.getMessage().contains("properties"));
        }

        // Missing all properties
        properties = new Properties();

        try {
            new JMSManager(properties);
            fail("Creating a JMSManager with no properties " +
                 "should throw an exception");
        } catch(MessagingException expected) {
            assertTrue(expected.getMessage().contains(Context.INITIAL_CONTEXT_FACTORY));
        }

        // Missing provider url property
        properties = new Properties();
        properties.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                               "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        properties.setProperty(JMSManager.CONNECTION_FACTORY_NAME,
                               "ConnectionFactory");

        try {
            new JMSManager(properties);
            fail("Creating a JMSManager with no provider url " +
                 "property should throw an exception");
        } catch(MessagingException expected) {
            assertTrue(expected.getMessage().contains(Context.PROVIDER_URL));
        }

        // Missing initial context factory property
        properties = new Properties();
        properties.setProperty(Context.PROVIDER_URL,
                               "vm://localhost");
        properties.setProperty(JMSManager.CONNECTION_FACTORY_NAME,
                               "ConnectionFactory");

        try {
            new JMSManager(properties);
            fail("Creating a JMSManager with no initial context factory " +
                 "property should throw an exception");
        } catch(MessagingException expected) {
            assertTrue(expected.getMessage().contains(Context.INITIAL_CONTEXT_FACTORY));
        }

        // Invalid initial context factory
        properties = new Properties();
        properties.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                               "test.InvalidInitialContextFactory");
        properties.setProperty(Context.PROVIDER_URL,
                               "vm://localhost");
        properties.setProperty(JMSManager.CONNECTION_FACTORY_NAME,
                               "ConnectionFactory");
        try {
            new JMSManager(properties);
            fail("Starting a JMSManager with an invalid initial " +
                 "context factory should throw an exception");
        } catch(MessagingException expected) {}

        // Invalid provider url
        properties = new Properties();
        properties.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                               "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        properties.setProperty(Context.PROVIDER_URL,
                               "tcp://localhost:00000");
        properties.setProperty(JMSManager.CONNECTION_FACTORY_NAME,
                               "ConnectionFactory");
        try {
            new JMSManager(properties);
            fail("Starting a JMSManager with an invalid " +
                 "provider url should throw an exception");
        } catch(MessagingException expected) {}
    }

    /**
     * Waits for a message and checks to see if it is valid.
     */
    private void checkMessage(String destination, DestinationType type, String messageText) throws Exception {
        long startTime = System.currentTimeMillis();

        while (true) { // Wait for the message
            if (messageCount > 0) {
                assertNotNull(currentMessage);
                if(currentMessage instanceof TextMessage) {
                    assertEquals(messageText, ((TextMessage)currentMessage).getText());
                }

                Destination messageDestination = currentMessage.getJMSDestination();
                if(type.equals(DestinationType.Topic)) {
                    if (messageDestination instanceof Topic) {
                        String topic = ((Topic) messageDestination).getTopicName();
                        assertEquals(topic, destination);
                    } else {
                        fail("Destination type for message should have been Topic");
                    }
                } else {
                    if (messageDestination instanceof Queue) {
                        String queue = ((Queue) messageDestination).getQueueName();
                        assertEquals(queue, destination);
                    } else {
                        fail("Destination type for message should have been Queue");
                    }
                }
                break;
            } else { // Check for timeout
                long currentTime = System.currentTimeMillis();
                if (currentTime > (startTime + timeout)) {
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
    }

    /**
     * Waits for a message to make sure none come through.
     */
    private void checkNoMessage() {
        long startTime = System.currentTimeMillis();

        while (true) { // Wait for the notification message
            if (messageCount > 0) {
                fail("No messages should be received during this test.");
                break;
            } else { // Check for timeout
                long currentTime = System.currentTimeMillis();
                if (currentTime > (startTime + timeout)) {
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
    }

    public void onMessage(Message message) {
        currentMessage = message;
        messageCount++;
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(JMSManagerTest.class);
    }

    public class JmsBroker extends Thread {

        private final String connectorUrl;
        private boolean stop = false;

        public JmsBroker(String connectorUrl) {
            this.connectorUrl = connectorUrl;
        }

        @Override
        public void run() {
            BrokerService broker = new BrokerService();
            try {
                broker.addConnector(connectorUrl);
                broker.start();
            } catch (Exception e) {
                System.err.println("Exception encountered starting "
                        + "broker with connectorUrl: " + connectorUrl
                        + ". Exception message: " + e.getMessage());
            }

            while (!stop) {
                // Run the broker
            }

            try {
                broker.stop();
            } catch (Exception e) {
                System.err.println("Exception encountered stopping "
                        + "broker with connectorUrl: " + connectorUrl
                        + ". Exception message: " + e.getMessage());
            }
        }

        public void exit() {
            stop = true;
        }
    }
}
