/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.httpclient.Header;
import org.apache.log4j.Logger;

import fedora.common.http.HttpInputStream;
import fedora.common.http.WebClient;
import fedora.server.Context;
import fedora.server.Module;
import fedora.server.ReadOnlyContext;
import fedora.server.Server;
import fedora.server.errors.GeneralException;
import fedora.server.errors.HttpServiceNotFoundException;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.authorization.AuthzException;
import fedora.server.security.Authorization;
import fedora.server.security.BackendPolicies;
import fedora.server.security.BackendSecurity;
import fedora.server.security.BackendSecuritySpec;
import fedora.server.storage.types.MIMETypedStream;
import fedora.server.storage.types.Property;
import fedora.server.utilities.ServerUtility;

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

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(DefaultExternalContentManager.class.getName());

    private static final String DEFAULT_MIMETYPE="text/plain";
    private String m_userAgent;

    private String fedoraServerHost;

    private String fedoraServerPort;

    private String fedoraServerRedirectPort;

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
     * <code>fedora.server.storage.DefaultExternalContentManager</code>.
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

            m_http = new WebClient();
            m_http.USER_AGENT = m_userAgent;

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
     * fedora.server.storage.ExternalContentManager#getExternalContent(fedora
     * .server.storage.ContentManagerParams)
     */
    public MIMETypedStream getExternalContent(ContentManagerParams params)
            throws GeneralException, HttpServiceNotFoundException{
        LOG.debug("in getExternalContent(), url=" + params.getUrl());
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
        LOG.debug("DefaultExternalContentManager.get(" + url + ")");
        try {
            HttpInputStream response = m_http.get(url, true, user, pass);
            String mimeType =
                    response.getResponseHeaderValue("Content-Type",
                                                    knownMimeType);
            Property[] headerArray =
                    toPropertyArray(response.getResponseHeaders());
            return new MIMETypedStream(mimeType, response, headerArray);
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

    /**
     * Creates a property array out of the MIME type and the length of the
     * provided file.
     * 
     * @param file
     *            the file containing the content.
     * @return an array of properties containing content-length and
     *         content-type.
     */
    private static Property[] getPropertyArray(File file, String mimeType) {
         Property[] props = new Property[2];
         Property clen = new Property("Content-Length",Long.toString(file.length()));
         Property ctype = new Property("Content-Type", mimeType);
         props[0] = clen;
         props[1] = ctype;
         return props;
    }
        
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
        LOG.debug("in getFile(), url=" + params.getUrl());

        try {
            URL fileUrl = new URL(params.getUrl());
            File cFile = new File(fileUrl.toURI()).getCanonicalFile();
            // security check
            URI cURI = cFile.toURI();
            LOG.info("Checking resolution security on " + cURI);
            Authorization authModule = (Authorization) getServer().getModule(
            "fedora.server.security.Authorization");
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
            return new MIMETypedStream(mimeType,fileUrl.openStream(),getPropertyArray(cFile,mimeType));
        }
        catch(AuthzException ae){
            LOG.error(ae.getMessage(),ae); 
            throw new HttpServiceNotFoundException("Policy blocked datastream resolution",ae);
        }
        catch (GeneralException me) {
            LOG.error(me.getMessage(),me); 
            throw me;
        } catch (Throwable th) {
            th.printStackTrace(System.err);
            // catch anything but generalexception
            LOG.error(th.getMessage(),th);
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
     * @param url
     *            The url pointing to the content.
     * @param context
     *            The Map containing parameters.
     * @param mimeType
     *            The default MIME type to be used in case no MIME type can be
     *            detected.
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
                            .getModule("fedora.server.security.BackendSecurity");
            try {
                m_beSS = m_beSecurity.getBackendSecuritySpec();
            } catch (Exception e) {
                throw new ModuleInitializationException(
                        "Can't intitialize BackendSecurity module (in default access) from Server.getModule",
                        getRole());
            }
            Hashtable<String, String> beHash =
                    m_beSS.getSecuritySpec(BackendPolicies.FEDORA_INTERNAL_CALL);
            username = (String) beHash.get("callUsername");
            password = (String) beHash.get("callPassword");
            backendSSL =
                    new Boolean((String) beHash.get("callSSL"))
                            .booleanValue();
            if (backendSSL) {
                if (params.getProtocol().equals("http:")) {
                    url = url.replaceFirst("http:", "https:");
                }
                url =
                        url.replaceFirst(":" + fedoraServerPort + "/",
                                            ":" + fedoraServerRedirectPort
                                                    + "/");
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("************************* backendUsername: "
                        + username + "     backendPassword: "
                        + password + "     backendSSL: " + backendSSL);
                LOG.debug("************************* doAuthnGetURL: " + url);
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

