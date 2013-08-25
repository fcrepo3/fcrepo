/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.apache.http.auth.UsernamePasswordCredentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;
import org.fcrepo.common.http.WebClient;
import org.fcrepo.common.http.WebClientConfiguration;
import org.fcrepo.server.config.Parameter;
import org.fcrepo.server.config.ServerConfiguration;
import org.fcrepo.server.config.ServerConfigurationParser;



public class ServerUtility {

    private static final Logger logger =
            LoggerFactory.getLogger(ServerUtility.class);

    public static final String HTTP = "http";

    public static final String HTTPS = "https";

    public static final String FEDORA_SERVER_HOST = "fedoraServerHost";

    public static final String FEDORA_SERVER_PORT = "fedoraServerPort";

    public static final String FEDORA_SERVER_CONTEXT = "fedoraAppServerContext";

    public static final String FEDORA_REDIRECT_PORT = "fedoraRedirectPort";

    private static ServerConfiguration CONFIG =
        getServerConfig();
    
    private static WebClient s_webClient =
            getWebClient();

    private static ServerConfiguration getServerConfig() {
        String fedoraHome = Constants.FEDORA_HOME;
        if (fedoraHome == null) {
            logger.warn("FEDORA_HOME not set; unable to initialize");
        } else {
            File fcfgFile = new File(fedoraHome, "server/config/fedora.fcfg");
            try {
                return new ServerConfigurationParser(new FileInputStream(fcfgFile))
                                .parse();
            } catch (IOException e) {
                logger.warn("Unable to read server configuration from "
                        + fcfgFile.getPath(), e);
            }
        }
        return null;
    }
    
    private static WebClient getWebClient() {
        WebClientConfiguration webconfig = new WebClientConfiguration();
        initWebClientConfig(webconfig);
        return new WebClient(webconfig);
    }

    /**
     * Tell whether the server is running by pinging it as a client.
     */
    public static boolean pingServer(String protocol, String user, String pass) {
        try {
            getServerResponse(protocol, user, pass, "/describe");
            return true;
        } catch (Exception e) {
            logger.debug("Assuming the server isn't running because "
                    + "describe request failed", e);
            return false;
        }
    }

    /**
     * Get the baseURL of the Fedora server from the host and port configured.
     * It will look like http://localhost:8080/fedora
     */
    public static String getBaseURL(String protocol) {
        String port;
        if (protocol.equals("http")) {
            port = CONFIG.getParameter(FEDORA_SERVER_PORT,Parameter.class).getValue();
        } else if (protocol.equals("https")) {
            port = CONFIG.getParameter(FEDORA_REDIRECT_PORT,Parameter.class).getValue();
        } else {
            throw new RuntimeException("Unrecogonized protocol: " + protocol);
        }
        return protocol + "://"
                + CONFIG.getParameter(FEDORA_SERVER_HOST,Parameter.class).getValue() + ":"
                + port + "/" + CONFIG.getParameter(FEDORA_SERVER_CONTEXT,Parameter.class).getValue();
    }

    /**
     * Signals for the server to reload its policies.
     */
    public static void reloadPolicies(String protocol, String user, String pass)
            throws IOException {
        getServerResponse(protocol,
                          user,
                          pass,
                          "/management/control?action=reloadPolicies");
    }

    /**
     * Hits the given Fedora Server URL and returns the response as a String.
     * Throws an IOException if the response code is not 200(OK).
     */
    private static String getServerResponse(String protocol,
                                            String user,
                                            String pass,
                                            String path) throws IOException {
        String url = getBaseURL(protocol) + path;
        logger.info("Getting URL: {}", url);
        UsernamePasswordCredentials creds =
                new UsernamePasswordCredentials(user, pass);
        return s_webClient.getResponseAsString(url, true, creds);
    }

