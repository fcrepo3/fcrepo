/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server;

import java.io.File;

import org.w3c.dom.Element;

import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ServerInitializationException;

/**
 * @author eddie
 */
public class DummyServer
        extends Server {

    public static String CONFIG_FILE = "test.fcfg";

    /**
     * @param rootConfigElement
     * @param homeDir
     * @throws ServerInitializationException
     * @throws ModuleInitializationException
     */
    protected DummyServer(Element rootConfigElement, File homeDir)
            throws ServerInitializationException, ModuleInitializationException {
        super(rootConfigElement, homeDir);
    }

}
