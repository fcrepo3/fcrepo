/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities;

import java.net.URL;
import java.net.URLConnection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Cleanup listener for general resource handling
 * 
 * @version $Id$
 */
public class CleanupContextListener implements ServletContextListener {

    /*
     * Initialize any resources required by the application
     * 
     * @see
     * javax.servlet.ServletContextListener#contextInitialized(javax.servlet
     * .ServletContextEvent)
     */
    public void contextInitialized(ServletContextEvent event) {
        // On Windows, URL caches can cause problems, particularly with
        // undeployment
        // So, here we attempt to disable them if we detect that we are running
        // on Windows
        try {
            String osName = System.getProperty("os.name");

            if (osName != null && osName.toLowerCase().contains("windows")) {
                URL url = new URL("http://localhost/");
                URLConnection urlConn = url.openConnection();
                urlConn.setDefaultUseCaches(false);
            }
        } catch (Throwable t) {
            // Any errors thrown in disabling the caches aren't significant to
            // the normal execution of the application, so we ignore them
        }
    }

    /**
     * Clean up resources used by the application when stopped
     * 
     * @seejavax.servlet.ServletContextListener#contextDestroyed(javax.servlet
     * .ServletContextEvent)
     */
    public void contextDestroyed(ServletContextEvent event) {
        // deregister database drivers
        try {
            for (Enumeration<Driver> e = DriverManager.getDrivers(); e
                    .hasMoreElements();) {
                Driver driver = e.nextElement();
                if (driver.getClass().getClassLoader() == getClass()
                        .getClassLoader()) {
                    DriverManager.deregisterDriver(driver);
                }
            }
        } catch (Throwable e) {
            // Any errors thrown in clearing the caches aren't significant to
            // the normal execution of the application, so we ignore them
        }
        // Clean the axis method cache, see FCREPO-496
        org.apache.axis.utils.cache.MethodCache.getInstance().clearCache();
    }
}
