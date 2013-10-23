package org.fcrepo.server.utilities.rebuild;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

import org.fcrepo.server.Server;
import org.fcrepo.server.config.ServerConfiguration;
import org.fcrepo.server.storage.ConnectionPool;
import org.fcrepo.server.storage.ConnectionPoolManagerImpl;
import org.fcrepo.server.storage.lowlevel.DefaultLowlevelStorageModule;
import org.fcrepo.server.utilities.SQLUtility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "org.apache.xerces.*", "javax.xml.*",
    "org.xml.sax.*", "javax.management.*"})
@PrepareForTest({Rebuild.class, SQLUtility.class})
public class SQLRebuilderTest {

    @Mock
    private Connection mockDefaultConnection;
    
    @Mock
    private Connection mockRWConnection;
    
    @Mock
    private ConnectionPool mockPool;
    
    @Mock
    private ConnectionPoolManagerImpl mockCPM;
    
    @Mock
    private DatabaseMetaData mockDBM;
    
    @Mock
    private DefaultLowlevelStorageModule mockLLS;
    
    @Mock
    private PreparedStatement mockCreateStmt;

    @Mock
    private PreparedStatement mockUpdateStmt;

    @Mock
    private ResultSet mockResults;
    
    @Mock
    private Server mockServer;
    
    @Mock
    private ServerConfiguration mockConfig;
    
    private SQLRebuilder test = new SQLRebuilder();
    
    @Before
    public void setUp() throws Exception {
        mockStatic(Rebuild.class);
        when(Rebuild.getServer()).thenReturn(mockServer);
        when(mockServer.getModule("org.fcrepo.server.storage.lowlevel.ILowlevelStorage"))
        .thenReturn(mockLLS);
        when(mockServer.getModule("org.fcrepo.server.storage.ConnectionPoolManager"))
        .thenReturn(mockCPM);
        when(mockCPM.getPool()).thenReturn(mockPool);
        when(mockPool.getReadWriteConnection()).thenReturn(mockRWConnection);
        mockStatic(SQLUtility.class);
        when(SQLUtility.getDefaultConnection(any(ServerConfiguration.class)))
        .thenReturn(mockDefaultConnection);
        when(mockDefaultConnection.getMetaData()).thenReturn(mockDBM);
        when(mockDBM.getTables(anyString(), anyString(), anyString(), any(String[].class)))
        .thenReturn(mockResults);
        
        when(mockRWConnection.prepareStatement(SQLRebuilder.CREATE_REBUILD_STATUS))
        .thenReturn(mockCreateStmt);
        when(mockRWConnection.prepareStatement(SQLRebuilder.UPDATE_REBUILD_STATUS))
        .thenReturn(mockUpdateStmt);
        
        test.setServerConfiguration(mockConfig);
    }
    
    @Test
    public void testGoodRebuild() throws Exception {
        test.start(new HashMap<String, String>());
        // expects to get a SQL msg with false on start
        verify(mockCreateStmt).setBoolean(1, false);
        verify(mockCreateStmt).execute();
        // expects to get a SQL msg with true when finished
        test.finish();
        verify(mockUpdateStmt).setBoolean(1, true);
        verify(mockUpdateStmt).execute();
    }

    @Test
    public void testBadRebuild() throws Exception {
        test.start(new HashMap<String, String>());
        // expects to get a SQL msg with false on start
        verify(mockCreateStmt).setBoolean(1, false);
        verify(mockCreateStmt).execute();
        // should not update without finishing normally
        verify(mockUpdateStmt, never()).setBoolean(1, true);
        verify(mockUpdateStmt, never()).execute();
    }
}
