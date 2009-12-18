/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import fedora.common.Constants;

/**
 * @author Bill Niebel
 */
public class BatchTool {

    private final Properties miscProperties;

    private final Properties datastreamProperties;

    private final Properties metadataProperties;

    private final Properties batchAdditionsValues;

    private final Properties batchXformsValues;

    private final Properties batchIngestValues;

    public BatchTool(Properties miscProperties,
                     Properties datastreamProperties,
                     Properties metadataProperties)
            throws Exception {
        this.miscProperties = miscProperties;
        this.metadataProperties = metadataProperties;
        this.datastreamProperties = datastreamProperties;

        batchAdditionsValues = (Properties) miscProperties.clone();
        batchXformsValues = (Properties) miscProperties.clone();
        batchIngestValues = (Properties) miscProperties.clone();
    }

    private boolean good2go = false;

    public final void prep() {
        good2go = true;
    }

    public final void process() throws Exception {
        if (good2go) {
            BatchAdditions batchAdditions = null;
            BatchXforms batchXforms = null;
            BatchIngest batchIngest = null;

            //make each phase

            if (miscProperties.getProperty(AGGREGATE) != null
                    && miscProperties.getProperty(AGGREGATE).equals("yes")) {
                batchAdditions =
                        new BatchAdditions(batchAdditionsValues,
                                           datastreamProperties,
                                           metadataProperties);
            }
            if (miscProperties.getProperty(DISCRETE) != null
                    && miscProperties.getProperty(DISCRETE).equals("yes")) {
                batchXforms = new BatchXforms(batchXformsValues);
            }
            if (miscProperties.getProperty(EAT) != null
                    && miscProperties.getProperty(EAT).equals("yes")) {
                batchIngest = new BatchIngest(batchIngestValues);
            }

            //check in with each phase
            if (miscProperties.getProperty(AGGREGATE) != null
                    && miscProperties.getProperty(AGGREGATE).equals("yes")) {
                batchAdditions.prep();
            }
            if (miscProperties.getProperty(DISCRETE) != null
                    && miscProperties.getProperty(DISCRETE).equals("yes")) {
                batchXforms.prep();
            }
            if (miscProperties.getProperty(EAT) != null
                    && miscProperties.getProperty(EAT).equals("yes")) {
                batchIngest.prep();
            }

            //perform each phase
            if (miscProperties.getProperty(AGGREGATE) != null
                    && miscProperties.getProperty(AGGREGATE).equals("yes")) {
                batchAdditions.process();
            }

            Vector buildKeys = null;
            if (miscProperties.getProperty(DISCRETE) == null
                    || !miscProperties.getProperty(DISCRETE).equals("yes")) {
                buildKeys = new Vector();
            } else {
                batchXforms.process();
                buildKeys = batchXforms.getKeys();
            }

            Hashtable ingestMaps = null;
            Vector ingestKeys = null;
            if (miscProperties.getProperty(EAT) == null
                    || !miscProperties.getProperty(EAT).equals("yes")) {
                ingestMaps = new Hashtable();
                ingestKeys = new Vector();
            } else {
                batchIngest.process();
                ingestMaps = batchIngest.getPidMaps();
                ingestKeys = batchIngest.getKeys();
            }

            String buildPath2directory =
                    batchXformsValues.getProperty(BatchTool.ADDITIONSPATH);
            String ingestPath2directory =
                    batchIngestValues.getProperty(BatchTool.OBJECTSPATH);
            String pidsPath = batchIngestValues.getProperty(BatchTool.PIDSPATH);

            String pidsFormat =
                    miscProperties.getProperty(BatchTool.PIDSFORMAT);
            String objectFormat =
                    miscProperties.getProperty(BatchTool.OBJECTFORMAT);
            PrintStream out = new PrintStream(new FileOutputStream(pidsPath)); //= System.err;

            //System.out.println("pidsFormat = [" + pidsFormat + "]");
            if (pidsFormat.equals("xml")) {
                out.println("<" + XMLREPORTROOT + ">");
            }
            for (int i = 0, j = 0; i < buildKeys.size()
                    || j < ingestKeys.size();) {
                //System.out.println("it = [" + i + "] [" + j + "]");
                String buildPath2file = "";
                String ingestPath2file = "";
                String pid = "";
                try {
                    String buildFilename = "";
                    String ingestFilename = "";
                    int compared = 0;
                    if (i < buildKeys.size()) {
                        buildFilename = (String) buildKeys.get(i);
                        compared = -1;
                    }
                    if (j < ingestKeys.size()) {
                        ingestFilename = (String) ingestKeys.get(j);
                        compared = 1;
                    }
                    if (i < buildKeys.size() & j < ingestKeys.size()) {
                        compared = buildFilename.compareTo(ingestFilename);
                    }
                    if (compared <= 0) {
                        buildPath2file =
                                buildPath2directory + File.separator
                                        + buildFilename;
                        ingestPath2file =
                                ingestPath2directory + File.separator
                                        + buildFilename;
                        i++;
                    }
                    if (compared >= 0) {
                        ingestPath2file =
                                ingestPath2directory + File.separator
                                        + ingestFilename;
                        pid = (String) ingestMaps.get(ingestFilename);
                        j++;
                    }
                    //System.out.println("no exceptions");
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    //System.out.println("exception = [" + e.getMessage() + "]");
                    Thread.dumpStack();
                    throw e;
                }

                if (pidsFormat.equals("xml")) {
                    //System.out.println("in loop, think it's xml i'm after]");
                    out.print("\t<map ");
                    if (buildPath2file != null && !buildPath2file.equals("")) {
                        out.print("path2spec=\"" + buildPath2file + "\" ");
                    }
                    if (ingestPath2file != null && !ingestPath2file.equals("")) {
                        out.print("path2object=\"" + ingestPath2file + "\" ");
                    }
                    if (pid != null && !pid.equals("")) {
                        out.print("pid=\"" + pid + "\" ");
                    }
                    //out.print("\t<map path2spec=\"" + buildPath2file + "\" path2object=\"" + ingestPath2file + "\" pid=\"" + pid + "\" />");
                    out.println("/>");
                } else if (pidsFormat.equals("text")) {
                    out.println(buildPath2file + "\t" + ingestPath2file + "\t"
                            + pid);
                }
            }
            if (pidsFormat.equals("xml")) {
                out.println("</" + XMLREPORTROOT + ">");
            }
            out.close();
        }
    }

