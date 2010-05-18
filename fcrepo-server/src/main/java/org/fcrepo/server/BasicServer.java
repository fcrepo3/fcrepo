/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Element;

import org.fcrepo.common.Constants;
import org.fcrepo.common.Models;
import org.fcrepo.common.PID;
import org.fcrepo.common.rdf.RDFName;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.errors.ServerInitializationException;
import org.fcrepo.server.storage.DOManager;
import org.fcrepo.server.storage.DOWriter;
import org.fcrepo.server.utilities.status.ServerState;
import org.fcrepo.server.utilities.status.ServerStatusFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Fedora Server.
 *
 * @author Chris Wilper
 */
public class BasicServer
        extends Server {

    private static final Logger logger =
            LoggerFactory.getLogger(BasicServer.class);

    public BasicServer(Element rootElement, File fedoraHomeDir)
            throws ServerInitializationException, ModuleInitializationException {
        super(rootElement, fedoraHomeDir);
    }

    @Override
    public void initServer() throws ServerInitializationException {

        String fedoraServerHost = null;
        String fedoraServerPort = null;

        // fedoraServerHost (required)
        fedoraServerHost = getParameter("fedoraServerHost");
        if (fedoraServerHost == null) {
            throw new ServerInitializationException("Parameter fedoraServerHost "
                    + "not given, but it's required.");
        }
        // fedoraServerPort (required)
        fedoraServerPort = getParameter("fedoraServerPort");
        if (fedoraServerPort == null) {
            throw new ServerInitializationException("Parameter fedoraServerPort "
                    + "not given, but it's required.");
        }

        logger.info("Fedora Version: " + Server.VERSION);
        logger.info("Fedora Build Date: " + Server.BUILD_DATE);
        logger.info("Fedora Build Number: " + Server.BUILD_NUMBER);

        ServerStatusFile status = getStatusFile();
        try {
            status.append(ServerState.STARTING,
                    "Fedora Version: " + Server.VERSION);
            status.append(ServerState.STARTING,
                    "Fedora Build Date: " + Server.BUILD_DATE);
            status.append(ServerState.STARTING,
                    "Fedora Build Number: " + Server.BUILD_NUMBER);
            status.append(ServerState.STARTING, "Server Host Name: "
                    + fedoraServerHost);
            status.append(ServerState.STARTING, "Server Port: "
                    + fedoraServerPort);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServerInitializationException("Unable to write to status file: "
                    + e.getMessage());
        }
    }

    /**
     * Gets the names of the roles that are required to be fulfilled by modules
     * specified in this server's configuration file.
     *
     * @return String[] The roles.
     */
    @Override
    public String[] getRequiredModuleRoles() {
        return new String[] {DOManager.class.getName()};
    }

    @Override
    public void postInitServer() throws ServerInitializationException {
        // check for system objects and pre-ingest them if necessary
        DOManager doManager = (DOManager) getModule(DOManager.class.getName());
        try {
            boolean firstRun = checkFirstRun();
            preIngestIfNeeded(firstRun, doManager, Models.CONTENT_MODEL_3_0);
            preIngestIfNeeded(firstRun, doManager, Models.FEDORA_OBJECT_3_0);
            preIngestIfNeeded(firstRun, doManager, Models.SERVICE_DEFINITION_3_0);
            preIngestIfNeeded(firstRun, doManager, Models.SERVICE_DEPLOYMENT_3_0);
        } catch (Exception e) {
            throw new ServerInitializationException("Failed to ingest "
                                                    + "system object(s)", e);
        }
    }

    private boolean checkFirstRun() throws IOException {
        File hasStarted = new File(FEDORA_HOME, "server/fedora-internal-use/has-started.txt");
        if (hasStarted.exists()) {
            return false;
        } else {
            hasStarted.createNewFile();
            return true;
        }
    }

    /**
     * Ingests the given system object if it doesn't exist, OR if it
     * exists, but this instance of Fedora has never been started.
     * This ensures that, upon upgrade, the old system object
     * is replaced with the new one.
     */
    private void preIngestIfNeeded(boolean firstRun,
                                   DOManager doManager,
                                   RDFName objectName) throws Exception {
        PID pid = new PID(objectName.uri.substring("info:fedora/".length()));
        boolean exists = doManager.objectExists(pid.toString());
        if (exists && firstRun) {
            logger.info("Purging old system object: " + pid.toString());
            Context context = ReadOnlyContext.getContext(null,
                                                         null,
                                                         null,
                                                         false);
            DOWriter w = doManager.getWriter(USE_DEFINITIVE_STORE,
                                             context,
                                             pid.toString());
            w.remove();
            try {
                w.commit("Purged by Fedora at startup (to be re-ingested)");
                exists = false;
            } finally {
                doManager.releaseWriter(w);
            }
        }
        if (!exists) {
            logger.info("Ingesting new system object: " + pid.toString());
            InputStream xml = getStream("org/fcrepo/server/resources/"
                                        + pid.toFilename() + ".xml");
            Context context = ReadOnlyContext.getContext(null,
                                                         null,
                                                         null,
                                                         false);
            DOWriter w = doManager.getIngestWriter(USE_DEFINITIVE_STORE,
                                                   context,
                                                   xml,
                                                   Constants.FOXML1_1.uri,
                                                   "UTF-8",
                                                   null);
            try {
                w.commit("Pre-ingested by Fedora at startup");
            } finally {
                doManager.releaseWriter(w);
            }
        }
    }

    private InputStream getStream(String path) throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(
                    path);
        if (stream == null) {
            throw new IOException("Classloader cannot find resource: " + path);
        } else {
            return stream;
        }
    }


}
