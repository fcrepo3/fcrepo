/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.http;

import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * Configures the proxy settings of the web client. Can either be created using
 * the system settings or with a custom configuration.
 * 
 * @version $Id$
 */
public class ProxyConfiguration {

    private String proxyHost;

    private int proxyPort;

    private String proxyUser;

    private String proxyPassword;

    private Pattern nonProxyPattern;

    private final Logger log = Logger.getLogger(this.getClass());

    /**
     * Default constructor. 
     * Takes the system provided proxy settings.
     */
    public ProxyConfiguration() {
        this.proxyHost = System.getProperty("http.proxyHost");
        this.proxyPort =
                Integer.parseInt(System.getProperty("http.proxyPort", "80"));
        this.proxyUser = System.getProperty("http.proxyUser");
        this.proxyPassword = System.getProperty("http.proxyPassword");
        this.nonProxyPattern = createNonProxyPattern(System.getProperty("http.nonProxyHosts"));
    }

    /**
     * Enables the creation of a proxy configuration using
     * custom values.
     * 
     * @param proxyHost the host name of the proxy
     * @param proxyPort the port of the proxy
     * @param proxyUser the username for the proxy
     * @param proxyPassword the password for the proxy
     * @param nonProxyHosts
     */
    public ProxyConfiguration(String proxyHost,
                              int proxyPort,
                              String proxyUser,
                              String proxyPassword,
                              String nonProxyHosts) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
        this.nonProxyPattern = createNonProxyPattern(nonProxyHosts);
    }

    /**
     * Mimics <a href=
     * "http://java.sun.com/javase/6/docs/technotes/guides/net/proxies.html">
     * the java nonproxyhost syntax</a> and turns the nonproxyhost expression
     * into a regex pattern. Basic idea taken from the HttpImpl class of the
     * Liferay Portal software.
     * 
     * @param nonProxyHosts
     *        the String that contains the http.nonProxyHosts value. If this
     *        value is empty or invalid, null is returned. If the value is
     *        valid, a pattern is retured.
     * @return the Pattern or null.
     */
    protected Pattern createNonProxyPattern(String nonProxyHosts) {
        if (nonProxyHosts == null || nonProxyHosts.equals("")) return null;

        // "*.fedora-commons.org" -> ".*?\.fedora-commons\.org" 
        nonProxyHosts = nonProxyHosts.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*?");

        // a|b|*.c -> (a)|(b)|(.*?\.c)
        nonProxyHosts = "(" + nonProxyHosts.replaceAll("\\|", ")|(") + ")";

        try {
            return Pattern.compile(nonProxyHosts);

            //we don't want to bring down the whole server by misusing the nonProxy pattern
            //therefore the error is logged and the web client moves on.
        } catch (Exception e) {
            log
                    .error("Creating the nonProxyHosts pattern failed for http.nonProxyHosts="
                            + nonProxyHosts
                            + " with the following exception: "
                            + e);
            return null;
        }
    }

    /**
     * Gets the proxy hostname.
     * @return the proxy hostname
     */
    public String getProxyHost() {
        return proxyHost;
    }

    /**
     * Gets the port of the proxy.
     * @return the proxy port
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /** 
     * Returns the proxy user.
     * @return the proxy user
     */
    public String getProxyUser() {
        return proxyUser;
    }

    /**
     * Gets the password for the proxy.
     * @return the password for the proxy
     */
    public String getProxyPassword() {
        return proxyPassword;
    }

    /**
     * Checks whether a proxy has been configured and the given host is not in
     * the nonProxyHost list or the nonProxyList is empty.
     * 
     * @param host
     *        the host to be matched
     * @return true if the host satifies the above stated condition, otherwise
     *         false.
     */
    public boolean isHostProxyable(String host) {
        return getProxyHost() != null
                && getProxyPort() > 0
                && (nonProxyPattern == null || !nonProxyPattern.matcher(host)
                        .matches());
    }

    /**
     * Checks whether the proxy credentials are valid. 
     * Username and password must be non-null and non-empty to qualify.
     * 
     * @return true if the credentials are valid, false otherwise
     */
    public boolean hasValidCredentials() {
        return getProxyUser() != null && !getProxyUser().equals("")
                && getProxyPassword() != null && !getProxyPassword().equals("");
    }

}
