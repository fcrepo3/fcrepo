/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import junit.framework.JUnit4TestAdapter;

import fedora.common.Constants;
import fedora.common.rdf.RDFName;

import fedora.server.Context;
import fedora.server.errors.InvalidStateException;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ModuleShutdownException;
import fedora.server.errors.ServerException;
import fedora.server.journal.entry.JournalEntryContext;
import fedora.server.management.Management;
import fedora.server.management.MockManagementDelegate;
import fedora.server.management.MockManagementDelegate.Call;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Check every management method to be sure:
 * <ul>
 * <li>that we know whether it is journaled or not</li>
 * <li>that we know what items are stored in the context for recovery</li>
 * <li>that a journaled method is played back the same as it was recorded</li>
 * <li>that a journaled method WILL NOT be accepted from an outside source by a
 * JournalConsumer</li>
 * <li>that a non-journaled method WILL be accepted from an outside source by a
 * JournalConsumer</li>
 * </ul>
 *
 * @author Jim Blake
 */
public class TestJournalRoundTrip {

    private static final String METHOD_GET_TEMP_STREAM = "getTempStream";

    private static final String THE_ROLE = "theRole";

    private static final String THE_SERVER_HASH = "theServerHash";

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestJournalRoundTrip.class);
    }

    /**
     * Defines the names of the classes used for reading, writing, and logging.
     */
    private Map<String, String> journalParameters;

    /**
     * Context items that will be provided to the JournalCreator.
     */
    private JournalEntryContext leadingContext;

    /**
     * Attributes that we expect to see added to the {@link #leadingContext}
     * when the management method is called.
     */
    private Map<RDFName, Object> contextAdditions;

    /**
     * Context items as expected after the call.
     */
    private JournalEntryContext expectedContext;

    /**
     * The journal module of the "leader" server.
     */
    private JournalCreator creator;

    /**
     * Records the calls that are submitted to the Management interface by the
     * JournalCreator.
     */
    private MockManagementDelegate leadingDelegate;

    /**
     * The journal module of the "follower" server.
     */
    private JournalConsumer consumer;

    /**
     * Records the calls that are submitted to the Management interface by the
     * JournalConsumer.
     */
    MockManagementDelegate followingDelegate;

    private Call expectedCall;

    /*
     * ------------------------------------------------------------------------
     * Setup
     * ------------------------------------------------------------------------
     */

    /**
     * For each test, set the parameters that will be required by the
     * {@link #creator} and the {@link #consumer}.
     */
    @Before
    public void initializeJournalParameters() {
        journalParameters = new HashMap<String, String>();
        journalParameters
                .put(JournalConstants.PARAMETER_JOURNAL_WRITER_CLASSNAME,
                     MockJournalWriter.class.getName());
        journalParameters
                .put(JournalConstants.PARAMETER_JOURNAL_READER_CLASSNAME,
                     MockJournalReader.class.getName());
        journalParameters
                .put(JournalConstants.PARAMETER_JOURNAL_RECOVERY_LOG_CLASSNAME,
                     MockJournalRecoveryLog.class.getName());
    }

    @Before
    public void initializeContextObjects() {
        leadingContext = new JournalEntryContext();
        expectedContext = null;
        contextAdditions = new HashMap<RDFName, Object>();
    }

    @Before
    public void initializeExpectedCall() {
        // Every test should call buildExpectedCall().
        expectedCall = null;
    }

    /*
     * ------------------------------------------------------------------------
     * The tests
     * ------------------------------------------------------------------------
     */

    @Test
    public void addDatastream() throws ServerException {
        expectInContext(Constants.RECOVERY.DATASTREAM_ID, "theDsId");
        testJournaledMethod(JournalConstants.METHOD_ADD_DATASTREAM,
                            leadingContext,
                            "thePid",
                            "theDsId",
                            new String[0],
                            "theDsLabel",
                            false,
                            "theMIMEType",
                            "theFormatURI",
                            "theLocation",
                            "theControlGroup",
                            "theDsState",
                            "theChecksumType",
                            "theChecksum",
                            "theLogMessage");
    }

    @Test
    public void addRelationship() throws ServerException {
        testJournaledMethod(JournalConstants.METHOD_ADD_RELATIONSHIP,
                            leadingContext,
                            "theSubject",
                            "relationship",
                            "anObject",
                            false,
                            "");
    }

    @Test
    public void compareDatastreamChecksum() throws ServerException {
        testNonJournaledMethod("compareDatastreamChecksum",
                               leadingContext,
                               "thePid",
                               "theDsId",
                               new Date(12345L));
    }

    @Test
    public void export() throws ServerException {
        testNonJournaledMethod("export",
                               leadingContext,
                               "PID",
                               "format",
                               "SomeExportContext",
                               "encoding");
    }

    @Test
    public void getDatastream() throws ServerException {
        testNonJournaledMethod("getDatastream",
                               leadingContext,
                               "PID",
                               "aDatastreamID",
                               new Date());
    }

    @Test
    public void getDatastreamHistory() throws ServerException {
        testNonJournaledMethod("getDatastreamHistory",
                               leadingContext,
                               "PID",
                               "anotherDatastreamID");
    }

    @Test
    public void getDatastreams() throws ServerException {
        testNonJournaledMethod("getDatastreams",
                               leadingContext,
                               "sonOfPID",
                               new Date(111111L),
                               "someStateString");
    }

    @Test
    public void getNextPID() throws ServerException {
        expectInContext(Constants.RECOVERY.PID_LIST, new String[] {
                "sillyPID_0", "sillyPID_1", "sillyPID_2", "sillyPID_3",
                "sillyPID_4"});
        testJournaledMethod(JournalConstants.METHOD_GET_NEXT_PID,
                            leadingContext,
                            5,
                            "myFavoriteNamespace");
    }

    @Test
    public void getObjectXML() throws ServerException {
        testNonJournaledMethod("getObjectXML",
                               leadingContext,
                               "myPID",
                               "encodingScheme");
    }

    @Test
    public void getRelationships() throws ServerException {
        testNonJournaledMethod("getRelationships",
                               leadingContext,
                               "mySubject",
                               "someRelationship");
    }

    /**
     * This one will always be special, in that it doesn't use a context as its
     * first argument. If it were a Journaled method, that would be a problem.
     */
    @Test
    public void getTempStream() throws ServerException {
        testNonJournaledMethod(METHOD_GET_TEMP_STREAM, "streamID");
    }

    @Test
    public void ingest() throws ServerException {
        expectInContext(Constants.RECOVERY.PID, "Ingest:1");
        testJournaledMethod(JournalConstants.METHOD_INGEST,
                            leadingContext,
                            new ByteArrayInputStream(new byte[0]),
                            "theLogMessage",
                            "aFormat",
                            "someEncoding",
                            true);
    }

    @Test
    public void modifyDatastreamByReference() throws ServerException {
        testJournaledMethod(JournalConstants.METHOD_MODIFY_DATASTREAM_BY_REFERENCE,
                            leadingContext,
                            "myPid",
                            "datastreamIdentifier",
                            new String[] {"altID"},
                            "datastreamLabel",
                            "mime/type",
                            "formatUri",
                            "dsLocation",
                            "checksumType",
                            "checksum",
                            "logMessage",
                            false);
    }

    @Test
    public void modifyDatastreamByValue() throws ServerException {
        testJournaledMethod(JournalConstants.METHOD_MODIFY_DATASTREAM_BY_VALUE,
                            leadingContext,
                            "myPid",
                            "datastreamIdentifier",
                            new String[] {"altID"},
                            "datastreamLabel",
                            "mime/type",
                            "formatUri",
                            new ByteArrayInputStream(new byte[0]),
                            "checksumType",
                            "checksum",
                            "logMessage",
                            false);
    }

    @Test
    public void modifyObject() throws ServerException {
        testJournaledMethod(JournalConstants.METHOD_MODIFY_OBJECT,
                            leadingContext,
                            "myPid",
                            "state",
                            "objectLabel",
                            "owner",
                            "logMessage");
    }

    @Test
    public void purgeDatastream() throws ServerException {
        testJournaledMethod(JournalConstants.METHOD_PURGE_DATASTREAM,
                            leadingContext,
                            "myPid",
                            "dsID",
                            new Date(123),
                            new Date(456),
                            "logMessage",
                            true);
    }

    @Test
    public void purgeObject() throws ServerException {
        testJournaledMethod(JournalConstants.METHOD_PURGE_OBJECT,
                            leadingContext,
                            "aPID",
                            "PurgeLogMessage",
                            true);
    }

    @Test
    public void purgeRelationship() throws ServerException {
        testJournaledMethod(JournalConstants.METHOD_PURGE_RELATIONSHIP,
                            leadingContext,
                            "aSubject",
                            "theRelationship",
                            "someObject",
                            false,
                            "datatype");
    }

    @Test
    public void putTempStream() throws ServerException {
        expectInContext(Constants.RECOVERY.UPLOAD_ID, "tempStreamId");
        testJournaledMethod(JournalConstants.METHOD_PUT_TEMP_STREAM,
                            leadingContext,
                            new ByteArrayInputStream(new byte[0]));
    }

    @Test
    public void setDatastreamState() throws ServerException {
        testJournaledMethod(JournalConstants.METHOD_SET_DATASTREAM_STATE,
                            leadingContext,
                            "pid",
                            "dsID",
                            "dsState",
                            "dsLogMessage");
    }

    @Test
    public void setDatastreamVersionable() throws ServerException {
        testJournaledMethod(JournalConstants.METHOD_SET_DATASTREAM_VERSIONABLE,
                            leadingContext,
                            "lastPID",
                            "lastDsID",
                            true,
                            "the Log!");
    }

    /*
     * ------------------------------------------------------------------------
     * Helper methods.
     * ------------------------------------------------------------------------
     */

    /**
     * What additional "recovery attributes" should we expect the leading
     * delegate to set into the context?
     */
    private void expectInContext(RDFName key, Object value) {
        contextAdditions.put(key, value);
    }

    /**
     * <p>
     * Test a Journaled method.
     * </p>
     * <p>
     * Call the selected method on the JournalCreator. Then tell the
     * JournalConsumer to process the journal. Compare the calls received by
     * both Management delegates to the call we expected them to see.
     * </p>
     * <p>
     * Calling the method directly on the JournalConsumer should produce an
     * exception.
     * </p>
     *
     * @throws ModuleInitializationException
     * @throws ModuleShutdownException
     */
    private void testJournaledMethod(String methodName, Object... arguments)
            throws ServerException {
        buildExpectedCall(methodName, arguments);

        setupLeader();
        executeManagmentMethod(creator, methodName, arguments);
        closeLeader();

        setupFollower();
        letFollowerCatchUp();

        assertExpectedCall("leading", leadingDelegate);
        assertExpectedCall("following", followingDelegate);

        try {
            executeManagmentMethod(consumer, methodName, arguments);
            fail("expected an InvalidStateException");
        } catch (InvalidStateException e) {
            // That's the one we expected.
        }
    }

    /**
     * <p>
     * Test a non-Journaled method.
     * </p>
     * <p>
     * Call the selected method on the JournalCreator. Check that the Journal is
     * empty. Compare the call received by the Management delegates to the call
     * we expected it to see.
     * </p>
     * <p>
     * Call the selected method on the JournalConsumer. Again, compare the call
     * to the one that we expected.
     * </p>
     */
    private void testNonJournaledMethod(String methodName, Object... arguments)
            throws ServerException {
        buildExpectedCall(methodName, arguments);

        setupLeader();
        executeManagmentMethod(creator, methodName, arguments);
        closeLeader();

        assertEmptyJournal();
        assertExpectedCall("leading", leadingDelegate);

        setupFollower();
        letFollowerCatchUp();
        executeManagmentMethod(consumer, methodName, arguments);
        assertExpectedCall("following", followingDelegate);
    }

    /**
     * <p>
     * The expected call includes the expected context, as well as the supplied
     * arguments.
     * </p>
     * <p>
     * Note that "getTempStream" is the only Management call that doesn't use
     * "context" as its first argument.
     * </p>
     */
    private void buildExpectedCall(String methodName, Object[] arguments) {
        loadExpectedContext();
        if (arguments[0] == leadingContext) {
            arguments[0] = expectedContext;
        }
        expectedCall = new Call(methodName, arguments);
    }

    /**
     * The {@link #expectedContext} should be like the {@link #leadingContext},
     * with the addition of anything we expect the management method to add.
     */
    private void loadExpectedContext() {
        expectedContext = new JournalEntryContext(leadingContext);
        for (Map.Entry<RDFName, Object> entry : contextAdditions.entrySet()) {
            try {
                expectedContext.getRecoveryAttributes().set(entry.getKey().uri,
                                                            entry.getValue());
            } catch (Exception e) {
                fail("Stupid design of MultiValueMap");
            }
        }
    }

    private void setupLeader() throws ModuleInitializationException {
        leadingDelegate = new MockManagementDelegate();
        MockServerForJournalTesting leadingServer =
                new MockServerForJournalTesting(leadingDelegate,
                                                THE_SERVER_HASH);
        creator =
                new JournalCreator(journalParameters, THE_ROLE, leadingServer);
        creator.setManagementDelegate(leadingDelegate);
    }

    /**
     * <p>
     * Note that "getTempStream" is the only Management call that doesn't use
     * "context" as its first argument.
     * </p>
     */
    private void executeManagmentMethod(Management management,
                                        String methodName,
                                        Object[] arguments)
            throws ServerException {
        Class<?>[] argTypes = new Class[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            argTypes[i] = getArgType(arguments[i]);
        }

        try {
            Method method =
                    Management.class.getDeclaredMethod(methodName, argTypes);
            method.invoke(management, arguments);
        } catch (SecurityException e) {
            e.printStackTrace();
            fail("Failed to invoke the method: " + e);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            fail("Failed to invoke the method: " + e);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            fail("Failed to invoke the method: " + e);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            fail("Failed to invoke the method: " + e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ServerException) {
                throw (ServerException) cause;
            } else {
                e.printStackTrace();
                fail("Failed to invoke the method: " + e);
            }
        }
    }

    /**
     * Figure the class of each argument to the call, assuming that we always
     * use <code>boolean</code> or <code>int</code> rather than
     * {@link Bookean} or {@link Integer}. Also, use {@link Context} and
     * {@link InputStream} rather than one of their subclasses.
     */
    private Class<?> getArgType(Object argument) {
        if (argument == null) {
            fail("Can't run unit test with null arguments.");
        }
        Class<?> argType = argument.getClass();
        if (argType.equals(Integer.class)) {
            argType = Integer.TYPE;
        }
        if (argType.equals(Boolean.class)) {
            argType = Boolean.TYPE;
        }
        if (Context.class.isAssignableFrom(argType)) {
            argType = Context.class;
        }
        if (InputStream.class.isAssignableFrom(argType)) {
            argType = InputStream.class;
        }
        return argType;
    }

    private void closeLeader() throws ModuleShutdownException {
        // Tell the leader to close the "file".
        creator.shutdown();
        // Transfer the journal contents to the reader.
        MockJournalReader.setBuffer(MockJournalWriter.getBuffer());
    }

    public void setupFollower() throws ModuleInitializationException {
        followingDelegate = new MockManagementDelegate();
        MockServerForJournalTesting followingServer =
                new MockServerForJournalTesting(followingDelegate,
                                                THE_SERVER_HASH);
        consumer =
                new JournalConsumer(journalParameters,
                                    THE_ROLE,
                                    followingServer);
    }

    private void letFollowerCatchUp() throws ModuleShutdownException {
        // Start the follower.
        consumer.setManagementDelegate(followingDelegate);
        // Wait for it to catch up.
        waitForConsumerThread();
        // Shut it down.
        consumer.shutdown();
    }

    /**
     * Compare the expected call with the call that the delegate actually
     * recorded.
     */
    private void assertExpectedCall(String label,
                                    MockManagementDelegate delegate) {
        if (delegate.getCallCount() != 1) {
            fail("Wrong number of " + label + " calls: expected 1, actual "
                    + delegate.getCallCount() + ". Calls are as follows:\n"
                    + delegate.getCalls());
        }
        assertEquals(label + " calls", expectedCall, delegate.getCalls().get(0));
    }

    private void assertEmptyJournal() {
        assertEquals("non-empty journal", 0, MockJournalWriter.getBuffer()
                .length());
    }

    /**
     * Let me know when the follower has caught up.
     */
    private void waitForConsumerThread() {
        try {
            // Shouldn't there be a better way?
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // Won't happen; wouldn't care.
        }
    }

}
