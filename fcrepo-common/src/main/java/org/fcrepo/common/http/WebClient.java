/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.common.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A general-purpose, connection-pooling HTTP Client. All methods are
 * thread-safe. Provides option for client to handle HTTP redirects
 *
 * @author Chris Wilper, Scott Prater
 * @version $Id$
 */
public class WebClient {

    private static final Logger logger =
        LoggerFactory.getLogger(WebClient.class);

    private static WebClientConfiguration wconfig= new WebClientConfiguration();

    private MultiThreadedHttpConnectionManager m_cManager;

    /**
     * The proxy configuration for the web client.
     */
    private static ProxyConfiguration proxy = new ProxyConfiguration();


    public WebClient() {
        configureConnectionManager();
    }

    public WebClient(WebClientConfiguration webconfig) {
        wconfig = webconfig;
        configureConnectionManager();
    }

    public WebClient(ProxyConfiguration proxyconfig){
        proxy = proxyconfig;
        configureConnectionManager();
    }

    public WebClient(WebClientConfiguration webconfig, ProxyConfiguration proxyconfig){
        wconfig = webconfig;
        proxy = proxyconfig;
        configureConnectionManager();
    }

    private void configureConnectionManager(){
        logger.debug("User-Agent is '" + wconfig.getUserAgent() + "'");
        logger.debug("Max total connections is " + wconfig.getMaxTotalConn());
        logger.debug("Max connections per host is " + wconfig.getMaxConnPerHost());
        logger.debug("Connection timeout is " + wconfig.getTimeoutSecs());
        logger.debug("Socket Connection timeout is " + wconfig.getSockTimeoutSecs());
        logger.debug("Follow redirects? " + wconfig.getFollowRedirects());
        logger.debug("Max number of redirects to follow is " + wconfig.getMaxRedirects());

        m_cManager = new MultiThreadedHttpConnectionManager();
        m_cManager.getParams().setDefaultMaxConnectionsPerHost(wconfig.getMaxConnPerHost());
        m_cManager.getParams().setMaxTotalConnections(wconfig.getMaxTotalConn());
        m_cManager.getParams().setConnectionTimeout(wconfig.getTimeoutSecs() * 1000);
        m_cManager.getParams().setSoTimeout(wconfig.getSockTimeoutSecs() * 1000);
    }

    public HttpClient getHttpClient(String hostOrUrl) throws IOException, ConnectTimeoutException {
        return getHttpClient(hostOrUrl, null);
    }

    public HttpClient getHttpClient(String hostOrURL,
                                    UsernamePasswordCredentials creds)
            throws IOException, ConnectTimeoutException {

        String host = null;

        if (hostOrURL != null) {
            if (hostOrURL.indexOf("/") != -1) {
                URL url = new URL(hostOrURL);
                host = url.getHost();
            } else {
                host = hostOrURL;
            }
        }

        HttpClient client = new HttpClient(m_cManager);
        if (host != null && creds != null) {
            client.getState().setCredentials(new AuthScope(host,
                                                           AuthScope.ANY_PORT),
                                             creds);
            client.getParams().setAuthenticationPreemptive(true);
        }

        if (proxy.isHostProxyable(host)) {
            client.getHostConfiguration().setProxy(proxy.getProxyHost(),
                                                   proxy.getProxyPort());
            if (proxy.hasValidCredentials()) {
                client.getState().setProxyCredentials(new AuthScope(proxy.getProxyHost(),
                                                           proxy.getProxyPort(),
                                                           null),
                                                      new UsernamePasswordCredentials(proxy.getProxyUser(),
                                                                             proxy.getProxyPassword()));
            }
        }
        return client;
    }


    public HttpInputStream get(String url, boolean failIfNotOK)
            throws IOException {
        return get(url, failIfNotOK, null);
    }

