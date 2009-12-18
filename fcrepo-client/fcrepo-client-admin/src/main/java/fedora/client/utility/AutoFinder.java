/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.utility;

import java.net.MalformedURLException;

import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.apache.axis.types.NonNegativeInteger;

import fedora.client.FedoraClient;

import fedora.common.Constants;

import fedora.server.access.FedoraAPIA;
import fedora.server.types.gen.FieldSearchQuery;
import fedora.server.types.gen.FieldSearchResult;
import fedora.server.types.gen.ListSession;
import fedora.server.types.gen.ObjectFields;

/**
 * @author Chris Wilper
 * @version $Id$
 */
public class AutoFinder {

    private final FedoraAPIA m_apia;

    public AutoFinder(FedoraAPIA apia)
            throws MalformedURLException, ServiceException {
        m_apia = apia;
    }

    public FieldSearchResult findObjects(String[] resultFields,
                                         int maxResults,
                                         FieldSearchQuery query)
            throws RemoteException {
        return findObjects(m_apia, resultFields, maxResults, query);
    }

    public FieldSearchResult resumeFindObjects(String sessionToken)
            throws RemoteException {
        return resumeFindObjects(m_apia, sessionToken);
    }

    public static FieldSearchResult findObjects(FedoraAPIA skeleton,
                                                String[] resultFields,
                                                int maxResults,
                                                FieldSearchQuery query)
            throws RemoteException {
        return skeleton.findObjects(resultFields, new NonNegativeInteger(""
                + maxResults), query);
    }

    public static FieldSearchResult resumeFindObjects(FedoraAPIA skeleton,
                                                      String sessionToken)
            throws RemoteException {
        return skeleton.resumeFindObjects(sessionToken);
    }

    public static void showUsage(String message) {
        System.err.println(message);
        System.err.println("Usage: fedora-find host port user password fields phrase protocol [context]");
        System.err.println("");
        System.err.println("    host     - The Fedora server host or ip address.");
        System.err.println("    port     - The Fedora server port.");
        System.err.println("    user     - The username of a repository user.");
        System.err.println("    password - The password of a repository user.");
        System.err.println("    fields   - Space-delimited list of fields.");
        System.err.println("    phrase   - Phrase to search for in any field (with ? and * wildcards)");
        System.err.println("    protocol - The protocol to communication with the Fedora server (http|https)");
        System.err.println("    context  - Optional, the name of the context the Fedora server is deployed in (default is fedora)");
    }

    public static void printValue(String name, String value) {
        if (value != null) {
            System.out.println("   " + name + "  " + value);
        }
    }

    public static void printValue(String name, String[] value) {
        if (value != null) {
            for (String element : value) {
                AutoFinder.printValue(name, element);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        if (args.length < 7 || args.length > 8) {
            AutoFinder.showUsage("Seven or eight arguments required.");
            System.exit(0);
        }

        String context = Constants.FEDORA_DEFAULT_APP_CONTEXT;

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String user = args[2];
        String pass = args[3];
        String fields = args[4];
        String phrase = args[5];
        String protocol = args[6];

        if (args.length == 8 && !args[7].equals("")){
            context = args[7];
        }

        try {
            // FIXME:  Get around hardcoding the path in the baseURL
            String baseURL = protocol + "://" + host + ":" + port + "/" + context;
            FedoraClient fc = new FedoraClient(baseURL, user, pass);
            AutoFinder finder = new AutoFinder(fc.getAPIA());

            FieldSearchQuery query = new FieldSearchQuery();
            query.setTerms(phrase);
            FieldSearchResult result =
                    finder.findObjects(fields.split(" "), 20, query);
            int matchNum = 0;
            while (result != null) {
                for (int i = 0; i < result.getResultList().length; i++) {
                    ObjectFields o = result.getResultList()[i];
                    matchNum++;
                    System.out.println("#" + matchNum);
                    AutoFinder.printValue("pid              ", o.getPid());
                    AutoFinder.printValue("state            ", o.getState());
                    AutoFinder.printValue("ownerId          ", o.getOwnerId());
                    AutoFinder.printValue("cDate            ", o.getCDate());
                    AutoFinder.printValue("mDate            ", o.getMDate());
                    AutoFinder.printValue("dcmDate          ", o.getDcmDate());
                    AutoFinder.printValue("title            ", o.getTitle());
                    AutoFinder.printValue("creator          ", o.getCreator());
                    AutoFinder.printValue("subject          ", o.getSubject());
                    AutoFinder.printValue("description      ", o.getDescription());
                    AutoFinder.printValue("publisher        ", o.getPublisher());
                    AutoFinder.printValue("contributor      ", o.getContributor());
                    AutoFinder.printValue("date             ", o.getDate());
                    AutoFinder.printValue("type             ", o.getType());
                    AutoFinder.printValue("format           ", o.getFormat());
                    AutoFinder.printValue("identifier       ", o.getIdentifier());
                    AutoFinder.printValue("source           ", o.getSource());
                    AutoFinder.printValue("language         ", o.getLanguage());
                    AutoFinder.printValue("relation         ", o.getRelation());
                    AutoFinder.printValue("coverage         ", o.getCoverage());
                    AutoFinder.printValue("rights           ", o.getRights());
                    System.out.println("");
                }
                ListSession sess = result.getListSession();
                if (sess != null) {
                    result = finder.resumeFindObjects(sess.getToken());
                } else {
                    result = null;
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getClass().getName()
                    + (e.getMessage() == null ? "" : ": " + e.getMessage()));
        }
    }

}
