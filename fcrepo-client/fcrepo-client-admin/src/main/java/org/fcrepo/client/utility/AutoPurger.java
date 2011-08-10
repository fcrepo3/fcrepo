/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.client.utility;

import java.io.File;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.fcrepo.client.FedoraClient;
import org.fcrepo.client.utility.validate.process.PidfileIterator;

import org.fcrepo.common.Constants;

import org.fcrepo.server.management.FedoraAPIMMTOM;


/**
 * @author Chris Wilper
 */
public class AutoPurger {

    private final FedoraAPIMMTOM m_apim;

    public AutoPurger(FedoraAPIMMTOM apim)
            throws MalformedURLException, ServiceException {
        m_apim = apim;
    }

    public void purge(String pid, String logMessage)
            throws RemoteException, IOException {
        purge(m_apim, pid, logMessage);
    }

    public void purgeFromList(File pFile, String logMessage)
            throws RemoteException, IOException {
        purgeFromList(m_apim, pFile, logMessage);
    }

    public static void purgeFromList(FedoraAPIMMTOM skeleton,
                               File pFile,
                               String logMessage) {
        PidfileIterator p_iter = new PidfileIterator(pFile);
        int objs_purged = 0;

        while ( p_iter.hasNext() ) {
            String pid = p_iter.next();
            try {
                skeleton.purgeObject(pid, logMessage, false);
                System.out.println("'" + pid +"' purged");
                objs_purged++;
            } catch (Exception e) {
                System.err.println("ERROR purging '" + pid + "' : " +
                                   (e.getMessage() == null ? "(no detail provided)" : e.getMessage()));
            }
        }
        System.out.println(objs_purged + " objects successfully purged.");
    }

    public static void purge(FedoraAPIMMTOM skeleton,
                             String pid,
                             String logMessage) throws RemoteException, IOException {
        skeleton.purgeObject(pid, logMessage, false);
    }

    /**
     * Print error message and show usage for command-line interface.
     */
    public static void showUsage(String msg) {
        System.err.println("Command: fedora-purge");
        System.err.println();
        System.err.println("Summary: Purges an object from the Fedora repository.");
        System.err.println();
        System.err.println("Syntax:");
        System.err.println("  fedora-purge host:port user password pid|fileURI protocol log [context]");
        System.err.println();
        System.err.println("Where:");
        System.err.println("  host          is the target repository hostname.");
        System.err.println("  port          is the target repository port number.");
        System.err.println("  user          is the id of the target repository user.");
        System.err.println("  password      is the password of the target repository user.");
        System.err.println("  pid|fileURI   is the id of the object to purge from the target repository");
        System.err.println("                OR the file URL for a file containing pids to purge, one pid per line");
        System.err.println("  protocol      is the protocol to communicate with repository (http or https)");
        System.err.println("  log           is a log message.");
        System.out.println(
                "  context  (optional) is a different web application server context of Fedora (default is fedora)");

        System.err.println();
        System.err.println("Examples:");
        System.err.println("fedora-purge myrepo.com:8443 jane janepw demo:5 https \"my message\"");
        System.err.println();
        System.err.println("  Purges the object whose pid is demo:5 from the");
        System.err.println("  target repository at myrepo.com:8443 using the secure https protocol (SSL)");
        System.err.println();
        System.err.println("fedora-purge myrepo.com:8443 jane janepw file:///some/dir/purge-pids.txt http \"my message\"");
        System.err.println();
        System.err.println("  Purges the objects whose pids are listed in the file /some/dir/purge-pids.txt from the");
        System.err.println("  target repository at myrepo.com:8443 using the non-secure http protocol");
        System.err.println();
        System.err.println("ERROR  : " + msg);
        System.exit(1);
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        try {
            if (args.length < 6 || args.length > 7) {
                AutoPurger
                        .showUsage("You must provide six or seven arguments.");
            } else {
                String[] hp = args[0].split(":");
                String hostName = hp[0];
                int portNum = Integer.parseInt(hp[1]);
                String third_param = args[3];
                String pid = null;
                URL fileURL = null;
                if (third_param.startsWith("file:/")) {
                    fileURL = new URL(third_param);
                } else {
                    pid = third_param;
                }
                String protocol = args[4];
                String logMessage = args[5];

                String context = Constants.FEDORA_DEFAULT_APP_CONTEXT;

                if (args.length == 7 && !args[6].equals("")) {
                    context = args[6];
                }
                // ******************************************
                // NEW: use new client utility class
                String baseURL =
                        protocol + "://" + hostName + ":" + portNum + "/"
                        + context;
                FedoraClient fc = new FedoraClient(baseURL, args[1], args[2]);
                AutoPurger a = new AutoPurger(fc.getAPIM());
                //*******************************************

                /* Single PID:  just purge it */
                if (pid != null) {
                    a.purge(pid, logMessage);
                /* read PIDs from a file */
                } else {
                    File pFile = new File(fileURL.toURI()).getCanonicalFile();
                    a.purgeFromList(pFile, logMessage);
                }
            }
        } catch (Exception e) {
            AutoPurger.showUsage(e.getClass().getName()
                                 + " - "
                                 + (e.getMessage() == null ? "(no detail provided)" : e
                    .getMessage()));
        }
    }
}