    /**
     * Hits the given Fedora Server URL and returns the response as a String.
     * Throws an IOException if the response code is not 200(OK).
     */
    private static InputStream getServerResponseAsStream(String protocol,
                                            String user,
                                            String pass,
                                            String path) throws IOException {
        String url = getBaseURL(protocol) + path;
        logger.info("Getting URL: {}", url);
        UsernamePasswordCredentials creds =
                new UsernamePasswordCredentials(user, pass);
        return s_webClient.get(url, true, creds);
    }


    /**
     * Tell whether the given URL appears to be referring to somewhere within
     * the Fedora webapp.
     */
    public static boolean isURLFedoraServer(String url) {

        // scheme must be http or https
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if (!scheme.equals("http") && !scheme.equals("https")) {
            return false;
        }

        // host must be configured hostname or localhost
        String fHost = CONFIG.getParameter(FEDORA_SERVER_HOST,Parameter.class).getValue();
        String host = uri.getHost();
        if (!host.equals(fHost) && !host.equals("localhost")) {
            return false;
        }

        // path must begin with configured webapp context
        String path = uri.getPath();
        String fedoraContext = CONFIG.getParameter(
                FEDORA_SERVER_CONTEXT,Parameter.class).getValue();
        if (!path.startsWith("/" + fedoraContext + "/")) {
            return false;
        }

        // port specification must match http or https port as appropriate
        String httpPort = CONFIG.getParameter(FEDORA_SERVER_PORT,Parameter.class).getValue();
        String httpsPort = CONFIG.getParameter(FEDORA_REDIRECT_PORT,Parameter.class).getValue();
        if (uri.getPort() == -1) {
            // unspecified, so fedoraPort must be 80 (http), or 443 (https)
            if (scheme.equals("http")) {
                return httpPort.equals("80");
            } else {
                return httpsPort.equals("443");
            }
        } else {
            // specified, so must match appropriate http or https port
            String port = "" + uri.getPort();
            if (scheme.equals("http")) {
                return port.equals(httpPort);
            } else {
                return port.equals(httpsPort);
            }
        }

    }


    public static InputStream modifyDatastreamControlGroup(String protocol, String user, String pass, String pidspec, String datastreamspec, String controlGroup, boolean addXMLHeader, boolean reformat, boolean setMIMETypeCharset) throws IOException {

        String path = "/management/control?action=modifyDatastreamControlGroup" +
                        "&pid=" + pidspec +
                        "&dsID=" + datastreamspec +
                        "&controlGroup=" + controlGroup +
                        "&addXMLHeader=" + addXMLHeader +
                        "&reformat=" + reformat +
                        "&setMIMETypeCharset=" + setMIMETypeCharset;

        return getServerResponseAsStream(protocol,
                          user,
                          pass,
                          path);


    }

    /**
     * Initializes the web client http connection settings.
     */
    private static void initWebClientConfig(WebClientConfiguration wconf) {

        if (CONFIG.getParameter("httpClientTimeoutSecs") != null)
            wconf.setTimeoutSecs(Integer.parseInt(CONFIG.getParameter("httpClientTimeoutSecs")));

        if (CONFIG.getParameter("httpClientSocketTimeoutSecs") != null)
            wconf.setSockTimeoutSecs(Integer.parseInt(CONFIG.getParameter("httpClientSocketTimeoutSecs")));

        if (CONFIG.getParameter("httpClientMaxConnectionsPerHost") != null)
            wconf.setMaxConnPerHost(Integer.parseInt(CONFIG.getParameter("httpClientMaxConnectionsPerHost")));

        if (CONFIG.getParameter("httpClientMaxTotalConnections") != null)
            wconf.setMaxTotalConn(Integer.parseInt(CONFIG.getParameter("httpClientMaxTotalConnections")));

        if (CONFIG.getParameter("httpClientFollowRedirects") != null)
            wconf.setFollowRedirects(Boolean.parseBoolean(CONFIG.getParameter("httpClientFollowRedirects")));

        if (CONFIG.getParameter("httpClientMaxFollowRedirects") != null)
            wconf.setMaxRedirects(Integer.parseInt(CONFIG.getParameter("httpClientMaxFollowRedirects")));

        if (CONFIG.getParameter("httpClientUserAgent") != null)
            wconf.setUserAgent(CONFIG.getParameter("httpClientUserAgent"));
    }

