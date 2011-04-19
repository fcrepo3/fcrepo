/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage;

import java.io.File;

import java.net.URI;
import java.net.URL;

import java.util.Hashtable;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.httpclient.Header;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.http.HttpInputStream;
import org.fcrepo.common.http.WebClient;
import org.fcrepo.common.http.WebClientConfiguration;

import org.fcrepo.server.Module;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.HttpServiceNotFoundException;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.security.Authorization;
import org.fcrepo.server.security.BackendPolicies;
import org.fcrepo.server.security.BackendSecurity;
import org.fcrepo.server.security.BackendSecuritySpec;
import org.fcrepo.server.storage.types.MIMETypedStream;
import org.fcrepo.server.storage.types.Property;
import org.fcrepo.server.utilities.ServerUtility;


/**
 * Provides a mechanism to obtain external HTTP-accessible content.
 *
 * @author Ross Wayland
 * @version $Id$
 *
 */
public class DefaultExternalContentManager
        extends Module
        implements ExternalContentManager {

    private static final Logger logger =
            LoggerFactory.getLogger(DefaultExternalContentManager.class);

    private static final String DEFAULT_MIMETYPE="text/plain";
    private String m_userAgent;

    private String fedoraServerHost;

    private String fedoraServerPort;

    private String fedoraServerRedirectPort;

    private WebClientConfiguration m_httpconfig;

    private WebClient m_http;

    /**
     * Creates a new DefaultExternalContentManager.
     *
     * @param moduleParameters
     *        The name/value pair map of module parameters.
     * @param server
     *        The server instance.
     * @param role
     *        The module role name.
     * @throws ModuleInitializationException
     *         If initialization values are invalid or initialization fails for
     *         some other reason.
     */
    public DefaultExternalContentManager(Map<String, String> moduleParameters,
                                         Server server,
                                         String role)
            throws ModuleInitializationException {
        super(moduleParameters, server, role);
    }

    /**
     * Initializes the Module based on configuration parameters. The
     * implementation of this method is dependent on the schema used to define
     * the parameter names for the role of
     * <code>org.fcrepo.server.storage.DefaultExternalContentManager</code>.
     *
     * @throws ModuleInitializationException
     *         If initialization values are invalid or initialization fails for
     *         some other reason.
     */
    @Override
    public void initModule() throws ModuleInitializationException {
        try {
            Server s_server = getServer();
            m_userAgent = getParameter("userAgent");
            if (m_userAgent == null) {
                m_userAgent = "Fedora";
            }

            fedoraServerPort = s_server.getParameter("fedoraServerPort");
            fedoraServerHost = s_server.getParameter("fedoraServerHost");
            fedoraServerRedirectPort =
                    s_server.getParameter("fedoraRedirectPort");

            m_httpconfig = s_server.getWebClientConfig();
            if (m_httpconfig.getUserAgent() == null ) {
                m_httpconfig.setUserAgent(m_userAgent);
            }

            m_http = new WebClient(m_httpconfig);

        } catch (Throwable th) {
            throw new ModuleInitializationException("[DefaultExternalContentManager] "
                                                            + "An external content manager "
                                                            + "could not be instantiated. The underlying error was a "
                                                            + th.getClass()
                                                                    .getName()
                                                            + "The message was \""
                                                            + th.getMessage()
                                                            + "\".",
                                                    getRole());
        }
    }

    /*
     * Retrieves the external content.
     * Currently the protocols <code>file</code> and
     * <code>http[s]</code> are supported.
     *
     * @see
     * org.fcrepo.server.storage.ExternalContentManager#getExternalContent(fedora
     * .server.storage.ContentManagerParams)
     */
    public MIMETypedStream getExternalContent(ContentManagerParams params)
            throws GeneralException, HttpServiceNotFoundException{
        logger.debug("in getExternalContent(), url=" + params.getUrl());
        try {
            if(params.getProtocol().equals("file")){
                return getFromFilesystem(params);
            }
            if (params.getProtocol().equals("http") || params.getProtocol().equals("https")){
                return getFromWeb(params);
            }
            throw new GeneralException("protocol for retrieval of external content not supported. URL: " + params.getUrl());
        } catch (Exception ex) {
            // catch anything but generalexception
            ex.printStackTrace();
            throw new HttpServiceNotFoundException("[" + this.getClass().getSimpleName() + "] "
                    + "returned an error.  The underlying error was a "
                    + ex.getClass().getName()
                    + "  The message "
                    + "was  \""
                    + ex.getMessage() + "\"  .  ",ex);
        }
    }

    /**
     * Get a MIMETypedStream for the given URL. If user or password are
     * <code>null</code>, basic authentication will not be attempted.
     */
    private MIMETypedStream get(String url, String user, String pass, String knownMimeType)
            throws GeneralException {
        logger.debug("DefaultExternalContentManager.get(" + url + ")");
        try {
            HttpInputStream response = m_http.get(url, true, user, pass);
            String mimeType =
                    response.getResponseHeaderValue("Content-Type",
                                                    knownMimeType);
            long length = Long.parseLong(response.getResponseHeaderValue("Content-Length","-1"));
            Property[] headerArray =
                    toPropertyArray(response.getResponseHeaders());
            if (mimeType == null || mimeType.equals("")) {
                mimeType = DEFAULT_MIMETYPE;
            }
            return new MIMETypedStream(mimeType, response, headerArray, length);
        } catch (Exception e) {
            throw new GeneralException("Error getting " + url, e);
        }
    }

    /**
     * Convert the given HTTP <code>Headers</code> to an array of
     * <code>Property</code> objects.
     */
    private static Property[] toPropertyArray(Header[] headers) {

        Property[] props = new Property[headers.length];
        for (int i = 0; i < headers.length; i++) {
            props[i] = new Property();
            props[i].name = headers[i].getName();
            props[i].value = headers[i].getValue();
        }
        return props;
    }

   /* *//**
     * Creates a property array out of the MIME type and the length of the
     * provided file.
     *
     * @param file
     *            the file containing the content.
     * @return an array of properties containing content-length and
     *         content-type.
     *//*
    private static Property[] getPropertyArray(File file, String mimeType) {
         Property[] props = new Property[2];
         Property clen = new Property("Content-Length",Long.toString(file.length()));
         Property ctype = new Property("Content-Type", mimeType);
         props[0] = clen;
         props[1] = ctype;
         return props;
    }*/

    /**
     * Get a MIMETypedStream for the given URL. If user or password are
     * <code>null</code>, basic authentication will not be attempted.
     *
     * @param params
     * @return
     * @throws HttpServiceNotFoundException
     * @throws GeneralException
     */
    private MIMETypedStream getFromFilesystem(ContentManagerParams params)
            throws HttpServiceNotFoundException,GeneralException {
        logger.debug("in getFile(), url=" + params.getUrl());

        try {
            URL fileUrl = new URL(params.getUrl());
            File cFile = new File(fileUrl.toURI()).getCanonicalFile();
            // security check
            URI cURI = cFile.toURI();
            logger.info("Checking resolution security on " + cURI);
            Authorization authModule = (Authorization) getServer().getModule(
            "org.fcrepo.server.security.Authorization");
            if (authModule == null) {
                throw new GeneralException(
                "Missing required Authorization module");
            }
            authModule.enforceRetrieveFile(params.getContext(), cURI.toString());
            // end security check
            String mimeType = params.getMimeType();

            // if mimeType was not given, try to determine it automatically
            if (mimeType == null || mimeType.equalsIgnoreCase("")){
                mimeType = determineMimeType(cFile);
            }
            return new MIMETypedStream(mimeType,fileUrl.openStream(),null,cFile.length());
        }
        catch(AuthzException ae){
            logger.error(ae.getMessage(),ae);
            throw new HttpServiceNotFoundException("Policy blocked datastream resolution",ae);
        }
        catch (GeneralException me) {
            logger.error(me.getMessage(),me);
            throw me;
        } catch (Throwable th) {
            th.printStackTrace(System.err);
            // catch anything but generalexception
            logger.error(th.getMessage(),th);
             throw new HttpServiceNotFoundException("[FileExternalContentManager] "
                    + "returned an error.  The underlying error was a "
                    + th.getClass().getName()
                    + "  The message "
                    + "was  \""
                    + th.getMessage() + "\"  .  ",th);
        }
    }

    /**
     * Retrieves external content via http or https.
     *
     * @return A MIMETypedStream
     * @throws ModuleInitializationException
     * @throws GeneralException
     */
    private MIMETypedStream getFromWeb(ContentManagerParams params)
            throws ModuleInitializationException, GeneralException {
           String username = params.getUsername();
        String password = params.getPassword();
        boolean backendSSL = false;
        String url = params.getUrl();

        if (ServerUtility.isURLFedoraServer(url) && !params.isBypassBackend()) {
            BackendSecuritySpec m_beSS;
            BackendSecurity m_beSecurity =
                    (BackendSecurity) getServer()
                            .getModule("org.fcrepo.server.security.BackendSecurity");
            try {
                m_beSS = m_beSecurity.getBackendSecuritySpec();
            } catch (Exception e) {
                throw new ModuleInitializationException(
                        "Can't intitialize BackendSecurity module (in default access) from Server.getModule",
                        getRole());
            }
            Hashtable<String, String> beHash =
                    m_beSS.getSecuritySpec(BackendPolicies.FEDORA_INTERNAL_CALL);
            username = beHash.get("callUsername");
            password = beHash.get("callPassword");
            backendSSL =
                    new Boolean(beHash.get("callSSL"))
                            .booleanValue();
            if (backendSSL) {
                if (params.getProtocol().equals("http")) {
                    url = url.replaceFirst("http:", "https:");
                }
                url =
                        url.replaceFirst(":" + fedoraServerPort + "/",
                                            ":" + fedoraServerRedirectPort
                                                    + "/");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("************************* backendUsername: "
                        + username + "     backendPassword: "
                        + password + "     backendSSL: " + backendSSL);
                logger.debug("************************* doAuthnGetURL: " + url);
            }

        }
        return get(url, username, password, params.getMimeType());
    }

/**
     * Determines the mime type of a given file
     *
     * @param file for which the mime type needs to be detected
     * @return the detected mime type
     */
    private String determineMimeType(File file){
        String mimeType = new MimetypesFileTypeMap().getContentType(file);
        // if mimeType detection failed, fall back to the default
        if (mimeType == null || mimeType.equalsIgnoreCase("")){
            mimeType = DEFAULT_MIMETYPE;
        }
        return mimeType;
    }
}

