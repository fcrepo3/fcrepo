/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.math.BigDecimal;

import org.apache.axis.types.NonNegativeInteger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;

import fedora.client.FedoraClient;

import fedora.common.Constants;

import fedora.server.access.FedoraAPIA;
import fedora.server.management.FedoraAPIM;

/**
 * Performs test to determine the time necessary to execute
 * various repository activities.
 *
 * @author Bill Branan
 */
public class PerformanceTests
    implements Constants {

    private FedoraAPIM apim;
    private FedoraAPIA apia;

    private static int iterations = 10;
    private static int threads = 10;

    private static final String pid = "demo:performance";

    private static String DEMO_FOXML_TEXT;

    private static String datastream =
        "http://local.fedora.server/fedora-demo/simple-image-demo/coliseum-thumb.jpg";

    private String host;
    private String port;
    private String context;
    private String username;
    private String password;


    private String[] PIDS;
    private byte[][] FOXML;

    static {
        // Test FOXML object
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<foxml:digitalObject PID=\"" + pid + "\" VERSION=\"1.1\" xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\"");
        sb.append("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd\">");
        sb.append("  <foxml:objectProperties>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"Active\"/>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#label\" VALUE=\"Test Object\"/>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#ownerId\" VALUE=\"fedoraAdmin\"/>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#createdDate\" VALUE=\"2008-07-09T19:28:04.890Z\"/>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/view#lastModifiedDate\" VALUE=\"2008-07-09T19:32:31.750Z\"/>");
        sb.append("  </foxml:objectProperties>");
        sb.append("  <foxml:datastream CONTROL_GROUP=\"X\" ID=\"AUDIT\" STATE=\"A\" VERSIONABLE=\"false\">");
        sb.append("    <foxml:datastreamVersion CREATED=\"2008-07-09T19:28:04.890Z\"");
        sb.append("      FORMAT_URI=\"info:fedora/fedora-system:format/xml.fedora.audit\" ID=\"AUDIT.0\" LABEL=\"Fedora Object Audit Trail\" MIMETYPE=\"text/xml\">");
        sb.append("      <foxml:xmlContent>");
        sb.append("        <audit:auditTrail xmlns:audit=\"info:fedora/fedora-system:def/audit#\">");
        sb.append("          <audit:record ID=\"AUDREC1\">");
        sb.append("            <audit:process type=\"Fedora API-M\"/>");
        sb.append("            <audit:action>ingest</audit:action>");
        sb.append("            <audit:componentID/>");
        sb.append("            <audit:responsibility>fedoraAdmin</audit:responsibility>");
        sb.append("            <audit:date>2008-07-09T19:28:04.890Z</audit:date>");
        sb.append("            <audit:justification>Created New Object</audit:justification>");
        sb.append("          </audit:record>");
        sb.append("        </audit:auditTrail>");
        sb.append("      </foxml:xmlContent>");
        sb.append("    </foxml:datastreamVersion>");
        sb.append("  </foxml:datastream>");
        sb.append("  <foxml:datastream CONTROL_GROUP=\"X\" ID=\"DC\" STATE=\"A\" VERSIONABLE=\"true\">");
        sb.append("    <foxml:datastreamVersion CREATED=\"2008-07-09T19:28:04.890Z\" ID=\"DC1.0\" LABEL=\"Dublin Core Metadata\"");
        sb.append("      MIMETYPE=\"text/xml\" SIZE=\"226\">");
        sb.append("      <foxml:contentDigest DIGEST=\"none\" TYPE=\"DISABLED\"/>");
        sb.append("      <foxml:xmlContent>");
        sb.append("        <oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\">");
        sb.append("          <dc:title>Test Object</dc:title>");
        sb.append("          <dc:identifier>" + pid + "</dc:identifier>");
        sb.append("        </oai_dc:dc>");
        sb.append("      </foxml:xmlContent>");
        sb.append("    </foxml:datastreamVersion>");
        sb.append("  </foxml:datastream>");
        sb.append("  <foxml:datastream CONTROL_GROUP=\"X\" ID=\"XDS1\" STATE=\"A\" VERSIONABLE=\"true\">");
        sb.append("    <foxml:datastreamVersion CREATED=\"2008-07-09T19:28:58.125Z\" ID=\"XDS1.0\" LABEL=\"XML Datastream 1\" MIMETYPE=\"text/xml\" SIZE=\"41\">");
        sb.append("      <foxml:contentDigest DIGEST=\"none\" TYPE=\"DISABLED\"/>");
        sb.append("      <foxml:xmlContent>");
        sb.append("        <xml>Datastream Content</xml>");
        sb.append("      </foxml:xmlContent>");
        sb.append("    </foxml:datastreamVersion>");
        sb.append("  </foxml:datastream>");
        sb.append("  <foxml:datastream CONTROL_GROUP=\"X\" ID=\"XDS2\" STATE=\"A\" VERSIONABLE=\"true\">");
        sb.append("    <foxml:datastreamVersion CREATED=\"2008-07-09T19:29:33.609Z\" ID=\"XDS2.0\" LABEL=\"XML Datastream 2\" MIMETYPE=\"text/xml\" SIZE=\"41\">");
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
        sb.append("            </foxml:binaryContent>");
        sb.append("    </foxml:datastreamVersion>");
        sb.append("  </foxml:datastream>");
        sb.append("  <foxml:datastream CONTROL_GROUP=\"M\" ID=\"MDS2\" STATE=\"A\" VERSIONABLE=\"true\">");
        sb.append("    <foxml:datastreamVersion CREATED=\"2008-07-09T19:32:31.750Z\" ID=\"MDS2.0\" LABEL=\"Managed Datastream 2\" MIMETYPE=\"text/xml\" SIZE=\"0\">");
        sb.append("      <foxml:contentDigest DIGEST=\"none\" TYPE=\"DISABLED\"/>");
        sb.append("      <foxml:binaryContent>");
        sb.append("              PHhtbD5EYXRhc3RyZWFtIENvbnRlbnQ8L3htbD4=");
        sb.append("            </foxml:binaryContent>");
        sb.append("    </foxml:datastreamVersion>");
        sb.append("  </foxml:datastream>");
        sb.append("</foxml:digitalObject>");

        DEMO_FOXML_TEXT = sb.toString();
    }

    public void init(String host, String port, String context, String username, String password) throws Exception {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.context = context;

        String baseURL =  "http://" + host + ":" + port + "/" + context;
        FedoraClient fedoraClient = new FedoraClient(baseURL, username, password);
        apim = fedoraClient.getAPIM();
        apia = fedoraClient.getAPIA();

        PIDS = apim.getNextPID(new NonNegativeInteger(Integer.valueOf(iterations).toString()), "demo");
        FOXML = new byte[iterations][];
        for(int i=0; i<iterations; i++) {
            FOXML[i] = DEMO_FOXML_TEXT.replaceAll(pid, PIDS[i]).getBytes("UTF-8");
        }
    }

    private void runIngest(byte[] foxml) throws Exception {
        apim.ingest(foxml, FOXML1_1.uri, "Ingest Test");
    }

    private void runAddDatastream(String pid, String dsId) throws Exception {
        apim.addDatastream(pid,
                           dsId,
                           null,
                           "New Datastream",
                           true,
                           "text/xml",
                           null,
                           datastream,
                           "M",
                           "A",
                           null,
                           null,
                           "Adding Test Datastream");
    }

    private void runModifyDatastreamByRef(String pid) throws Exception {
        apim.modifyDatastreamByReference(pid,
                                         "MDS1",
                                         null,
                                         "New Label",
                                         "text/xml",
                                         null,
                                         datastream,
                                         null,
                                         null,
                                         "Modify Datastream Test",
                                         false);
    }

    private void runModifyDatastreamByValue(String pid) throws Exception {
        String dsContent = "<xml>Updated Content</xml>";
        apim.modifyDatastreamByValue(pid,
                                     "XDS1",
                                     null,
                                     "New Label",
                                     "text/xml",
                                     null,
                                     dsContent.getBytes(),
                                     null,
                                     null,
                                     "Modify Datastream Test",
                                     false);
    }

    private void runPurgeDatastream(String pid, String dsId) throws Exception {
        apim.purgeDatastream(pid, dsId, null, null, "Purge Datastream Test", false);
    }

    private void runPurgeObject(String pid) throws Exception {
        apim.purgeObject(pid, "Removing Test Object", false);
    }

    private void runGetDatastream(String pid) throws Exception {
        apia.getDatastreamDissemination(pid, "MDS1", null);
    }

    private void runGetDatastreamRest(String pid) throws Exception {
        HttpMethod httpMethod = getHttpMethod(pid);
        HttpClient client = getHttpClient();
        client.executeMethod(httpMethod);
        InputStream in = httpMethod.getResponseBodyAsStream();
        int input = in.read();
        while(input > 0) {
            input = in.read();
        }
    }

    /**
     * @return average run time for a single operation
     */
    public long runIngestTest() throws Exception {
        long totalTime = 0;
        long startTime = 0;
        long stopTime = 0;
        for(int i=0; i<iterations; i++) {
            startTime = System.currentTimeMillis();
            runIngest(FOXML[0]);
            stopTime = System.currentTimeMillis();
            totalTime += (stopTime - startTime);
            runPurgeObject(PIDS[0]);
        }
        long average = totalTime/iterations;
        return average;
    }

    /**
     * @return average run time for a single operation
     */
    public long runAddDatastreamTest() throws Exception {
        long totalTime = 0;
        long startTime = 0;
        long stopTime = 0;
        runIngest(FOXML[0]);
        for(int i=0; i<iterations; i++) {
            startTime = System.currentTimeMillis();
            runAddDatastream(PIDS[0], "MDS3");
            stopTime = System.currentTimeMillis();
            totalTime += (stopTime - startTime);
            runPurgeDatastream(PIDS[0], "MDS3");
        }
        runPurgeObject(PIDS[0]);
        long average = totalTime/iterations;
        return average;
    }

    /**
     * @return average run time for a single operation
     */
    public long runModifyDatastreamByRefTest() throws Exception {
        long totalTime = 0;
        long startTime = 0;
        long stopTime = 0;
        runIngest(FOXML[0]);
        for(int i=0; i<iterations; i++) {
            startTime = System.currentTimeMillis();
            runModifyDatastreamByRef(PIDS[0]);
            stopTime = System.currentTimeMillis();
            totalTime += (stopTime - startTime);
        }
        runPurgeObject(PIDS[0]);
        long average = totalTime/iterations;
        return average;
    }

    /**
     * @return average run time for a single operation
     */
    public long runModifyDatastreamByValueTest() throws Exception {
        long totalTime = 0;
        long startTime = 0;
        long stopTime = 0;
        runIngest(FOXML[0]);
        for(int i=0; i<iterations; i++) {
            startTime = System.currentTimeMillis();
            runModifyDatastreamByValue(PIDS[0]);
            stopTime = System.currentTimeMillis();
            totalTime += (stopTime - startTime);
        }
        runPurgeObject(PIDS[0]);
        long average = totalTime/iterations;
        return average;
    }

    /**
     * @return average run time for a single operation
     */
    public long runPurgeDatastreamTest() throws Exception {
        long totalTime = 0;
        long startTime = 0;
        long stopTime = 0;
        runIngest(FOXML[0]);
        for(int i=0; i<iterations; i++) {
            runAddDatastream(PIDS[0], "MDS3");
            startTime = System.currentTimeMillis();
            runPurgeDatastream(PIDS[0], "MDS3");
            stopTime = System.currentTimeMillis();
            totalTime += (stopTime - startTime);
        }
        runPurgeObject(PIDS[0]);
        long average = totalTime/iterations;
        return average;
    }

    /**
     * @return average run time for a single operation
     */
    public long runPurgeObjectTest() throws Exception {
        long totalTime = 0;
        long startTime = 0;
        long stopTime = 0;
        for(int i=0; i<iterations; i++) {
            runIngest(FOXML[0]);
            startTime = System.currentTimeMillis();
            runPurgeObject(PIDS[0]);
            stopTime = System.currentTimeMillis();
            totalTime += (stopTime - startTime);
        }
        long average = totalTime/iterations;
        return average;
    }

    /**
     * @return total time to run all iterations
     */
    public long runGetDatastreamTest() throws Exception {
        long totalTime = 0;
        long startTime = 0;
        long stopTime = 0;
        runIngest(FOXML[0]);
        startTime = System.currentTimeMillis();
        for(int i=0; i<iterations; i++) {
            runGetDatastream(PIDS[0]);
        }
        stopTime = System.currentTimeMillis();
        totalTime = (stopTime - startTime);
        runPurgeObject(PIDS[0]);

        return totalTime;
    }

    /**
     * @return total time to run all iterations
     */
    public long runGetDatastreamRestTest() throws Exception {
        long totalTime = 0;
        long startTime = 0;
        long stopTime = 0;
        runIngest(FOXML[0]);
        HttpMethod httpMethod = getHttpMethod(PIDS[0]);
        HttpClient client = getHttpClient();
        startTime = System.currentTimeMillis();
        for(int i=0; i<iterations; i++) {
            client.executeMethod(httpMethod);
            InputStream in = httpMethod.getResponseBodyAsStream();
            int input = in.read();
            while(input > 0) {
                input = in.read();
            }
        }
        stopTime = System.currentTimeMillis();
        totalTime = (stopTime - startTime);
        runPurgeObject(PIDS[0]);

        return totalTime;
    }

    private HttpMethod getHttpMethod(String pid) {
        String url = "http://" + host + ":" + port + "/" + context + "/get/" + pid + "/" + "MDS1";
        HttpMethod httpMethod = new GetMethod(url);
        httpMethod.setDoAuthentication(true);
        httpMethod.getParams().setParameter("Connection", "Keep-Alive");
        return httpMethod;
    }

    private HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(
                new AuthScope(host, Integer.valueOf(port), "realm"),
                new UsernamePasswordCredentials(username, password));
        return client;
    }

    /**
     *  @return total time to run all iterations
     *          {ingest, addDatastream, modifyDatastreamByReference,
     *           modifyDatastreamByValue, purgeDatastream, purgeObject}
     */
    public long[] runThroughputTests() throws Exception {
        long ingestTime = 0;
        long addDsTime = 0;
        long modifyRefTime = 0;
        long modifyValTime = 0;
        long purgeDsTime = 0;
        long purgeObjectTime = 0;
        long startTime = 0;
        long stopTime = 0;

        // Ingest
        startTime = System.currentTimeMillis();
        for(int i=0; i<iterations; i++) {
            runIngest(FOXML[i]);
        }
        stopTime = System.currentTimeMillis();
        ingestTime = (stopTime - startTime);

        // Add Datastream
        startTime = System.currentTimeMillis();
        for(int i=0; i<iterations; i++) {
            runAddDatastream(PIDS[i], "MDS3");
        }
        stopTime = System.currentTimeMillis();
        addDsTime = (stopTime - startTime);

        // Modify Datastream By Reference
        startTime = System.currentTimeMillis();
        for(int i=0; i<iterations; i++) {
            runModifyDatastreamByRef(PIDS[i]);
        }
        stopTime = System.currentTimeMillis();
        modifyRefTime = (stopTime - startTime);

        // Modify Datastream By Value
        startTime = System.currentTimeMillis();
        for(int i=0; i<iterations; i++) {
            runModifyDatastreamByValue(PIDS[i]);
        }
        stopTime = System.currentTimeMillis();
        modifyValTime = (stopTime - startTime);

        // Purge Datastream
        startTime = System.currentTimeMillis();
        for(int i=0; i<iterations; i++) {
            runPurgeDatastream(PIDS[i], "MDS1");
        }
        stopTime = System.currentTimeMillis();
        purgeDsTime = (stopTime - startTime);

        // Purge Object
        startTime = System.currentTimeMillis();
        for(int i=0; i<iterations; i++) {
            runPurgeObject(PIDS[i]);
        }
        stopTime = System.currentTimeMillis();
        purgeObjectTime = (stopTime - startTime);

        // Get Datastream and Get Datastream REST results
        // do not change from round-trip tests

        long[] totals = {ingestTime,
                         addDsTime,
                         modifyRefTime,
                         modifyValTime,
                         purgeDsTime,
                         purgeObjectTime};
        return totals;
    }

    /**
     *  @return total time to run all iterations with threading
     *          {ingest, addDatastream, modifyDatastreamByReference,
     *           modifyDatastreamByValue, purgeDatastream, purgeObject,
     *           getDatastream, getDatastreamREST}
     */
    public long[] runThreadedThroughputTests() throws Exception {
        long ingestTime = 0;
        long addDsTime = 0;
        long modifyRefTime = 0;
        long modifyValTime = 0;
        long purgeDsTime = 0;
        long purgeObjectTime = 0;
        long getDatastreamTime = 0;
        long getDatastreamRestTime = 0;
        long startTime = 0;
        long stopTime = 0;

        ExecutorService threadPool = Executors.newFixedThreadPool(threads);

        // Ingest
        ArrayList<Callable<Boolean>> ingestRunnerList = new ArrayList<Callable<Boolean>>();
        for(int i=0; i<iterations; i++) {
            ingestRunnerList.add(new MethodRunner(MethodType.INGEST, i));
        }
        startTime = System.currentTimeMillis();
        threadPool.invokeAll(ingestRunnerList);
        stopTime = System.currentTimeMillis();
        ingestTime = (stopTime - startTime);

        // Add Datastream
        ArrayList<Callable<Boolean>> addDatastreamRunnerList = new ArrayList<Callable<Boolean>>();
        for(int i=0; i<iterations; i++) {
            addDatastreamRunnerList.add(new MethodRunner(MethodType.ADD_DATASTREAM, i));
        }
        startTime = System.currentTimeMillis();
        threadPool.invokeAll(addDatastreamRunnerList);
        stopTime = System.currentTimeMillis();
        addDsTime = (stopTime - startTime);

        // Modify Datastream By Reference
        ArrayList<Callable<Boolean>> modDatastreamRefRunnerList = new ArrayList<Callable<Boolean>>();
        for(int i=0; i<iterations; i++) {
            modDatastreamRefRunnerList.add(new MethodRunner(MethodType.MODIFY_DATASTREAM_REF, i));
        }
        startTime = System.currentTimeMillis();
        threadPool.invokeAll(modDatastreamRefRunnerList);
        stopTime = System.currentTimeMillis();
        modifyRefTime = (stopTime - startTime);

        // Modify Datastream By Value
        ArrayList<Callable<Boolean>> modDatastreamValRunnerList = new ArrayList<Callable<Boolean>>();
        for(int i=0; i<iterations; i++) {
            modDatastreamValRunnerList.add(new MethodRunner(MethodType.MODIFY_DATASTREAM_VAL, i));
        }
        startTime = System.currentTimeMillis();
        threadPool.invokeAll(modDatastreamValRunnerList);
        stopTime = System.currentTimeMillis();
        modifyValTime = (stopTime - startTime);

        // Purge Datastream
        ArrayList<Callable<Boolean>> purgeDatastreamRunnerList = new ArrayList<Callable<Boolean>>();
        for(int i=0; i<iterations; i++) {
            purgeDatastreamRunnerList.add(new MethodRunner(MethodType.PURGE_DATASTREAM, i));
        }
        startTime = System.currentTimeMillis();
        threadPool.invokeAll(purgeDatastreamRunnerList);
        stopTime = System.currentTimeMillis();
        purgeDsTime = (stopTime - startTime);

        // Purge Object
        ArrayList<Callable<Boolean>> purgeObjectRunnerList = new ArrayList<Callable<Boolean>>();
        for(int i=0; i<iterations; i++) {
            purgeObjectRunnerList.add(new MethodRunner(MethodType.PURGE_OBJECT, i));
        }
        startTime = System.currentTimeMillis();
        threadPool.invokeAll(purgeObjectRunnerList);
        stopTime = System.currentTimeMillis();
        purgeObjectTime = (stopTime - startTime);

        // Get Datastream
        ArrayList<Callable<Boolean>> getDatastreamRunnerList = new ArrayList<Callable<Boolean>>();
        for(int i=0; i<iterations; i++) {
            getDatastreamRunnerList.add(new MethodRunner(MethodType.GET_DATASTREAM, i));
        }
        startTime = System.currentTimeMillis();
        threadPool.invokeAll(getDatastreamRunnerList);
        stopTime = System.currentTimeMillis();
        getDatastreamTime = (stopTime - startTime);

        // Get Datastream REST
        ArrayList<Callable<Boolean>> getDatastreamRestRunnerList = new ArrayList<Callable<Boolean>>();
        for(int i=0; i<iterations; i++) {
            getDatastreamRestRunnerList.add(new MethodRunner(MethodType.GET_DATASTREAM_REST, i));
        }
        startTime = System.currentTimeMillis();
        threadPool.invokeAll(getDatastreamRestRunnerList);
        stopTime = System.currentTimeMillis();
        getDatastreamRestTime = (stopTime - startTime);

        long[] totals = {ingestTime,
                         addDsTime,
                         modifyRefTime,
                         modifyValTime,
                         purgeDsTime,
                         purgeObjectTime,
                         getDatastreamTime,
                         getDatastreamRestTime};
        return totals;
    }

    private enum MethodType {
        INGEST,
        ADD_DATASTREAM,
        MODIFY_DATASTREAM_REF,
        MODIFY_DATASTREAM_VAL,
        PURGE_DATASTREAM,
        PURGE_OBJECT,
        GET_DATASTREAM,
        GET_DATASTREAM_REST;
    }

    /*
     * Runnable class to use within threads. Executes a
     * Fedora method based on the MethodType defined.
     */
    private class MethodRunner implements Callable<Boolean> {

        MethodType methodType;
        int index;

        public MethodRunner(MethodType methodType, int index) {
            this.methodType = methodType;
            this.index = index;
        }

        public Boolean call() throws Exception {
            if(methodType.equals(MethodType.INGEST)) {
                runIngest(FOXML[index]);
            }else if(methodType.equals(MethodType.ADD_DATASTREAM)) {
               runAddDatastream(PIDS[index], "MDS3");
            } else if(methodType.equals(MethodType.MODIFY_DATASTREAM_REF)) {
                runModifyDatastreamByRef(PIDS[index]);
            } else if(methodType.equals(MethodType.MODIFY_DATASTREAM_VAL)) {
                runModifyDatastreamByValue(PIDS[index]);
            } else if(methodType.equals(MethodType.PURGE_DATASTREAM)) {
                runPurgeDatastream(PIDS[index], "MDS1");
            } else if(methodType.equals(MethodType.PURGE_OBJECT)) {
                runPurgeObject(PIDS[index]);
            } else if(methodType.equals(MethodType.GET_DATASTREAM)) {
                runGetDatastream(PIDS[index]);
            } else if(methodType.equals(MethodType.GET_DATASTREAM_REST)) {
                runGetDatastreamRest(PIDS[index]);
            }
            return new Boolean(true);
        }
    }

    private static double round(double d) {
        int decimalPlace = 5;
        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.doubleValue();
    }

    private static void usage() {
        System.out.println("Runs a set of performance tests over a running Fedora repository.");
        System.out.println("USAGE: ant performance-tests " +
                                   "-Dhost=HOST " +
                                   "-Dport=PORT " +
                                   "-Dusername=USERNAME " +
                                   "-Dpassword=PASSWORD " +
                                   "-Diterations=NUM-ITERATIONS " +
                                   "-Dthreads=NUM-THREADS " +
                                   "-Dfile=OUTPUT-FILE " +
                                   "-Dname=TEST-NAME" +
                                   "[-Dcontext=CONTEXT]");
        System.out.println("Where:");
        System.out.println("  HOST = Host on which Fedora server is running.");
        System.out.println("  PORT = Port on which the Fedora server APIs can be accessed.");
        System.out.println("  USERNAME = A fedora user with administrative privileges.");
        System.out.println("  PASSWORD = The fedora user's password.");
        System.out.println("  NUM-ITERATIONS = The number of times to perform each operation.");
        System.out.println("  NUM-THREADS = The number of threads to use in the thread pool");
        System.out.println("                when running threaded tests.");
        System.out.println("  OUTPUT-FILE = The file to which the test results will be written.");
        System.out.println("                If the file does not exist, it will be created, if the");
        System.out.println("                file does exist the new results will be appended.");
        System.out.println("  TEST-NAME   = A name for this test run.");
        System.out.println("  CONTEXT     = The application server context Fedora is deployed in. This parameter is optional");
        System.out.println("Example:");
        System.out.println("ant performance-tests " +
                           "-Dhost=localhost " +
                           "-Dport=8080 " +
                           "-Dusername=fedoraAdmin " +
                           "-Dpassword=fedoraAdmin " +
                           "-Diterations=100 " +
                           "-Dthreads=10 " +
                           "-Dfile=C:\\temp\\performance_testing_output.txt " +
                           "-Dname=\"Test 1\"" +
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
        String itr = args[4];
        String thrds = args[5];
        String output = args[6];
        String name = args[7];
        String context  = Constants.FEDORA_DEFAULT_APP_CONTEXT;

        if(args.length == 9 && !args[8].equals("")){
            context = args[8];
        }

        if(host == null || host.startsWith("$") ||
           port == null || port.startsWith("$") ||
           username == null || username.startsWith("$") ||
           password == null || password.startsWith("$") ||
           itr == null || itr.startsWith("$") ||
           thrds == null || thrds.startsWith("$") ||
           output == null || output.startsWith("$") ||
           name == null || name.startsWith("$")) {
            usage();
        }
        name = name.replaceAll(",", ";");
        iterations = Integer.parseInt(itr);
        threads = Integer.parseInt(thrds);

        boolean newFile = true;
        File outputFile = new File(output);

        File tempFile = null;
        BufferedReader reader = null;
        String line = "";
        if(outputFile.exists()) {
            newFile = false;

            // Create a copy of the file to read from
            tempFile = File.createTempFile("performance-test", "tmp");
            BufferedReader input = new BufferedReader(new FileReader(outputFile));
            PrintStream tempOut = new PrintStream(tempFile);

            while ((line = input.readLine()) != null) {
                tempOut.println(line);
            }
            input.close();
            tempOut.close();

            reader = new BufferedReader(new FileReader(tempFile));
        }
        PrintStream out = new PrintStream(outputFile);

        if(newFile) {
            out.println("--------------------------------------------------------------" +
                        " Performance Test Results " +
                        "--------------------------------------------------------------");
        }

        PerformanceTests tests = new PerformanceTests();
        tests.init(host, port, context, username, password);
        System.out.println("Running Ingest Round-Trip Test...");
        long ingestResults = tests.runIngestTest();
        System.out.println("Running AddDatastream Round-Trip Test...");
        long addDsResults = tests.runAddDatastreamTest();
        System.out.println("Running ModifyDatastreamByReference Round-Trip Test...");
        long modifyRefResults = tests.runModifyDatastreamByRefTest();
        System.out.println("Running ModifyDatastreamByValue Round-Trip Test...");
        long modifyValResults = tests.runModifyDatastreamByValueTest();
        System.out.println("Running PurgeDatastream Round-Trip Test...");
        long purgeDsResults = tests.runPurgeDatastreamTest();
        System.out.println("Running PurgeObject Round-Trip Test...");
        long purgeObjectResults = tests.runPurgeObjectTest();
        System.out.println("Running GetDatastream Round-Trip Test...");
        long getDatastreamResults = tests.runGetDatastreamTest();
        System.out.println("Running GetDatastreamREST Round-Trip Test...");
        long getDatastreamRestResults = tests.runGetDatastreamRestTest();
        System.out.println("Running Throughput Tests...");
        long[] tpResults = tests.runThroughputTests();
        System.out.println("Running Threaded Throughput Tests...");
        long[] tptResults = tests.runThreadedThroughputTests();

        if(newFile) {
            out.println("1. Test performing each operation in isolation. Time (in ms) is the average required to perform each operation.");
            out.println("test name, ingest, addDatastream, modifyDatastreamByReference, modifyDatastreamByValue, purgeDatastream, purgeObject, getDatastream, getDatastreamREST");
        } else {
            line = reader.readLine();
            while (line != null && line.length() > 2) {
                out.println(line);
                line = reader.readLine();
            }
        }
        out.println(name + ", " +
                    ingestResults + ", " +
                    addDsResults + ", " +
                    modifyRefResults + ", " +
                    modifyValResults + ", " +
                    purgeDsResults + ", " +
                    purgeObjectResults + ", " +
                    getDatastreamResults/iterations + ", " +
                    getDatastreamRestResults/iterations);

        out.println();
        if(newFile) {
            out.println("2. Operations-Per-Second based on results listed in item 1.");
            out.println("test name, ingest, addDatastream, modifyDatastreamByReference, modifyDatastreamByValue, purgeDatastream, purgeObject, getDatastream, getDatastreamREST");
        } else {
            line = reader.readLine();
            while (line != null && line.length() > 2) {
                out.println(line);
                line = reader.readLine();
            }
        }
        double ingestPerSecond = 1000/(double)ingestResults;
        double addDsPerSecond = 1000/(double)addDsResults;
        double modifyRefPerSecond = 1000/(double)modifyRefResults;
        double modifyValPerSecond = 1000/(double)modifyValResults;
        double purgeDsPerSecond = 1000/(double)purgeDsResults;
        double purgeObjPerSecond = 1000/(double)purgeObjectResults;
        double getDatastreamPerSecond = 1000/((double)getDatastreamResults/iterations);
        double getDatastreamRestPerSecond = 1000/((double)getDatastreamRestResults/iterations);
        out.println(name + ", " +
                    round(ingestPerSecond) + ", " +
                    round(addDsPerSecond) + ", " +
                    round(modifyRefPerSecond) + ", " +
                    round(modifyValPerSecond) + ", " +
                    round(purgeDsPerSecond) + ", " +
                    round(purgeObjPerSecond) + ", " +
                    round(getDatastreamPerSecond) + ", " +
                    round(getDatastreamRestPerSecond));

        out.println();
        if(newFile) {
            out.println("3. Test performing operations back-to-back. Time (in ms) is that required to perform all iterations.");
            out.println("test name, ingest, addDatastream, modifyDatastreamByReference, modifyDatastreamByValue, purgeDatastream, purgeObject, getDatastream, getDatastreamREST");
        } else {
            line = reader.readLine();
            while (line != null && line.length() > 2) {
                out.println(line);
                line = reader.readLine();
            }
        }
        out.println(name + ", " +
                    tpResults[0] + ", " +
                    tpResults[1] + ", " +
                    tpResults[2] + ", " +
                    tpResults[3] + ", " +
                    tpResults[4] + ", " +
                    tpResults[5] + ", " +
                    getDatastreamResults + ", " +
                    getDatastreamRestResults);

        out.println();
        if(newFile) {
            out.println("4. Operations-Per-Second based on results listed in item 3.");
            out.println("test name, ingest, addDatastream, modifyDatastreamByReference, modifyDatastreamByValue, purgeDatastream, purgeObject, getDatastream, getDatastreamREST");
        } else {
            line = reader.readLine();
            while (line != null && line.length() > 2) {
                out.println(line);
                line = reader.readLine();
            }
        }
        double ingestItPerSecond = (double)(iterations * 1000)/tpResults[0];
        double addDsItPerSecond = (double)(iterations * 1000)/tpResults[1];
        double modifyRefItPerSecond = (double)(iterations * 1000)/tpResults[2];
        double modifyValItPerSecond = (double)(iterations * 1000)/tpResults[3];
        double purgeDsItPerSecond = (double)(iterations * 1000)/tpResults[4];
        double purgeObjItPerSecond = (double)(iterations * 1000)/tpResults[5];
        double getDsItPerSecond = (double)(iterations * 1000)/getDatastreamResults;
        double getDsRestItPerSecond = (double)(iterations * 1000)/getDatastreamRestResults;
        out.println(name + ", " +
                    round(ingestItPerSecond) + ", " +
                    round(addDsItPerSecond) + ", " +
                    round(modifyRefItPerSecond) + ", " +
                    round(modifyValItPerSecond) + ", " +
                    round(purgeDsItPerSecond) + ", " +
                    round(purgeObjItPerSecond) + ", " +
                    round(getDsItPerSecond) + ", " +
                    round(getDsRestItPerSecond));

        out.println();
        if(newFile) {
            out.println("5. Test performing operations using a thread pool. Time (in ms) is that required to perform all iterations.");
            out.println("test name, ingest, addDatastream, modifyDatastreamByReference, modifyDatastreamByValue, purgeDatastream, purgeObject, getDatastream, getDatastreamREST");
        } else {
            line = reader.readLine();
            while (line != null && line.length() > 2) {
                out.println(line);
                line = reader.readLine();
            }
        }
        out.println(name + ", " +
                    tptResults[0] + ", " +
                    tptResults[1] + ", " +
                    tptResults[2] + ", " +
                    tptResults[3] + ", " +
                    tptResults[4] + ", " +
                    tptResults[5] + ", " +
                    tptResults[6] + ", " +
                    tptResults[7]);

        out.println();
        if(newFile) {
            out.println("6. Operations-Per-Second based on results listed in item 5.");
            out.println("test name, ingest, addDatastream, modifyDatastreamByReference, modifyDatastreamByValue, purgeDatastream, purgeObject, getDatastream, getDatastreamREST");
        } else {
            line = reader.readLine();
            while (line != null && line.length() > 2) {
                out.println(line);
                line = reader.readLine();
            }
        }
        double thrdIngestItPerSecond = (double)(iterations * 1000)/tptResults[0];
        double thrdAddDsItPerSecond = (double)(iterations * 1000)/tptResults[1];
        double thrdModifyRefItPerSecond = (double)(iterations * 1000)/tptResults[2];
        double thrdModifyValItPerSecond = (double)(iterations * 1000)/tptResults[3];
        double thrdPurgeDsItPerSecond = (double)(iterations * 1000)/tptResults[4];
        double thrdPurgeObjItPerSecond = (double)(iterations * 1000)/tptResults[5];
        double thrdGetDsItPerSecond = (double)(iterations * 1000)/tptResults[6];
        double thrdGetDsRestItPerSecond = (double)(iterations * 1000)/tptResults[7];
        out.println(name + ", " +
                    round(thrdIngestItPerSecond) + ", " +
                    round(thrdAddDsItPerSecond) + ", " +
                    round(thrdModifyRefItPerSecond) + ", " +
                    round(thrdModifyValItPerSecond) + ", " +
                    round(thrdPurgeDsItPerSecond) + ", " +
                    round(thrdPurgeObjItPerSecond) + ", " +
                    round(thrdGetDsItPerSecond) + ", " +
                    round(thrdGetDsRestItPerSecond));

        if(!newFile){
            reader.close();
            tempFile.delete();
        }
        out.close();

        System.out.println("Performance Tests Complete.");
    }

}
