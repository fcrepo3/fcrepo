/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.batch;

import java.io.File;

import java.util.Properties;

import fedora.common.Constants;

/**
 * Auto Batch Ingest.
 *
 * @author Ross Wayland
 */
public class AutoBatchIngest
        implements Constants {

    private final Properties batchProperties = new Properties();

    public AutoBatchIngest(String objectDir,
                           String logFile,
                           String logFormat,
                           String objectFormat,
                           String host,
                           String port,
                           String username,
                           String password,
                           String protocol,
                           String context)
            throws Exception {

        batchProperties.setProperty("ingest", "yes");
        batchProperties.setProperty("objects", objectDir);
        batchProperties.setProperty("ingested-pids", logFile);
        batchProperties.setProperty("pids-format", logFormat);
        batchProperties.setProperty("object-format", objectFormat);
        batchProperties.setProperty("server-fqdn", host);
        batchProperties.setProperty("server-port", port);
        batchProperties.setProperty("server-context", context);
        batchProperties.setProperty("username", username);
        batchProperties.setProperty("password", password);
        batchProperties.setProperty("server-protocol", protocol);

        BatchTool batchTool = new BatchTool(batchProperties, null, null);
        batchTool.prep();
        batchTool.process();
    }

    public static final void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        String context = Constants.FEDORA_DEFAULT_APP_CONTEXT;
        boolean errors = false;
        if (args.length == 8 || args.length == 9) {
            if (!new File(args[0]).isDirectory()) {
                System.out.println("Specified object directory: \"" + args[0]
                        + "\" is not a directory.");
                errors = true;
            }
            if (!args[2].equals("xml") && !args[2].equals("text")) {
                System.out
                        .println("Format for log file must must be either: \""
                                + "\"xml\"  or  \"txt\"");
                errors = true;
            }
            if (!args[3].equals(FOXML1_1.uri)
                    && !args[3].equals(METS_EXT1_1.uri)) {
                System.out.println("Object format must must be either: \""
                        + "\"" + FOXML1_1.uri + "\" or \"" + METS_EXT1_1.uri
                        + "\"");
                errors = true;
            }
            String[] server = args[4].split(":");
            if (server.length != 2) {
                System.out.println("Specified server name does not specify "
                        + "port number: \"" + args[4] + "\" .");
                errors = true;
            }
            if (!args[7].equals("http") && !args[7].equals("https")) {
                System.out.println("Protocl must be either: \""
                        + "\"http\"  or  \"https\"");
                errors = true;
            }

            if (args.length == 9 && !args[8].equals("")) {
                context = args[8];
            }
            if (!errors) {
                AutoBatchIngest autoBatch =
                        new AutoBatchIngest(args[0],
                                            args[1],
                                            args[2],
                                            args[3],
                                            server[0],
                                            server[1],
                                            args[5],
                                            args[6],
                                            args[7],
                                            context);
            }
        } else {
            System.out.println("ERROR: Invalid number of arguments:");
            System.out.println("");
            System.out.println("Command: fedora-batch-ingest");
            System.out.println("Syntax:");
            System.out.println("  fedora-batch-ingest object-directory log-filepath log-format host:port user password protocol [context]");
            System.out.println("");
            System.out.println("  Where:");
            System.out.println("   object-directory - the full path to the directory containing the objects to be ingested");
            System.out.println("   log-filepath     - the full path to the file where logs will be written");
            System.out.println("   log-format       - the format of the log file. Valid values are text or xml");
            System.out.println("   host:port        - the hostname and port of the target Fedora server");
            System.out.println("   user             - the Fedora administrator username (e.g., fedoraAdmin)");
            System.out.println("   password         - the password for the Fedora administrator user");
            System.out.println("   protocol         - the protocol to communicate with Fedora server, either http or https.");
            System.out.println("   context          - an _optional_ parameter indicating the webapp context. This is only necessary if the Fedora server was installed under a context name other than 'fedora'.");

        }

    }

}
