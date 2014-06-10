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

import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
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

    private final WebClientConfiguration wconfig;

    private final PoolingClientConnectionManager cManager;

    /**
     * The proxy configuration for the web client.
     */
    private final ProxyConfiguration proxy;


    public WebClient() {
        wconfig = new WebClientConfiguration();
        proxy =  new ProxyConfiguration();
        cManager = configureConnectionManager(wconfig);
    }

    public WebClient(WebClientConfiguration webconfig) {
        wconfig = webconfig;
        proxy =  new ProxyConfiguration();
        cManager = configureConnectionManager(wconfig);
    }

    public WebClient(ProxyConfiguration proxyconfig){
        wconfig = new WebClientConfiguration();
        proxy = proxyconfig;
        cManager = configureConnectionManager(wconfig);
    }

    public WebClient(WebClientConfiguration webconfig, ProxyConfiguration proxyconfig){
        wconfig = webconfig;
        proxy = proxyconfig;
        cManager = configureConnectionManager(wconfig);
    }

    private PoolingClientConnectionManager configureConnectionManager(
            WebClientConfiguration wconfig){
        logger.debug("User-Agent is '" + wconfig.getUserAgent() + "'");
        logger.debug("Max total connections is " + wconfig.getMaxTotalConn());
        logger.debug("Max connections per host is " + wconfig.getMaxConnPerHost());
        logger.debug("Connection timeout is " + wconfig.getTimeoutSecs());
        logger.debug("Socket Connection timeout is " + wconfig.getSockTimeoutSecs());
        logger.debug("Follow redirects? " + wconfig.getFollowRedirects());
        logger.debug("Max number of redirects to follow is " + wconfig.getMaxRedirects());

        PoolingClientConnectionManager cManager = new PoolingClientConnectionManager();
        cManager.setDefaultMaxPerRoute(wconfig.getMaxConnPerHost());
        cManager.setMaxTotal(wconfig.getMaxTotalConn());
        //TODO pick the ports up from configuration
        cManager.getSchemeRegistry().register(
                new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));
        cManager.getSchemeRegistry().register(
                new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));

        return cManager;
    }
    
    public void shutDown() {
        cManager.shutdown();
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

        DefaultHttpClient client;
        if (host != null && creds != null) {
            client = new PreemptiveAuth(cManager);
            client.getCredentialsProvider().setCredentials(new AuthScope(host,
                                                           AuthScope.ANY_PORT),
                                             creds);
        } else {
            client = new DefaultHttpClient(cManager);
        }
        client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, wconfig.getTimeoutSecs() * 1000);
        client.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, wconfig.getSockTimeoutSecs() * 1000);

        if (proxy.isHostProxyable(host)) {
            HttpHost proxyHost =
                new HttpHost(proxy.getProxyHost(), proxy.getProxyPort(), "http");
            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxyHost);
            if (proxy.hasValidCredentials()) {
                client.getCredentialsProvider().setCredentials(new AuthScope(proxy.getProxyHost(),
                                                           proxy.getProxyPort()),
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
        return get(url, failIfNotOK, user, pass, null, null, null);
    }

    public HttpInputStream get(String url,
            boolean failIfNotOK,
            String user,
            String pass,
            String ifNoneMatch,
            String ifModifiedSince,
            String range) throws IOException {
        UsernamePasswordCredentials creds = null;
        if (user != null && !user.isEmpty() && pass != null && !pass.isEmpty())
            creds = new UsernamePasswordCredentials(user, pass);
        return get(url, failIfNotOK, creds, ifNoneMatch, ifModifiedSince, range);
}

    public HttpInputStream head(String url, boolean failIfNotOK)
            throws IOException {
        return head(url, failIfNotOK, null);
    }

    public HttpInputStream head(String url,
            boolean failIfNotOK,
            String user,
            String pass) throws IOException {
        UsernamePasswordCredentials creds = null;
        if (user != null && !user.isEmpty() && pass != null && !pass.isEmpty())
            creds = new UsernamePasswordCredentials(user, pass);
        return head(url, failIfNotOK, creds);
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
        return execute(new HttpGet(url), url, failIfNotOK, creds, null, null, null);
    }

    public HttpInputStream get(String url,
                               boolean failIfNotOK,
                               UsernamePasswordCredentials creds,
                               String ifNoneMatch,
                               String ifModifiedSince,
                               String range)
            throws IOException {
        return execute(new HttpGet(url), url, failIfNotOK, creds, ifNoneMatch, ifModifiedSince, range);
    }

    public HttpInputStream head(String url,
                     boolean failIfNotOK,
                     UsernamePasswordCredentials creds)
            throws IOException {
        return execute(new HttpHead(url), url, failIfNotOK, creds, null, null, null);
    }

    private HttpInputStream execute(HttpUriRequest request,
                                    String url,
                                    boolean failIfNotOK,
                                    UsernamePasswordCredentials creds,
                                    String ifNoneMatch,
                                    String ifModifiedSince,
                                    String range)
            throws IOException {
        HttpClient client;

        setHeaders(request, wconfig.getUserAgent(), ifNoneMatch, ifModifiedSince, range);
        if (creds != null && creds.getUserName() != null
                && creds.getUserName().length() > 0) {
            client = getHttpClient(url, creds);
        } else {
            client = getHttpClient(url);
        }

        HttpInputStream in = new HttpInputStream(client, request);
        int status = in.getStatusCode();
        if (failIfNotOK) {
            if (status != HttpStatus.SC_OK && status != HttpStatus.SC_NOT_MODIFIED) {
                //if (followRedirects && in.getStatusCode() == 302){
                if (wconfig.getFollowRedirects() && 300 <= status && status <= 399) {
                    int count = 1;
                    while (300 <= status && status <= 399
                            && count <= wconfig.getMaxRedirects()) {
                        if (in.getResponseHeader(HttpHeaders.LOCATION) == null) {
                            in.close();
                            throw new IOException("Redirect HTTP response provided no location header.");
                        }
                        url = in.getResponseHeader(HttpHeaders.LOCATION).getValue();
                        in.close();
                        setHeaders(request, wconfig.getUserAgent(), ifNoneMatch, ifModifiedSince, range);
                        in = new HttpInputStream(client, request);
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

    private static void setHeaders(
            HttpUriRequest request,
            String ua,
            String ifNoneMatch,
            String ifModifiedSince,
            String range) {
        if (ifNoneMatch != null) {
            request.setHeader(HttpHeaders.IF_NONE_MATCH, ifNoneMatch);
        }
        if (ifModifiedSince != null) {
            request.setHeader(HttpHeaders.IF_MODIFIED_SINCE, ifModifiedSince);
        }
        if (range != null) {
            request.setHeader(HttpHeaders.RANGE, range);
        }
        if (ua != null) {
            request.setHeader(HttpHeaders.USER_AGENT, ua);
        }
    }
}
