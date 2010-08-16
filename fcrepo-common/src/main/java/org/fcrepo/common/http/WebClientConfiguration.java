/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.common.http;

/**
 * Configuration for the http connection settings of the web client.
 *
 * @version $Id$
 */
public class WebClientConfiguration {

    /** Seconds to wait before a connection is established. */
    private int timeout_secs = 20;

    /** Seconds to wait while waiting for data over the socket (SO_TIMEOUT). */
    private int sock_timeout_secs = 120;

    /** Maximum http connections per host */
    private int max_conn_per_host = 5;

    /** Maximum total http connections */
    private int max_total_conn = 20;

    /** Whether to automatically follow HTTP redirects. */
    private boolean follow_redirects = true;

    /**
     * Maximum number of redirects to follow per request if FOLLOW_REDIRECTS is
     * true.
     */
    private int max_redirects = 3;

    /**
     * What the "User-Agent" request header should say. Default is null, which
     * indicates that the header should not be provided.
     */
    private String user_agent = null;

    /**
     * Default constructor.
     */
    public WebClientConfiguration() {
        super();
    }

    /**
     * Constructor to set web client connection properties.
     *
     * @param timeout_secs Seconds to wait before a connection is established
     * @param sock_timeout_secs Seconds to wait while waiting for data over the socket (SO_TIMEOUT)
     * @param max_conn_per_host Maximum number of http connections per host
     * @param max_total_conn Maximum total number of http connections
     * @param follow_redirects Whether to automatically follow HTTP redirects
     * @param max_redirects Maximum number of redirects to follow per request if FOLLOW_REDIRECTS is true
     * @param user_agent Value of the "User-Agent" request header
     */
    public WebClientConfiguration(int timeout_secs,
                                  int sock_timeout_secs,
                                  int max_conn_per_host,
                                  int max_total_conn,
                                  boolean follow_redirects,
                                  int max_redirects,
                                  String user_agent) {
        this.timeout_secs = timeout_secs;
        this.sock_timeout_secs = sock_timeout_secs;
        this.max_conn_per_host = max_conn_per_host;
        this.max_total_conn = max_total_conn;
        this.follow_redirects = follow_redirects;
        this.max_redirects = max_redirects;
        this.user_agent = user_agent;
    }

    /**
     * Gets the timeout seconds.
     * @return the number of seconds to wait for a connection before timing out.
     */
    public int getTimeoutSecs() {
        return this.timeout_secs;
    }

    /**
     * Sets the number of seconds to wait for a connection before timing out..
     */
    public void setTimeoutSecs(int timeout_secs) {
        this.timeout_secs = timeout_secs;
    }

    /**
     * Gets the socket timeout seconds.
     * @return the number of seconds to wait for data on a socket before timing out.
     */
    public int getSockTimeoutSecs() {
        return this.sock_timeout_secs;
    }

    /**
     * Sets the number of seconds to wait for data on a socket before timing out.
     */
    public void setSockTimeoutSecs(int sock_timeout_secs) {
        this.sock_timeout_secs = sock_timeout_secs;
    }

    /**
     * Gets the maximum number of client connections per host.
     * @return the maximum number of client connections per host.
     */
    public int getMaxConnPerHost() {
        return this.max_conn_per_host;
    }

    /**
     * Sets the maximum number of client connections per host.
     */
    public void setMaxConnPerHost(int max_conn_per_host) {
        this.max_conn_per_host = max_conn_per_host;
    }

    /**
     * Gets the maximum number of total http connections.
     * @return the maximum number of total http connections.
     */
    public int getMaxTotalConn() {
        return this.max_total_conn;
    }

    /**
     * Sets the maximum number of total http connections.
     */
    public void setMaxTotalConn(int max_total_conn) {
        this.max_total_conn = max_total_conn;
    }

    /**
     * Flag to indicate whether redirects should be followed.
     * @return the flag indicating if redirects should be followed.
     */
    public boolean getFollowRedirects() {
        return this.follow_redirects;
    }

    /**
     * Sets the the flag indicating if redirects should be followed.
     */
    public void setFollowRedirects(boolean follow_redirects) {
        this.follow_redirects = follow_redirects;
    }

    /**
     * Gets the maximum number of redirects to follow, if set to follow redirects.
     * @return the maximum number of redirects to follow
     */
    public int getMaxRedirects() {
        return this.max_redirects;
    }

    /**
     * Sets the maximum number of redirects to follow, if set to follow redirects.
     */
    public void setMaxRedirects(int max_redirects) {
        this.max_redirects = max_redirects;
    }

    /**
     * Gets the value to be used for the User-Agent request header.
     * @return the User-Agent string.
     */
    public String getUserAgent() {
        return this.user_agent;
    }

    /**
     * Sets the value to be used for the User-Agent request header.
     */
    public void setUserAgent(String user_agent) {
        this.user_agent = user_agent;
    }

}
