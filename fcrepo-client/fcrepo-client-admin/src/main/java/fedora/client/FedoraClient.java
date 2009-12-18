/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.xml.rpc.ServiceException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;

import org.apache.log4j.Logger;

import org.jrdf.graph.Literal;
import org.jrdf.graph.Node;

import org.trippi.RDFFormat;
import org.trippi.TrippiException;
import org.trippi.TupleIterator;

import fedora.common.Constants;

import fedora.server.access.FedoraAPIA;
import fedora.server.management.FedoraAPIM;
import fedora.server.utilities.DateUtility;

/**
 * General-purpose utility class for Fedora clients. Provides methods to get
 * SOAP stubs for Fedora APIs. Also serves as one-stop-shopping for issuing HTTP
 * requests using Apache's HttpClient. Provides option for client to handle HTTP
 * redirects (notably 302 status that occurs with SSL auto-redirects at server.)
 *
 * @author Chris Wilper
 * @author Sandy Payette
 */
public class FedoraClient
        implements Constants {

    public static final String FEDORA_URI_PREFIX = "info:fedora/";

    /**
     * Should FedoraClient take over log4j configuration?
     * <h2>Deprecated as of Fedora 2.2</h2>
     * FedoraClient no longer takes over Log4J configuration, and setting this
     * value has no effect. Applications that use this class should configure
     * Log4J themselves, or make a log4j.xml or log4j.properties file available
     * from the runtime CLASSPATH.
     */
    @Deprecated
    public static boolean FORCE_LOG4J_CONFIGURATION = false;

    /** Seconds to wait before a connection is established. */
    public int TIMEOUT_SECONDS = 20;

    /** Seconds to wait while waiting for data over the socket (SO_TIMEOUT). */
    public int SOCKET_TIMEOUT_SECONDS = 1800; // 30 minutes

    /** Maxiumum http connections per host (for REST calls only). */
    public int MAX_CONNECTIONS_PER_HOST = 15;

    /** Maxiumum total http connections (for REST calls only). */
    public int MAX_TOTAL_CONNECTIONS = 30;

    /** Whether to automatically follow HTTP redirects. */
    public boolean FOLLOW_REDIRECTS = true;

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(FedoraClient.class.getName());

    private final SOAPEndpoint m_accessEndpoint = new SOAPEndpoint("access");

    private final SOAPEndpoint m_managementEndpoint =
            new SOAPEndpoint("management");

    private String m_baseURL;

    private final String m_user;

    private final String m_pass;

    private final AuthScope m_authScope;

    private final UsernamePasswordCredentials m_creds;

    private final MultiThreadedHttpConnectionManager m_cManager;

    private String m_serverVersion;

    /**
     * Location of Fedora's upload interface, set on first call to
     * getUploadURL().
     */
    private String m_uploadURL;

    public FedoraClient(String baseURL, String user, String pass)
            throws MalformedURLException {
        m_baseURL = baseURL;
        m_user = user;
        m_pass = pass;
        if (!baseURL.endsWith("/")) {
            m_baseURL += "/";
        }
        URL url = new URL(m_baseURL);
        m_authScope =
                new AuthScope(url.getHost(),
                              AuthScope.ANY_PORT,
                              AuthScope.ANY_REALM);
        m_creds = new UsernamePasswordCredentials(user, pass);
        m_cManager = new MultiThreadedHttpConnectionManager();
    }

    public HttpClient getHttpClient() {
        m_cManager.getParams()
                .setDefaultMaxConnectionsPerHost(MAX_CONNECTIONS_PER_HOST);
        m_cManager.getParams().setMaxTotalConnections(MAX_TOTAL_CONNECTIONS);
        m_cManager.getParams().setConnectionTimeout(TIMEOUT_SECONDS * 1000);
        m_cManager.getParams().setSoTimeout(SOCKET_TIMEOUT_SECONDS * 1000);
        HttpClient client = new HttpClient(m_cManager);
        client.getState().setCredentials(m_authScope, m_creds);
        client.getParams().setAuthenticationPreemptive(true);
        return client;
    }

    /**
     * Upload the given file to Fedora's upload interface via HTTP POST.
     *
     * @return the temporary id which can then be passed to API-M requests as a
     *         URL. It will look like uploaded://123
     */
    public String uploadFile(File file) throws IOException {
        PostMethod post = null;
        try {
            // prepare the post method
            post = new PostMethod(getUploadURL());
            post.setDoAuthentication(true);
            post.getParams().setParameter("Connection", "Keep-Alive");

            // chunked encoding is not required by the Fedora server,
            // but makes uploading very large files possible
            post.setContentChunked(true);

            // add the file part
            Part[] parts = {new FilePart("file", file)};
            post.setRequestEntity(new MultipartRequestEntity(parts, post
                    .getParams()));

            // execute and get the response
            int responseCode = getHttpClient().executeMethod(post);
            String body = null;
            try {
                body = post.getResponseBodyAsString();
            } catch (Exception e) {
                LOG.warn("Error reading response body", e);
            }
            if (body == null) {
                body = "[empty response body]";
            }
            body = body.trim();
            if (responseCode != HttpStatus.SC_CREATED) {
                throw new IOException("Upload failed: "
                        + HttpStatus.getStatusText(responseCode) + ": "
                        + replaceNewlines(body, " "));
            } else {
                return replaceNewlines(body, "");
            }
        } finally {
            if (post != null) {
                post.releaseConnection();
            }
        }
    }

    /**
     * Replace newlines with the given string.
     */
    private static String replaceNewlines(String in, String replaceWith) {
        return in.replaceAll("\r", replaceWith).replaceAll("\n", replaceWith);
    }

    /**
     * Get the URL to which API-M upload requests will be sent.
     */
    public synchronized String getUploadURL() throws IOException {
        if (m_uploadURL != null) {
            return m_uploadURL;
        } else {
            m_uploadURL = m_baseURL + "management/upload";
            if (m_uploadURL.startsWith("http:")) {
                URL redirectURL = getRedirectURL(m_uploadURL);
                if (redirectURL != null) {
                    m_uploadURL = redirectURL.toString();
                }
            }
            return m_uploadURL;
        }
    }

    /**
     * Get an HTTP resource with the response as an InputStream, given a
     * resource locator that either begins with 'info:fedora/' , 'http://', or
     * '/'. This method will follow redirects if FOLLOW_REDIRECTS is true. Note
     * that if the HTTP response has no body, the InputStream will be empty. The
     * success of a request can be checked with getResponseCode(). Usually
     * you'll want to see a 200. See
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html for other codes.
     *
     * @param locator
     *        A URL, relative Fedora URL, or Fedora URI that we want to do an
     *        HTTP GET upon
     * @param failIfNotOK
     *        boolean value indicating if an exception should be thrown if we do
     *        NOT receive an HTTP 200 response (OK)
     * @return HttpInputStream the HTTP response
     * @throws IOException
     */
    public HttpInputStream get(String locator, boolean failIfNotOK)
            throws IOException {
        return get(locator, failIfNotOK, FOLLOW_REDIRECTS);
    }

    /**
     * Get an HTTP resource with the response as an InputStream, given a URL.
     * This method will follow redirects if FOLLOW_REDIRECTS is true. Note that
     * if the HTTP response has no body, the InputStream will be empty. The
     * success of a request can be checked with getResponseCode(). Usually
     * you'll want to see a 200. See
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html for other codes.
     *
     * @param url
     *        A URL that we want to do an HTTP GET upon
     * @param failIfNotOK
     *        boolean value indicating if an exception should be thrown if we do
     *        NOT receive an HTTP 200 response (OK)
     * @return HttpInputStream the HTTP response
     * @throws IOException
     */
    public HttpInputStream get(URL url, boolean failIfNotOK) throws IOException {
        return get(url, failIfNotOK, FOLLOW_REDIRECTS);
    }

    /**
     * Get an HTTP resource with the response as an InputStream, given a
     * resource locator that either begins with 'info:fedora/' , 'http://', or
     * '/'. Note that if the HTTP response has no body, the InputStream will be
     * empty. The success of a request can be checked with getResponseCode().
     * Usually you'll want to see a 200. See
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html for other codes.
     *
     * @param locator
     *        A URL, relative Fedora URL, or Fedora URI that we want to do an
     *        HTTP GET upon
     * @param failIfNotOK
     *        boolean value indicating if an exception should be thrown if we do
     *        NOT receive an HTTP 200 response (OK)
     * @param followRedirects
     *        boolean value indicating whether HTTP redirects should be handled
     *        in this method, or be passed along so that they can be handled
     *        later.
     * @return HttpInputStream the HTTP response
     * @throws IOException
     */
    public HttpInputStream get(String locator,
                               boolean failIfNotOK,
                               boolean followRedirects) throws IOException {

        // Convert the locator to a proper Fedora URL and the do a get.
        String url = getLocatorAsURL(locator);
        return get(new URL(url), failIfNotOK, followRedirects);
    }

    /**
     * Get an HTTP resource with the response as an InputStream, given a URL.
     * Note that if the HTTP response has no body, the InputStream will be
     * empty. The success of a request can be checked with getResponseCode().
     * Usually you'll want to see a 200. See
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html for other codes.
     *
     * @param url
     *        A URL that we want to do an HTTP GET upon
     * @param failIfNotOK
     *        boolean value indicating if an exception should be thrown if we do
     *        NOT receive an HTTP 200 response (OK)
     * @param followRedirects
     *        boolean value indicating whether HTTP redirects should be handled
     *        in this method, or be passed along so that they can be handled
     *        later.
     * @return HttpInputStream the HTTP response
     * @throws IOException
     */
    public HttpInputStream get(URL url,
                               boolean failIfNotOK,
                               boolean followRedirects) throws IOException {

        String urlString = url.toString();
        LOG.debug("FedoraClient is getting " + urlString);
        HttpClient client = getHttpClient();
        GetMethod getMethod = new GetMethod(urlString);
        getMethod.setDoAuthentication(true);
        getMethod.setFollowRedirects(followRedirects);
        HttpInputStream in = new HttpInputStream(client, getMethod, urlString);
        int status = in.getStatusCode();
        if (failIfNotOK) {
            if (status != 200) {
                if (followRedirects && 300 <= status && status <= 399) {
                    // Handle the redirect here !
                    LOG
                            .debug("FedoraClient is handling redirect for HTTP STATUS="
                                    + status);
                    Header hLoc = in.getResponseHeader("location");
                    if (hLoc != null) {
                        LOG.debug("FedoraClient is trying redirect location: "
                                + hLoc.getValue());
                        // Try the redirect location, but don't try to handle another level of redirection.
                        return get(hLoc.getValue(), true, false);
                    } else {
                        try {
                            throw new IOException("Request failed [" + status
                                    + " " + in.getStatusText() + "]");
                        } finally {
                            try {
                                in.close();
                            } catch (Exception e) {
                                LOG.error("Can't close InputStream: "
                                        + e.getMessage());
                            }
                        }
                    }
                } else {
                    try {
                        throw new IOException("Request failed ["
                                + in.getStatusCode() + " " + in.getStatusText()
                                + "] : " + urlString);
                    } finally {
                        try {
                            in.close();
                        } catch (Exception e) {
                            LOG.error("Can't close InputStream: "
                                    + e.getMessage());
                        }
                    }
                }
            }
        }
        return in;
    }

    /**
     * Get an HTTP resource with the response as a String instead of an
     * InputStream, given a resource locator that either begins with
     * 'info:fedora/' , 'http://', or '/'.
     *
     * @param locator
     *        A URL, relative Fedora URL, or Fedora URI that we want to do an
     *        HTTP GET upon
     * @param failIfNotOK
     *        boolean value indicating if an exception should be thrown if we do
     *        NOT receive an HTTP 200 response (OK)
     * @param followRedirects
     *        boolean value indicating whether HTTP redirects should be handled
     *        in this method, or be passed along so that they can be handled
     *        later.
     * @return String the HTTP response as a string
     * @throws IOException
     */
    public String getResponseAsString(String locator,
                                      boolean failIfNotOK,
                                      boolean followRedirects)
            throws IOException {

        InputStream in = get(locator, failIfNotOK, followRedirects);

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
                LOG.error("Can't close InputStream: " + e.getMessage());
            }
        }
    }

    private String getLocatorAsURL(String locator) throws IOException {

        String url;
        if (locator.startsWith(FEDORA_URI_PREFIX)) {
            url =
                    m_baseURL + "get/"
                            + locator.substring(FEDORA_URI_PREFIX.length());
        } else if (locator.startsWith("http://")
                || locator.startsWith("https://")) {
            url = locator;
        } else if (locator.startsWith("/")) {
            // assume it's for something within this Fedora server
            while (locator.startsWith("/")) {
                locator = locator.substring(1);
            }
            url = m_baseURL + locator;
        } else {
            throw new IOException("Bad locator (must start with '"
                    + FEDORA_URI_PREFIX + "', 'http[s]://', or '/'");
        }
        return url;
    }

    /**
     * Get a new SOAP stub for API-A. If the baseURL for this
     * <code>FedoraClient</code> specifies "http", regular HTTP communication
     * will be attempted first. If the server redirects this client to use HTTPS
     * instead, the redirect will be followed and SSL will be used
     * automatically.
     */
    public FedoraAPIA getAPIA() throws ServiceException, IOException {
        return (FedoraAPIA) getSOAPStub(m_accessEndpoint);
    }

    public URL getAPIAEndpointURL() throws IOException {
        return m_accessEndpoint.getURL();
    }

    /**
     * Get a new SOAP stub for API-M. If the baseURL for this
     * <code>FedoraClient</code> specifies "http", regular HTTP communication
     * will be attempted first. If the server redirects this client to use HTTPS
     * instead, the redirect will be followed and SSL will be used
     * automatically.
     */
    public FedoraAPIM getAPIM() throws ServiceException, IOException {
        return (FedoraAPIM) getSOAPStub(m_managementEndpoint);
    }

    public URL getAPIMEndpointURL() throws IOException {
        return m_managementEndpoint.getURL();
    }

    /**
     * Get the appropriate API-A/M stub, given a SOAPEndpoint.
     */
    private Object getSOAPStub(SOAPEndpoint endpoint) throws ServiceException,
            IOException {

        URL url = endpoint.getURL();

        String protocol = url.getProtocol();
        String host = url.getHost();
        int port = url.getPort();
        //String path = url.getPath();
        if (port == -1) {
            port = url.getDefaultPort();
        }

        if (endpoint == m_accessEndpoint) {
            APIAStubFactory.SOCKET_TIMEOUT_SECONDS = SOCKET_TIMEOUT_SECONDS;
            return APIAStubFactory.getStubAltPath(protocol, host, port, url
                    .getPath(), m_user, m_pass);
        } else if (endpoint == m_managementEndpoint) {
            APIMStubFactory.SOCKET_TIMEOUT_SECONDS = SOCKET_TIMEOUT_SECONDS;
            return APIMStubFactory.getStubAltPath(protocol, host, port, url
                    .getPath(), m_user, m_pass);
        } else {
            throw new IllegalArgumentException("Unrecognized endpoint: "
                    + endpoint.getName());
        }
    }

    public static String getVersion() {

        ResourceBundle bundle =
                ResourceBundle.getBundle("fedora.client.resources.Client");
        return bundle.getString("version");
    }

    public static List<String> getCompatibleServerVersions() {

        ResourceBundle bundle =
                ResourceBundle.getBundle("fedora.client.resources.Client");
        List<String> list = new ArrayList<String>();

        String versions = bundle.getString("compatibleServerVersions");
        if (versions != null && versions.trim().length() > 0) {
            String[] va = versions.trim().split(" ");
            for (String element : va) {
                list.add(element);
            }
        }
        String clientVersion = getVersion();
        if (!list.contains(clientVersion)) {
            list.add(getVersion());
        }

        return list;
    }

    /**
     * Get the version reported by the remote Fedora server.
     */
    public String getServerVersion() throws IOException {
        // only do this once -- future invocations will use the known value
        if (m_serverVersion == null) {
            // Make the APIA call for describe repository
            // and make sure that HTTP 302 status is handled.
            String desc = getResponseAsString("/describe?xml=true", true, true);
            LOG.debug("describeRepository response:\n" + desc);
            String[] parts = desc.split("<repositoryVersion>");
            if (parts.length < 2) {
                throw new IOException("Could not find repositoryVersion element in content of /describe?xml=true");
            }
            int i = parts[1].indexOf("<");
            if (i == -1) {
                throw new IOException("Could not find end of repositoryVersion element in content of /describe?xml=true");
            }
            m_serverVersion = parts[1].substring(0, i).trim();
            LOG.debug("Server version is " + m_serverVersion);
        }
        return m_serverVersion;
    }

    /**
     * Return the current date as reported by the Fedora server.
     *
     * @throws IOException
     *         if the HTTP Date header is not provided by the server for any
     *         reason, or it is in the wrong format.
     */
    public Date getServerDate() throws IOException {
        HttpInputStream in = get("/describe", false, false);
        String dateString = null;
        try {
            Header header = in.getResponseHeader("Date");
            if (header == null) {
                throw new IOException("Date was not supplied in HTTP response "
                        + "header for " + m_baseURL + "describe");
            }
            dateString = header.getValue();

            // This is the date format recommended by RFC2616
            SimpleDateFormat format =
                    new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
                                         Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format.parse(dateString);

        } catch (ParseException e) {
            throw new IOException("Unparsable date (" + dateString
                    + ") in HTTP response header for " + m_baseURL + "describe");
        } finally {
            in.close();
        }
    }

    public Date getLastModifiedDate(String locator) throws IOException {
        if (locator.startsWith(FEDORA_URI_PREFIX)) {
            String query =
                    "select $date " + "from <#ri> " + "where <" + locator
                            + "> <" + VIEW.LAST_MODIFIED_DATE.uri + "> $date";
            Map<String, String> map = new HashMap<String, String>();
            map.put("lang", "itql");
            map.put("query", query);
            TupleIterator tuples = getTuples(map);
            try {
                if (tuples.hasNext()) {
                    Map<String, Node> row = tuples.next();
                    Literal dateLiteral = (Literal) row.get("date");
                    if (dateLiteral == null) {
                        throw new IOException("A row was returned, but it did not contain a 'date' binding");
                    }
                    return DateUtility.parseDateAsUTC(dateLiteral
                            .getLexicalForm());
                } else {
                    throw new IOException("No rows were returned");
                }
            } catch (TrippiException e) {
                throw new IOException(e.getMessage());
            } finally {
                try {
                    tuples.close();
                } catch (Exception e) {
                }
            }
        } else {
            HttpClient client = getHttpClient();

            HeadMethod head = new HeadMethod(locator);
            head.setDoAuthentication(true);
            head.setFollowRedirects(FOLLOW_REDIRECTS);

            try {
                int statusCode = client.executeMethod(head);
                if (statusCode != HttpStatus.SC_OK) {
                    throw new IOException("Method failed: "
                            + head.getStatusLine());
                }
                //Header[] headers = head.getResponseHeaders();

                // Retrieve just the last modified header value.
                Header header = head.getResponseHeader("last-modified");
                if (header != null) {
                    String lastModified = header.getValue();
                    return DateUtility.convertStringToDate(lastModified);
                } else {
                    // return current date time
                    return new Date();
                }
            } finally {
                head.releaseConnection();
            }
        }
    }

    public void reloadPolicies() throws IOException {

        InputStream in = null;
        try {
            in = get("/management/control?action=reloadPolicies", true, true);
        } finally {
            try {
                in.close();
            } catch (Exception e) {
                LOG.error("Can't close InputStream: " + e.getMessage());
            }
        }
    }

    /**
     * Get tuples from the remote resource index. The map contains
     * <em>String</em> values for parameters that should be passed to the
     * service. Two parameters are required: 1) lang 2) query Two parameters to
     * the risearch service are implied: 1) type = tuples 2) format = sparql See
     * http://www.fedora.info/download/2.0/userdocs/server/webservices/risearch/#app.tuples
     */
    public TupleIterator getTuples(Map<String, String> params)
            throws IOException {
        params.put("type", "tuples");
        params.put("format", RDFFormat.SPARQL.getName());
        try {
            String url = getRIQueryURL(params);
            return TupleIterator.fromStream(get(url, true, true),
                                            RDFFormat.SPARQL);
        } catch (TrippiException e) {
            throw new IOException("Error getting tuple iterator: "
                    + e.getMessage());
        }
    }

    private String getRIQueryURL(Map<String, String> params) throws IOException {
        if (params.get("type") == null) {
            throw new IOException("'type' parameter is required");
        }
        if (params.get("lang") == null) {
            throw new IOException("'lang' parameter is required");
        }
        if (params.get("query") == null) {
            throw new IOException("'query' parameter is required");
        }
        if (params.get("format") == null) {
            throw new IOException("'format' parameter is required");
        }
        return m_baseURL + "risearch?" + encodeParameters(params);
    }

    private String encodeParameters(Map<String, String> params) {
        StringBuffer encoded = new StringBuffer();
        Iterator<String> iter = params.keySet().iterator();
        int n = 0;
        while (iter.hasNext()) {
            String name = iter.next();
            if (n > 0) {
                encoded.append("&");
            }
            n++;
            encoded.append(name);
            encoded.append('=');
            try {
                encoded.append(URLEncoder.encode(params.get(name), "UTF-8"));
            } catch (UnsupportedEncodingException e) { // UTF-8 won't fail
            }
        }
        return encoded.toString();
    }

    /**
     * Ping the given endpoint to see if an HTTP 302 status code is returned. If
     * so, return the location given in the HTTP response header. If not, return
     * null.
     */
    private URL getRedirectURL(String location) throws IOException {
        HttpInputStream in = get(location, false, false);
        try {
            if (in.getStatusCode() == 302) {
                Header h = in.getResponseHeader("location");
                if (h != null) {
                    return new URL(h.getValue());
                }
            }
            return null;
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Class for storing a Fedora SOAP endpoint, which consists of an endpoint
     * name and a URL. The endpoint name is provided to the constructor. The URL
     * is determined automatically, once, based on:
     * <ul>
     * <li> The baseURL provided to the FedoraClient instance.</li>
     * <li> The server version.</li>
     * <li> Whether the server automatically redirects non-SSL SOAP requests to
     * an SSL endpoint.</li>
     * </ul>
     */
    public class SOAPEndpoint {

        String m_name;

        URL m_url;

        public SOAPEndpoint(String name) {
            m_name = name;
        }

        public String getName() {
            return m_name;
        }

        public URL getURL() throws IOException {
            // only do this once -- future invocations will use the saved value.
            if (m_url == null) {
                // The endpoint URL has not been determined yet.
                // It will be determined in the following manner:
                //
                // If the Fedora server version is 2.0:
                //   url = (baseURL)/(endpointName)/soap
                // Otherwise, the Fedora server version must be 2.1+:
                //   If (baseURL) specifies "http":
                //     Hit (baseURL)/services/(endpointName).
                //     If it redirects:
                //       url = (redirectURL)
                //     Otherwise:
                //       url = (baseURL)/services/(endpointName)
                //   Otherwise:
                //       url = (baseURL)/services/(endpointName)
                //
                if (getServerVersion().equals("2.0")) {
                    m_url = new URL(m_baseURL + m_name + "/soap");
                } else {
                    if (m_baseURL.startsWith("http:")) {
                        m_url = getRedirectURL("/services/" + m_name);
                    }
                    if (m_url == null) {
                        m_url = new URL(m_baseURL + "services/" + m_name);
                    }
                }
            }
            return m_url;
        }

    }

}
