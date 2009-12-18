/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.localservices.saxon;

import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.FeatureKeys;
import net.sf.saxon.value.StringValue;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * A service that transforms a supplied input document using a supplied
 * stylesheet, with stylesheet caching. Adapted from the SaxonServlet.java
 * example file contained in the source distribution of "The SAXON XSLT
 * Processor from Michael Kay".
 * 
 * <pre>
 * -----------------------------------------------------------------------------
 * The original code is Copyright &copy; 2001 by Michael Kay. All rights
 * reserved. The current project homepage for Saxon may be found at:
 * <a href="http://saxon.sourceforge.net/">http://saxon.sourceforge.net/</a>.
 *
 * Portions created for the Fedora Repository System are Copyright &copy; 2002-2007
 * by The Rector and Visitors of the University of Virginia and Cornell
 * University. All rights reserved.
 * -----------------------------------------------------------------------------
 * </pre>
 * 
 * @author Michael Kay
 * @author Ross Wayland
 * @author Chris Wilper
 * @version $Id$
 */
public class SaxonServlet
        extends HttpServlet {

    private static final long serialVersionUID = 2L;

    /** time to wait for getting data via http before giving up */
    public final int TIMEOUT_SECONDS = 10;

    /** start string for a servlet config parameter name that gives creds */
    private final String CRED_PARAM_START = "credentials for ";

    /** urlString-to-Templates map of cached stylesheets */
    private Map<String, Templates> m_cache;

    /** pathString-to-Credentials map of configured credentials */
    private Map<String, UsernamePasswordCredentials> m_creds;

    /** provider of http connections */
    private MultiThreadedHttpConnectionManager m_cManager;

    /**
     * Initialize the servlet by setting up the stylesheet cache, the http
     * connection manager, and configuring credentials for the http client.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        m_cache = new HashMap<String, Templates>();
        m_creds = new HashMap<String, UsernamePasswordCredentials>();
        m_cManager = new MultiThreadedHttpConnectionManager();
        m_cManager.getParams().setConnectionTimeout(TIMEOUT_SECONDS * 1000);

        Enumeration<?> enm = config.getInitParameterNames();
        while (enm.hasMoreElements()) {
            String name = (String) enm.nextElement();
            if (name.startsWith(CRED_PARAM_START)) {
                String value = config.getInitParameter(name);
                if (value.indexOf(":") == -1) {
                    throw new ServletException("Malformed credentials for "
                            + name + " -- expected ':' user/pass delimiter");
                }
                String[] parts = value.split(":");
                String user = parts[0];
                StringBuffer pass = new StringBuffer();
                for (int i = 1; i < parts.length; i++) {
                    if (i > 1) {
                        pass.append(':');
                    }
                    pass.append(parts[i]);
                }
                m_creds.put(name.substring(CRED_PARAM_START.length()),
                            new UsernamePasswordCredentials(user, pass
                                    .toString()));
            }
        }
    }

    /**
     * Accept a GET request and produce a response. HTTP Request Parameters:
     * <ul>
     * <li>source - URL of source document</li>
     * <li>style - URL of stylesheet</li>
     * <li>clear-stylesheet-cache - if set to yes, empties the cache before
     * running.
     * </ul>
     * 
     * @param req
     *        The HTTP request
     * @param res
     *        The HTTP response
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        String source = req.getParameter("source");
        String style = req.getParameter("style");
        String clear = req.getParameter("clear-stylesheet-cache");

        if (clear != null && clear.equals("yes")) {
            synchronized (m_cache) {
                m_cache = new HashMap<String, Templates>();
            }
        }

        try {
            apply(style, source, req, res);
        } catch (Exception e) {
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
                    .getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Accept an POST request and produce a response (same behavior as GET).
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        doGet(req, res);
    }

    @Override
    public String getServletInfo() {
        return "Calls SAXON to apply a stylesheet to a source document";
    }

    /**
     * Apply stylesheet to source document
     */
    private void apply(String style,
                       String source,
                       HttpServletRequest req,
                       HttpServletResponse res) throws Exception {

        // Validate parameters
        if (style == null) {
            throw new TransformerException("No style parameter supplied");
        }
        if (source == null) {
            throw new TransformerException("No source parameter supplied");
        }

        InputStream sourceStream = null;
        try {
            // Load the stylesheet (adding to cache if necessary)
            Templates pss = tryCache(style);
            Transformer transformer = pss.newTransformer();

            Enumeration<?> p = req.getParameterNames();
            while (p.hasMoreElements()) {
                String name = (String) p.nextElement();
                if (!(name.equals("style") || name.equals("source"))) {
                    String value = req.getParameter(name);
                    transformer.setParameter(name, new StringValue(value));
                }
            }

            // Start loading the document to be transformed
            sourceStream = getInputStream(source);

            // Set the appropriate output mime type
            String mime =
                    pss.getOutputProperties()
                            .getProperty(OutputKeys.MEDIA_TYPE);
            if (mime == null) {
                res.setContentType("text/html");
            } else {
                res.setContentType(mime);
            }

            // Transform
            StreamSource ss = new StreamSource(sourceStream);
            ss.setSystemId(source);
            transformer.transform(ss, new StreamResult(res.getOutputStream()));

        } finally {
            if (sourceStream != null) {
                try {
                    sourceStream.close();
                } catch (Exception e) {
                }
            }
        }

    }

    /**
     * Maintain prepared stylesheets in memory for reuse
     */
    private Templates tryCache(String url) throws Exception {
        Templates x = (Templates) m_cache.get(url);
        if (x == null) {
            synchronized (m_cache) {
                if (!m_cache.containsKey(url)) {
                    TransformerFactory factory = TransformerFactory.newInstance();
                    if (factory.getClass().getName().equals("net.sf.saxon.TransformerFactoryImpl")) {
                        factory.setAttribute(FeatureKeys.VERSION_WARNING, Boolean.FALSE);
                    }
                    StreamSource ss = new StreamSource(getInputStream(url));
                    ss.setSystemId(url);
                    x = factory.newTemplates(ss);
                    m_cache.put(url, x);
                }
            }
        }
        return x;
    }

    /**
     * Get the content at the given location using the configured credentials
     * (if any).
     */
    private InputStream getInputStream(String url) throws Exception {
        GetMethod getMethod = new GetMethod(url);
        HttpClient client = new HttpClient(m_cManager);
        UsernamePasswordCredentials creds = getCreds(url);
        if (creds != null) {
            client.getState().setCredentials(AuthScope.ANY, creds);
            client.getParams().setAuthenticationPreemptive(true);
            getMethod.setDoAuthentication(true);
        }
        getMethod.setFollowRedirects(true);
        HttpInputStream in = new HttpInputStream(client, getMethod, url);
        if (in.getStatusCode() != 200) {
            try {
                in.close();
            } catch (Exception e) {
            }
            throw new IOException("HTTP request failed.  Got status code "
                    + in.getStatusCode()
                    + " from remote server while attempting to GET " + url);
        } else {
            return in;
        }
    }

    /**
     * Return the credentials for the realmPath that most closely matches the
     * given url, or null if none found.
     */
    private UsernamePasswordCredentials getCreds(String url) throws Exception {
        url = normalizeURL(url);
        url = url.substring(url.indexOf("/") + 2);

        UsernamePasswordCredentials longestMatch = null;
        int longestMatchLength = 0;

        Iterator<String> iter = m_creds.keySet().iterator();
        while (iter.hasNext()) {
            String realmPath = (String) iter.next();
            if (url.startsWith(realmPath)) {
                int matchLength = realmPath.length();
                if (matchLength > longestMatchLength) {
                    longestMatchLength = matchLength;
                    longestMatch =
                            (UsernamePasswordCredentials) m_creds
                                    .get(realmPath);
                }
            }
        }
        return longestMatch;
    }

    /**
     * Return a URL string in which the port is always specified.
     */
    private static String normalizeURL(String urlString)
            throws MalformedURLException {
        URL url = new URL(urlString);
        if (url.getPort() == -1) {
            return url.getProtocol() + "://" + url.getHost() + ":"
                    + url.getDefaultPort() + url.getFile()
                    + (url.getRef() != null ? "#" + url.getRef() : "");
        } else {
            return urlString;
        }
    }

}
