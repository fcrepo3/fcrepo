/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.net.URI;

import java.util.Properties;

import org.apache.commons.httpclient.UsernamePasswordCredentials;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import fedora.common.Constants;
import fedora.common.http.WebClient;

import fedora.server.config.ServerConfiguration;
import fedora.server.config.ServerConfigurationParser;

public class ServerUtility {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(ServerUtility.class.getName());

    public static final String HTTP = "http";

    public static final String HTTPS = "https";

    public static final String FEDORA_SERVER_HOST = "fedoraServerHost";

    public static final String FEDORA_SERVER_PORT = "fedoraServerPort";

    public static final String FEDORA_SERVER_CONTEXT = "fedoraAppServerContext";

    public static final String FEDORA_REDIRECT_PORT = "fedoraRedirectPort";

    private static ServerConfiguration CONFIG;

    static {
        String fedoraHome = Constants.FEDORA_HOME;
        if (fedoraHome == null) {
            LOG.warn("FEDORA_HOME not set; unable to initialize");
        } else {
            File fcfgFile = new File(fedoraHome, "server/config/fedora.fcfg");
            try {
                CONFIG =
                        new ServerConfigurationParser(new FileInputStream(fcfgFile))
                                .parse();
            } catch (IOException e) {
                LOG.warn("Unable to read server configuration from "
                        + fcfgFile.getPath(), e);
            }
        }
    }

    /**
     * Tell whether the server is running by pinging it as a client.
     */
    public static boolean pingServer(String protocol, String user, String pass) {
        try {
            getServerResponse(protocol, user, pass, "/describe");
            return true;
        } catch (Exception e) {
            LOG.debug("Assuming the server isn't running because "
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
            port = CONFIG.getParameter(FEDORA_SERVER_PORT).getValue();
        } else if (protocol.equals("https")) {
            port = CONFIG.getParameter(FEDORA_REDIRECT_PORT).getValue();
        } else {
            throw new RuntimeException("Unrecogonized protocol: " + protocol);
        }
        return protocol + "://"
                + CONFIG.getParameter(FEDORA_SERVER_HOST).getValue() + ":"
                + port + "/" + CONFIG.getParameter(FEDORA_SERVER_CONTEXT).getValue();
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
        LOG.info("Getting URL: " + url);
        UsernamePasswordCredentials creds =
                new UsernamePasswordCredentials(user, pass);
        return new WebClient().getResponseAsString(url, true, creds);
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
        String fHost = CONFIG.getParameter(FEDORA_SERVER_HOST).getValue();
        String host = uri.getHost();
        if (!host.equals(fHost) && !host.equals("localhost")) {
            return false;
        }

        // path must begin with configured webapp context
        String path = uri.getPath();
        String fedoraContext = CONFIG.getParameter(
                FEDORA_SERVER_CONTEXT).getValue();
        if (!path.startsWith("/" + fedoraContext + "/")) {
            return false;
        }

        // port specification must match http or https port as appropriate
        String httpPort = CONFIG.getParameter(FEDORA_SERVER_PORT).getValue();
        String httpsPort = CONFIG.getParameter(FEDORA_REDIRECT_PORT).getValue();
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

    /**
     * Initializes logging to use Log4J and to send WARN messages to STDOUT for
     * command-line use.
     */
    private static void initLogging() {
        // send all log4j output to STDOUT and configure levels
        Properties props = new Properties();
        props.setProperty("log4j.appender.STDOUT",
                          "org.apache.log4j.ConsoleAppender");
        props.setProperty("log4j.appender.STDOUT.layout",
                          "org.apache.log4j.PatternLayout");
        props.setProperty("log4j.appender.STDOUT.layout.ConversionPattern",
                          "%p: %m%n");
        props.setProperty("log4j.rootLogger", "WARN, STDOUT");
        PropertyConfigurator.configure(props);

        // tell commons-logging to use Log4J
        final String pfx = "org.apache.commons.logging.";
        System.setProperty(pfx + "LogFactory", pfx + "impl.Log4jFactory");
        System.setProperty(pfx + "Log", pfx + "impl.Log4JLogger");
    }

    /**
     * Command-line entry point to reload policies. Takes 3 args: protocol user
     * pass
     */
    public static void main(String[] args) {
        initLogging();
        if (args.length == 3) {
            try {
                reloadPolicies(args[0], args[1], args[2]);
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
    }

}
