package org.fcrepo.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.fcrepo.common.FaultException;

/**
 * Log configuration utility methods.
 * <p>
 * This class provides convenience methods for configuring Logback when
 * the default configuration files (logback.xml or logback-test.xml) are not
 * expected to be found in the classpath of the running application.
 * <p>
 * Note:
 * <ul>
 *   <li> This class intentionally has no compile or run-time dependencies on
 *        Logback.  If Logback is not in the classpath, calling these methods
 *        will have no effect on the application's logging behavior.</li>
 *   <li> If the <code>logback.configurationFile</code> system property is
 *        already defined, calling these methods will have no effect.</li>
 * </ul>
 *
 * @author Chris Wilper
 */
public abstract class LogConfig {

    private static final String CONFIG_FILE_PROPERTY = "logback.configurationFile";

    /**
     * Initializes logging with the given file.
     *
     * @param configFile the logback.xml file to use.
     */
    public static void initFromFile(File configFile) {
        if (!alreadyConfigured()) {
            System.setProperty(CONFIG_FILE_PROPERTY, configFile.getPath());
        }
    }

    /**
     * Initializes logging to the console (standard error) with minimal logging.
     * <p>
     * For all categories, only WARN and ERROR messages will be printed,
     * and the messages will be of the form "LEVEL time (classname) message"
     */
    public static void initMinimal() {
        if (!alreadyConfigured()) {
            initFromFile(createTempConfigFile("WARN"));
        }
    }

    private static boolean alreadyConfigured() {
        return System.getProperty(CONFIG_FILE_PROPERTY) != null;
    }

    private static File createTempConfigFile(String level) {
        PrintWriter writer = null;
        try {
            File tempFile = File.createTempFile("fedora-logback-minimal", null);
            tempFile.deleteOnExit();
            writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(tempFile)));
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<configuration>");
            writer.println("  <appender name=\"STDERR\" class=\"ch.qos.logback.core.ConsoleAppender\">");
            writer.println("    <Target>System.err</Target>");
            writer.println("    <layout class=\"ch.qos.logback.classic.PatternLayout\">");
            writer.println("      <Pattern>%p %d{HH:mm:ss.SSS} (%c{0}\\\\) %m%n</Pattern>");
            writer.println("    </layout>");
            writer.println("  </appender>");
            writer.println("  <root additivity=\"false\" level=\"" + level + "\">");
            writer.println("    <appender-ref ref=\"STDERR\"/>");
            writer.println("  </root>");
            writer.println("</configuration>");
            return tempFile;
        } catch (IOException e) {
            throw new FaultException("Error creating temporary log config file", e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

}