    public HttpInputStream get(String url,
                               boolean failIfNotOK,
                               String user,
                               String pass) throws IOException {
        UsernamePasswordCredentials creds = null;
        if (user != null && !user.equals("") && pass != null && !pass.equals(""))
            creds = new UsernamePasswordCredentials(user, pass);
        return get(url, failIfNotOK, creds);
    }

    /**
     * Get an HTTP resource with the response as an InputStream, given a URL. If
     * FOLLOW_REDIRECTS is true, up to MAX_REDIRECTS redirects will be followed.
     * Note that if credentials are provided, for security reasons they will
     * only be provided to the FIRST url in a chain of redirects. Note that if
     * the HTTP response has no body, the InputStream will be empty. The success
     * of a request can be checked with getResponseCode(). Usually you'll want
     * to see a 200. See http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html
     * for other codes.
     *
     * @param url
     *        A URL that we want to do an HTTP GET upon
     * @param failIfNotOK
     *        boolean value indicating if an exception should be thrown if we do
     *        NOT receive an HTTP 200 response (OK)
     * @return HttpInputStream the HTTP response
     * @throws IOException
     */
    public HttpInputStream get(String url,
                               boolean failIfNotOK,
                               UsernamePasswordCredentials creds)
            throws IOException {

        HttpClient client;
        GetMethod getMethod = new GetMethod(url);

        if (wconfig.getUserAgent() != null) {
            getMethod.setRequestHeader("User-Agent", wconfig.getUserAgent());
        }
        if (creds != null && creds.getUserName() != null
                && creds.getUserName().length() > 0) {
            client = getHttpClient(url, creds);
            getMethod.setDoAuthentication(true);
        } else {
            client = getHttpClient(url);
        }

        HttpInputStream in = new HttpInputStream(client, getMethod, url);
        int status = in.getStatusCode();
        if (failIfNotOK) {
            if (status != 200) {
                //if (followRedirects && in.getStatusCode() == 302){
                if (wconfig.getFollowRedirects() && 300 <= status && status <= 399) {
                    int count = 1;
                    while (300 <= status && status <= 399
                            && count <= wconfig.getMaxRedirects()) {
                        if (in.getResponseHeader("location") == null) {
                            throw new IOException("Redirect HTTP response provided no location header.");
                        }
                        url = in.getResponseHeader("location").getValue();
                        in.close();
                        getMethod = new GetMethod(url);
                        if (wconfig.getUserAgent() != null) {
                            getMethod
                                    .setRequestHeader("User-Agent", wconfig.getUserAgent());
                        }
                        in = new HttpInputStream(client, getMethod, url);
                        status = in.getStatusCode();
                        count++;
                    }
                    if (300 <= status && status <= 399) {
                        in.close();
                        throw new IOException("Too many redirects");
                    } else if (status != 200) {
                        in.close();
                        throw new IOException("Request failed ["
                                + in.getStatusCode() + " " + in.getStatusText()
                                + "]");
                    }
                    // redirect was successful!
                } else {
                    try {
                        throw new IOException("Request failed ["
                                + in.getStatusCode() + " " + in.getStatusText()
                                + "]");
                    } finally {
                        try {
                            in.close();
                        } catch (Exception e) {
                            logger.error("Can't close InputStream: "
                                    + e.getMessage());
                        }
                    }
                }
            }
        }
        return in;
    }

    public String getResponseAsString(String url, boolean failIfNotOK)
            throws IOException {
        return getResponseAsString(url, failIfNotOK, null);
    }

    public String getResponseAsString(String url,
                                      boolean failIfNotOK,
                                      UsernamePasswordCredentials creds)
            throws IOException {

        InputStream in = get(url, failIfNotOK, creds);

        // Convert the response into a String.
        try {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(in));
            StringBuffer buffer = new StringBuffer();
            String line = reader.readLine();
            while (line != null) {
                buffer.append(line + "\n");
                line = reader.readLine();
            }
            return buffer.toString();
        } finally {
            try {
                in.close();
            } catch (Exception e) {
                logger.error("Can't close InputStream: " + e.getMessage());
            }
        }
    }

}
