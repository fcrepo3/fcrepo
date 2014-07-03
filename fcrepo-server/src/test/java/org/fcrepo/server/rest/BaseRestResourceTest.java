/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;

import junit.framework.JUnit4TestAdapter;

import org.apache.http.HttpStatus;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.DatastreamLockedException;
import org.fcrepo.server.errors.DatastreamNotFoundException;
import org.fcrepo.server.errors.ObjectLockedException;
import org.fcrepo.server.errors.ObjectNotFoundException;
import org.fcrepo.server.errors.ObjectNotInLowlevelStorageException;
import org.fcrepo.server.errors.RangeNotSatisfiableException;
import org.fcrepo.server.storage.DOManager;
import org.fcrepo.server.storage.DefaultDOManager;
import org.fcrepo.server.storage.types.MIMETypedStream;
import org.fcrepo.utilities.io.NullInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class BaseRestResourceTest {

    @Mock
    private Server mockServer;

    @Mock
    private DefaultDOManager mockManager;

    private BaseRestResource test;

    @Before
    public void setUp() {
        when(mockServer.getModule(DOManager.class.getName())).thenReturn(mockManager);
        test = new BaseRestResource(mockServer);    
    }

    @Test
    public void testBuildResponseStatus() throws Exception {
       MIMETypedStream input = new MIMETypedStream("lol/wut", NullInputStream.NULL_STREAM, null);
       Response output = null;
       input.setStatusCode(HttpStatus.SC_OK);
       output = test.buildResponse(input);
       assertEquals(HttpStatus.SC_OK, output.getStatus());
       input.setStatusCode(HttpStatus.SC_NOT_MODIFIED);
       output = test.buildResponse(input);
       assertEquals(HttpStatus.SC_NOT_MODIFIED, output.getStatus());
       input.setStatusCode(HttpStatus.SC_PARTIAL_CONTENT);
       output = test.buildResponse(input);
       assertEquals(HttpStatus.SC_PARTIAL_CONTENT, output.getStatus());
       input.setStatusCode(HttpStatus.SC_MOVED_TEMPORARILY);
       output = test.buildResponse(input);
       assertEquals(HttpStatus.SC_TEMPORARY_REDIRECT, output.getStatus());
    }

    @Test
    public void testExceptionResponses() throws Exception {
       Response output = null;
       
       output = test.handleException(new ObjectNotFoundException("This is testing exception handling"), false);
       assertEquals(HttpStatus.SC_NOT_FOUND, output.getStatus());
       output = test.handleException(new ObjectNotInLowlevelStorageException("This is testing exception handling"), false);
       assertEquals(HttpStatus.SC_NOT_FOUND, output.getStatus());
       output = test.handleException(new DatastreamNotFoundException("This is testing exception handling"), false);
       assertEquals(HttpStatus.SC_NOT_FOUND, output.getStatus());
       output = test.handleException(new ObjectLockedException("This is testing exception handling"), false);
       assertEquals(HttpStatus.SC_CONFLICT, output.getStatus());
       output = test.handleException(new DatastreamLockedException("This is testing exception handling"), false);
       assertEquals(HttpStatus.SC_CONFLICT, output.getStatus());
       output = test.handleException(new RangeNotSatisfiableException("This is testing exception handling"), false);
       assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE, output.getStatus());
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(BaseRestResourceTest.class);
    }
    
}
