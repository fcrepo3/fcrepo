/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import java.util.Map;

import org.apache.commons.httpclient.UsernamePasswordCredentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.http.HttpInputStream;
import org.fcrepo.common.http.WebClient;
import org.fcrepo.common.http.WebClientConfiguration;

import org.fcrepo.server.Context;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.types.DigitalObject;



/**
 * DefaultDOManager extension that updates a GSearch (Fedora Generic Search)
 * service as object changes are committed.
 * <p>
 * To use, simply change fedora.fcfg, replacing "DefaultDOManager" with
 * "GSearchDOManager", and add the following xml param elements:
 * </p>
 * <p>
 * Required:
 * <ul>
 * <li> &lt;param name="gSearchRESTURL"
 * value="http://localhost:8080/fedoragsearch/rest"/&gt;</li>
 * </ul>
 * </p>
 * <p>
 * Optional (only needed if basic auth is required for GSearch REST access):
 * <ul>
 * <li> &lt;param name="gSearchUsername" value="exampleUsername"/&gt;</li>
 * <li> &lt;param name="gSearchPassword" value="examplePassword"/&gt;</li>
 * </ul>
 * </p>
 *
 * @author Chris Wilper
 */
public class GSearchDOManager
        extends DefaultDOManager {

    private static final Logger logger =
            LoggerFactory.getLogger(GSearchDOManager.class);

    /** Required param: URL of GSearch REST interface. */
    public static final String GSEARCH_REST_URL = "gSearchRESTURL";

    /** Optional param: User to authenticate to GSearch as. */
    public static final String GSEARCH_USERNAME = "gSearchUsername";

    /** Optional param: Password to use for GSearch authentication. */
    public static final String GSEARCH_PASSWORD = "gSearchPassword";

    /** Configured value for GSEARCH_REST_URL parameter. */
    private String _gSearchRESTURL;

    /** Credentials we'll use for GSearch authentication, if enabled. */
    private UsernamePasswordCredentials _gSearchCredentials;

    /** HTPP client configuration */
    private WebClientConfiguration _webClientConfig;

    /** HTTP client we'll use for sending GSearch update signals. */
    private WebClient _webClient;

    /**
     * Delegates construction to the superclass.
     */
    public GSearchDOManager(Map moduleParameters, Server server, String role)
            throws ModuleInitializationException {
        super(moduleParameters, server, role);
    }

    //
    // Overrides of DefaultDOManager methods
    //

    /**
     * Performs superclass post-initialization, then completes initialization
     * using GSearch-specific parameters.
     */
    @Override
    public void postInitModule() throws ModuleInitializationException {

        super.postInitModule();

        // validate required param: GSEARCH_REST_URL
        _gSearchRESTURL = getParameter(GSEARCH_REST_URL);
        if (_gSearchRESTURL == null) {
            throw new ModuleInitializationException("Required parameter, "
                    + GSEARCH_REST_URL + " was not specified", getRole());
        } else {
            try {
                new URL(_gSearchRESTURL);
                logger.debug("Configured GSearch REST URL: " + _gSearchRESTURL);
            } catch (MalformedURLException e) {
                throw new ModuleInitializationException("Malformed URL given "
                        + "for " + GSEARCH_REST_URL + " parameter: "
                        + _gSearchRESTURL, getRole());
            }
        }

        // validate credentials: if GSEARCH_USERNAME is given, GSEARCH_PASSWORD
        // should also be.
        String user = getParameter(GSEARCH_USERNAME);
        if (user != null) {
            logger.debug("Will authenticate to GSearch service as user: " + user);
            String pass = getParameter(GSEARCH_PASSWORD);
            if (pass != null) {
                _gSearchCredentials =
                        new UsernamePasswordCredentials(user, pass);
            } else {
                throw new ModuleInitializationException(GSEARCH_PASSWORD
                        + " must be specified because " + GSEARCH_USERNAME
                        + " was specified", getRole());
            }
        } else {
            logger.debug(GSEARCH_USERNAME + " unspecified; will not attempt "
                    + "to authenticate to GSearch service");
        }

        // finally, init the http client we'll use
        _webClientConfig = getServer().getWebClientConfig();
        _webClient = new WebClient(_webClientConfig);
    }

    /**
     * Commits the changes to the given object as usual, then attempts to
     * propagate the change to the GSearch service.
     */
    @Override
    public void doCommit(boolean cachedObjectRequired,
                         Context context,
                         DigitalObject obj,
                         String logMessage,
                         boolean remove) throws ServerException {

        super.doCommit(cachedObjectRequired, context, obj, logMessage, remove);

        // determine the url we need to invoke
        StringBuffer url = new StringBuffer();
        url.append(_gSearchRESTURL + "?operation=updateIndex");
        String pid = obj.getPid();
        url.append("&value=" + urlEncode(pid));
        if (remove) {
            logger.info("Signaling removal of " + pid + " to GSearch");
            url.append("&action=deletePid");
        } else {
            if (logger.isInfoEnabled()) {
                if (obj.isNew()) {
                    logger.info("Signaling add of " + pid + " to GSearch");
                } else {
                    logger.info("Signaling mod of " + pid + " to GSearch");
                }
            }
            url.append("&action=fromPid");
        }

        // send the signal
        sendRESTMessage(url.toString());
    }

    //
    // Private utility methods
    //

    /**
     * Performs the given HTTP request, logging a warning if we don't get a 200
     * OK response.
     */
    private void sendRESTMessage(String url) {
        HttpInputStream response = null;
        try {
            logger.debug("Getting " + url);
            response = _webClient.get(url, false, _gSearchCredentials);
            int code = response.getStatusCode();
            if (code != 200) {
                logger.warn("Error sending update to GSearch service (url=" + url
                        + ").  HTTP response code was " + code + ". "
                        + "Body of response from GSearch follows:\n"
                        + getString(response));
            }
        } catch (Exception e) {
            logger.warn("Error sending update to GSearch service via URL: " + url,
                     e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (Exception e) {
                    logger.warn("Error closing GSearch response", e);
                }
            }
        }
    }

    /**
     * Read the remainder of the given stream as a String and return it, or an
     * error message if we encounter an error.
     */
    private static String getString(InputStream in) {
        try {
            StringBuffer out = new StringBuffer();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(in));
            String line = reader.readLine();
            while (line != null) {
                out.append(line + "\n");
                line = reader.readLine();
            }
            return out.toString();
        } catch (Exception e) {
            return "[Error reading response body: " + e.getClass().getName()
                    + ": " + e.getMessage() + "]";
        }
    }

    /**
     * URL-encode the given string using UTF-8 encoding.
     */
    private static final String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            logger.warn("Failed to encode '" + s + "'", e);
            return s;
        }
    }

}
