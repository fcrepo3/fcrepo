/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import java.net.MalformedURLException;

import javax.xml.rpc.ServiceException;

import fedora.client.FedoraClient;
import fedora.client.Uploader;

import fedora.common.Constants;

import fedora.server.access.FedoraAPIA;
import fedora.server.management.FedoraAPIM;
import fedora.server.utilities.StreamUtility;

/**
 * Command-line version of the Batch Modify utility that's available
 * in the admin GUI client.
 *
 * This utility processes an xml input file containing modify directives
 * enabling mass updating of existing objects. It has six required arguments:
 * <ol>
 * <li>host - Name of the Fedora repository server.</li>
 * <li>port - port number o fthe Fedora server.</li>
 * <li>user - username of the Fedora server admin user</li>
 * <li>password - password of the Fedora server admin user</li>
 * <li>directivesFilePath - absolute file path of the input file containing
 * modify directives. Note that his file should should validate against the
 * batchModify schema.</li>
 * <li>logFilePath - absolute file path of the log file; an xml file providing
 * a history of the transactions processed.</li>
 * </ol>
 *
 * @author Ross Wayland
 */
public class AutoModify {

    private static String s_rootName = null;

    private static PrintStream s_log = null;

    private static FedoraAPIM s_APIM = null;

    private static FedoraAPIA s_APIA = null;

    private static Uploader s_UPLOADER = null;

    public static FedoraAPIA APIA = null;

    public static FedoraAPIM APIM = null;

    //public AutoModify(String protocol, String host, int port, String user, String pass)
    //    throws MalformedURLException, ServiceException, IOException
    //{

    //    AutoModify.s_APIM=APIMStubFactory.getStub(protocol, host, port, user, pass);
    //    AutoModify.s_APIA=APIAStubFactory.getStub(protocol, host, port, user, pass);
    //    AutoModify.s_UPLOADER = new Uploader(protocol, host, port, user, pass);
    //}

    /**
     * <p>
     * Constructor for the class.
     * </p>
     *
     * @param apia -
     *        SOAP stub for APIA service.
     * @param apim -
     *        SOAP stub for APIM service.
     * @param host -
     *        Hostname of the Fedora server.
     * @param port -
     *        Port number of the Fedora server.
     * @param user -
     *        username of the Fedora server admin user.
     * @param pass -
     *        password of the Fedora server admin user.
     * @throws MalformedURLException -
     *         If the URL generated from host and port is invalid.
     * @throws ServiceException -
     *         If unable to connect via SOAP to the Fedora API-M web service.
     * @throws IOException -
     *         If an error occurs in creating an instance of the Uploader.
     */
    public AutoModify(FedoraAPIA apia,
                      FedoraAPIM apim,
                      String protocol,
                      String host,
                      int port,
                      String context,
                      String user,
                      String pass)
            throws MalformedURLException, ServiceException, IOException {

        AutoModify.s_APIM = apim;
        AutoModify.s_APIA = apia;
        AutoModify.s_UPLOADER = new Uploader(protocol, host, port, context, user, pass);

    }

    /**
     * <p>
     * Processes the modify directives.
     * </p>
     *
     * @param directivesFilePath -
     *        The absolute file path of the file containing the modify
     *        directives.
     * @param logFilePath -
     *        The absolute file path of the log file.
     * @param isValidateOnly -
     *        Boolean flag; true indicates validate only; false indicates
     *        process the directives file.
     */
    public void modify(String directivesFilePath,
                       String logFilePath,
                       boolean isValidateOnly) {
        modify(s_APIM,
               s_UPLOADER,
               s_APIA,
               directivesFilePath,
               logFilePath,
               isValidateOnly);
    }

