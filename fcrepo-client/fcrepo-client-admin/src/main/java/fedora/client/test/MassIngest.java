/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;

import fedora.client.FedoraClient;
import fedora.client.utility.ingest.AutoIngestor;

import fedora.common.Constants;

import fedora.oai.sample.RandomDCMetadataFactory;

import fedora.server.access.FedoraAPIA;
import fedora.server.management.FedoraAPIM;

/**
 * @author Chris Wilper
 * @version $Id$
 */
public class MassIngest {

    public static FedoraAPIA APIA = null;

    public static FedoraAPIM APIM = null;

    public MassIngest(AutoIngestor ingestor,
                      File templateFile,
                      File dictFile,
                      String format,
                      int numTimes)
            throws Exception {
        // load the template file into two parts... with splitter=##SPLITTER##
        BufferedReader in = new BufferedReader(new FileReader(templateFile));
        String nextLine = "";
        StringBuffer startBuffer = new StringBuffer();
        StringBuffer endBuffer = new StringBuffer();
        boolean seenSplitter = false;
        while (nextLine != null) {
            nextLine = in.readLine();
            if (nextLine != null) {
                if (!seenSplitter) {
                    if (nextLine.startsWith("##SPLITTER##")) {
                        seenSplitter = true;
                    } else {
                        startBuffer.append(nextLine + "\n");
                    }
                } else {
                    endBuffer.append(nextLine + "\n");
                }
            }
        }
        in.close();
        String start = startBuffer.toString();
        String end = endBuffer.toString();
        RandomDCMetadataFactory dcFactory =
                new RandomDCMetadataFactory(dictFile);
        for (int i = 0; i < numTimes; i++) {
            String xml = start + dcFactory.get(2, 13) + end;
            String pid =
                    ingestor
                            .ingestAndCommit(new ByteArrayInputStream(xml
                                                     .getBytes("UTF-8")),
                                             format,
                                             "part of massingest of "
                                                     + numTimes
                                                     + " auto-generated objects.");
            int t = i + 1;
            System.out.println(pid + " " + t + "/" + numTimes);
        }

    }

    public static void showUsage(String message) {
        System.out.println("ERROR: " + message);
        System.out
                .println("Usage: MassIngest host port username password templateFile dictionaryFile format numTimes protocol [context]");
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        try {
            if (args.length < 9 || args.length > 10) {
                MassIngest.showUsage("You must provide nine or ten arguments.");
            } else {
                String hostName = args[0];
                int portNum = Integer.parseInt(args[1]);
                String username = args[2];
                String password = args[3];
                File dictFile = new File(args[5]);
                String format = args[6];
                // third arg==file... must exist
                File f = new File(args[4]);
                String protocol = args[8];
                String context = Constants.FEDORA_DEFAULT_APP_CONTEXT;
                if (args.length == 10 && !args[9].equals("")){
                    context = args[9];
                }


                // ******************************************
                // NEW: use new client utility class
                String baseURL =
                        protocol + "://" + hostName + ":" + portNum + "/"
                                + context;
                FedoraClient fc = new FedoraClient(baseURL, username, password);
                APIA = fc.getAPIA();
                APIM = fc.getAPIM();
                //*******************************************
                AutoIngestor autoIngestor = new AutoIngestor(APIA, APIM);

                new MassIngest(autoIngestor, f, dictFile, format, Integer
                        .parseInt(args[7]));
            }
        } catch (Exception e) {
            MassIngest.showUsage(e.getClass().getName()
                    + " - "
                    + (e.getMessage() == null ? "(no detail provided)" : e
                            .getMessage()));
        }
    }

}
