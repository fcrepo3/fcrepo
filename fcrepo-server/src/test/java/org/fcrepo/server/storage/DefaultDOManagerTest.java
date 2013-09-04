/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.storage;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.fcrepo.common.PID;
import org.fcrepo.server.Context;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ObjectExistsException;
import org.fcrepo.server.errors.ObjectLockedException;
import org.fcrepo.server.management.BasicPIDGenerator;
import org.fcrepo.server.management.ManagementModule;
import org.fcrepo.server.resourceIndex.ResourceIndexModule;
import org.fcrepo.server.search.FieldSearch;
import org.fcrepo.server.storage.lowlevel.DefaultLowlevelStorageModule;
import org.fcrepo.server.storage.translation.DOTranslatorModule;
import org.fcrepo.server.storage.types.XMLDatastreamProcessor;
import org.fcrepo.server.utilities.SQLUtility;
import org.fcrepo.server.validation.DOObjectValidatorModule;
import org.fcrepo.server.validation.DOValidatorModule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "org.apache.xerces.*", "javax.xml.*",
    "org.xml.sax.*", "javax.management.*"})
@PrepareForTest({Server.class})
public class DefaultDOManagerTest
{
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DefaultDOManagerTest.class);

    private final String FORMAT = "info:fedora/fedora-system:FOXML-1.1";

    private final String ENCODING = "UTF-8";

    private static final String DUMMY_PID = "obj:1";

    private static final PID DUMMY_PID_OBJECT = PID.getInstance(DUMMY_PID);

    private static final FedoraStorageHintProvider DUMMY_HINTS =
        new NullStorageHintsProvider();

    @Mock
    private Server mockServer;

    @Mock
    private Context mockContext;

    @Mock
    private ManagementModule mockManagement;

    @Mock
    private DefaultExternalContentManager mockExternalContent;

    @Mock
    private BasicPIDGenerator mockPidGenerator;

    @Mock
    private DOTranslatorModule mockTranslatorModule;

    @Mock
    private DOValidatorModule mockValidatorModule;

    @Mock
    private DOObjectValidatorModule mockObjectValidatorModule;

    @Mock
    private ResourceIndexModule mockResourceIndexModule;

    @Mock
    private ConnectionPoolManagerImpl mockConnectionPoolManager;

    @Mock
    private ConnectionPool mockPool;

    @Mock
    private Connection mockROConnection;

    @Mock
    private DefaultLowlevelStorageModule mockLowLevelStorage;

    @Mock
    private SQLUtility mockSqlUtility;
    
    private DOReaderCache mockReaderCache = new DOReaderCache();

    @Mock
    private ResultSet pidExists;

    @Mock
    private FieldSearch mockFieldSearch;
    
    private DefaultDOManager testObj;
    
    /**
     * Reach into the clazz and set a static member's value
     * @param clazz
     * @param name
     * @param instance
     */
    private static void setStaticMember(Class<?> clazz, String name, Object instance) {
        try {
            Field instanceField = clazz.getDeclaredField(name);
            instanceField.setAccessible(true);
            instanceField.set(null, instance);
        } catch (SecurityException e) {
            fail("Failed to set static member: " + e);
        } catch (NoSuchFieldException e) {
            fail("Failed to set static member: " + e);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            fail("Failed to set static member: " + e);
        } catch (IllegalAccessException e) {
            fail("Failed to set static member: " + e);
        }
    }

    @Before
    public void setUp() throws Exception {
        // Easiest to just short-circuit FEDORA_HOME in XMLDatastreamProcessor
        setStaticMember(XMLDatastreamProcessor.class, "initialized", true);
        setStaticMember(XMLDatastreamProcessor.class, "DC_DEFAULT_CONTROLGROUP", "X");
        setStaticMember(XMLDatastreamProcessor.class, "RELS_DEFAULT_CONTROLGROUP", "X");
        
        // Server.getPID must be overridden
        mockStatic(Server.class);
        when(Server.getPID(any(String.class))).thenReturn(DUMMY_PID_OBJECT);

    	testObj = getInstance();
    }

    DefaultDOManager getInstance() throws Exception {
        
        final Map<String, String> dummyParams = new HashMap<String,String>();
        dummyParams.put("pidNamespace", "changeme");
        dummyParams.put("defaultExportFormat", "info:fedora/fedora-system:FOXML-1.1");

        final DefaultDOManager instance = new DefaultDOManager(dummyParams, mockServer, "DOManager");

        instance.initModule();

        // postInitModule expectations
        when(mockServer.getModule("org.fcrepo.server.management.Management"))
            .thenReturn(mockManagement);
        when(mockServer.getModule("org.fcrepo.server.storage.ExternalContentManager"))
            .thenReturn(mockExternalContent);
        when(mockServer.getModule("org.fcrepo.server.management.PIDGenerator")).thenReturn(mockPidGenerator);
        when(mockServer.getModule("org.fcrepo.server.storage.translation.DOTranslator")).thenReturn(mockTranslatorModule);
        when(mockServer.getModule("org.fcrepo.server.validation.DOValidator")).thenReturn(mockValidatorModule);
        when(mockServer.getModule("org.fcrepo.server.validation.DOObjectValidator")).thenReturn(mockObjectValidatorModule);
        when(mockServer.getModule("org.fcrepo.server.resourceIndex.ResourceIndex")).thenReturn(mockResourceIndexModule);
        when(mockServer.getModule("org.fcrepo.server.storage.ConnectionPoolManager")).thenReturn(mockConnectionPoolManager);
        when(mockServer.getModule("org.fcrepo.server.storage.lowlevel.ILowlevelStorage")).thenReturn(mockLowLevelStorage);
        when(mockServer.getBean("org.fcrepo.server.search.FieldSearch", FieldSearch.class))
            .thenReturn(mockFieldSearch);
        when(mockServer.getBean("fedoraStorageHintProvider")).thenReturn(DUMMY_HINTS);
        when(mockServer.getBean("org.fcrepo.server.readerCache")).thenReturn(mockReaderCache);
        
        when(mockConnectionPoolManager.getPool()).thenReturn(mockPool);
        when(mockConnectionPoolManager.getPool(anyString())).thenReturn(mockPool);
        
        when(mockPool.getReadOnlyConnection()).thenReturn(mockROConnection);

        setStaticMember(SQLUtility.class, "instance", mockSqlUtility);
        when(mockServer.getModule("org.fcrepo.server.storage.DOManager")).thenReturn(instance);

        PreparedStatement mockStmt = mock(PreparedStatement.class);
        ResultSet mockResult = mock(ResultSet.class);
        when(mockStmt.executeQuery()).thenReturn(mockResult);
        when(mockROConnection.prepareStatement(
                eq(DefaultDOManager.CMODEL_QUERY),
                eq(ResultSet.TYPE_FORWARD_ONLY),
                eq(ResultSet.CONCUR_READ_ONLY)))
                .thenReturn(mockStmt);
        
        PreparedStatement mockExistsStmt = mock(PreparedStatement.class);
        when(mockExistsStmt.executeQuery()).thenReturn(pidExists);

        when(mockROConnection.prepareStatement(eq(DefaultDOManager.REGISTERED_PID_QUERY)))
            .thenReturn(mockExistsStmt);
        instance.postInitModule();

        return instance;
    }

    @Test
    public void testGetIngestWriterSucceeds()
            throws Exception {
        InputStream in = new ByteArrayInputStream("".getBytes(ENCODING));

        when(pidExists.next()).thenReturn(false).thenReturn(true);
        Connection mockRWConnection = mock(Connection.class);
        when(mockPool.getReadWriteConnection()).thenReturn(mockRWConnection);
        
        PreparedStatement mockInsert = mock(PreparedStatement.class);
        when(mockRWConnection.prepareStatement(
                eq(DefaultDOManager.INSERT_PID_QUERY))).thenReturn(mockInsert);
        testObj.getIngestWriter(Server.USE_DEFINITIVE_STORE, mockContext, in, FORMAT, ENCODING, DUMMY_PID);
        verify(mockInsert).executeUpdate();
    }

    @Test (expected=ObjectExistsException.class)
    public void testGetIngestWriterThrowsIfPidAlreadyRegistered() throws Exception
    {
        InputStream in = new ByteArrayInputStream("".getBytes(ENCODING));

        when(pidExists.next()).thenReturn(true);

        testObj.getIngestWriter(Server.USE_DEFINITIVE_STORE, mockContext, in, FORMAT, ENCODING, DUMMY_PID);
    }

    @Test (expected=ObjectExistsException.class)
    public void testGetIngestWriterThrowsIfObjectAlreadyExists() throws Exception
    {
        InputStream in = new ByteArrayInputStream("".getBytes(ENCODING));

        when(pidExists.next()).thenReturn(false);
        when(mockLowLevelStorage.objectExists(DUMMY_PID)).thenReturn(true);

        testObj.getIngestWriter(Server.USE_DEFINITIVE_STORE, mockContext, in, FORMAT, ENCODING, DUMMY_PID);
    }

    @Test
    public void testMultithreadedThreadSwitchesBetweenCheckAndRegisterObject() throws Throwable {
        // mock the changing result of the existing pid check
        final AtomicBoolean registered = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);

        // because we want to test a situation in which registerObject() is called
        // by a parallel thread before objectExists() finishes, so we cache the
        // existence check, and wait on a countdown latch in registerObject.
        // If the object locking strategy is ineffective, then the two threads
        // will both be able to proceed as if the object did not exist,
        // resulting in an unexpected storage error from registerObject
        when(pidExists.next()).thenAnswer(new Answer<Boolean>(){

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                boolean result = registered.get();
                LOGGER.debug("pidExists returning {}, waiting...", result);
                return result;
            }

        });

        // mock the read/write connection to insert the new pid
        Connection mockRWConnection = mock(Connection.class);
        when(mockPool.getReadWriteConnection()).thenReturn(mockRWConnection);

        final PreparedStatement mockInsert = mock(PreparedStatement.class);
        when(mockRWConnection.prepareStatement(
                eq(DefaultDOManager.INSERT_PID_QUERY))).thenAnswer(
                        new Answer<PreparedStatement>() {

                            @Override
                            public PreparedStatement answer(
                                    InvocationOnMock invocation)
                                            throws Throwable {
                                latch.await();
                                if (registered.getAndSet(true)) {
                                    throw new SQLException("object already exists!");
                                } else {
                                    return mockInsert;
                                }
                            }

                        });

        PreparedStatement mockVersionQuery = mock(PreparedStatement.class);
        when(mockRWConnection.prepareStatement(DefaultDOManager.PID_VERSION_QUERY))
        .thenReturn(mockVersionQuery);
        ResultSet versionResults = mock(ResultSet.class);
        when(versionResults.next()).thenReturn(true).thenReturn(false);
        when(mockVersionQuery.executeQuery()).thenReturn(versionResults);
        PreparedStatement mockVersionUpdate = mock(PreparedStatement.class);
        when(mockRWConnection.prepareStatement(DefaultDOManager.PID_VERSION_UPDATE))
        .thenReturn(mockVersionUpdate);

        ThreadSwitchRunnable t1 = new ThreadSwitchRunnable(testObj);
        ThreadSwitchRunnable t2 = new ThreadSwitchRunnable(testObj);
        Thread t1t = new Thread(t1);
        Thread t2t = new Thread(t2);
        t1t.start();
        t2t.start();
        // release the threads!
        latch.countDown();
        t1t.join();
        t2t.join();
        int successes = t1.successes.get() + t2.successes.get();
        int expectedFailures = t1.expectedFailures.get() + t2.expectedFailures.get();
        int unexpectedFailures = t1.unexpectedFailures.get() + t2.unexpectedFailures.get();
        assertEquals( 1, successes );
        assertEquals( 1, expectedFailures );
        assertEquals( 0, unexpectedFailures );
        assertTrue((t1.successes.get() ==1) ^ (t1.expectedFailures.get() == 1));
        assertTrue((t2.successes.get() ==1) ^ (t2.expectedFailures.get() == 1));
    }
    
    class ThreadSwitchRunnable implements Runnable {
    	DefaultDOManager manager;
    	AtomicInteger successes = new AtomicInteger();
    	AtomicInteger expectedFailures = new AtomicInteger();
    	AtomicInteger unexpectedFailures = new AtomicInteger();
    	
    	ThreadSwitchRunnable(DefaultDOManager manager) {
   			this.manager = manager;
    	}
    	
    	public void run() {
          InputStream in = null;
          try {
              in = new ByteArrayInputStream("".getBytes(ENCODING));
              DOWriter ingestWriter = manager.getIngestWriter(Server.USE_DEFINITIVE_STORE, mockContext, in, FORMAT, ENCODING, DUMMY_PID);
              ingestWriter.commit( "" );
              manager.releaseWriter( ingestWriter );
              successes.incrementAndGet();
              LOGGER.info( "{} - thread task completed", Thread.currentThread().getName() );
              try {
                  in.close();
              } catch( IOException ex ) {
                  ex.printStackTrace();
              }
          } catch ( ObjectLockedException ole ) {
              LOGGER.info( "{} - thread caught expected exception: {}", Thread.currentThread().getName(), ole );
              expectedFailures.incrementAndGet();
              try {
                  in.close();
              } catch( IOException ioe ) {
                  ole.printStackTrace();
              }
          } catch (ObjectExistsException oee ) {
              LOGGER.info( "{} - thread caught expected exception: {}", Thread.currentThread().getName(), oee );
              expectedFailures.incrementAndGet();
              try {
                  in.close();
              } catch( IOException ioe ) {
                  oee.printStackTrace();
              }
          } catch( Exception ex ) {
              LOGGER.error( Thread.currentThread().getName() + " - Exception", ex);
              unexpectedFailures.incrementAndGet();
              try {
                  in.close();
              } catch( IOException ioe ) {
                  ex.printStackTrace();
              }
          }
    	}
    }

}
