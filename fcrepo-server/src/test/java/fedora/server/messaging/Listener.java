/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.messaging;

import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.naming.Context;

/**
 * Test class for listening to a JMS topic.
 *
 * @author Edwin Shin
 * @version $Id$
 */
public class Listener
        implements MessageListener {

    private final JMSManager jmsMgr;

    /**
     * Create a Listener object for the specified JMS topic.
     *
     * @param topicName the JMS topic to listen to, e.g. "fedora.apim.update"
     * @param jndiProps Properties file that contains, at a minimum, values for
     * Context.INITIAL_CONTEXT_FACTORY and Context.PROVIDER_URL
     * @throws Exception
     */
    public Listener(String topicName, Properties jndiProps)
            throws Exception {
        jmsMgr = new JMSManager(jndiProps);
        System.out.println("*** Listening for messages on topic: " + topicName);
        jmsMgr.listen(topicName, this);

    }

    public void onMessage(Message message) {
        try {
            TextMessage textMessage = (TextMessage) message;
            String text = textMessage.getText();
            System.out.println(text);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: " + Listener.class.getName()
                    + " jmsTopicName [providerURL]");
            System.out.println("\te.g. " + Class.class.getName()
                    + " fedora.apim.update tcp://localhost:61616");
        }

        String topic = args[0];

        Properties jndiProps = null;
        if (args.length == 2) {
            jndiProps = new Properties();
            jndiProps
                    .setProperty(Context.INITIAL_CONTEXT_FACTORY,
                                 "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            jndiProps.setProperty(Context.PROVIDER_URL, args[1]); // e.g. tcp://localhost:61616
            jndiProps.setProperty("topic." + topic, topic);
        }
        new Listener(topic, jndiProps);
    }
}
