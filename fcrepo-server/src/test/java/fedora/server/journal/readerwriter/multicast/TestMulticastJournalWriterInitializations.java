/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import fedora.server.journal.JournalException;
import fedora.server.journal.JournalWriter;
import fedora.server.journal.MockServerForJournalTesting;
import fedora.server.journal.ServerInterface;
import fedora.server.management.MockManagementDelegate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import static fedora.server.journal.readerwriter.multicast.MulticastJournalWriter.TRANSPORT_PARAMETER_PREFIX;

/**
 * @author jblake
 */
public class TestMulticastJournalWriterInitializations {

    private static final String TEST_CLASS_NAME =
            "fedora.server.journal."
                    + "readerwriter.multicast.MulticastJournalWriter";

    private static final String DUMMY_ROLE = "dummyRole";

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestMulticastJournalWriterInitializations.class);
    }

    private ServerInterface server;

    private Map<String, String> parameters;

    @Before
    public void initializeMockServer() {
        server =
                new MockServerForJournalTesting(new MockManagementDelegate(),
                                                "myHashValue");
    }

    @Before
    public void initializeBasicParameters() {
        parameters = new HashMap<String, String>();
        parameters.put("journalWriterClassname", TEST_CLASS_NAME);
    }

    @Test
    public void testInvalidTransportParameter() {
        parameters.put(TRANSPORT_PARAMETER_PREFIX + "noperiodseparator",
                       "junkValue");
        try {
            JournalWriter.getInstance(parameters, DUMMY_ROLE, server);
            fail("Expected a JournalException");
        } catch (JournalException e) {
            // expected the exception.
        }
    }

    @Test
    public void testNoTransports() {
        try {
            JournalWriter.getInstance(parameters, DUMMY_ROLE, server);
            fail("Expected a JournalException");
        } catch (JournalException e) {
            // expected the exception.
        }
    }

    @Test
    public void testMissingClassnameOnTransport() {
        addTransportParameter("one", "crucial", "false");
        addTransportParameter("two", "classname", "classTwo");
        addTransportParameter("one", "crucial", "true");
        try {
            JournalWriter.getInstance(parameters, DUMMY_ROLE, server);
            fail("Expected a JournalException");
        } catch (JournalException e) {
            // expected the exception.
        }
    }

    @Test
    public void testMissingCrucialOnTransport() {
        addTransportParameter("one", "classname", "classOne");
        addTransportParameter("two", "classname", "classTwo");
        addTransportParameter("two", "crucial", "false");
        try {
            JournalWriter.getInstance(parameters, DUMMY_ROLE, server);
            fail("Expected a JournalException");
        } catch (JournalException e) {
            // expected the exception.
        }
    }

    @Test
    public void testNoCrucialTransport() {
        addTransportParameter("one", "classname", "classOne");
        addTransportParameter("one", "crucial", "false");
        addTransportParameter("two", "classname", "classTwo");
        addTransportParameter("two", "crucial", "false");
        try {
            JournalWriter.getInstance(parameters, DUMMY_ROLE, server);
            fail("Expected a JournalException");
        } catch (JournalException e) {
            // expected the exception.
        }
    }

    @Test
    public void testBadTransportClassname() {
        addTransportParameter("one", "classname", "classOne");
        addTransportParameter("one", "crucial", "true");
        try {
            JournalWriter.getInstance(parameters, DUMMY_ROLE, server);
            fail("Expected a JournalException");
        } catch (JournalException e) {
            // expected the exception.
        }
    }

    /**
     * Transports are created; they have the correct classes; they have the
     * correct flags and parameters.
     */
    @Test
    public void testSuccessfulCreation() throws JournalException {
        Map<String, String> transportOneParameters =
                buildTransportParameters(new String[][] {
                        {"classname", MockTransport.class.getName()},
                        {"crucial", "false"}});
        Map<String, String> transportTwoParameters =
                buildTransportParameters(new String[][] {
                        {"classname", MockTransport.class.getName()},
                        {"crucial", "true"}});
        addTransportParameters("one", transportOneParameters);
        addTransportParameters("two", transportTwoParameters);

        JournalWriter writer =
                JournalWriter.getInstance(parameters, DUMMY_ROLE, server);
        assertSame("JournalWriter is the wrong class",
                   MulticastJournalWriter.class,
                   writer.getClass());
        MulticastJournalWriter mjw = (MulticastJournalWriter) writer;

        Map<String, Transport> transports = mjw.getTransports();
        assertEquals("should be two transports", 2, transports.size());
        assertEquals("transport one class", MockTransport.class, transports
                .get("one").getClass());
        assertEquals("transport one crucial flag", false, transports.get("one")
                .isCrucial());
        assertEquals("transport one parameters",
                     transportOneParameters,
                     transports.get("one").getParameters());
        assertEquals("transport two class", MockTransport.class, transports
                .get("two").getClass());
        assertEquals("transport two crucial flag", true, transports.get("two")
                .isCrucial());
        assertEquals("transport two parameters",
                     transportTwoParameters,
                     transports.get("two").getParameters());
    }

    /** Build a map of parameters from a N x 2 array of Strings */
    private Map<String, String> buildTransportParameters(String[][] rawData) {
        Map<String, String> parameters = new HashMap<String, String>();
        for (String[] pair : rawData) {
            parameters.put(pair[0], pair[1]);
        }
        return parameters;
    }

    /** Add a set of parameters for a given transport. */
    private void addTransportParameters(String transportName,
                                        Map<String, String> parameters) {
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            addTransportParameter(transportName, entry.getKey(), entry
                    .getValue());
        }
    }

    /** Add a single parameter for a given transport. */
    private void addTransportParameter(String transportName,
                                       String transportParameterKey,
                                       String value) {
        parameters.put(TRANSPORT_PARAMETER_PREFIX + transportName + "."
                + transportParameterKey, value);
    }

}
