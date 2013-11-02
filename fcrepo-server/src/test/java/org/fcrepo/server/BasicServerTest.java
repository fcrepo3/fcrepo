package org.fcrepo.server;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;

import org.fcrepo.server.config.ServerConfiguration;
import org.fcrepo.server.errors.ServerInitializationException;
import org.fcrepo.server.storage.ConnectionPool;
import org.fcrepo.server.utilities.SQLUtility;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "org.apache.xerces.*", "javax.xml.*",
    "org.xml.sax.*", "javax.management.*"})
@PrepareForTest({SQLUtility.class})
public class BasicServerTest {

    private static TemporaryFolder tmpFolder;

    @Mock
    private Connection mockDefaultConnection;
    
    @Mock
    private Connection mockRWConnection;
    
    @Mock
    private ConnectionPool mockPool;
        
    private static File fedoraHomeFixture;
    
    private BasicServer test;
    
    @AfterClass
    public static void cleanUp() {
        if (fedoraHomeFixture != null) fedoraHomeFixture.delete();
        tmpFolder.delete();
    }

    @BeforeClass
    public static void bootstrap() throws IOException {
        tmpFolder = new TemporaryFolder();
        tmpFolder.create();
        fedoraHomeFixture = fakeFedoraHome();
    }
    
    private static File fakeFedoraHome() throws IOException {
        File fake = tmpFolder.newFolder("fedoraHome");
        System.setProperty("fedora.home", fake.getAbsolutePath());
        new File(fake, "server/management/uplaods").mkdirs();
        new File(fake, "server/logs").mkdir();
        new File(fake, "server/config").mkdir();
        File fcfg = new File(fake, "server/config/fedora.fcfg");
        FileWriter writer = new FileWriter(fcfg);
        writer.write("<xml></xml>");
        writer.close();
        return fake;
    }
    
    @Before
    public void setUp() throws Exception {
        fedoraHomeFixture = fakeFedoraHome();
        
        mockStatic(SQLUtility.class);
        when(SQLUtility.getConnectionPool(any(ServerConfiguration.class)))
        .thenReturn(mockPool);
        when(mockPool.getReadWriteConnection()).thenReturn(mockRWConnection);
        when(SQLUtility.getDefaultConnection(any(ServerConfiguration.class)))
        .thenReturn(mockDefaultConnection);
        
        
        test = new BasicServer(new HashMap<String, String>(), fedoraHomeFixture);
    }
    
    @Test
    public void testFirstRunEmptyDatabase() throws Exception {
        when(SQLUtility.getMostRecentRebuild(mockRWConnection))
        .thenReturn(-1L);
        test.checkRebuildHasRun(true);
        verifyStatic();
        SQLUtility.recordSuccessfulRebuild(
                eq(mockRWConnection), any(Long.class));
    }

    @Test
    public void testFirstRunNonemptyDatabase() throws Exception {
        long now = System.currentTimeMillis();
        when(SQLUtility.getMostRecentRebuild(mockRWConnection))
        .thenReturn(now);
        when(SQLUtility.getRebuildStatus(mockRWConnection, now)).thenReturn(true);
        test.checkRebuildHasRun(true);
        verifyStatic();
        SQLUtility.getRebuildStatus(mockRWConnection, now);
    }

    @Test(expected = ServerInitializationException.class)
    public void testSubsequentRunEmptyDatabase() throws Exception {
        when(SQLUtility.getMostRecentRebuild(mockRWConnection))
        .thenReturn(-1L);
        test.checkRebuildHasRun(false);
    }

    @Test(expected = ServerInitializationException.class)
    public void testSubsequentRunBadRebuild() throws Exception {
        long now = System.currentTimeMillis();
        when(SQLUtility.getMostRecentRebuild(mockRWConnection))
        .thenReturn(now);
        when(SQLUtility.getRebuildStatus(eq(mockRWConnection), any(Long.class)))
        .thenReturn(false);
        test.checkRebuildHasRun(false);
    }

    @Test
    public void testSubsequentRunGoodRebuild() throws Exception {
        long now = System.currentTimeMillis();
        when(SQLUtility.getMostRecentRebuild(mockRWConnection))
        .thenReturn(now);
        when(SQLUtility.getRebuildStatus(mockRWConnection, now))
        .thenReturn(true);
        test.checkRebuildHasRun(false);
        verifyStatic();
        SQLUtility.getRebuildStatus(mockRWConnection, now);
    }
}
