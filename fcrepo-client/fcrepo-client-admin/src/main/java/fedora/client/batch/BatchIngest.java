/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.batch;

import java.io.File;
import java.io.FileInputStream;

import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import fedora.client.FedoraClient;
import fedora.client.utility.ingest.AutoIngestor;

import fedora.common.Constants;

import fedora.server.access.FedoraAPIA;
import fedora.server.management.FedoraAPIM;

/**
 * Batch Ingest.
 *
 * @author Bill Niebel
 */
class BatchIngest
        implements Constants {

    String protocol = null;

    String host = null;

    int port = 0;

    String username;

    String password;

    String context = "fedora";

    FedoraAPIA APIA;

    FedoraAPIM APIM;

    //set by arguments to constructor
    String objectsPath = null;

    String pidsPath = null;

    String pidsFormat = null;

    String objectFormat = null;

    BatchIngest(Properties optValues)
            throws Exception {
        objectsPath = optValues.getProperty(BatchTool.OBJECTSPATH);
        pidsPath = optValues.getProperty(BatchTool.PIDSPATH);
        pidsFormat = optValues.getProperty(BatchTool.PIDSFORMAT);
        protocol = optValues.getProperty(BatchTool.SERVERPROTOCOL);
        host = optValues.getProperty(BatchTool.SERVERFQDN);
        String serverPortAsString = optValues.getProperty(BatchTool.SERVERPORT);
        username = optValues.getProperty(BatchTool.USERNAME);
        password = optValues.getProperty(BatchTool.PASSWORD);
        context = optValues.getProperty(BatchTool.CONTEXT);
        objectFormat = optValues.getProperty(BatchTool.OBJECTFORMAT);
        if (!BatchTool.argOK(objectsPath)) {
            System.err.println("objectsPath required");
            throw new Exception();
        }
        if (!BatchTool.argOK(pidsPath)) {
            System.err.println("pidsPath required");
            throw new Exception();
        }
        if (!BatchTool.argOK(pidsFormat)) {
            System.err.println("pids-format required");
            throw new Exception();
        }
        if (!BatchTool.argOK(host)) {
            System.err.println("server-fqdn required");
            throw new Exception();
        }
        if (!BatchTool.argOK(serverPortAsString)) {
            System.err.println("server-port required");
            throw new Exception();
        } else {
            port = Integer.parseInt(serverPortAsString);
        }
        if (!BatchTool.argOK(username)) {
            System.err.println("username required");
            throw new Exception();
        }
        if (!BatchTool.argOK(password)) {
            System.err.println("password required");
            throw new Exception();
        }
        if (!BatchTool.argOK(objectFormat)) {
            System.err.println("template format required");
            throw new Exception();
        }
        if (!BatchTool.argOK(protocol)) {
            System.err.println("server protocol required");
            throw new Exception();
        }

        // ******************************************
        // NEW: use new client utility class for SOAP stubs
        String baseURL = protocol + "://" + host + ":" + port + "/" + context;
        FedoraClient fc = new FedoraClient(baseURL, username, password);
        APIA = fc.getAPIA();
        APIM = fc.getAPIM();
        //*******************************************

    }

    final void prep() throws Exception {
    }

    private Hashtable<String, String> pidMaps = null;

    private Vector<String> keys = null;

    /* package */Hashtable getPidMaps() {
        return pidMaps;
    }

    /* package */Vector getKeys() {
        return keys;
    }

    final void process() throws Exception {
        //System.err.println("in BatchIngest.process()");
        pidMaps = new Hashtable<String, String>();
        keys = new Vector<String>();
        //AutoIngestor autoIngestor = new AutoIngestor(protocol, host, port, username, password);
        AutoIngestor autoIngestor = new AutoIngestor(APIA, APIM);

        //get files from batchDirectory
        File[] files = null;
        {
            File batchDirectory = new File(objectsPath);
            files = batchDirectory.listFiles();
        }

        if (!(pidsFormat.equals("xml") || pidsFormat.equals("text"))) {
            System.err.println("bad pidsFormat: " + pidsFormat);
        } else if (!objectFormat.equals(FOXML1_1.uri)
                && !objectFormat.equals(METS_EXT1_1.uri)) {
            System.err.println("bad objectFormat: " + objectFormat);
        } else {
            int badFileCount = 0;
            int succeededIngestCount = 0;
            int failedIngestCount = 0;
            String logMessage = "another fedora object";
            for (int i = 0; i < files.length; i++) {
                if (!files[i].isFile()) {
                    badFileCount++;
                    System.err
                            .println("batch directory contains unexpected directory or file: "
                                    + files[i].getName());
                } else {
                    String pid = null;
                    try {
                        pid =
                                autoIngestor
                                        .ingestAndCommit(new FileInputStream(files[i]),
                                                         objectFormat,
                                                         logMessage);
                    } catch (Exception e) {
                        System.err.println("ingest failed for: "
                                + files[i].getName());
                        System.err.println("\t" + e.getClass().getName());
                        System.err.println("\t" + e.getMessage());
                        System.err.println("ingest format specified was: \""
                                + objectFormat + "\"");
                        System.err.println("===BATCH HAS FAILED===");
                        System.err
                                .println("consider manually backing out "
                                        + "any objects which were already successfully ingested in this batch");
                        throw e;
                    }
                    if (pid == null || pid.equals("")) {
                        failedIngestCount++;
                        System.err.println("ingest failed for: "
                                + files[i].getName());
                    } else {
                        succeededIngestCount++;
                        System.out.println("ingest succeeded for: "
                                + files[i].getName());
                        keys.add(files[i].getName());
                        pidMaps.put(files[i].getName(), pid);
                    }
                }
            }
            System.err.println("\n" + "Batch Ingest Summary");
            System.err.println("\n"
                    + (succeededIngestCount + failedIngestCount + badFileCount)
                    + " files processed in this batch");
            System.err.println("\t" + succeededIngestCount
                    + " objects successfully ingested into Fedora");
            System.err.println("\t" + failedIngestCount + " objects failed");
            System.err.println("\t" + badFileCount
                    + " unexpected files in directory");
            System.err
                    .println("\t"
                            + (files.length - (succeededIngestCount
                                    + failedIngestCount + badFileCount))
                            + " files ignored after error");
        }
    }

    public static final void main(String[] args) {
        try {
            Properties miscProperties = new Properties();
            miscProperties
                    .load(new FileInputStream("c:\\batchdemo\\batchtool.properties"));
            BatchIngest batchIngest = new BatchIngest(miscProperties);
            batchIngest.prep();
            batchIngest.process();
        } catch (Exception e) {
        }
    }
}