    /**
     * Command-line entry point to reload policies. Takes 3 args: protocol user
     * pass
     */
    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Parameters:  method arg1 arg2 arg3 etc");
            System.out.println("");
            System.out.println("Methods:");
            System.out.println("    reloadpolicies");
            System.out.println("    migratedatastreamcontrolgroup");
            System.exit(0);
        }

        String method = args[0].toLowerCase();

        if (method.equals("reloadpolicies")) {
            if (args.length == 4) {
            try {
                    reloadPolicies(args[1], args[2], args[3]);
                System.out.println("SUCCESS: Policies have been reloaded");
                System.exit(0);
            } catch (Throwable th) {
                th.printStackTrace();
                System.err
                        .println("ERROR: Reloading policies failed; see above");
                System.exit(1);
            }
        } else {
            System.err.println("ERROR: Three arguments required: "
                    + "http|https username password");
            System.exit(1);
        }

        } else if (method.equals("migratedatastreamcontrolgroup")) {

            // too many args
            if (args.length > 10) {
                System.err.println("ERROR: too many arguments provided");
                System.exit(1);
    }

            // too few
            if (args.length < 7) {

                System.err.println("ERROR: insufficient arguments provided.  Arguments are: ");
                System.err.println("    protocol [http|https]"); // 1; 0 is method
                System.err.println("    user"); // 2
                System.err.println("    password"); // 3
                System.err.println("    pid - either"); // 4
                System.err.println("        a single pid, eg demo:object");
                System.err.println("        list of pids separated by commas, eg demo:object1,demo:object2");
                System.err.println("        name of file containing pids, eg file:///path/to/file");
                System.err.println("    dsid - either"); // 5
                System.err.println("        a single datastream id, eg DC");
                System.err.println("        list of ids separated by commas, eg DC,RELS-EXT");
                System.err.println("    controlGroup - target control group (note only M is implemented)"); // 6
                System.err.println("    addXMLHeader - add an XML header to the datastream [true|false, default false]"); // 7
                System.err.println("    reformat - reformat the XML [true|false, default false]"); // 8
                System.err.println("    setMIMETypeCharset - add charset=UTF-8 to the MIMEType [true|false, default false]"); // 9
                System.exit(1);
            }


            try {
                // optional args
                boolean addXMLHeader = getArgBoolean(args, 7, false);
                boolean reformat = getArgBoolean(args, 8, false);
                boolean setMIMETypeCharset = getArgBoolean(args, 9, false);;

                InputStream is = modifyDatastreamControlGroup(args[1], args[2], args[3], args[4], args[5], args[6], addXMLHeader, reformat, setMIMETypeCharset);

                IOUtils.copy(is, System.out);
                is.close();

                System.out.println("SUCCESS: Datastreams modified");
                System.exit(0);

            } catch (Throwable th) {
                th.printStackTrace();
                System.err
                .println("ERROR: migrating datastream control group failed; see above");
                System.exit(1);
            }


        } else {
            System.err.println("ERROR: unrecognised method " + method);
            System.exit(1);
        }
    }
    /**
     * Get boolean argument from list of arguments at position; use defaultValue if not present
     * @param args
     * @param position
     * @param defaultValue
     * @return
     */
    private static boolean getArgBoolean(String[] args, int position, boolean defaultValue) {
        if (args.length > position) {
            String lowerArg = args[position].toLowerCase();
            if (lowerArg.equals("true") || lowerArg.equals("yes")) {
                return true;
            } else if (lowerArg.equals("false") || lowerArg.equals("no")) {
                return false;
            } else {
                throw new IllegalArgumentException(args[position] + " not a valid value.  Specify true or false");
            }
        } else {
            return defaultValue;
        }
    }

}
