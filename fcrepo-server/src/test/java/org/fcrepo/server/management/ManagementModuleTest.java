/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.management;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.fcrepo.server.BasicServer;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;


/**
 * @author Edwin Shin
 *
 */
public class ManagementModuleTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * Test method for {@link org.fcrepo.server.management.ManagementModule#initModule()}.
     */
    @Test
    public void testInitModuleFailsWithoutUploadDirectory() throws Exception {
        thrown.expect(ModuleInitializationException.class);
        thrown.expectMessage(JUnitMatchers
                .containsString("Failed to create temp dir at"));
        File badUploadDir = folder.newFile("fileNotDirectory");

        Server mockedServer = mock(BasicServer.class);
        when(mockedServer.getUploadDir()).thenReturn(badUploadDir);

        Map<String, String> params = new HashMap<String, String>();
        ManagementModule mm = new ManagementModule(params, null, null);
        ManagementModule spy = spy(mm);
        when(spy.getServer()).thenReturn(mockedServer);
        
        spy.initModule();
    }
}
