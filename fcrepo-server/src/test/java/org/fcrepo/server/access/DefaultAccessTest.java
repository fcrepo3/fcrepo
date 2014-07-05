/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.access;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.fcrepo.server.Context;
import org.fcrepo.server.Module;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.DatastreamNotFoundException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.security.Authorization;
import org.fcrepo.server.security.DefaultAuthorization;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.DefaultDOManager;
import org.fcrepo.server.storage.types.DatastreamManagedContent;
import org.fcrepo.server.storage.types.MIMETypedStream;
import org.fcrepo.utilities.io.NullInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import junit.framework.JUnit4TestAdapter;


@RunWith(MockitoJUnitRunner.class)
public class DefaultAccessTest {

    private static final String TEST_PID = "lol:wut";
    private static final String TEST_DSID = "foo";
    private static final String TEST_ETAG_CHECKSUM = "checksum";
    @Mock
    private Server mockServer;

    @Mock
    private DefaultAuthorization mockAuthz;

    @Mock
    private DefaultDOManager mockManager;

    @Mock
    private Module mockOai;

    @Mock
    private DOReader mockReader;

    private DefaultAccess test;

    @Before
    public void setUp() throws ServerException {
        when(mockServer.getBean("org.fcrepo.server.security.Authorization", Authorization.class)).thenReturn(mockAuthz);
        when(mockServer.getModule("org.fcrepo.server.storage.DOManager")).thenReturn(mockManager);
        when(mockServer.getModule("org.fcrepo.oai.OAIProvider")).thenReturn(mockOai);
        when(mockOai.getParameter("repositoryDomainName")).thenReturn("lol.edu");
        when(mockManager.getReader(any(Boolean.class), any(Context.class), any(String.class))).thenReturn(mockReader);
        HashMap<String, String> parms = new HashMap<String, String>();
        parms.put("doMediateDatastreams", "ok");
        test = new DefaultAccess(parms, mockServer, Access.class.getName());
        test.initModule();test.postInitModule();
    }

    private DatastreamManagedContent mockDatastream() throws StreamIOException {
        return mockDatastream(NullInputStream.NULL_STREAM, 0);
    }
    
    private DatastreamManagedContent mockDatastream(String data) throws StreamIOException {
        byte[] bytes = data.getBytes();
        return mockDatastream(new ByteArrayInputStream(bytes), bytes.length);
    }

    private DatastreamManagedContent mockDatastream(InputStream content, long size) throws StreamIOException {
        DatastreamManagedContent mock = mock(DatastreamManagedContent.class);
        mock.DatastreamID = TEST_DSID;
        mock.DSChecksum = TEST_ETAG_CHECKSUM;
        mock.DSCreateDT = new Date(System.currentTimeMillis() - 1000);
        mock.DSSize = size;
        when(mock.getContentStream(any(Context.class))).thenReturn(content);
        when(mock.isRepositoryManaged()).thenReturn(true);
        return mock;
    }
    @Test
    public void testNormalDatastreamDissemination() throws Exception {
        DatastreamManagedContent ds = mockDatastream();
        when(mockReader.GetDatastream(any(String.class), any(Date.class))).thenReturn(ds);
        Context context = getContext();
        MIMETypedStream output = test.getDatastreamDissemination(context, TEST_PID, TEST_DSID, null);
        assertEquals(HttpStatus.SC_OK, output.getStatusCode());
    }

    @Test(expected=DatastreamNotFoundException.class)
    public void testMissingDatastreamDissemination() throws Exception {
        Context context = getContext();
        test.getDatastreamDissemination(context, TEST_PID, TEST_DSID, null);
    }

    @Test
    public void testCachedDatastreamDissemination() throws Exception {
        DatastreamManagedContent ds = mockDatastream();
        when(mockReader.GetDatastream(any(String.class), any(Date.class))).thenReturn(ds);
        Context context = getContext();
        // header names are stored and compared lower-cased in ReadOnlyContext
        context.getHeaders().set(HttpHeaders.IF_NONE_MATCH.toLowerCase(), TEST_ETAG_CHECKSUM);
        MIMETypedStream output = test.getDatastreamDissemination(context, TEST_PID, TEST_DSID, null);
        assertEquals(HttpStatus.SC_NOT_MODIFIED, output.getStatusCode());
    }

    @Test
    public void testPartialDatastreamDissemination() throws Exception {
        DatastreamManagedContent ds = mockDatastream("0123456789abcdef");
        when(mockReader.GetDatastream(any(String.class), any(Date.class))).thenReturn(ds);
        Context context = getContext();
        // header names are stored and compared lower-cased in ReadOnlyContext
        context.getHeaders().set(HttpHeaders.RANGE.toLowerCase(), "bytes=0-12");
        MIMETypedStream output = test.getDatastreamDissemination(context, TEST_PID, TEST_DSID, null);
        assertEquals(13l,output.getSize());
        assertEquals("0123456789abc", IOUtils.toString(output.getStream()));
        assertEquals(HttpStatus.SC_PARTIAL_CONTENT, output.getStatusCode());
    }

    private Context getContext() throws Exception {
        return ReadOnlyContext.getContext("http", "lolUser", "wutPassword", false);
    }
    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(DefaultAccessTest.class);
    }
}
