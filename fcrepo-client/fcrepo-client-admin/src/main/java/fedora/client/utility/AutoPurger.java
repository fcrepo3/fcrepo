/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.utility;

import java.io.IOException;

import java.net.MalformedURLException;

import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import fedora.client.FedoraClient;

import fedora.common.Constants;

import fedora.server.management.FedoraAPIM;

/**
 * @author Chris Wilper
 */
public class AutoPurger {

    private final FedoraAPIM m_apim;

    public AutoPurger(FedoraAPIM apim)
            throws MalformedURLException, ServiceException {
        m_apim = apim;
    }

    public void purge(String pid, String logMessage, boolean force)
            throws RemoteException, IOException {
        purge(m_apim, pid, logMessage, force);
    }

    public static void purge(FedoraAPIM skeleton,
                             String pid,
                             String logMessage,
                             boolean force) throws RemoteException, IOException {
        skeleton.purgeObject(pid, logMessage, force);
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
        System.err.println("  fedora-purge host:port user password pid protocol log [context]");
        System.err.println();
        System.err.println("Where:");
        System.err.println("  host     is the target repository hostname.");
        System.err.println("  port     is the target repository port number.");
        System.err.println("  user     is the id of the target repository user.");
        System.err.println("  password is the password of the target repository user.");
        System.err.println("  pid      is the id of the object to purge from the target repository.");
        System.err.println("  protocol is the protocol to communicate with repository (http or https)");
        System.err.println("  log      is a log message.");
        System.out.println("  context  (optional) is a different web application server context of Fedora (default is fedora)");

        System.err.println();
        System.err.println("Example:");
        System.err.println("fedora-purge myrepo.com:8443 jane janepw demo:5 https \"my message\"");
        System.err.println();
        System.err.println("  Purges the object whose pid is demo:5 from the");
        System.err.println("  target repository at myrepo.com:8443 using the secure https protocol (SSL)");
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
                String pid = args[3];
                String protocol = args[4];
                String logMessage = args[5];

                String context = Constants.FEDORA_DEFAULT_APP_CONTEXT;

                if (args.length == 7 && !args[6].equals("")){
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
                a.purge(pid, logMessage, false); // DEFAULT_FORCE_PURGE
            }
        } catch (Exception e) {
            AutoPurger.showUsage(e.getClass().getName()
                    + " - "
                    + (e.getMessage() == null ? "(no detail provided)" : e
                            .getMessage()));
        }
    }

}
