/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fcrepo.server.storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.fcrepo.common.Constants;
import org.fcrepo.common.MalformedPIDException;
import org.fcrepo.common.PID;
import org.fcrepo.server.Context;
import org.fcrepo.server.Module;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.MalformedPidException;
import org.fcrepo.server.errors.ObjectExistsException;
import org.fcrepo.server.errors.ObjectLockedException;
import org.fcrepo.server.errors.StorageDeviceException;
import org.fcrepo.server.management.BasicPIDGenerator;
import org.fcrepo.server.management.ManagementModule;
import org.fcrepo.server.resourceIndex.ResourceIndexModule;
import org.fcrepo.server.storage.lowlevel.DefaultLowlevelStorageModule;
import org.fcrepo.server.storage.translation.DOTranslatorModule;
import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.server.utilities.SQLUtility;
import org.fcrepo.server.validation.DOObjectValidatorModule;
import org.fcrepo.server.validation.DOValidatorModule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "org.apache.xerces.*", "javax.xml.*",
    "org.xml.sax.*", "javax.management.*"})
@PrepareForTest({Server.class, SQLUtility.class})
public class DefaultDOManagerTest
{
    private static final Logger logger =
            LoggerFactory.getLogger(DefaultDOManagerTest.class.getName());

    private final String FORMAT = "info:fedora/fedora-system:FOXML-1.1";
    private final String ENCODING = "UTF-8";

    final String obj1 = "obj:1";

    @Mock Server server;
    @Mock Context context;
    @Mock ManagementModule management;
    @Mock DefaultExternalContentManager externalContentManager;
    @Mock BasicPIDGenerator pidGenerator;
    @Mock DOTranslatorModule translatorModule;
    @Mock DOValidatorModule validatorModule;
    @Mock DOObjectValidatorModule objectValidatorModule;
    @Mock ResourceIndexModule resourceIndexModule;
    @Mock ConnectionPoolManagerImpl connectionPoolManager;
    @Mock ConnectionPool mockPool;
    @Mock DefaultLowlevelStorageModule lowlevelStorage;
    @Mock Module doManager;
    
    @Before
    public void setUp() {
    	System.setProperty("fedora.home", "src/main/resources/fcfg");
    }

    DefaultDOManager getInstance() throws Exception {
        final Map<String, String> params = new HashMap<String,String>();
        params.put("pidNamespace", "changeme");
        params.put("defaultExportFormat", "info:fedora/fedora-system:FOXML-1.1");

        final DefaultDOManager instance = new DefaultDOManager(params, server, "DOManager");

        instance.initModule();

        // postInitModule expectations
        when(server.getModule("org.fcrepo.server.management.Management"))
            .thenReturn(management);
        when(server.getModule("org.fcrepo.server.storage.ExternalContentManager"))
            .thenReturn(externalContentManager);
        when(server.getModule("org.fcrepo.server.management.PIDGenerator")).thenReturn(pidGenerator);
        when(server.getModule("org.fcrepo.server.storage.translation.DOTranslator")).thenReturn(translatorModule);
        when(server.getModule("org.fcrepo.server.validation.DOValidator")).thenReturn(validatorModule);
        when(server.getModule("org.fcrepo.server.validation.DOObjectValidator")).thenReturn(objectValidatorModule);
        when(server.getModule("org.fcrepo.server.resourceIndex.ResourceIndex")).thenReturn(resourceIndexModule);
        when(server.getModule("org.fcrepo.server.storage.ConnectionPoolManager")).thenReturn(connectionPoolManager);
        when(server.getModule("org.fcrepo.server.storage.lowlevel.ILowlevelStorage")).thenReturn(lowlevelStorage);
        
        when(connectionPoolManager.getPool()).thenReturn(mockPool);
        when(connectionPoolManager.getPool(anyString())).thenReturn(mockPool);

        // Server.getPID must be overridden

        // XMLDatastreamProcessor mocks
        // Static method needs to be mocked- do we import PowerMockito?
        mockStatic(Server.class);
        when(Server.getInstance(any(File.class), eq(false))).thenReturn(server);
        mockStatic(SQLUtility.class);
        //result = server;
        when(server.getModule("org.fcrepo.server.storage.DOManager")).thenReturn(instance);

        instance.postInitModule();

        return instance;
    }

    @Test
    public void testGetIngestWriterSucceeds() throws Exception
    {
        final DefaultDOManager instance = getInstance();
        InputStream in = new ByteArrayInputStream("".getBytes(ENCODING));

        when(instance.objectExists(anyString())).thenReturn(false);
        //verify(instance).registerObject(any(DigitalObject.class));
        //invoke(instance, "registerObject", withAny(DigitalObject.class));
        instance.getIngestWriter(Server.USE_DEFINITIVE_STORE, context, in, FORMAT, ENCODING, obj1);
    }

