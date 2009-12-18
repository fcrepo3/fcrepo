/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast.rmi;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import fedora.server.journal.JournalException;
import fedora.server.journal.MockServerForJournalTesting;
import fedora.server.journal.readerwriter.multicast.MockMulticastJournalWriter;
import fedora.server.management.MockManagementDelegate;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * NOTE: The tests which require a functioning RMI receiver do not work in all
 * environments, and have been disabled with the "Ignore" annotation. At this
 * time, I don't know where the problem lies.
 *
 * @author Jim Blake
 */
public class TestRmiTransport {

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestRmiTransport.class);
    }

    private Process rmiRegistryProcess;

    private StringWriter rmiRegistryProcessOutput;

    // immaterial to the test - required by the constructor.
    private static final boolean CRUCIAL = true;

    private Map<String, String> parameters;

    private MockMulticastJournalWriter parent;

    @Before
    public void initalizeBasicParameters() throws UnknownHostException {
        parameters = new HashMap<String, String>();
        parameters.put(RmiTransport.PARAMETER_HOST_NAME, InetAddress
                .getLocalHost().getHostName());
        parameters.put(RmiTransport.PARAMETER_SERVICE_NAME,
                       "RmiJournalReceiver");
    }

    @Before
    public void initializeTransportParent() throws JournalException {
        MockServerForJournalTesting server =
                new MockServerForJournalTesting(new MockManagementDelegate(),
                                                "myHashValue");
        parent =
                new MockMulticastJournalWriter(new HashMap<String, String>(),
                                               null,
                                               server);
    }

    @After
    public void stopRmiRegistry() throws InterruptedException {
        if (rmiRegistryProcess != null) {
            rmiRegistryProcess.destroy();
            rmiRegistryProcess.waitFor();
            rmiRegistryProcess = null;
            Thread.sleep(3000);
        }
    }

    @Test(expected = JournalException.class)
    public void testNoHostParameter() throws JournalException {
        parameters.remove(RmiTransport.PARAMETER_HOST_NAME);
        new RmiTransport(parameters, CRUCIAL, parent);
    }

    @Test(expected = JournalException.class)
    public void testInvalidHostParameter() throws JournalException {
        parameters.put(RmiTransport.PARAMETER_HOST_NAME, "BogusHost");
        new RmiTransport(parameters, CRUCIAL, parent);
    }

    @Test(expected = JournalException.class)
    public void testInvalidPortParameter() throws JournalException {
        parameters.put(RmiTransport.PARAMETER_PORT_NUMBER, "BogusPort");
        new RmiTransport(parameters, CRUCIAL, parent);
    }

    @Test(expected = JournalException.class)
    public void testNoServiceNameParameter() throws JournalException {
        parameters.remove(RmiTransport.PARAMETER_SERVICE_NAME);
        new RmiTransport(parameters, CRUCIAL, parent);
    }

    @Test(expected = JournalException.class)
    public void testNoRegistry() throws JournalException {
        new RmiTransport(parameters, CRUCIAL, parent);
    }

    @Ignore
    @Test(expected = JournalException.class)
    public void testNoSuchService() throws JournalException, IOException {
        startMockRmiJournalReceiver();
        parameters.put(RmiTransport.PARAMETER_SERVICE_NAME, "BogusService");
        new RmiTransport(parameters, CRUCIAL, parent);
    }

    @Ignore
    @Test
    public void testSuccessfulConnection() throws JournalException, IOException {
        startMockRmiJournalReceiver();
        new RmiTransport(parameters, CRUCIAL, parent);
    }

    @Ignore
    @Test
    public void testOpenCloseShutdownSequence() throws JournalException,
            IOException {
        startMockRmiJournalReceiver();
        RmiTransport transport = new RmiTransport(parameters, CRUCIAL, parent);

        transport.openFile("someHash", "aFileName", new Date());
        assertCorrectNumberOfCalls(1, 0, 0);

        transport.closeFile();
        assertCorrectNumberOfCalls(1, 1, 1);

        transport.shutdown();
        assertCorrectNumberOfCalls(1, 1, 1);
    }

    @Ignore
    @Test
    public void testWritesWithSmallBuffer() throws JournalException,
            IOException, XMLStreamException {
        startMockRmiJournalReceiver();
        parameters.put(RmiTransport.PARAMETER_BUFFER_SIZE, "100");
        RmiTransport transport = new RmiTransport(parameters, CRUCIAL, parent);

        transport.openFile("someHash", "aFileName", new Date());
        assertCorrectNumberOfCalls(1, 1, 0);

        XMLEventFactory factory = XMLEventFactory.newInstance();
        QName name1 = new QName("junkyElement1");
        QName name2 = new QName("junkyElement12");
        transport.getWriter()
                .add(factory.createStartElement(name1, null, null));
        transport.getWriter().add(factory.createEndElement(name1, null));
        assertCorrectNumberOfCalls(1, 1, 0);

        transport.getWriter()
                .add(factory.createStartElement(name2, null, null));
        transport.getWriter().add(factory.createEndElement(name2, null));

        transport.closeFile();
        assertCorrectNumberOfCalls(1, 3, 1);

        transport.shutdown();
        assertCorrectNumberOfCalls(1, 3, 1);
    }

    @Ignore
    @Test(expected = JournalException.class)
    public void testReceiverThrowsException() throws IOException,
            JournalException {
        startMockRmiJournalReceiver(true);
        RmiTransport transport = new RmiTransport(parameters, CRUCIAL, parent);
        transport.openFile("someHash", "aFileName", new Date());
        fail("Expected an exception.");
    }

    private void startMockRmiJournalReceiver() throws IOException {
        startMockRmiJournalReceiver(false);
    }

    private void startMockRmiJournalReceiver(boolean throwException)
            throws IOException {
        String exceptionOption =
                throwException ? "throwException" : "dontThrow";

        ProcessBuilder pb =
                new ProcessBuilder("java", MockRmiJournalReceiver.class
                        .getName(), exceptionOption);
        pb.environment()
                .put("CLASSPATH", System.getProperty("java.class.path"));
        pb.redirectErrorStream(true);
        rmiRegistryProcess = pb.start();

        StreamEater outputEater =
                new StreamEater(rmiRegistryProcess.getInputStream());
        rmiRegistryProcessOutput = outputEater.getOutput();
    }

    private void assertCorrectNumberOfCalls(int i, int j, int k) {
        // Give the output a chance to catch up with the registry process.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore an exception
        }
        String outputString = rmiRegistryProcessOutput.toString();

        String[] split1 = outputString.split("openFile\\(");
        assertEquals("wrong number of openFile() calls", i, split1.length - 1);

        String[] split2 = outputString.split("writeText\\(");
        assertEquals("wrong number of writeText() calls", j, split2.length - 1);

        String[] split3 = outputString.split("closeFile\\(");
        assertEquals("wrong number of closeFile() calls", k, split3.length - 1);
    }

    private static class StreamEater
            extends Thread {

        private final InputStream stream;

        private final StringWriter output = new StringWriter();

        private final byte[] buffer = new byte[4096];

        public StreamEater(InputStream stream) {
            this.stream = stream;
            start();
        }

        @Override
        public void run() {
            try {
                int howMany = 0;
                while (true) {
                    howMany = stream.read(buffer);
                    if (howMany > 0) {
                        output.write(new String(buffer, 0, howMany));
                    } else if (howMany == 0) {
                        Thread.yield();
                    } else {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public StringWriter getOutput() {
            return output;
        }
    }
}
