/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.utilities;

import static java.lang.System.setProperty;
import static java.util.logging.LogManager.getLogManager;
import static org.fcrepo.common.Constants.FEDORA_HOME;
import static org.slf4j.LoggerFactory.getILoggerFactory;
import static org.slf4j.bridge.SLF4JBridgeHandler.install;

import java.io.File;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Context listener for logging initialization.
 * <p>
 * This ensures that logging is initialized before any filters or servlets are
 * started.
 *
 * @author Chris Wilper
 */
public class LogSetupContextListener implements ServletContextListener {

    final private static Logger logger = LoggerFactory
            .getLogger(LogSetupContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent event) {
        // If fedora.home servlet context init param is defined, make sure
        // it is used for the value of Constants.FEDORA_HOME
        String contextFH =
                event.getServletContext().getInitParameter("fedora.home");
        if (contextFH != null && !contextFH.equals("")) {
            setProperty("servlet.fedora.home", contextFH);
        }

        // Configure logging from file
        setProperty("fedora.home", FEDORA_HOME);
        LoggerContext lc = (LoggerContext) getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);
        lc.reset();
        final File logbackConfigFile =
                new File(new File(FEDORA_HOME), "server/config/logback.xml");
        try {
            configurator.doConfigure(logbackConfigFile);
        } catch (JoranException e) {
            throw new IllegalStateException(
                    "Could not configure Logback! Tried using configuration file: " +
                            logbackConfigFile.getAbsolutePath(), e);
        }
        logger.info(
                "Configured Fedora Logback system with config file from: {}",
                logbackConfigFile.getAbsolutePath());
        /*
         * LogConfig.initFromFile(new File(new File(Constants.FEDORA_HOME),
         * "server/config/logback.xml"));
         */

        // Replace java.util.logging's default handlers with one that
        // redirects everything to SLF4J
        java.util.logging.Logger rootLogger = getLogManager().getLogger("");
        java.util.logging.Handler[] handlers = rootLogger.getHandlers();
        for (int i = 0; i < handlers.length; i++) {
            rootLogger.removeHandler(handlers[i]);
        }
        install();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // no cleanup needed for this listener
    }
}