    @Test (expected=ObjectExistsException.class)
    public void testGetIngestWriterThrowsIfObjectAlreadyExists() throws Exception
    {
        final DefaultDOManager instance = getInstance();
        InputStream in = new ByteArrayInputStream("".getBytes(ENCODING));

        String objectExistsQuery = "SELECT doPID FROM doRegistry WHERE doPID=?";
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(mockConn.prepareStatement(objectExistsQuery))
            .thenReturn(mockStmt);
        when(mockPool.getReadOnlyConnection()).thenReturn(mockConn);
        when(mockStmt.execute()).thenReturn(true);
        ResultSet mockResults = mock(ResultSet.class);
        
        when(mockStmt.getResultSet()).thenReturn(mockResults);
        when(mockResults.next()).thenReturn(true);

        instance.getIngestWriter(Server.USE_DEFINITIVE_STORE, context, in, FORMAT, ENCODING, obj1);
    }

    @Test (expected=ObjectExistsException.class)
    public void testGetIngestWriterThrowsIfObjectIsCreatedTwice() throws Exception {
        final DefaultDOManager instance = getInstance();
        InputStream in = new ByteArrayInputStream("".getBytes(ENCODING));
        String objectExistsQuery = "SELECT doPID FROM doRegistry WHERE doPID=?";
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(mockConn.prepareStatement(objectExistsQuery))
            .thenReturn(mockStmt);
        when(mockPool.getReadOnlyConnection()).thenReturn(mockConn);
        when(mockStmt.execute()).thenReturn(true);
        ResultSet mockResults = mock(ResultSet.class);
        
        when(mockStmt.getResultSet()).thenReturn(mockResults);
        when(mockResults.next()).thenReturn(false).thenReturn(true);

        DOWriter ingestWriter = instance.getIngestWriter(Server.USE_DEFINITIVE_STORE, context, in, FORMAT, ENCODING, obj1);
        ingestWriter.commit( "" );
        instance.releaseWriter( ingestWriter );
        ingestWriter = instance.getIngestWriter(Server.USE_DEFINITIVE_STORE, context, in, FORMAT, ENCODING, obj1);
        instance.releaseWriter( ingestWriter );
    }

//    class TestWhenThreadSwitchesBetweenCheckAndRegisterObject extends MultithreadedTestCase {
//        @Override
//        public void initialize() {
//            super.initialize();
//            try {
//                manager = getInstance();
//                final AtomicBoolean objectRegistered = new AtomicBoolean(false);
//                final AtomicBoolean objectExistsFirst = new AtomicBoolean(true);
//                new Expectations(manager) {
//                    {
//                        manager.objectExists(anyString);
//                        minTimes = 1;
//                        result = new Delegate() {
//                            public boolean objectExists(String pid) {
//                                logger.info( "{} - enter objectExists", Thread.currentThread().getName() );
//                                boolean exists = objectRegistered.get();
//                                logger.info( "{} - objectExists: {}", Thread.currentThread().getName(), exists );
//                                // First thread that passes here waits and lets the other thread register the object first
//                                if (objectExistsFirst.getAndSet( false )) {
//                                    logger.info( "{} - putting thread in wait", Thread.currentThread().getName() );
//                                    waitForTick(1);
//                                }
//                                return exists;
//                            }
//                        };
//                        invoke(manager, "registerObject", withAny(DigitalObject.class));
//                        minTimes = 1;
//                        result = new Delegate() {
//                            public void registerObject(DigitalObject ovj) throws StorageDeviceException
//                            {
//                                logger.info( "{} - enter registerObject", Thread.currentThread().getName() );
//                                // First thread that passes here waits and lets the other thread register the object first
//
//                                if (objectRegistered.getAndSet( true))
//                                {
//                                    throw new StorageDeviceException( "duplicate registration");
//                                }
//                                logger.info( "{} - registerObject object registered", Thread.currentThread().getName() );
//                                waitForTick(1);
//                            }
//                        };
//                        invoke(manager, "unregisterObject", withAny(DigitalObject.class));
//                        times = 1;
//                    }
//                };
//
//            } catch( Exception ex ) {
//                ex.printStackTrace();
//                fail( ex.toString() );
//            }
//        }
//
//        private void taskForThread()
//        {
//            InputStream in = null;
//            try {
//                in = new ByteArrayInputStream("".getBytes(ENCODING));
//                DOWriter ingestWriter = manager.getIngestWriter(Server.USE_DEFINITIVE_STORE, context, in, FORMAT, ENCODING, obj1);
//                successes.incrementAndGet();
//                logger.info( "{} - thread task completed", Thread.currentThread().getName() );
//                waitForTick( 2 );
//                manager.releaseWriter( ingestWriter );
//            } catch ( ObjectLockedException | ObjectExistsException ex ) {
//                logger.info( "{} - thread caught expected exception: {}", Thread.currentThread().getName(), ex );
//                expectedFailures.incrementAndGet();
//            } catch( Exception ex ) {
//                logger.error( Thread.currentThread().getName() + " - Exception", ex);
//                unexpectedFailures.incrementAndGet();
//            }
//            finally {
//                try {
//                    in.close();
//                } catch( IOException ex ) {
//                    ex.printStackTrace();
//                }
//            }
//        }
//        public void thread1() throws InterruptedException {
//            taskForThread();
//        }
//
//        public void thread2() throws InterruptedException {
//            taskForThread();
//        }
//    }

//    @Test
//    public void testMultithreadedThreadSwitchesBetweenCheckAndRegisterObject() throws Throwable {
//        TestWhenThreadSwitchesBetweenCheckAndRegisterObject test = new TestWhenThreadSwitchesBetweenCheckAndRegisterObject();
//        int count = 1;
//        TestFramework.runManyTimes( test, count );
//        assertEquals( count, successes.get() );
//        assertEquals( count, expectedFailures.get() );
//        assertEquals( 0, unexpectedFailures.get() );
//    }
//
//    class TestWhenThreadSwitchesBetweenCheckAndRegisterObjectAndSecondThreadCompletesFirst extends MultithreadedTestCase
//    {
//        @Override
//        public void initialize() {
//            super.initialize();
//            try {
//                manager = getInstance();
//                final AtomicBoolean objectRegistered = new AtomicBoolean(false);
//                final AtomicBoolean objectExistsFirst = new AtomicBoolean(true);
//                new Expectations(manager) {
//                    {
//                        manager.objectExists(anyString);
//                        minTimes = 1;
//                        result = new Delegate() {
//                            public boolean objectExists(String pid) {
//                                logger.info( "{} - enter objectExists", Thread.currentThread().getName() );
//                                boolean exists = objectRegistered.get();
//                                logger.info( "{} - objectExists: {}", Thread.currentThread().getName(), exists );
//                                // First thread that passes here waits and lets the other thread register the object first
//                                if (objectExistsFirst.getAndSet( false )) {
//                                    logger.info( "{} - putting thread in wait", Thread.currentThread().getName() );
//                                    waitForTick(1);
//                                }
//                                return exists;
//                            }
//                        };
//                        invoke(manager, "registerObject", withAny(DigitalObject.class));
//                        minTimes = 1;
//                        result = new Delegate() {
//                            public void registerObject(DigitalObject ovj) throws StorageDeviceException
//                            {
//                                logger.info( "{} - enter registerObject", Thread.currentThread().getName() );
//
//                                if (objectRegistered.getAndSet( true)) {
//                                    throw new StorageDeviceException( "duplicate registration");
//                                }
//                                logger.info( "{} - registerObject object registered", Thread.currentThread().getName() );
//                            }
//                        };
//                        //invoke(manager, "unregisterObject", withAny(DigitalObject.class));
//                        manager.doCommit( anyBoolean, context, (DigitalObject)any, anyString, anyBoolean );
//                        times = 1;
//                    }
//                };
//
//            } catch( Exception ex ) {
//                ex.printStackTrace();
//                fail( ex.toString() );
//            }
//        }
//
//        private void taskForThread()
//        {
//            InputStream in = null;
//            try {
//                in = new ByteArrayInputStream("".getBytes(ENCODING));
//                DOWriter ingestWriter = manager.getIngestWriter(Server.USE_DEFINITIVE_STORE, context, in, FORMAT, ENCODING, obj1);
//                successes.incrementAndGet();
//                ingestWriter.commit( "" );
//                manager.releaseWriter( ingestWriter );
//                logger.info( "{} - thread task completed", Thread.currentThread().getName() );
//                // Waiting thread is resumed after writer is released
//                waitForTick(1);
//            } catch ( ObjectLockedException | ObjectExistsException ex ) {
//                logger.info( "{} - thread caught expected exception: {}", Thread.currentThread().getName(), ex );
//                expectedFailures.incrementAndGet();
//            } catch( Exception ex ) {
//                logger.error( Thread.currentThread().getName() + " - Exception", ex);
//                unexpectedFailures.incrementAndGet();
//            } finally {
//                try {
//                    in.close();
//                } catch( IOException ex ) {
//                    ex.printStackTrace();
//                }
//            }
//        }
//        public void thread1() throws InterruptedException {
//            taskForThread();
//        }
//
//        public void thread2() throws InterruptedException {
//            taskForThread();
//        }
//    }
//
//    @Test
//    public void testMultithreadedThreadWhenThreadSwitchesBetweenCheckAndRegisterObjectAndSecondThreadCompletesFirst() throws Throwable {
//        TestWhenThreadSwitchesBetweenCheckAndRegisterObjectAndSecondThreadCompletesFirst test =
//                new TestWhenThreadSwitchesBetweenCheckAndRegisterObjectAndSecondThreadCompletesFirst();
//        int count = 1;
//        TestFramework.runManyTimes( test, count );
//        assertEquals( count, successes.get() );
//        assertEquals( count, expectedFailures.get() );
//        assertEquals( 0, unexpectedFailures.get() );
//    }
//
//
//    DefaultDOManager manager;
//    AtomicInteger successes = new AtomicInteger();
//    AtomicInteger expectedFailures = new AtomicInteger();
//    AtomicInteger unexpectedFailures = new AtomicInteger();
//    // Supports legacy test runners
//    public static junit.framework.Test suite() {
//        return new junit.framework.JUnit4TestAdapter(DefaultDOManagerTest.class);
//    }
}
