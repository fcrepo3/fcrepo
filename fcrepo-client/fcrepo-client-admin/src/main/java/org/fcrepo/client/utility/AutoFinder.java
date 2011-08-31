/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.client.utility;

import java.net.MalformedURLException;

import java.rmi.RemoteException;

import java.util.Arrays;

import java.math.BigInteger;

import javax.xml.rpc.ServiceException;

import org.fcrepo.client.FedoraClient;

import org.fcrepo.common.Constants;

import org.fcrepo.server.access.FedoraAPIAMTOM;
import org.fcrepo.server.types.mtom.gen.ArrayOfString;
import org.fcrepo.server.types.mtom.gen.FieldSearchQuery;
import org.fcrepo.server.types.mtom.gen.FieldSearchResult;
import org.fcrepo.server.types.mtom.gen.ObjectFields;




/**
 * @author Chris Wilper
 * @version $Id$
 */
public class AutoFinder {

    private final FedoraAPIAMTOM m_apia;

    public AutoFinder(FedoraAPIAMTOM apia)
            throws MalformedURLException, ServiceException {
        m_apia = apia;
    }

    public FieldSearchResult findObjects(ArrayOfString resultFields,
                                         int maxResults,
                                         FieldSearchQuery query)
            throws RemoteException {
        return findObjects(m_apia, resultFields, maxResults, query);
    }

    public FieldSearchResult resumeFindObjects(String sessionToken)
            throws RemoteException {
        return resumeFindObjects(m_apia, sessionToken);
    }

    public static FieldSearchResult findObjects(FedoraAPIAMTOM skeleton,
                                                ArrayOfString resultFields,
                                                int maxResults,
                                                FieldSearchQuery query)
            throws RemoteException {
        return skeleton.findObjects(resultFields, new BigInteger(""
                + maxResults), query);
    }

    public static FieldSearchResult resumeFindObjects(FedoraAPIAMTOM skeleton,
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
            org.fcrepo.server.types.mtom.gen.ObjectFactory factory =
                new org.fcrepo.server.types.mtom.gen.ObjectFactory();
            query.setTerms(factory.createFieldSearchQueryTerms(phrase));
            String[] arrayS = fields.split(" ");
            ArrayOfString aux = new ArrayOfString();
            aux.getItem().addAll(Arrays.asList(arrayS));
            FieldSearchResult result =
                    finder.findObjects(aux, 20, query);
            int matchNum = 0;
            while (result != null) {
                for (int i = 0; i < result.getResultList().getObjectFields().size(); i++) {
                    ObjectFields o = result.getResultList().getObjectFields().get(i);
                    matchNum++;
                    System.out.println("#" + matchNum);
                    AutoFinder.printValue("pid              ", o.getPid() != null ? o.getPid().getValue() : null);
                    AutoFinder.printValue("state            ", o.getState() != null ? o.getState().getValue() : null);
                    AutoFinder.printValue("ownerId          ", o.getOwnerId() != null ? o.getOwnerId().getValue() : null);
                    AutoFinder.printValue("cDate            ", o.getCDate() != null ? o.getCDate().getValue() : null);
                    AutoFinder.printValue("mDate            ", o.getMDate() != null ? o.getMDate().getValue() : null);
                    AutoFinder.printValue("dcmDate          ", o.getDcmDate() != null ? o.getDcmDate().getValue() : null);
                    AutoFinder.printValue("title            ", o.getTitle() != null ? o.getTitle().toString() : null);
                    AutoFinder.printValue("creator          ", o.getCreator() != null ? o.getCreator().toString() : null);
                    AutoFinder.printValue("subject          ", o.getSubject() != null ? o.getSubject().toString() : null);
                    AutoFinder.printValue("description      ", o.getDescription() != null ? o.getDescription().toString() : null);
                    AutoFinder.printValue("publisher        ", o.getPublisher() != null ? o.getPublisher().toString() : null);
                    AutoFinder.printValue("contributor      ", o.getContributor() != null ? o.getContributor().toString() : null);
                    AutoFinder.printValue("date             ", o.getDate() != null ? o.getDate().toString() : null);
                    AutoFinder.printValue("type             ", o.getType() != null ? o.getType().toString() : null);
                    AutoFinder.printValue("format           ", o.getFormat() != null ? o.getFormat().toString() : null);
                    AutoFinder.printValue("identifier       ", o.getIdentifier() != null ? o.getIdentifier().toString() : null);
                    AutoFinder.printValue("source           ", o.getSource() != null ? o.getSource().toString() : null);
                    AutoFinder.printValue("language         ", o.getLanguage() != null ? o.getLanguage().toString() : null);
                    AutoFinder.printValue("relation         ", o.getRelation() != null ? o.getRelation().toString() : null);
                    AutoFinder.printValue("coverage         ", o.getCoverage() != null ? o.getCoverage().toString() : null);
                    AutoFinder.printValue("rights           ", o.getRights() != null ? o.getRights().toString() : null);
                    System.out.println("");
                }
                if (result.getListSession() != null && result.getListSession().getValue() != null) {
                    result = finder.resumeFindObjects(result.getListSession().getValue().getToken());
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
