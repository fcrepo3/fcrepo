/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.test;

import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fedora.client.FedoraClient;

import fedora.common.Constants;

import fedora.server.management.FedoraAPIM;

/**
 * Performs test to determine the maximum number of objects
 * which can be stored in a Fedora repository.
 *
 * @author Bill Branan
 */
public class ScalabilityTests
    implements Constants {

    private FedoraAPIM apim;
    private PrintStream out;

    private static String DEMO_FOXML_TEXT;
    private static byte[] DEMO_FOXML_BYTES;

    private static final Boolean TRUE = new Boolean(true);

    private long totalIngested = 0;
    private static final int defaultBatchSize = 10;
    private static final int defaultNumBatches = 1;
    private static final int defaultNumThreads = 1;
    private int batchSize = 1;
    private int numBatches = 1;
    private int numThreads = 1;

    private ExecutorService threadPool = null;
    private ArrayList<Callable<Boolean>> ingestRunnerList = null;

    static {
        // Test FOXML object
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<foxml:digitalObject VERSION=\"1.1\" xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\"");
        sb.append("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd\">");
        sb.append("  <foxml:objectProperties>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"Active\"/>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#label\" VALUE=\"Test Object\"/>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#ownerId\" VALUE=\"fedoraAdmin\"/>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#createdDate\" VALUE=\"2008-07-09T19:28:04.890Z\"/>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/view#lastModifiedDate\" VALUE=\"2008-07-09T19:32:31.750Z\"/>");
        sb.append("  </foxml:objectProperties>");
        sb.append("  <foxml:datastream CONTROL_GROUP=\"X\" ID=\"XDS1\" STATE=\"A\" VERSIONABLE=\"true\">");
        sb.append("    <foxml:datastreamVersion CREATED=\"2008-07-09T19:28:58.125Z\" ID=\"XDS1.0\" LABEL=\"XML Datastream 1\" MIMETYPE=\"text/xml\" SIZE=\"41\">");
        sb.append("      <foxml:contentDigest DIGEST=\"none\" TYPE=\"DISABLED\"/>");
        sb.append("      <foxml:xmlContent>");
        sb.append("        <xml>Datastream Content</xml>");
        sb.append("      </foxml:xmlContent>");
        sb.append("    </foxml:datastreamVersion>");
        sb.append("  </foxml:datastream>");
        sb.append("  <foxml:datastream CONTROL_GROUP=\"M\" ID=\"MDS1\" STATE=\"A\" VERSIONABLE=\"true\">");
        sb.append("    <foxml:datastreamVersion CREATED=\"2008-07-09T19:31:57.593Z\" ID=\"MDS1.0\" LABEL=\"Managed Datastream 1\" MIMETYPE=\"text/xml\" SIZE=\"0\">");
        sb.append("      <foxml:contentDigest DIGEST=\"none\" TYPE=\"DISABLED\"/>");
        sb.append("      <foxml:binaryContent>");
        sb.append("              PHhtbD5EYXRhc3RyZWFtIENvbnRlbnQ8L3htbD4=");
        sb.append("      </foxml:binaryContent>");
        sb.append("    </foxml:datastreamVersion>");
        sb.append("  </foxml:datastream>");
        sb.append("</foxml:digitalObject>");

        DEMO_FOXML_TEXT = sb.toString();
        DEMO_FOXML_BYTES = DEMO_FOXML_TEXT.getBytes();
    }

    public void init(String host, String port, String username, String password, String batch, String batches, String threads, String outputFileLocation, String context) throws Exception {

        String baseURL =  "http://" + host + ":" + port + "/" + context;
        FedoraClient fedoraClient = new FedoraClient(baseURL, username, password);
        apim = fedoraClient.getAPIM();

        try {
            batchSize = Integer.valueOf(batch);
        } catch (NumberFormatException nfe) {
            System.err.println("Batch Size value could not be " +
                               "converted to an integer, using the default value (" +
                               defaultBatchSize + ") instead");
            batchSize = defaultBatchSize;
        }

        try {
            numBatches = Integer.valueOf(batches);
        } catch (NumberFormatException nfe) {
            System.err.println("Number of Batches value could not be " +
                               "converted to an integer, using the default value (" +
                               defaultNumBatches + ") instead");
            numBatches = defaultNumBatches;
        }

        try {
            numThreads = Integer.valueOf(threads);
        } catch (NumberFormatException nfe) {
            System.err.println("Number of Threads value could not be " +
                               "converted to an integer, using the default value (" +
                               defaultNumThreads + ") instead");
            numThreads = defaultNumThreads;
        }

        File outputFile = new File(outputFileLocation);
        out = new PrintStream(outputFile);
        out.println("--- Scalability Test Results ---");
        out.println("Total Objects Ingested, " +
                    "Time (ms) To Ingest Batch of " + batchSize + " Objects, " +
                    "Average Ingest Time (ms) Per Object");

        threadPool = Executors.newFixedThreadPool(numThreads);
        ingestRunnerList = new ArrayList<Callable<Boolean>>();
        for(int i=0; i<batchSize; i++) {
            ingestRunnerList.add(new IngestRunner());
        }
    }

    public void close() {
        out.close();
    }

    /**
     * Runs the ingest over the set number of batches
     * or runs continuously if the number of batches
     * is <= 0.
     */
    public void runIngestTest() throws Exception {
        if(numBatches > 0) {
            for(int i=0; i<numBatches; i++) {
                runIngestBatch();
            }
        } else {
            while(true) {
                runIngestBatch();
            }
        }
    }

    /**
     * Runs a batch of ingests using a thread pool.
     * Prints the time required to perform all ingests.
     */
    public void runIngestBatch() throws Exception {
        long startTime = 0;
        long stopTime = 0;

        startTime = System.currentTimeMillis();
        threadPool.invokeAll(ingestRunnerList);
        stopTime = System.currentTimeMillis();

        totalIngested += batchSize;
        long ingestTime = stopTime - startTime;

        out.println(totalIngested + ", " + ingestTime + ", " + ingestTime/batchSize);
    }

    /**
     * Runnable class to use within threads. Executes Fedora ingests.
     */
    private class IngestRunner implements Callable<Boolean> {
        public Boolean call() throws Exception {
            apim.ingest(DEMO_FOXML_BYTES, FOXML1_1.uri, "Ingest Test");
            return TRUE;
        }
    }

    private static void usage() {
        System.out.println("Runs a scalability test over a running Fedora repository.");
        System.out.println("USAGE: ant scalability-tests " +
                                   "-Dhost=HOST " +
                                   "-Dport=PORT " +
                                   "-Dusername=USERNAME " +
                                   "-Dpassword=PASSWORD " +
                                   "-Dbatchsize=BATCH-SIZE " +
                                   "-Dbatches=NUM-BATCHES " +
                                   "-Dthreads=NUM-THREADS " +
                                   "-Dfile=OUTPUT-FILE " +
                                   "[-Dcontext=CONTEXT]");
        System.out.println("Where:");
        System.out.println("  HOST = Host on which Fedora server is running.");
        System.out.println("  PORT = Port on which the Fedora server APIs can be accessed.");
        System.out.println("  USERNAME = A fedora user with administrative privileges.");
        System.out.println("  PASSWORD = The fedora user's password.");
        System.out.println("  BATCH-SIZE = The size of the ingest batch. Statements are only printed");
        System.out.println("               to the output file at the end of each batch.");
        System.out.println("  NUM-BATCHES = The number of batches to run. Set to 0 to run indefinitely.");
        System.out.println("  NUM-THREADS = The number of threads to use in the thread pool.");
        System.out.println("  OUTPUT-FILE = The file to which the test results will be written.");
        System.out.println("                If the file does not exist, it will be created, if the");
        System.out.println("                file does exist the new results will be appended.");
        System.out.println("  CONTEXT     = The application server context Fedora is deployed in. This parameter is optional");
        System.out.println("Example:");
        System.out.println("ant scalability-tests " +
                           "-Dhost=localhost " +
                           "-Dport=8080 " +
                           "-Dusername=fedoraAdmin " +
                           "-Dpassword=fedoraAdmin " +
                           "-Dbatchsize=100 " +
                           "-Dbatches=10 " +
                           "-Dthreads=5 " +
                           "-Dfile=C:\\temp\\scalability_testing_output.txt " +
                           "-Dcontext=my-fedora");
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {

        if(args.length < 8 || args.length > 9) {
            usage();
        }

        String host = args[0];
        String port = args[1];
        String username = args[2];
        String password = args[3];
        String batchSize = args[4];
        String numBatches = args[5];
        String threads = args[6];
        String output = args[7];
        String context = Constants.FEDORA_DEFAULT_APP_CONTEXT;

        if (args.length == 9  && !args[8].equals("")){
            context = args[8];
        }

        if(host == null || host.startsWith("$") ||
           port == null || port.startsWith("$") ||
           username == null || username.startsWith("$") ||
           password == null || password.startsWith("$") ||
           batchSize == null || batchSize.startsWith("$") ||
           numBatches == null || numBatches.startsWith("$") ||
           threads == null || threads.startsWith("$") ||
           output == null || output.startsWith("$")) {
           usage();
        }

        ScalabilityTests tests = new ScalabilityTests();
        tests.init(host, port, username, password, batchSize, numBatches, threads, output, context);
        System.out.println("Running Scalability Test...");
        tests.runIngestTest();
        tests.close();
    }

}
