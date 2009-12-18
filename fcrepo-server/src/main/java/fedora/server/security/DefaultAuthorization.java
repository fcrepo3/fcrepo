/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;

import fedora.common.Constants;

import fedora.server.Context;
import fedora.server.Module;
import fedora.server.MultiValueMap;
import fedora.server.Server;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.authorization.AuthzException;
import fedora.server.errors.authorization.AuthzOperationalException;
import fedora.server.storage.DOManager;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.status.ServerState;
import fedora.server.validation.ValidationUtility;

import fedora.utilities.XmlTransformUtility;

/**
 * The Authorization module, protecting access to Fedora's API-A and API-M
 * endpoints.
 *
 * The following attributes are available for use in authorization policies
 * during any enforce call.
 * </p>
 * <p>
 * subject attributes
 * <ul>
 * <li>urn:fedora:names:fedora:2.1:subject:loginId (available only if user
 * has authenticated)</li>
 * <li>urn:fedora:names:fedora:2.1:subject:<i>x</i> (available if
 * authenticated user has attribute <i>x</i>)</li>
 * </ul>
 * </p>
 * <p>
 * environment attributes derived from HTTP request
 * <ul>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:security
 * <ul>
 * <li>==
 * urn:fedora:names:fedora:2.1:environment:httpRequest:security-secure(i.e.,
 * request is HTTPS/SSL)</li>
 * <li>==
 * urn:fedora:names:fedora:2.1:environment:httpRequest:security-insecure(i.e.,
 * request is HTTP/non-SSL)</li>
 * </ul>
 * </li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:messageProtocol
 * <ul>
 * <li>==
 * urn:fedora:names:fedora:2.1:environment:httpRequest:messageProtocol-soap(i.e.,
 * request is over SOAP/Axis)</li>
 * <li>==
 * urn:fedora:names:fedora:2.1:environment:httpRequest:messageProtocol-rest(i.e.,
 * request is over non-SOAP/Axis ("REST") HTTP call)</li>
 * </ul>
 * </li>
 * </ul>
 * </p>
 * <p>
 * environment attributes directly from HTTP request
 * <ul>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:authType</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:clientFqdn</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:clientIpAddress</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:contentLength</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:contentType</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:method</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:protocol</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:scheme</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:serverFqdn</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:serverIpAddress</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:serverPort</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:sessionEncoding</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:sessionStatus</li>
 * </ul>
 * </p>
 * <p>
 * other environment attributes
 * <ul>
 * <li>urn:fedora:names:fedora:2.1:currentDateTime</li>
 * <li>urn:fedora:names:fedora:2.1:currentDate</li>
 * <li>urn:fedora:names:fedora:2.1:currentTime</li>
 * </ul>
 * </p>
 *
 * @see <a
 *      href="http://java.sun.com/products/servlet/2.2/javadoc/javax/servlet/http/HttpServletRequest.html">HttpServletRequest
 *      interface documentation</a>
 */