    /**
     * <p>
     * Process the modify directives.
     * </p>
     *
     * @param APIM -
     *        An instance of FedoraAPIM.
     * @param UPLOADER -
     *        An instance of the Uploader.
     * @param directivesFilePath -
     *        The absolute file path of the file containing the modify
     *        directives.
     * @param APIA -
     *        An instance of FedoraAPIA.
     * @param logFilePath -
     *        The absolute file path of the log file.
     * @param isValidateOnly -
     *        Boolean flag; true indicates validate only; false indicates
     *        process the directives file.
     */
    public static void modify(FedoraAPIM APIM,
                              Uploader UPLOADER,
                              FedoraAPIA APIA,
                              String directivesFilePath,
                              String logFilePath,
                              boolean isValidateOnly) {

        InputStream in = null;
        BatchModifyParser bmp = null;
        BatchModifyValidator bmv = null;
        long st = System.currentTimeMillis();
        long et = 0;
        try {
            in = new FileInputStream(directivesFilePath);
            if (isValidateOnly) {
                openLog(logFilePath, "validate-modify-directives");
                bmv = new BatchModifyValidator(in, s_log);
            } else {
                openLog(logFilePath, "modify-batch");
                bmp = new BatchModifyParser(UPLOADER, APIM, APIA, in, s_log);
            }

        } catch (Exception e) {
            System.out.println(e.getClass().getName()
                    + " - "
                    + (e.getMessage() == null ? "(no detail provided)" : e
                            .getMessage()));

        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (s_log != null) {
                    et = System.currentTimeMillis();
                    if (bmp != null) {
                        if (bmp.getFailedCount() == -1) {
                            System.out
                                    .println("\n\n"
                                            + bmp.getSucceededCount()
                                            + " modify directives successfully processed.\n"
                                            + "Parser error encountered.\n"
                                            + "An unknown number of modify directives were not processed.\n"
                                            + "See log file for details of those directives processed before the error.\n"
                                            + "Time elapsed: "
                                            + getDuration(et - st));
                            s_log.println("  <summary>");
                            s_log
                                    .println("    "
                                            + StreamUtility
                                                    .enc(bmp
                                                            .getSucceededCount()
                                                            + " modify directives successfully processed.\n"
                                                            + "    Parser error encountered.\n"
                                                            + "    An unknown number of modify directives were not processed.\n"
                                                            + "    Time elapsed: "
                                                            + getDuration(et
                                                                    - st)));
                            s_log.println("  </summary>");
                        } else {
                            System.out
                                    .println("\n\n"
                                            + bmp.getSucceededCount()
                                            + " modify directives successfully processed.\n"
                                            + bmp.getFailedCount()
                                            + " modify directives failed.\n"
                                            + "See log file for details.\n"
                                            + "Time elapsed: "
                                            + getDuration(et - st));
                            s_log.println("  <summary>");
                            s_log
                                    .println("    "
                                            + StreamUtility
                                                    .enc(bmp
                                                            .getSucceededCount()
                                                            + " modify directives successfully processed.\n    "
                                                            + bmp
                                                                    .getFailedCount()
                                                            + " modify directives failed.\n"
                                                            + "    Time elapsed: "
                                                            + getDuration(et
                                                                    - st)));
                            s_log.println("  </summary>");
                        }
                    } else if (bmv != null) {
                        et = System.currentTimeMillis();
                        if (bmv.isValid()) {
                            System.out
                                    .println("Modify Directives File in \n"
                                            + directivesFilePath
                                            + "\n is Valid !"
                                            + "\nTime elapsed: "
                                            + getDuration(et - st));
                            s_log.println("  <summary>");
                            s_log.println("    Modify Directives File: \n    "
                                    + directivesFilePath + "\n    is Valid !"
                                    + "\n    Time elapsed: "
                                    + getDuration(et - st));
                            s_log.println("  </summary>");
                        } else {
                            System.out
                                    .println(bmv.getErrorCount()
                                            + " XML validation Errors found in Modify Directives file.\n"
                                            + "See log file for details.\n"
                                            + "Time elapsed: "
                                            + getDuration(et - st));
                            s_log.println("  <summary>");
                            s_log
                                    .println("    "
                                            + StreamUtility
                                                    .enc(bmv.getErrorCount()
                                                            + " XML validation Errors found in Modify Directives file.\n"
                                                            + "    See log file for details.\n"
                                                            + "    Time elapsed: "
                                                            + getDuration(et
                                                                    - st)));
                            s_log.println("  </summary>");
                        }
                    }
                    closeLog();
                    System.out.println("A detailed log file was created at\n"
                            + logFilePath + "\n\n");
                }
            } catch (Exception e) {
                System.out.println(e.getClass().getName()
                        + " - "
                        + (e.getMessage() == null ? "(no detail provided)" : e
                                .getMessage()));
            }
        }
    }

    /**
     * <p>
     * Convert the duration time from milliseconds to standard hours, minutes,
     * and seconds format.
     * </p>
     *
     * @param millis -
     *        The time interval to convert in miliseconds.
     * @return A string with the converted time.
     */
    private static String getDuration(long millis) {
        long tsec = millis / 1000;
        long h = tsec / 60 / 60;
        long m = (tsec - h * 60 * 60) / 60;
        long s = tsec - h * 60 * 60 - m * 60;
        StringBuffer out = new StringBuffer();
        if (h > 0) {
            out.append(h + " hour");
            if (h > 1) {
                out.append('s');
            }
        }
        if (m > 0) {
            if (h > 0) {
                out.append(", ");
            }
            out.append(m + " minute");
            if (m > 1) {
                out.append('s');
            }
        }
        if (s > 0 || h == 0 && m == 0) {
            if (h > 0 || m > 0) {
                out.append(", ");
            }
            out.append(s + " second");
            if (s != 1) {
                out.append('s');
            }
        }
        return out.toString();
    }

    /**
     * <p>
     * Initializes the log file for writing.
     * </p>
     *
     * @param outFile -
     *        The absolute file path of the log file.
     * @param rootName -
     *        The name of the root element for the xml log file.
     * @throws Exception -
     *         If any type of error occurs in trying to open the log file for
     *         writing.
     */
    private static void openLog(String outFile, String rootName)
            throws Exception {
        s_rootName = rootName;
        s_log = new PrintStream(new FileOutputStream(outFile), true, "UTF-8");
        s_log.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        s_log.println("<" + s_rootName + ">");
    }

    /**
     * <p>
     * Closes the log file.
     * </p>
     *
     * @throws Exception -
     *         If any type of error occurs in closing the log file.
     */
    private static void closeLog() throws Exception {
        s_log.println("</" + s_rootName + ">");
        s_log.close();
        s_log = null;
    }

    /**
     * <p>
     * Displays the command-line syntax.
     * </p>
     *
     * @param errMessage -
     *        The error message to be displayed.
     */
    public static void showUsage(String errMessage) {
        System.out.println("Error: " + errMessage);
        System.out.println("");
        System.out
                .println("Usage: AutoModify host:port user password "
                        + "directives-filepath log-filepath protocol [validate-only-option] [context]");
        System.out.println("Note: protocol must be either http or https.");
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");

        String logFilePath = null;
        String directivesFilePath = null;
        String protocol = null;
        String hostName = null;
        String username = null;
        String password = null;
        int portNum = 0;
        boolean isValidateOnly = true;

        try {
            if (args.length < 6 || args.length > 8) {
                AutoModify
                        .showUsage("You must provide either 6, 7 or 8 arguments.");
            } else {

                String[] hostPort = args[0].split(":");
                if (hostPort.length != 2) {
                    AutoModify
                            .showUsage("First argument must contain target"
                                    + " Fedora server hostname and port using the syntax"
                                    + " \"hostname:port\"");
                }
                hostName = hostPort[0];
                portNum = Integer.parseInt(hostPort[1]);
                username = args[1];
                password = args[2];
                directivesFilePath = args[3];
                logFilePath = args[4];
                protocol = args[5];

                if (args.length >= 7) {
                    isValidateOnly = false;
                } else {
                    isValidateOnly = true;
                }

                String context = Constants.FEDORA_DEFAULT_APP_CONTEXT;
                if (args.length == 8 && !args[7].equals("")){
                    context = args[7];
                }

                if (new File(directivesFilePath).exists()) {
                    System.out.println("\nCONNECTING to Fedora server....");

                    // ******************************************
                    // NEW: use new client utility class
                    String baseURL =
                            protocol + "://" + hostName + ":" + portNum
                                    + "/" + context;
                    FedoraClient fc =
                            new FedoraClient(baseURL, username, password);
                    APIA = fc.getAPIA();
                    APIM = fc.getAPIM();
                    //*******************************************

                    AutoModify am =
                            new AutoModify(APIA,
                                           APIM,
                                           protocol,
                                           hostName,
                                           portNum,
                                           context,
                                           username,
                                           password);

                    if (isValidateOnly) {
                        System.out
                                .println("\n----- VALIDATING DIRECTIVES FILE ONLY -----\n");
                    } else {
                        System.out
                                .println("\n----- PROCESSING DIRECTIVES FILE -----\n");
                    }
                    am.modify(directivesFilePath, logFilePath, isValidateOnly);
                } else {
                    AutoModify
                            .showUsage("Directives input file does not exist: "
                                    + directivesFilePath + " .");
                }
            }
        } catch (Exception e) {
            AutoModify.showUsage(e.getClass().getName()
                    + " - "
                    + (e.getMessage() == null ? "(no detail provided)" : e
                            .getMessage()));
        }
    }

}
