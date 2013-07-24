/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
import org.fcrepo.server.search.FieldSearch;
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
    @Mock Connection mockROConnection;
    @Mock DefaultLowlevelStorageModule lowlevelStorage;
    FedoraStorageHintProvider hints = new NullStorageHintsProvider();
    @Mock Module doManager;
    @Mock ResultSet pidExists;
    @Mock FieldSearch fieldSearch;
    
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
        when(server.getBean("org.fcrepo.server.search.FieldSearch", FieldSearch.class))
            .thenReturn(fieldSearch);
        when(server.getBean("fedoraStorageHintProvider")).thenReturn(hints);
        
        when(connectionPoolManager.getPool()).thenReturn(mockPool);
        when(connectionPoolManager.getPool(anyString())).thenReturn(mockPool);
        
        when(mockPool.getReadOnlyConnection()).thenReturn(mockROConnection);

        // Server.getPID must be overridden

        // XMLDatastreamProcessor mocks
        // Static method needs to be mocked- do we import PowerMockito?
        mockStatic(Server.class);
        when(Server.getInstance(any(File.class), eq(false))).thenReturn(server);
        when(Server.getPID(anyString())).thenReturn(new PID(obj1));
        mockStatic(SQLUtility.class);
        //result = server;
        when(server.getModule("org.fcrepo.server.storage.DOManager")).thenReturn(instance);

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
        final DefaultDOManager instance = getInstance();
        InputStream in = new ByteArrayInputStream("".getBytes(ENCODING));

        when(pidExists.next()).thenReturn(false).thenReturn(true);
        Connection mockRWConnection = mock(Connection.class);
        when(mockPool.getReadWriteConnection()).thenReturn(mockRWConnection);
        
        PreparedStatement mockInsert = mock(PreparedStatement.class);
        when(mockRWConnection.prepareStatement(
                eq(DefaultDOManager.INSERT_PID_QUERY))).thenReturn(mockInsert);
        instance.getIngestWriter(Server.USE_DEFINITIVE_STORE, context, in, FORMAT, ENCODING, obj1);
        verify(mockInsert).executeUpdate();
    }

    @Test (expected=ObjectExistsException.class)
    public void testGetIngestWriterThrowsIfObjectAlreadyExists() throws Exception
    {
        final DefaultDOManager instance = getInstance();
        InputStream in = new ByteArrayInputStream("".getBytes(ENCODING));

        when(pidExists.next()).thenReturn(true);

        instance.getIngestWriter(Server.USE_DEFINITIVE_STORE, context, in, FORMAT, ENCODING, obj1);
    }

    @Test
    public void testMultithreadedThreadSwitchesBetweenCheckAndRegisterObject() throws Throwable {
    	// mock the changing result of the existing pid check
        when(pidExists.next()).thenReturn(false).thenReturn(true);
        
        // mock the read/write connection to insert the new pid
        Connection mockRWConnection = mock(Connection.class);
        when(mockPool.getReadWriteConnection()).thenReturn(mockRWConnection);
        
        PreparedStatement mockInsert = mock(PreparedStatement.class);
        when(mockRWConnection.prepareStatement(
                eq(DefaultDOManager.INSERT_PID_QUERY))).thenReturn(mockInsert);

        PreparedStatement mockVersionQuery = mock(PreparedStatement.class);
        when(mockRWConnection.prepareStatement(DefaultDOManager.PID_VERSION_QUERY))
            .thenReturn(mockVersionQuery);
        ResultSet versionResults = mock(ResultSet.class);
        when(versionResults.next()).thenReturn(true).thenReturn(false);
        when(mockVersionQuery.executeQuery()).thenReturn(versionResults);
        PreparedStatement mockVersionUpdate = mock(PreparedStatement.class);
        when(mockRWConnection.prepareStatement(DefaultDOManager.PID_VERSION_UPDATE))
        .thenReturn(mockVersionUpdate);
        
        ThreadSwitchRunnable t1 = new ThreadSwitchRunnable();
        ThreadSwitchRunnable t2 = new ThreadSwitchRunnable();
        AssertConcurrent.assertConcurrent("unexpected", 2, t1, t2);
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
    	
    	ThreadSwitchRunnable() {
    		try {
    			manager = getInstance();
    		} catch( Exception ex ) {
    			ex.printStackTrace();
    			fail( ex.toString() );
    		}
    	}
    	
    	public void run() {
          InputStream in = null;
          try {
              in = new ByteArrayInputStream("".getBytes(ENCODING));
              DOWriter ingestWriter = manager.getIngestWriter(Server.USE_DEFINITIVE_STORE, context, in, FORMAT, ENCODING, obj1);
              successes.incrementAndGet();
              ingestWriter.commit( "" );
              manager.releaseWriter( ingestWriter );
              logger.info( "{} - thread task completed", Thread.currentThread().getName() );
              // Waiting thread is resumed after writer is released
              //waitForTick(1);
          } catch ( ObjectLockedException | ObjectExistsException ex ) {
              logger.info( "{} - thread caught expected exception: {}", Thread.currentThread().getName(), ex );
              expectedFailures.incrementAndGet();
          } catch( Exception ex ) {
              logger.error( Thread.currentThread().getName() + " - Exception", ex);
              unexpectedFailures.incrementAndGet();
          } finally {
              try {
                  in.close();
              } catch( IOException ex ) {
                  ex.printStackTrace();
              }
          }
    	}
    }
    
}
