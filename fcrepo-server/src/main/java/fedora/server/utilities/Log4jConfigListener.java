/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.FileWatchdog;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Log4j Configuration Listener.
 * 
 * <p>An implementation of ServletContextListener. To use, add a listener 
 * element to web.xml that specifies this class.</p>
 * 
 * <p>Two configuration parameters are supported via context-param entries. 
 * {@value #CONFIG_LOCATION_PARAM} defines the location of the log4j 
 * configuration file. {@value #REFRESH_INTERVAL_PARAM} defines the interval,
 * in milliseconds, between config file refresh checks.</p>
 * 
 * <p>Limited variable substitutions are supported in the log4j configuration
 * file. System properties or environment variables enclosed with curly braces
 * and prefixed with a dollar sign (e.g. <code>${fedora.home}</code>) will be
 * automatically replaced.</p>
 * 
 * @author Edwin Shin
 * @version $Id$
 */
public class Log4jConfigListener
        implements ServletContextListener {
    
    /**
     * Parameter name for the location of the log4j configuration file.
     */
    public static final String CONFIG_LOCATION_PARAM = "log4j-configLocation";
    
    /**
     * Parameter name for the interval between config file refresh checks, 
     * in milliseconds.
     */
    public static final String REFRESH_INTERVAL_PARAM = "log4j-refreshInterval";
    
    public static final String FEDORA_HOME_PARAM = "fedora.home";

    /**
     * {@inheritDoc}
     */
    public void contextInitialized(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        String fedoraHome = servletContext.getInitParameter(FEDORA_HOME_PARAM);
        if (fedoraHome == null) {
            servletContext.log("init-param, " + FEDORA_HOME_PARAM + " was not set.");
        } else {
            System.setProperty(FEDORA_HOME_PARAM, fedoraHome);
        }
        initLogging(servletContext);
    }
    
    /**
     * {@inheritDoc}
     */
    public void contextDestroyed(ServletContextEvent event) {
        shutdownLogging(event.getServletContext());
    }

    public static void initLogging(ServletContext servletContext) {
        String location = servletContext.getInitParameter(CONFIG_LOCATION_PARAM);        
        if (location == null || location.length() == 0) {
            servletContext.log("init-param ," + CONFIG_LOCATION_PARAM + 
                               " not set, skipping log4j configuration.");
            return;
        }
        
        String intervalString = servletContext.getInitParameter(REFRESH_INTERVAL_PARAM);
        long refreshInterval = FileWatchdog.DEFAULT_DELAY;
        if (intervalString != null) {
            refreshInterval = Long.parseLong(intervalString);
        }
        try {
            initLogging(location, refreshInterval);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Invalid Log4jConfigListener parameter: " + location);
        }
    }

    public static void shutdownLogging(ServletContext servletContext) {
        servletContext.log("Shutting down log4j...");
        LogManager.shutdown();
    }
    
    private static void initLogging(String location, long refreshInterval) throws FileNotFoundException {
        assert (location != null && location.length() > 0);
        String resolvedLocation = dereferenceSystemProperties(location);    
        File file = new File(resolvedLocation);
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath() + " does not exist.");
        }
        if (location.toLowerCase().endsWith(".xml")) {
            DOMConfigurator.configureAndWatch(file.getAbsolutePath(), refreshInterval);
        } else {
            PropertyConfigurator.configureAndWatch(file.getAbsolutePath(), refreshInterval);
        }
    }
    
    /**
     * Replaces System Properties or environment variables surrounded by curly 
     * braces and prefixed by a dollar sign {e.g. <code>${fedora.home}</code>} 
     * with their values.
     * 
     * @param s the input string to perform substitutions on.
     * @return the string with the variables replaced.
     */
    public static String dereferenceSystemProperties(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        String prefix = "${";
        String suffix = "}";
        // e.g., match ${fedora.home}
        Pattern pattern = Pattern.compile("(\\$\\{.*?\\})");
        Matcher matcher = pattern.matcher(s);
        
        while(matcher.find()) {
            String match = matcher.group();
            String propertyName = match.substring(prefix.length(), match.length() - suffix.length());
            String propertyValue = System.getProperty(propertyName);
            if (propertyValue == null) {
                propertyValue = System.getenv(propertyName);
            }
            if (propertyValue != null) {
                s = s.replace(match, propertyValue);
            } else {
                System.out.println("Could not match: " + propertyName + 
                                   " to a System Property or environment variable.");
            }
        }
        return s;
    }
}
