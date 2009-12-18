/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.messaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.naming.Context;

import junit.framework.JUnit4TestAdapter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fedora.server.MockContext;
import fedora.server.errors.MessagingException;
import fedora.server.management.Management;
import fedora.server.management.MockManagementDelegate;
import fedora.server.messaging.Messaging.MessageType;
import fedora.server.proxy.ProxyFactory;
import fedora.test.FedoraTestCase;

/**
 * Test sending of messages by NotificationInvocationHandler.
 *
 * @author Edwin Shin
 * @version $Id$
 */
public class NotificationInvocationHandlerTest
        extends FedoraTestCase {

    private JMSManager jmsMgr;

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(NotificationInvocationHandlerTest.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        Properties props = new Properties();
        props
                .setProperty(Context.INITIAL_CONTEXT_FACTORY,
                             "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        props
                .setProperty(Context.PROVIDER_URL,
                             "vm://localhost?broker.useShutdownHook=false&broker.persistent=false");
        props.setProperty(JMSManager.CONNECTION_FACTORY_NAME,
                          "ConnectionFactory");
        props.setProperty("topic.fedora.apim.update", "fedora.apim.update");

        jmsMgr = new JMSManager(props);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        if (jmsMgr != null) {
            jmsMgr.close();
        }
    }

    @Test
    public void testAPIMPubAndReceipt() throws Exception {

        // Create a listener
        CountDownLatch listeningSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(1);
        Thread listener = new ListenerThread(listeningSignal, doneSignal);
        listener.start();
        listeningSignal.await();

        // Publish an APIM event
        try {
            Map<String, List<String>> mdMap =
                    new HashMap<String, List<String>>();
            List<String> destinations = new ArrayList<String>();
            destinations.add("fedora.apim.update");
            mdMap.put(MessageType.apimUpdate.toString(), destinations);
            Messaging msg = new MessagingImpl(getBaseURL(), mdMap, jmsMgr);
            Object[] invocationHandlers =
                    {new NotificationInvocationHandler(msg)};
            Management mgmt = new MockManagementDelegate();
            Management proxy =
                    (Management) ProxyFactory
                            .getProxy(mgmt, invocationHandlers);
            proxy.purgeObject(new MockContext(), "demo:test", null, false);
        } catch (Exception e) {
            listener.interrupt();
            throw e;
        }
        // await message receipt & evaluation
        doneSignal.await();
    }

    class ListenerThread
            extends Thread
            implements MessageListener {

        private final CountDownLatch listeningSignal;

        private final CountDownLatch doneSignal;

        ListenerThread(CountDownLatch listeningSignal, CountDownLatch doneSignal) {
            this.listeningSignal = listeningSignal;
            this.doneSignal = doneSignal;
        }

        @Override
        public void run() {
            try {
                jmsMgr.listen("fedora.apim.update", this);
            } catch (MessagingException e) {
                fail(e.getMessage());
            } finally {
                listeningSignal.countDown();
            }
        }

        @Override
        public void interrupt() {
            try {
                jmsMgr.close();
            } catch (MessagingException e) {
                fail(e.getMessage());
            } finally {
                super.interrupt();
            }
        }

        /**
         *
         * {@inheritDoc}
         */
        public void onMessage(Message msg) {
            try {
                TextMessage message = (TextMessage) msg;
                APIMMessage apim = new AtomAPIMMessage(message.getText());
                assertEquals("demo:test", apim.getPID());
            } catch (Exception e) {
                fail(e.getMessage());
            } finally {
                doneSignal.countDown();
            }
        }
    }
}
