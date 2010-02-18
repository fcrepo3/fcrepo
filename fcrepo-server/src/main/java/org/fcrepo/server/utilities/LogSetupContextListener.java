/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.utilities;

import java.io.File;

import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.fcrepo.common.Constants;
import org.fcrepo.utilities.LogConfig;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Context listener for logging initialization.
 * <p>
 * This ensures that logging is initialized before any filters or servlets are
 * started.
 *
 * @author Chris Wilper
 */
public class LogSetupContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        // If fedora.home servlet context init param is defined, make sure
        // it is used for the value of Constants.FEDORA_HOME
        String contextFH = event.getServletContext().getInitParameter("fedora.home");
        if (contextFH != null && !contextFH.equals("")) {
            System.setProperty("servlet.fedora.home", contextFH);
        }

        // Configure logging from file
        System.setProperty("fedora.home", Constants.FEDORA_HOME);
        LogConfig.initFromFile(new File(new File(Constants.FEDORA_HOME),
                                        "server/config/logback.xml"));

        // Replace java.util.logging's default handlers with one that
        // redirects everything to SLF4J
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        for (int i = 0; i < handlers.length; i++) {
            rootLogger.removeHandler(handlers[i]);
        }
        SLF4JBridgeHandler.install();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // no cleanup needed for this listener
    }
}