public class DefaultAuthorization
        extends Module
        implements Authorization {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(DefaultAuthorization.class.getName());

    private static final String XACML_DIST_BASE = "fedora-internal-use";

    private static final String DEFAULT_REPOSITORY_POLICIES_DIRECTORY =
            XACML_DIST_BASE
                    + "/fedora-internal-use-repository-policies-approximating-2.0";

    private static final String BE_SECURITY_XML_LOCATION =
            "config/beSecurity.xml";

    private static final String BACKEND_POLICIES_ACTIVE_DIRECTORY =
            XACML_DIST_BASE + "/fedora-internal-use-backend-service-policies";

    private static final String BACKEND_POLICIES_XSL_LOCATION =
            XACML_DIST_BASE + "/build-backend-policy.xsl";

    private static final String REPOSITORY_POLICIES_DIRECTORY_KEY =
            "REPOSITORY-POLICIES-DIRECTORY";

    private static final String REPOSITORY_POLICY_GUITOOL_DIRECTORY_KEY =
            "REPOSITORY-POLICY-GUITOOL-POLICIES-DIRECTORY";

    private static final String COMBINING_ALGORITHM_KEY = "XACML-COMBINING-ALGORITHM";

    private static final String ENFORCE_MODE_KEY = "ENFORCE-MODE";

    private static final String POLICY_SCHEMA_PATH_KEY = "POLICY-SCHEMA-PATH";

    private static final String VALIDATE_REPOSITORY_POLICIES_KEY =
            "VALIDATE-REPOSITORY-POLICIES";

    private static final String VALIDATE_OBJECT_POLICIES_FROM_DATASTREAM_KEY =
            "VALIDATE-OBJECT-POLICIES-FROM-DATASTREAM";

    private static final String OWNER_ID_SEPARATOR_KEY = "OWNER-ID-SEPARATOR";

    private final PolicyParser m_policyParser;

    private PolicyEnforcementPoint xacmlPep;

    private String repositoryPoliciesActiveDirectory = "";

    private String repositoryPolicyGuitoolDirectory = "";

    private String combiningAlgorithm = "";

    private String enforceMode = "";

    private String ownerIdSeparator = ",";

    boolean enforceListObjectInFieldSearchResults = true;

    boolean enforceListObjectInResourceIndexResults = true;

    /**
     * Creates and initializes the Access Module. When the server is starting
     * up, this is invoked as part of the initialization process.
     *
     * @param moduleParameters
     *        A pre-loaded Map of name-value pairs comprising the intended
     *        configuration of this Module.
     * @param server
     *        The <code>Server</code> instance.
     * @param role
     *        The role this module fulfills, a java class name.
     * @throws ModuleInitializationException
     *         If initilization values are invalid or initialization fails for
     *         some other reason.
     */
    public DefaultAuthorization(Map moduleParameters, Server server, String role)
            throws ModuleInitializationException {
        super(moduleParameters, server, role);
        String serverHome = null;
        try {
            serverHome =
                    server.getHomeDir().getCanonicalPath() + File.separator;
        } catch (IOException e1) {
            throw new ModuleInitializationException("couldn't get server home",
                                                    role,
                                                    e1);
        }

        if (moduleParameters.containsKey(REPOSITORY_POLICIES_DIRECTORY_KEY)) {
            repositoryPoliciesActiveDirectory =
                    getParameter(REPOSITORY_POLICIES_DIRECTORY_KEY, true);
        }
        if (moduleParameters
                .containsKey(REPOSITORY_POLICY_GUITOOL_DIRECTORY_KEY)) {
            repositoryPolicyGuitoolDirectory =
                    getParameter(REPOSITORY_POLICY_GUITOOL_DIRECTORY_KEY, true);
        }
        if (moduleParameters.containsKey(COMBINING_ALGORITHM_KEY)) {
            combiningAlgorithm =
                    (String) moduleParameters.get(COMBINING_ALGORITHM_KEY);
        }
        if (moduleParameters.containsKey(ENFORCE_MODE_KEY)) {
            enforceMode = (String) moduleParameters.get(ENFORCE_MODE_KEY);
        }
        if (moduleParameters.containsKey(OWNER_ID_SEPARATOR_KEY)) {
            ownerIdSeparator =
                    (String) moduleParameters.get(OWNER_ID_SEPARATOR_KEY);
            LOG.debug("ownerIdSeparator is [" + ownerIdSeparator + "]");
        }

        // Initialize the policy parser given the POLICY_SCHEMA_PATH_KEY
        if (moduleParameters.containsKey(POLICY_SCHEMA_PATH_KEY)) {
            String schemaPath =
                    (((String) moduleParameters.get(POLICY_SCHEMA_PATH_KEY))
                            .startsWith(File.separator) ? "" : serverHome)
                            + (String) moduleParameters
                                    .get(POLICY_SCHEMA_PATH_KEY);
            try {
                FileInputStream in = new FileInputStream(schemaPath);
                m_policyParser = new PolicyParser(in);
                ValidationUtility.setPolicyParser(m_policyParser);
            } catch (Exception e) {
                throw new ModuleInitializationException("Error loading policy"
                        + " schema: " + schemaPath, role, e);
            }
        } else {
            throw new ModuleInitializationException("Policy schema path not"
                    + " specified.  Must be given as " + POLICY_SCHEMA_PATH_KEY,
                    role);
        }

        if (moduleParameters.containsKey(VALIDATE_REPOSITORY_POLICIES_KEY)) {
            validateRepositoryPolicies =
                    (new Boolean((String) moduleParameters
                            .get(VALIDATE_REPOSITORY_POLICIES_KEY)))
                            .booleanValue();
        }
        if (moduleParameters
                .containsKey(VALIDATE_OBJECT_POLICIES_FROM_DATASTREAM_KEY)) {
            try {
                validateObjectPoliciesFromDatastream =
                        Boolean.parseBoolean((String) moduleParameters
                               .get(VALIDATE_OBJECT_POLICIES_FROM_DATASTREAM_KEY));
            } catch (Exception e) {
                throw new ModuleInitializationException("bad init parm boolean value for "
                                                                + VALIDATE_OBJECT_POLICIES_FROM_DATASTREAM_KEY,
                                                        role,
                                                        e);
            }
        }
    }

    @Override
    public void initModule() throws ModuleInitializationException {
    }

    private boolean validateRepositoryPolicies = false;

    private boolean validateObjectPoliciesFromDatastream = false;

    private static boolean mkdir(String dirPath) {
        boolean createdOnThisCall = false;
        File directory = new File(dirPath);
        if (!directory.exists()) {
            directory.mkdirs();
            createdOnThisCall = true;
        }
        return createdOnThisCall;
    }

    private static final int BUFFERSIZE = 4096;

    private static void filecopy(String srcPath, String destPath)
            throws Exception {
        File srcFile = new File(srcPath);
        FileInputStream fis = new FileInputStream(srcFile);
        File destFile = new File(destPath);
        try {
            destFile.createNewFile();
        } catch (Exception e) {
        }
        FileOutputStream fos = new FileOutputStream(destFile);
        byte[] buffer = new byte[BUFFERSIZE];
        boolean reading = true;
        while (reading) {
            int bytesRead = fis.read(buffer);
            if (bytesRead > 0) {
                fos.write(buffer, 0, bytesRead);
            }
            reading = bytesRead > -1;
        }
        fis.close();
        fos.close();
    }

    private static void dircopy(String srcPath, String destPath)
            throws Exception {
        File srcDir = new File(srcPath);
        String[] paths = srcDir.list();
        for (String element : paths) {
            String absSrcPath = srcPath + File.separator + element;
            String absDestPath = destPath + File.separator + element;
            filecopy(absSrcPath, absDestPath);
        }
    }

    private static void deldirfiles(String path) throws Exception {
        File srcDir = new File(path);
        if (srcDir.exists()) {
            String[] paths = srcDir.list();
            for (String element : paths) {
                String absPath = path + File.separator + element;
                File f = new File(absPath);
                f.delete();
            }
        } else {
            srcDir.mkdirs();
        }
    }

    private final void generateBackendPolicies() throws Exception {
        String fedoraHome =
                ((Module) this).getServer().getHomeDir().getAbsolutePath();
        deldirfiles(fedoraHome + File.separator
                + BACKEND_POLICIES_ACTIVE_DIRECTORY);
        BackendPolicies backendPolicies =
                new BackendPolicies(fedoraHome + File.separator
                        + BE_SECURITY_XML_LOCATION);
        Hashtable tempfiles = backendPolicies.generateBackendPolicies();
        TransformerFactory tfactory = XmlTransformUtility.getTransformerFactory();
        try {
            Iterator iterator = tempfiles.keySet().iterator();
            while (iterator.hasNext()) {
                File f =
                        new File(fedoraHome + File.separator
                                + BACKEND_POLICIES_XSL_LOCATION); // <<stylesheet
                // location
                StreamSource ss = new StreamSource(f);
                Transformer transformer = tfactory.newTransformer(ss); // xformPath
                String key = (String) iterator.next();
                File infile = new File((String) tempfiles.get(key));
                FileInputStream fis = new FileInputStream(infile);
                FileOutputStream fos =
                        new FileOutputStream(fedoraHome + File.separator
                                + BACKEND_POLICIES_ACTIVE_DIRECTORY
                                + File.separator + key);
                transformer.transform(new StreamSource(fis),
                                      new StreamResult(fos));
            }
        } finally {
            // we're done with temp files now, so delete them
            Iterator iter = tempfiles.keySet().iterator();
            while (iter.hasNext()) {
                File tempFile = new File((String) tempfiles.get(iter.next()));
                tempFile.delete();
            }
        }
    }

    private static final String DEFAULT = "default";

    private void setupActivePolicyDirectories() throws Exception {
        String fedoraHome =
                ((Module) this).getServer().getHomeDir().getAbsolutePath();
        mkdir(repositoryPoliciesActiveDirectory);
        if (mkdir(repositoryPoliciesActiveDirectory + File.separator + DEFAULT)) {
            dircopy(fedoraHome + File.separator
                            + DEFAULT_REPOSITORY_POLICIES_DIRECTORY,
                    repositoryPoliciesActiveDirectory + File.separator
                            + DEFAULT);
        }
        generateBackendPolicies();
    }

    @Override
    public void postInitModule() throws ModuleInitializationException {
        DOManager m_manager =
                (DOManager) getServer()
                        .getModule("fedora.server.storage.DOManager");
        if (m_manager == null) {
            throw new ModuleInitializationException("Can't get a DOManager from Server.getModule",
                                                    getRole());
        }
        try {
            getServer().getStatusFile()
                    .append(ServerState.STARTING,
                            "Initializing XACML Authorization Module");
            setupActivePolicyDirectories();
            xacmlPep = PolicyEnforcementPoint.getInstance();
            String fedoraHome =
                    ((Module) this).getServer().getHomeDir().getAbsolutePath();
            xacmlPep.initPep(enforceMode,
                             combiningAlgorithm,
                             repositoryPoliciesActiveDirectory,
                             fedoraHome + File.separator
                                     + BACKEND_POLICIES_ACTIVE_DIRECTORY,
                             repositoryPolicyGuitoolDirectory,
                             m_manager,
                             validateRepositoryPolicies,
                             validateObjectPoliciesFromDatastream,
                             m_policyParser,
                             ownerIdSeparator);
        } catch (Throwable e1) {
            throw new ModuleInitializationException(e1.getMessage(),
                                                    getRole(),
                                                    e1);
        }
    }

    public void reloadPolicies(Context context) throws Exception {
        enforceReloadPolicies(context);
        generateBackendPolicies();
        xacmlPep.newPdp();
    }

    private final String extractNamespace(String pid) {
        String namespace = "";
        int colonPosition = pid.indexOf(':');
        if (-1 < colonPosition) {
            namespace = pid.substring(0, colonPosition);
        }
        return namespace;
    }

    /**
     * Enforce authorization for adding a datastream to an object. Provide
     * attributes for the authorization decision and wrap that xacml decision.
     * <p>
     * The following attributes are available for use in authorization policies
     * during a call to this method.
     * </p>
     * <p>
     * action attributes
     * <ul>
     * <li>urn:fedora:names:fedora:2.1:action:id ==
     * urn:fedora:names:fedora:2.1:action:id-addDatastream</li>
     * <li>urn:fedora:names:fedora:2.1:action:api ==
     * urn:fedora:names:fedora:2.1:action:api-m</li>
     * </ul>
     * </p>
     * <p>
     * resource attributes of object to which datastream would be added
     * <ul>
     * <li>urn:fedora:names:fedora:2.1:resource:object:pid</li>
     * <li>urn:fedora:names:fedora:2.1:resource:object:namespace (if pid is
     * "x:y", namespace is "x")</li>
     * </ul>
     * </p>
     * <p>
     * resource attributes of datastream which would be added
     * <ul>
     * <li>urn:fedora:names:fedora:2.1:resource:datastream:mimeType</li>
     * <li>urn:fedora:names:fedora:2.1:resource:datastream:formatUri</li>
     * <li>urn:fedora:names:fedora:2.1:resource:datastream:state</li>
     * <li>urn:fedora:names:fedora:2.1:resource:datastream:id</li>
     * <li>urn:fedora:names:fedora:2.1:resource:datastream:location</li>
     * <li>urn:fedora:names:fedora:2.1:resource:datastream:controlGroup</li>
     * <li>urn:fedora:names:fedora:2.1:resource:datastream:altIds</li>
     * <li>urn:fedora:names:fedora:2.1:resource:datastream:checksumType</li>
     * <li>urn:fedora:names:fedora:2.1:resource:datastream:checksum</li>
     * </ul>
     * </p>
     *
     * @see #enforceMethods common attributes available on any fedora interface
     *      call
     */
    public final void enforceAddDatastream(Context context,
                                           String pid,
                                           String dsId,
                                           String[] altIDs,
                                           String MIMEType,
                                           String formatURI,
                                           String dsLocation,
                                           String controlGroup,
                                           String dsState,
                                           String checksumType,
                                           String checksum)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceAddDatastream");
            String target = Constants.ACTION.ADD_DATASTREAM.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.MIME_TYPE.uri,
                                           MIMEType);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.FORMAT_URI.uri,
                                           formatURI);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.STATE.uri,
                                           dsState);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.ID.uri, dsId);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.LOCATION.uri,
                                           dsLocation);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.CONTROL_GROUP.uri,
                                           controlGroup);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.ALT_IDS.uri,
                                           altIDs);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.CHECKSUM_TYPE.uri,
                                           checksumType);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.CHECKSUM.uri,
                                           checksum);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep
                    .enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceAddDatastream");
        }
    }

    public final void enforceExport(Context context,
                                    String pid,
                                    String format,
                                    String exportContext,
                                    String exportEncoding)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceExport");
            String target = Constants.ACTION.EXPORT.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes
                                .setReturn(Constants.OBJECT.FORMAT_URI.uri,
                                           format);
                name = resourceAttributes
                                .setReturn(Constants.OBJECT.CONTEXT.uri,
                                           exportContext);
                name = resourceAttributes
                                .setReturn(Constants.OBJECT.ENCODING.uri,
                                           exportEncoding);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceExport");
        }
    }

    /**
     * @deprecated in Fedora 3.0, use enforceExport() instead
     */
    @Deprecated
    public final void enforceExportObject(Context context,
                                          String pid,
                                          String format,
                                          String exportContext,
                                          String exportEncoding)
            throws AuthzException {
        enforceExport(context, pid, format, exportContext, exportEncoding);
    }

    public final void enforceGetNextPid(Context context,
                                        String namespace,
                                        int nNewPids) throws AuthzException {
        try {
            LOG.debug("Entered enforceGetNextPid");
            String target = Constants.ACTION.GET_NEXT_PID.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                String nNewPidsAsString = Integer.toString(nNewPids);
                name =
                        resourceAttributes
                                .setReturn(Constants.OBJECT.N_PIDS.uri,
                                           nNewPidsAsString);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             "",
                             namespace,
                             context);
        } finally {
            LOG.debug("Exiting enforceGetNextPid");
        }
    }

    public final void enforceGetDatastream(Context context,
                                           String pid,
                                           String datastreamId,
                                           Date asOfDateTime)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceGetDatastream");
            String target = Constants.ACTION.GET_DATASTREAM.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.ID.uri,
                                           datastreamId);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.AS_OF_DATETIME.uri,
                                           ensureDate(asOfDateTime, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceGetDatastream");
        }
    }

    public final void enforceGetDatastreamHistory(Context context,
                                                  String pid,
                                                  String datastreamId)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceGetDatastreamHistory");
            String target = Constants.ACTION.GET_DATASTREAM_HISTORY.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.ID.uri,
                                           datastreamId);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceGetDatastreamHistory");
        }
    }

    private final String ensureDate(Date date, Context context)
            throws AuthzOperationalException {
        if (date == null) {
            date = context.now();
        }
        String dateAsString;
        try {
            dateAsString = dateAsString(date);
        } catch (Throwable t) {
            throw new AuthzOperationalException("couldn't make date a string",
                                                t);
        }
        return dateAsString;
    }

    public final void enforceGetDatastreams(Context context,
                                            String pid,
                                            Date asOfDate,
                                            String datastreamState)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceGetDatastreams");
            String target = Constants.ACTION.GET_DATASTREAMS.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.STATE.uri,
                                           datastreamState);
                name = resourceAttributes
                                .setReturn(Constants.RESOURCE.AS_OF_DATETIME.uri,
                                           ensureDate(asOfDate, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep
                    .enforce(context
                                     .getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceGetDatastreams");
        }
    }

    public final void enforceGetObjectXML(Context context,
                                          String pid,
                                          String objectXmlEncoding)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceGetObjectXML");
            String target = Constants.ACTION.GET_OBJECT_XML.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes
                                .setReturn(Constants.OBJECT.ENCODING.uri,
                                           objectXmlEncoding);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceGetObjectXML");
        }
    }

    public final void enforceIngest(Context context,
                                    String pid,
                                    String format,
                                    String ingestEncoding)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceIngest");
            String target = Constants.ACTION.INGEST.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes
                                .setReturn(Constants.OBJECT.FORMAT_URI.uri,
                                           format);
                name = resourceAttributes
                                .setReturn(Constants.OBJECT.ENCODING.uri,
                                           ingestEncoding);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep
                    .enforce(context
                                     .getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceIngest");
        }
    }

    /**
     * @deprecated in Fedora 3.0, use enforceIngest() instead
     */
    @Deprecated
    public final void enforceIngestObject(Context context,
                                          String pid,
                                          String format,
                                          String ingestEncoding)
            throws AuthzException {
        enforceIngest(context, pid, format, ingestEncoding);
    }

    public final void enforceListObjectInFieldSearchResults(Context context,
                                                            String pid)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceListObjectInFieldSearchResults");
            String target =
                    Constants.ACTION.LIST_OBJECT_IN_FIELD_SEARCH_RESULTS.uri;
            if (enforceListObjectInFieldSearchResults) {
                context.setActionAttributes(null);
                context.setResourceAttributes(null);
                xacmlPep
                        .enforce(context
                                         .getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                                 target,
                                 Constants.ACTION.APIA.uri,
                                 pid,
                                 extractNamespace(pid),
                                 context);
            }
        } finally {
            LOG.debug("Exiting enforceListObjectInFieldSearchResults");
        }
    }

    public final void enforceListObjectInResourceIndexResults(Context context,
                                                              String pid)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceListObjectInResourceIndexResults");
            String target =
                    Constants.ACTION.LIST_OBJECT_IN_RESOURCE_INDEX_RESULTS.uri;
            if (enforceListObjectInResourceIndexResults) {
                context.setActionAttributes(null);
                context.setResourceAttributes(null);
                xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                                 target,
                                 Constants.ACTION.APIA.uri,
                                 pid,
                                 extractNamespace(pid),
                                 context);
            }
        } finally {
            LOG.debug("Exiting enforceListObjectInResourceIndexResults");
        }
    }

    public final void enforceModifyDatastreamByReference(Context context,
                                                         String pid,
                                                         String datastreamId,
                                                         String[] datastreamNewAltIDs,
                                                         String datastreamNewMimeType,
                                                         String datastreamNewFormatURI,
                                                         String datastreamNewLocation,
                                                         String datastreamNewChecksumType,
                                                         String datastreamNewChecksum)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceModifyDatastreamByReference");
            String target = Constants.ACTION.MODIFY_DATASTREAM_BY_REFERENCE.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.ID.uri,
                                           datastreamId);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.NEW_MIME_TYPE.uri,
                                           datastreamNewMimeType);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.NEW_FORMAT_URI.uri,
                                           datastreamNewFormatURI);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.NEW_LOCATION.uri,
                                           datastreamNewLocation);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.NEW_ALT_IDS.uri,
                                           datastreamNewAltIDs);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.NEW_CHECKSUM_TYPE.uri,
                                           datastreamNewChecksumType);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.NEW_CHECKSUM.uri,
                                           datastreamNewChecksum);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceModifyDatastreamByReference");
        }
    }

    public final void enforceModifyDatastreamByValue(Context context,
                                                     String pid,
                                                     String datastreamId,
                                                     String[] newDatastreamAltIDs,
                                                     String newDatastreamMimeType,
                                                     String newDatastreamFormatURI,
                                                     String newDatastreamChecksumType,
                                                     String newDatastreamChecksum)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceModifyDatastreamByValue");
            String target = Constants.ACTION.MODIFY_DATASTREAM_BY_VALUE.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.ID.uri,
                                           datastreamId);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.NEW_MIME_TYPE.uri,
                                           newDatastreamMimeType);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.NEW_FORMAT_URI.uri,
                                           newDatastreamFormatURI);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.NEW_ALT_IDS.uri,
                                           newDatastreamAltIDs);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.NEW_CHECKSUM_TYPE.uri,
                                           newDatastreamChecksumType);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.NEW_CHECKSUM.uri,
                                           newDatastreamChecksum);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceModifyDatastreamByValue");
        }
    }

    public final void enforceModifyObject(Context context,
                                          String pid,
                                          String objectNewState,
                                          String objectNewOwnerId)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceModifyObject");
            String target = Constants.ACTION.MODIFY_OBJECT.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes
                                .setReturn(Constants.OBJECT.NEW_STATE.uri,
                                           objectNewState);
                name = resourceAttributes
                                .setReturn(Constants.OBJECT.OWNER.uri,
                                           objectNewOwnerId);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep
                    .enforce(context
                                     .getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceModifyObject");
        }
    }

    public final void enforcePurgeDatastream(Context context,
                                             String pid,
                                             String datastreamId,
                                             Date endDT) throws AuthzException {
        try {
            LOG.debug("Entered enforcePurgeDatastream");
            String target = Constants.ACTION.PURGE_DATASTREAM.uri;
            String name = "";
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            name = "";
            try {
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.ID.uri,
                                           datastreamId);
                name = resourceAttributes
                                .setReturn(Constants.RESOURCE.AS_OF_DATETIME.uri,
                                           ensureDate(endDT, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforcePurgeDatastream");
        }
    }

    public final void enforcePurgeObject(Context context, String pid)
            throws AuthzException {
        try {
            LOG.debug("Entered enforcePurgeObject");
            String target = Constants.ACTION.PURGE_OBJECT.uri;
            context.setActionAttributes(null);
            context.setResourceAttributes(null);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforcePurgeObject");
        }
    }

    public final void enforceSetDatastreamState(Context context,
                                                String pid,
                                                String datastreamId,
                                                String datastreamNewState)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceSetDatastreamState");
            String target = Constants.ACTION.SET_DATASTREAM_STATE.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.ID.uri,
                                           datastreamId);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.NEW_STATE.uri,
                                           datastreamNewState);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceSetDatastreamState");
        }
    }

    public final void enforceSetDatastreamVersionable(Context context,
                                                      String pid,
                                                      String datastreamId,
                                                      boolean datastreamNewVersionable)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceSetDatastreamVersionable");
            String target = Constants.ACTION.SET_DATASTREAM_VERSIONABLE.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.ID.uri,
                                           datastreamId);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.NEW_VERSIONABLE.uri,
                                           new Boolean(datastreamNewVersionable)
                                                   .toString());
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceSetDatastreamVersionable");
        }
    }

    public final void enforceCompareDatastreamChecksum(Context context,
                                                       String pid,
                                                       String datastreamId,
                                                       Date versionDate)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceCompareDatastreamChecksum");
            String target = Constants.ACTION.COMPARE_DATASTREAM_CHECKSUM.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";

            try {
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.ID.uri,
                                           datastreamId);
                name = resourceAttributes
                                .setReturn(Constants.RESOURCE.AS_OF_DATETIME.uri,
                                           ensureDate(versionDate, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceCompareDatastreamChecksum");
        }
    }

    public void enforceDescribeRepository(Context context)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceDescribeRepository");
            String target = Constants.ACTION.DESCRIBE_REPOSITORY.uri;
            context.setActionAttributes(null);
            context.setResourceAttributes(null);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             "",
                             "",
                             context);
        } finally {
            LOG.debug("Exiting enforceDescribeRepository");
        }
    }

    public void enforceFindObjects(Context context) throws AuthzException {
        try {
            LOG.debug("Entered enforceFindObjects");
            String target = Constants.ACTION.FIND_OBJECTS.uri;
            context.setActionAttributes(null);
            context.setResourceAttributes(null);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             "",
                             "",
                             context);
        } finally {
            LOG.debug("Exiting enforceFindObjects");
        }
    }

    public void enforceRIFindObjects(Context context) throws AuthzException {
        try {
            LOG.debug("Entered enforceRIFindObjects");
            String target = Constants.ACTION.RI_FIND_OBJECTS.uri;
            context.setActionAttributes(null);
            context.setResourceAttributes(null);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             "",
                             "",
                             context);
        } finally {
            LOG.debug("Exiting enforceRIFindObjects");
        }
    }

    public void enforceGetDatastreamDissemination(Context context,
                                                  String pid,
                                                  String datastreamId,
                                                  Date asOfDate)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceGetDatastreamDissemination");
            String target = Constants.ACTION.GET_DATASTREAM_DISSEMINATION.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.ID.uri,
                                           datastreamId);
                name = resourceAttributes
                                .setReturn(Constants.RESOURCE.AS_OF_DATETIME.uri,
                                           ensureDate(asOfDate, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceGetDatastreamDissemination");
        }
    }

    public void enforceGetDissemination(Context context,
                                        String pid,
                                        String sDefPid,
                                        String methodName,
                                        Date asOfDate,
                                        String objectState,
                                        String sDefState,
                                        String sDepPid,
                                        String sDepState,
                                        String dissState) throws AuthzException {
        try {
            LOG.debug("Entered enforceGetDissemination");
            String target = Constants.ACTION.GET_DISSEMINATION.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes.setReturn(Constants.SDEF.PID.uri,
                                                     sDefPid);
                name = resourceAttributes
                                .setReturn(Constants.SDEF.NAMESPACE.uri,
                                           extractNamespace(sDefPid));
                name = resourceAttributes
                                .setReturn(Constants.DISSEMINATOR.METHOD.uri,
                                           methodName);
                name = resourceAttributes.setReturn(Constants.SDEP.PID.uri,
                                                     sDepPid);
                name = resourceAttributes
                                .setReturn(Constants.SDEP.NAMESPACE.uri,
                                           extractNamespace(sDepPid));
                name = resourceAttributes
                                .setReturn(Constants.OBJECT.STATE.uri,
                                           objectState);
                name = resourceAttributes
                                .setReturn(Constants.DISSEMINATOR.STATE.uri,
                                           dissState);
                name = resourceAttributes.setReturn(Constants.SDEF.STATE.uri,
                                                     sDefState);
                name = resourceAttributes.setReturn(Constants.SDEP.STATE.uri,
                                                     sDepState);
                name = resourceAttributes
                                .setReturn(Constants.RESOURCE.AS_OF_DATETIME.uri,
                                           ensureDate(asOfDate, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceGetDissemination");
        }
    }

    public void enforceGetObjectHistory(Context context, String pid)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceGetObjectHistory");
            String target = Constants.ACTION.GET_OBJECT_HISTORY.uri;
            context.setActionAttributes(null);
            context.setResourceAttributes(null);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceGetObjectHistory");
        }
    }

    public void enforceGetObjectProfile(Context context,
                                        String pid,
                                        Date asOfDate) throws AuthzException {
        try {
            LOG.debug("Entered enforceGetObjectProfile");
            String target = Constants.ACTION.GET_OBJECT_PROFILE.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes
                                .setReturn(Constants.RESOURCE.AS_OF_DATETIME.uri,
                                           ensureDate(asOfDate, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceGetObjectProfile");
        }
    }

    public void enforceListDatastreams(Context context,
                                       String pid,
                                       Date asOfDate) throws AuthzException {
        try {
            LOG.debug("Entered enforceListDatastreams");
            String target = Constants.ACTION.LIST_DATASTREAMS.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes
                                .setReturn(Constants.RESOURCE.AS_OF_DATETIME.uri,
                                           ensureDate(asOfDate, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceListDatastreams");
        }
    }

    public void enforceListMethods(Context context, String pid, Date asOfDate)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceListMethods");
            String target = Constants.ACTION.LIST_METHODS.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes
                                .setReturn(Constants.RESOURCE.AS_OF_DATETIME.uri,
                                           ensureDate(asOfDate, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceListMethods");
        }
    }

    public void enforceServerStatus(Context context) throws AuthzException {
        try {
            LOG.debug("Entered enforceServerStatus");
            String target = Constants.ACTION.SERVER_STATUS.uri;
            context.setActionAttributes(null);
            context.setResourceAttributes(null);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             "",
                             "",
                             "",
                             context);
        } finally {
            LOG.debug("Exiting enforceServerStatus");
        }
    }

    public void enforceOAIRespond(Context context) throws AuthzException {
        try {
            LOG.debug("Entered enforceOAIRespond");
            String target = Constants.ACTION.OAI.uri;
            context.setActionAttributes(null);
            context.setResourceAttributes(null);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             "",
                             "",
                             "",
                             context);
        } finally {
            LOG.debug("Exiting enforceOAIRespond");
        }
    }

    public void enforceUpload(Context context) throws AuthzException {
        try {
            LOG.debug("Entered enforceUpload");
            String target = Constants.ACTION.UPLOAD.uri;
            context.setActionAttributes(null);
            context.setResourceAttributes(null);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             "",
                             "",
                             "",
                             context);
        } finally {
            LOG.debug("Exiting enforceUpload");
        }
    }

    public void enforce_Internal_DSState(Context context,
                                         String id,
                                         String state) throws AuthzException {
        try {
            LOG.debug("Entered enforce_Internal_DSState");
            String target = Constants.ACTION.INTERNAL_DSSTATE.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.ID.uri, id);
                name = resourceAttributes
                                .setReturn(Constants.DATASTREAM.STATE.uri,
                                           state);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             "",
                             "",
                             context);
        } finally {
            LOG.debug("Exiting enforce_Internal_DSState");
        }
    }

    public void enforceResolveDatastream(Context context,
                                         Date ticketIssuedDateTime)
            throws AuthzException {
        try {
            LOG.debug("Entered enforceResolveDatastream");
            String target = Constants.ACTION.RESOLVE_DATASTREAM.uri;
            context.setResourceAttributes(null);
            MultiValueMap actionAttributes = new MultiValueMap();
            String name = "";
            try {
                String ticketIssuedDateTimeString =
                        DateUtility.convertDateToString(ticketIssuedDateTime);
                name = actionAttributes
                                .setReturn(Constants.RESOURCE.TICKET_ISSUED_DATETIME.uri,
                                           ticketIssuedDateTimeString);
            } catch (Exception e) {
                context.setActionAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setActionAttributes(actionAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             "",
                             "",
                             "",
                             context);
        } finally {
            LOG.debug("Exiting enforceResolveDatastream");
        }
    }

    public void enforceReloadPolicies(Context context) throws AuthzException {
        try {
            LOG.debug("Entered enforceReloadPolicies");
            String target = Constants.ACTION.RELOAD_POLICIES.uri;
            context.setResourceAttributes(null);
            context.setActionAttributes(null);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             "",
                             "",
                             "",
                             context);
        } finally {
            LOG.debug("Exiting enforceReloadPolicies");
        }
    }

    public static final String dateAsString(Date date) throws Exception {
        return DateUtility.convertDateToString(date, false);
    }

    public void enforceGetRelationships(Context context,
                                        String pid,
                                        String predicate) throws AuthzException {
        try {
            LOG.debug("Entered enforceGetRelationships");
            String target = Constants.ACTION.GET_RELATIONSHIPS.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes.setReturn(Constants.OBJECT.PID.uri,
                                                    pid);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceGetRelationships");
        }
    }

    public void enforceAddRelationship(Context context,
                                       String pid,
                                       String predicate,
                                       String object,
                                       boolean isLiteral,
                                       String datatype) throws AuthzException {
        try {
            LOG.debug("Entered enforceAddRelationship");
            String target = Constants.ACTION.ADD_RELATIONSHIP.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes.setReturn(Constants.OBJECT.PID.uri,
                                                    pid);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforceAddRelationship");
        }
    }

    public void enforcePurgeRelationship(Context context,
                                         String pid,
                                         String predicate,
                                         String object,
                                         boolean isLiteral,
                                         String datatype) throws AuthzException {
        try {
            LOG.debug("Entered enforcePurgeRelationship");
            String target = Constants.ACTION.PURGE_RELATIONSHIP.uri;
            context.setActionAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try {
                name = resourceAttributes.setReturn(Constants.OBJECT.PID.uri,
                                                    pid);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                        + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            LOG.debug("Exiting enforcePurgeRelationship");
        }
    }

    public void enforceRetrieveFile(Context context, String fileURI) throws AuthzException {
        try {
            LOG.debug("Entered enforceRetrieveFile");
            String target = Constants.ACTION.RETRIEVE_FILE.uri;
            context.setActionAttributes(null);
            context.setResourceAttributes(null);
            MultiValueMap resourceAttributes = new MultiValueMap();
            String name = "";
            try{
                name = resourceAttributes.setReturn(Constants.DATASTREAM.FILE_URI.uri, fileURI);
            } catch (Exception e){
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't be set " + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context
                    .getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                    target,
                    Constants.ACTION.APIM.uri,
                    fileURI,
                    extractNamespace(fileURI),
                    context);
            } finally {
                LOG.debug("Exiting enforceRetrieveFile");
            }
    }
}