    static final String XMLREPORTROOT = "object-processing-map";

    static final String STARTOBJECT = "initial-pid";

    static final String KEYPATH = "key-path";

    static final String METADATAPATH = "metadata";

    static final String URLPATH = "url";

    static final String DATAPATH = "data";

    static final String STRINGPREFIX = "url-prefix";

    static final String DECLARATIONS = "namespace-declarations";

    private static final String AGGREGATE = "process-tree";

    static final String ADDITIONSPATH = "specifics";

    static final String XFORMPATH = "xform";

    static final String CMODEL = "template";

    private static final String DISCRETE = "merge-objects";

    static final String OBJECTSPATH = "objects";

    static final String SERVERPROTOCOL = "server-protocol";

    static final String SERVERFQDN = "server-fqdn";

    static final String SERVERPORT = "server-port";

    static final String CONTEXT = "server-context";

    static final String USERNAME = "username";

    static final String PASSWORD = "password";

    private static final String EAT = "ingest";

    static final String PIDSPATH = "ingested-pids";

    static final String PIDSFORMAT = "pids-format";

    static final String OBJECTFORMAT = "object-format";

    static final boolean argOK(String value) {
        return value != null && !value.equals("");
    }

    public static final void main(String[] args) throws Exception {
        Properties defaults = new Properties();
        String defaultsPath =
                Constants.FEDORA_HOME + "\\..\\batch\\default.properties";
        //System.err.println("defaultsPath=[" + defaultsPath + "]");
        defaults.load(new FileInputStream(defaultsPath)); //"dist/batch/default.properties"));
        //System.err.println("after loading defaults");
        Properties miscProperties = new Properties(defaults);
        Properties datastreamProperties = new Properties();
        Properties metadataProperties = new Properties();

        /*
         * Getopt getopt = new Getopt("thispgm",args,"g:d:m:"); int c; while ((c =
         * getopt.getopt()) != -1) { switch (c) { case 'g': String temp =
         * getopt.getOptarg(); miscProperties.load(new FileInputStream(temp));
         * //"c:\\batchdemo\\batchtool.properties")); break; case 'd':
         * datastreamProperties.load(new FileInputStream(getopt.getOptarg()));
         * //"c:\\batchdemo\\batchtool.properties")); break; case 'm':
         * metadataProperties.load(new FileInputStream(getopt.getOptarg()));
         * //"c:\\batchdemo\\batchtool.properties")); break; } }
         */
        miscProperties.load(new FileInputStream(args[0]));
        BatchTool batchTool =
                new BatchTool(miscProperties,
                              datastreamProperties,
                              metadataProperties);
        batchTool.prep();
        batchTool.process();
    }

}
