/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.utility.ingest;

import java.io.File;
import java.io.PrintStream;

import fedora.common.Constants;

import fedora.server.utilities.StreamUtility;

/**
 * Methods to create and update a log of ingest activity.
 * 
 * @author Chris Wilper
 */
public class IngestLogger {

    public static File newLogFile(String logRootName) {

        String fileName =
                logRootName + "-" + System.currentTimeMillis() + ".xml";
        File outFile;
        String fedoraHome = Constants.FEDORA_HOME;
        if (fedoraHome == null) {
            // to current dir
            outFile = new File(fileName);
        } else {
            // to client/log
            File logDir =
                    new File(new File(new File(fedoraHome), "client"), "logs");
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            outFile = new File(logDir, fileName);
        }
        return outFile;
    }

    public static void openLog(PrintStream log, String rootName)
            throws Exception {
        log.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        log.println("<" + rootName + ">");
    }

    public static void logFromFile(PrintStream log, File f, String pid)
            throws Exception {
        log.println("  <ingested file=\"" + f.getPath() + "\" targetPID=\""
                + pid + "\" />");
    }

    public static void logFailedFromFile(PrintStream log, File f, Exception e)
            throws Exception {
        String message = e.getMessage();
        if (message == null) {
            message = e.getClass().getName();
        }
        log.println("  <failed file=\"" + f.getPath() + "\">");
        log.println("    " + StreamUtility.enc(message));
        log.println("  </failed>");
    }

    public static void logFromRepos(PrintStream log,
                                    String sourcePID,
                                    String targetPID) {
        log.println("  <ingested sourcePID=\"" + sourcePID + "\" targetPID=\""
                + targetPID + "\"/>");
    }

    public static void logFailedFromRepos(PrintStream log,
                                          String sourcePID,
                                          Exception e) {
        String message = e.getMessage();
        if (message == null) {
            message = e.getClass().getName();
        }
        log.println("  <failed sourcePID=\"" + sourcePID + "\">");
        log.println("    " + StreamUtility.enc(message));
        log.println("  </failed>");
    }

    public static void closeLog(PrintStream log, String rootName)
            throws Exception {
        log.println("</" + rootName + ">");
        log.close();
    }
}