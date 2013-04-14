/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fcrepo.server.storage;

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import mockit.Deencapsulation;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import org.fcrepo.common.Constants;
import org.fcrepo.common.MalformedPIDException;
import org.fcrepo.common.PID;
import org.fcrepo.server.Context;
import org.fcrepo.server.Module;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.MalformedPidException;
import org.fcrepo.server.errors.ObjectExistsException;
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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDOManagerTest extends MultithreadedTestCase
{
    private static final Logger logger =
            LoggerFactory.getLogger(DefaultDOManagerTest.class.getName());

    private final String FORMAT = "info:fedora/fedora-system:FOXML-1.1";
    private final String ENCODING = "UTF-8";

    final String obj1 = "obj:1";

    @Mocked Server server;
    @Mocked Context context;

    DefaultDOManager getInstance() throws Exception
    {
        final Map<String, String> params = new HashMap<String,String>();
        params.put("pidNamespace", "changeme");
        params.put("defaultExportFormat", "info:fedora/fedora-system:FOXML-1.1");

        Deencapsulation.setField( Constants.class, "FEDORA_HOME", "src/main/resources/fcfg" );

        final DefaultDOManager instance = new DefaultDOManager(params, server, "DOManager");

        // Init module expectations
        new NonStrictExpectations(instance, SQLUtility.class)
        {
            {
                instance.getParameter(anyString); result = new Delegate()
                {
                    String getParameter(String name)
                    {
                        return params.get(name);
                    }

                };
            }
        };
        instance.initModule();

        // postInitModule expectations
        new NonStrictExpectations(instance, SQLUtility.class )
        {
            @Mocked ManagementModule management;
            @Mocked DefaultExternalContentManager externalContentManager;
            @Mocked BasicPIDGenerator pidGenerator;
            @Mocked DOTranslatorModule translatorModule;
            @Mocked DOValidatorModule validatorModule;
            @Mocked DOObjectValidatorModule objectValidatorModule;
            @Mocked ResourceIndexModule resourceIndexModule;
            @Mocked ConnectionPoolManagerImpl connectionPoolManager;
            @Mocked DefaultLowlevelStorageModule lowlevelStorage;
            @Mocked Module doManager;
            {
                instance.getServer(); result = server;
                server.getModule("org.fcrepo.server.management.Management"); result = management;
                server.getModule("org.fcrepo.server.storage.ExternalContentManager"); result = externalContentManager;
                server.getModule("org.fcrepo.server.management.PIDGenerator"); result = pidGenerator;
                server.getModule("org.fcrepo.server.storage.translation.DOTranslator"); result = translatorModule;
                server.getModule("org.fcrepo.server.validation.DOValidator"); result = validatorModule;
                server.getModule("org.fcrepo.server.validation.DOObjectValidator"); result = objectValidatorModule;
                server.getModule("org.fcrepo.server.resourceIndex.ResourceIndex"); result = resourceIndexModule;
                server.getModule("org.fcrepo.server.storage.ConnectionPoolManager"); result = connectionPoolManager;
                server.getModule("org.fcrepo.server.storage.lowlevel.ILowlevelStorage"); result = lowlevelStorage;
                SQLUtility.createNonExistingTables((ConnectionPool) any, (InputStream) any);
                invoke(instance, "initializeCModelDeploymentCache");
                // Server.getPID must be overridden
                Server.getPID(anyString);
                result = new Delegate()
                {
                    PID getPID(String pidString) throws MalformedPidException
                    {
                        try {
                            return new PID(pidString);
                        } catch (MalformedPIDException e) {
                            throw new MalformedPidException(e.getMessage());
                        }
                    }

                };
                // XMLDatastreamProcessor mocks
                Server.getInstance((File) any, false); result = server;
                server.getModule("org.fcrepo.server.storage.DOManager"); result = instance;
            }
        };

        instance.postInitModule();

        return instance;
    }

    @Test
    public void testGetIngestWriterSucceeds() throws Exception
    {
        final DefaultDOManager instance = getInstance();
        InputStream in = new ByteArrayInputStream("".getBytes(ENCODING));

        new Expectations(instance)
        {
            {
                instance.objectExists(anyString); result = false;
                invoke(instance, "registerObject", withAny(DigitalObject.class));
            }
        };
        instance.getIngestWriter(Server.USE_DEFINITIVE_STORE, context, in, FORMAT, ENCODING, obj1);
    }

    @Test (expected=ObjectExistsException.class)
    public void testGetIngestWriterThrowsIfObjectAlreadyExists() throws Exception
    {
        final DefaultDOManager instance = getInstance();
        InputStream in = new ByteArrayInputStream("".getBytes(ENCODING));

        new Expectations(instance)
        {
            {
                instance.objectExists(anyString); result = true;
            }
        };
        instance.getIngestWriter(Server.USE_DEFINITIVE_STORE, context, in, FORMAT, ENCODING, obj1);
    }

    @Test (expected=ObjectExistsException.class)
    public void testGetIngestWriterThrowsIfObjectIsCreatedTwice() throws Exception
    {
        final DefaultDOManager instance = getInstance();
        InputStream in = new ByteArrayInputStream("".getBytes(ENCODING));

        new Expectations(instance)
        {
            {
                instance.objectExists(anyString);
                times = 2;
                result = false;
                result = true;
                invoke(instance, "registerObject", withAny(DigitalObject.class));
            }
        };
        instance.getIngestWriter(Server.USE_DEFINITIVE_STORE, context, in, FORMAT, ENCODING, obj1);
        instance.getIngestWriter(Server.USE_DEFINITIVE_STORE, context, in, FORMAT, ENCODING, obj1);
    }

    @Test
    public void testMultithreaded() throws Throwable
    {
        int count = 1;
        TestFramework.runManyTimes( this, count );
        assertEquals( count, successes.get() );
        assertEquals( count, expectedFailures.get() );
        assertEquals( 0, unexpectedFailures.get() );
    }

    DefaultDOManager manager;
    AtomicInteger successes = new AtomicInteger();
    AtomicInteger expectedFailures = new AtomicInteger();
    AtomicInteger unexpectedFailures = new AtomicInteger();

    @Override
    public void initialize()
    {
        super.initialize();
        try
        {
            manager = getInstance();
            final AtomicBoolean objectRegistered = new AtomicBoolean(false);
            final AtomicBoolean registerObjectFirst = new AtomicBoolean(true);
            new Expectations(manager)
            {
                {
                    manager.objectExists(anyString);
                    times = 2;
                    result = new Delegate() {
                        public boolean objectExists(String pid) {
                            logger.info( "{} - enter objectExists", Thread.currentThread().getName() );
                            boolean exists = objectRegistered.get();
                            logger.info( "{} - objectExists: {}", Thread.currentThread().getName(), exists );
                            return exists;
                        }
                    };
                    invoke(manager, "registerObject", withAny(DigitalObject.class));
                    minTimes = 1;
                    result = new Delegate() {
                        public void registerObject(DigitalObject ovj) throws StorageDeviceException
                        {
                            logger.info( "{} - enter registerObject", Thread.currentThread().getName() );
                            // First thread that passes here waits and lets the other thread register the object first
                            boolean localRegisterObjectFirst = registerObjectFirst.getAndSet( false );
                            if ( localRegisterObjectFirst ) {
                                waitForTick(1);
                            }

                            if (objectRegistered.getAndSet( true))
                            {
                                throw new StorageDeviceException( "duplicate registration");
                            }
                            logger.info( "{} - registerObject object registered", Thread.currentThread().getName() );

                            // Second thread that passes here lets the first thread continue
                            if ( !localRegisterObjectFirst ) {
                                waitForTick(1);
                            }
                        }
                    };
                    invoke(manager, "unregisterObject", withAny(DigitalObject.class));
                    times = 1;
                }
            };

        }
        catch( Exception ex )
        {
            ex.printStackTrace();
            fail( ex.toString() );
        }
    }

    private void taskForThread()
    {
        InputStream in = null;
        try
        {
            in = new ByteArrayInputStream("".getBytes(ENCODING));
            DOWriter ingestWriter = manager.getIngestWriter(Server.USE_DEFINITIVE_STORE, context, in, FORMAT, ENCODING, obj1);
            successes.incrementAndGet();
            waitForTick( 2 );
            manager.releaseWriter( ingestWriter );
        }
        catch ( ObjectExistsException ex )
        {
            expectedFailures.incrementAndGet();
        }
        catch( Exception ex )
        {
            logger.error( Thread.currentThread().getName() + " - Exception", ex);
            unexpectedFailures.incrementAndGet();
        }
        finally
        {
            try
            {
                in.close();
            }
            catch( IOException ex )
            {
                ex.printStackTrace();
            }
        }
    }
    public void thread1() throws InterruptedException {
        taskForThread();
    }

    public void thread2() throws InterruptedException {
        taskForThread();
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(DefaultDOManagerTest.class);
    }
}
