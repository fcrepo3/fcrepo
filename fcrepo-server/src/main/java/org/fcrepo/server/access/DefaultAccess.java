/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.access;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fcrepo.common.Constants;
import org.fcrepo.common.Models;
import org.fcrepo.server.Context;
import org.fcrepo.server.Module;
import org.fcrepo.server.Server;
import org.fcrepo.server.access.dissemination.DisseminationService;
import org.fcrepo.server.errors.DatastreamNotFoundException;
import org.fcrepo.server.errors.DisseminationException;
import org.fcrepo.server.errors.DisseminatorNotFoundException;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.InvalidUserParmException;
import org.fcrepo.server.errors.MethodNotFoundException;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.search.FieldSearchQuery;
import org.fcrepo.server.search.FieldSearchResult;
import org.fcrepo.server.security.Authorization;
import org.fcrepo.server.storage.ContentManagerParams;
import org.fcrepo.server.storage.DOManager;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.ExternalContentManager;
import org.fcrepo.server.storage.ServiceDefinitionReader;
import org.fcrepo.server.storage.ServiceDeploymentReader;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.DatastreamDef;
import org.fcrepo.server.storage.types.DatastreamManagedContent;
import org.fcrepo.server.storage.types.DatastreamReferencedContent;
import org.fcrepo.server.storage.types.DatastreamXMLMetadata;
import org.fcrepo.server.storage.types.DeploymentDSBindRule;
import org.fcrepo.server.storage.types.DeploymentDSBindSpec;
import org.fcrepo.server.storage.types.DisseminationBindingInfo;
import org.fcrepo.server.storage.types.MIMETypedStream;
import org.fcrepo.server.storage.types.MethodDef;
import org.fcrepo.server.storage.types.MethodDefOperationBind;
import org.fcrepo.server.storage.types.MethodParmDef;
import org.fcrepo.server.storage.types.ObjectMethodsDef;
import org.fcrepo.server.storage.types.Property;
import org.fcrepo.server.storage.types.RelationshipTuple;
import org.fcrepo.utilities.DateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Access Module, providing support for the Fedora Access subsystem.
 *
 * @author Ross Wayland
 * @version $Id$
 */
public class DefaultAccess
        extends Module
        implements Access {

    private static final Logger logger =
            LoggerFactory.getLogger(DefaultAccess.class);

    private static final MethodParmDef[] METHOD_PARM_DEF_TYPE =
            new MethodParmDef[0];
    /** Current DOManager of the Fedora server. */
    private DOManager m_manager;

    /** OAI Provider domain name, for the describe request's identifier info. */
    private String m_repositoryDomainName;

    /** Dynamic Access Module */
    // FIXME is this the right way to associate the dynamic access module???
    private DynamicAccessModule m_dynamicAccess;

    private ExternalContentManager m_externalContentManager;

    private Authorization m_authorizationModule;

    /**
     * <p>
     * Creates and initializes the Access Module. When the server is starting
     * up, this is invoked as part of the initialization process.
     * </p>
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
    public DefaultAccess(Map<String, String> moduleParameters,
                         Server server,
                         String role)
            throws ModuleInitializationException {
        super(moduleParameters, server, role);
    }

    /**
     * <p>
     * Initializes the module.
     * </p>
     *
     * @throws ModuleInitializationException
     *         If the module cannot be initialized.
     */
    @Override
    public void initModule() throws ModuleInitializationException {

        String dsMediation = getParameter("doMediateDatastreams");
        if (dsMediation == null) {
            throw new ModuleInitializationException("doMediateDatastreams parameter must be specified.",
                                                    getRole());
        }
    }

    @Override
    public void postInitModule() throws ModuleInitializationException {
        // get ref to DOManager
        m_manager =
                (DOManager) getServer()
                        .getModule("org.fcrepo.server.storage.DOManager");
        if (m_manager == null) {
            throw new ModuleInitializationException("Can't get a DOManager "
                                                    + "from Server.getModule", getRole());
        }
        // get ref to DynamicAccess module
        m_dynamicAccess =
                (DynamicAccessModule) getServer()
                        .getModule("org.fcrepo.server.access.DynamicAccess");

        // get ref to ExternalContentManager
        m_externalContentManager =
                (ExternalContentManager) getServer()
                        .getModule("org.fcrepo.server.storage.ExternalContentManager");

        // get ref to OAIProvider, for repositoryDomainName param for oai info
        Module oaiProvider = getServer().getModule("org.fcrepo.oai.OAIProvider");
        if (oaiProvider == null) {
            throw new ModuleInitializationException("DefaultAccess module requires that the server "
                                                    + "has an OAIProvider module configured so that it can get the repositoryDomainName parameter.",
                                                    getRole());
        }
        m_repositoryDomainName =
                oaiProvider.getParameter("repositoryDomainName");
        if (m_repositoryDomainName == null) {
            throw new ModuleInitializationException("DefaultAccess module requires that the OAIProvider "
                                                    + "module has the repositoryDomainName parameter specified.",
                                                    getRole());
        }

        m_authorizationModule =
                getServer().getBean("org.fcrepo.server.security.Authorization", Authorization.class);
        if (m_authorizationModule == null) {
            throw new ModuleInitializationException("Can't get an Authorization module (in default access) from Server.getModule",
                                                    getRole());
        }

    }

    private static final Hashtable<String, String> accessActionAttributes =
            new Hashtable<String, String>();
    static {
        accessActionAttributes.put("api", "apia");
    }

    /**
     * <p>
     * Disseminates the content produced by executing the specified method of
     * the associated deployment object of the specified digital object.
     * </p>
     *
     * @param context
     *        The context of this request.
     * @param PID
     *        The persistent identifier of the digital object.
     * @param sDefPID
     *        The persistent identifier of the Service Definition object.
     * @param methodName
     *        The name of the method to be executed.
     * @param userParms
     *        An array of user-supplied method parameters consisting of
     *        name/value pairs.
     * @param asOfDateTime
     *        The versioning datetime stamp.
     * @return A MIME-typed stream containing the result of the dissemination.
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    @Override
    public MIMETypedStream getDissemination(Context context,
                                            String PID,
                                            String sDefPID,
                                            String methodName,
                                            Property[] userParms,
                                            Date asOfDateTime)
            throws ServerException {
        PID = Server.getPID(PID).toString();
        sDefPID = Server.getPID(sDefPID).toString();
        long initStartTime = (logger.isDebugEnabled()) ? System.currentTimeMillis() : 0;
        long startTime = initStartTime;
        long stopTime;
        long interval;
        ServiceDeploymentReader deploymentReader = null;

        DOReader reader =
                m_manager.getReader(asOfDateTime == null, context, PID);
        String authzAux_objState = reader.GetObjectState();

        // DYNAMIC!! If service deployment is defined as dynamic, then
        // perform the dissemination via the DynamicAccess module.
        if (m_dynamicAccess.isDynamicService(context, PID, sDefPID)) {
            m_authorizationModule.enforceGetDissemination(context,
                                                          PID,
                                                          sDefPID,
                                                          methodName,
                                                          asOfDateTime,
                                                          authzAux_objState,
                                                          "A",
                                                          "fedora-system:4",
                                                          "A",
                                                          "A");
            MIMETypedStream retVal =
                    m_dynamicAccess.getDissemination(context,
                                                     PID,
                                                     sDefPID,
                                                     methodName,
                                                     userParms,
                                                     asOfDateTime);
            if (logger.isDebugEnabled()) {
                stopTime = System.currentTimeMillis();
                interval = stopTime - startTime;
                logger.debug("Roundtrip DynamicDisseminator: {} milliseconds.", interval);
            }
            return retVal;
        }

        /*
         * Find the service deployment that is contractor for a model this
         * object has, and deploys the requested service. If object<->model
         * mappings are ever stored in the registry, this may be simplified.
         */
        String serviceDeploymentPID = null;
        for (String cModelURI: reader.getContentModels()){
            String cModelPID = cModelURI.substring("info:fedora/".length());

            String foundDeploymentPID =
                    m_manager.lookupDeploymentForCModel(cModelPID, sDefPID);

            if (foundDeploymentPID != null) {
                if (serviceDeploymentPID != null
                    && !foundDeploymentPID.equals(serviceDeploymentPID)) {
                    throw new DisseminationException("More than one deployment ("
                                                     + foundDeploymentPID
                                                     + ", "
                                                     + serviceDeploymentPID
                                                     + ") found for service "
                                                     + sDefPID
                                                     + " in model "
                                                     + cModelPID);

                }

                serviceDeploymentPID = foundDeploymentPID;
            } else {
                logger.debug("No deployment for ({}, {})", cModelPID, sDefPID);
            }
        }

        if (serviceDeploymentPID != null) {
            deploymentReader =
                    m_manager.getServiceDeploymentReader(false,
                                                         context,
                                                         serviceDeploymentPID);
        }

        ServiceDefinitionReader sDefReader =
                m_manager.getServiceDefinitionReader(asOfDateTime == null,
                                                     context,
                                                     sDefPID);

        String authzAux_sdefState = sDefReader.GetObjectState();

        String authzAux_dissState = "unknown";

        /*
         * if reader is null, it means that no suitable deployments have been
         * found. This can happen if (a), the object does not have any models
         * that have that service, or (b) the object has a suitable model, but
         * no implementation of that service has been deployed. We do a bit of
         * checking here to determine which case this represents, as the error
         * message could be very useful.
         */
        if (deploymentReader == null) {

            boolean suitableModelFound = false;
            String cModelPID = null;
            String message = null;

            models: for (String cm:reader.getContentModels()){
                cModelPID = cm.substring(12);

                /* Skip over system models */
                if (Models.contains("info:fedora/" + cModelPID)) {
                    continue;
                }

                /* Open up each model and peek at its sDefs for a match */
                for (RelationshipTuple r : m_manager.getReader(false,
                                                               context,
                                                               cModelPID)
                        .getRelationships(MODEL.HAS_SERVICE, null)) {
                    if (sDefPID.equals(r.getObjectPID())) {
                        suitableModelFound = true;
                        break models;
                    }
                }
            }

            if (suitableModelFound) {
                message =
                        "Unable to find deployment for service " + sDefPID
                        + " on " + reader.GetObjectPID() + " in model "
                        + cModelPID;
            } else {
                message =
                        reader.GetObjectPID()
                        + " does not have a model with service "
                        + sDefPID;
            }
            throw new DisseminatorNotFoundException(message);
        }
        if (logger.isDebugEnabled()) {
            stopTime = System.currentTimeMillis();
            interval = stopTime - startTime;
            logger.debug("Roundtrip Looping Diss: {} milliseconds.", interval);
        }
        // Check deployment object state
        String authzAux_sDepState = deploymentReader.GetObjectState();
        String authzAux_sDepPID = deploymentReader.GetObjectPID();

        m_authorizationModule.enforceGetDissemination(context,
                                                      PID,
                                                      sDefPID,
                                                      methodName,
                                                      asOfDateTime,
                                                      authzAux_objState,
                                                      authzAux_sdefState,
                                                      authzAux_sDepPID,
                                                      authzAux_sDepState,
                                                      authzAux_dissState);

        // Get method parms
        Hashtable<String, String> h_userParms = new Hashtable<String, String>();
        MIMETypedStream dissemination = null;
        MethodParmDef[] defaultMethodParms = null;

        startTime = new Date().getTime();
        // Put any user-supplied method parameters into hash table
        if (userParms != null) {
            for (Property element : userParms) {
                h_userParms.put(element.name, element.value);
            }
        }
        // Validate user-supplied parameters
        validateUserParms(context,
                          PID,
                          sDefPID,
                          deploymentReader,
                          methodName,
                          h_userParms,
                          asOfDateTime);

        if (logger.isDebugEnabled()) {
            stopTime = System.currentTimeMillis();
            interval = stopTime - startTime;
            logger.debug("Roundtrip Get/Validate User Parms:  milliseconds.", interval);

            startTime = System.currentTimeMillis();
        }
        // SDP: GET INFO FROM DEPLOYMENT READER:
        // Add any default method parameters to validated user parm list
        defaultMethodParms =
                deploymentReader
                        .getServiceMethodParms(methodName, asOfDateTime);
        for (int i = 0; i < defaultMethodParms.length; i++) {
            if (!defaultMethodParms[i].parmType
                    .equals(MethodParmDef.DATASTREAM_INPUT)) {
                if (!h_userParms.containsKey(defaultMethodParms[i].parmName)) {
                    logger.debug("addedDefaultName: {}",
                                 defaultMethodParms[i].parmName);
                    String pdv = defaultMethodParms[i].parmDefaultValue;
                    try {
                        // here we make sure the PID is decoded so that encoding
                        // later won't doubly-encode it
                        if (pdv.equalsIgnoreCase("$pid")) {
                            pdv = URLDecoder.decode(PID, "UTF-8");
                        } else if (pdv.equalsIgnoreCase("$objuri")) {
                            pdv =
                                    "info:fedora/"
                                    + URLDecoder.decode(PID, "UTF-8");
                        }
                    } catch (UnsupportedEncodingException uee) {
                    }
                    logger.debug("addedDefaultValue: {}", pdv);
                    h_userParms.put(defaultMethodParms[i].parmName, pdv);
                }
            }
        }

        if (logger.isDebugEnabled()) {
            stopTime = System.currentTimeMillis();
            interval = stopTime - startTime;
            logger.debug("Roundtrip Get Deployment Parms: {} milliseconds.", interval);
            startTime = System.currentTimeMillis();
        }

        DisseminationBindingInfo[] dissBindInfo;
        dissBindInfo =
                getDisseminationBindingInfo(context,
                                            reader,
                                            deploymentReader,
                                            methodName,
                                            asOfDateTime);

        // Assemble and execute the dissemination request from the binding info.
        DisseminationService dissService = new DisseminationService(getServer());
        dissemination =
                dissService.assembleDissemination(context,
                                                  PID,
                                                  h_userParms,
                                                  dissBindInfo,
                                                  authzAux_sDepPID,
                                                  deploymentReader,
                                                  methodName);

        if (logger.isDebugEnabled()) {
            stopTime = System.currentTimeMillis();
            interval = stopTime - startTime;
            logger.debug("Roundtrip Assemble Dissemination: {} milliseconds.", interval);

            interval = stopTime - initStartTime;
            logger.debug("Roundtrip GetDissemination: {} milliseconds.", interval);
        }
        return dissemination;
    }

    private DisseminationBindingInfo[] getDisseminationBindingInfo(Context context,
                                                                   DOReader dObj,
                                                                   ServiceDeploymentReader bmReader,
                                                                   String methodName,
                                                                   Date versDateTime)
            throws MethodNotFoundException, ServerException {

        // The sDep reader provides information about the service and params.
        MethodParmDef[] methodParms =
                bmReader.getServiceMethodParms(methodName, versDateTime);
        // Find the operation bindings for the method in question
        MethodDefOperationBind[] opBindings =
                bmReader.getServiceMethodBindings(versDateTime);

        String addressLocation = null;
        String operationLocation = null;
        String protocolType = null;
        boolean foundMethod = false;
        for (MethodDefOperationBind element : opBindings) {
            if (element.methodName.equals(methodName)) {
                foundMethod = true;
                addressLocation = element.serviceBindingAddress;
                operationLocation = element.operationLocation;
                protocolType = element.protocolType;
            }
        }
        if (!foundMethod) {
            throw new MethodNotFoundException("Method " + methodName
                                              + " was not found in " + bmReader.GetObjectPID()
                                              + "'s operation " + " binding.");
        }

        DeploymentDSBindSpec dsBindSpec =
                bmReader.getServiceDSInputSpec(versDateTime);
        DeploymentDSBindRule[] dsBindRules =
                dsBindSpec.dsBindRules == null ? new DeploymentDSBindRule[0]
                                               : dsBindSpec.dsBindRules;

        // Results will be returned in this list, one item per *existing*
        // datastream. If a datastream mentioned in the dsBindRules is not
        // present in the object, it will not be present in this list.
        // If the datastream is *really* required in order to invoke the
        // dissemination method in question, rest assured it will fail later.
        List<DisseminationBindingInfo> bindingInfoList =
                new ArrayList<DisseminationBindingInfo>(dsBindRules.length);

        for (int i = 0; i < dsBindRules.length; i++) {
            DeploymentDSBindRule dsBindRule = dsBindRules[i];
            String dsPid =
                    dsBindRule.pid == null ? dObj.GetObjectPID()
                                           : dsBindRule.pid;
            String dsId = dsBindRule.bindingKeyName;

            DOReader reader = m_manager.getReader(false, context, dsPid);
            Datastream ds = reader.GetDatastream(dsId, versDateTime);

            if (ds != null) {
                DisseminationBindingInfo bindingInfo =
                        new DisseminationBindingInfo();
                bindingInfo.DSBindKey = dsId;
                bindingInfo.dsLocation = ds.DSLocation;
                bindingInfo.dsControlGroupType = ds.DSControlGrp;
                bindingInfo.dsID = ds.DatastreamID;
                bindingInfo.dsVersionID = ds.DSVersionID;
                bindingInfo.dsState = ds.DSState;
                bindingInfo.dsCreateDT = ds.DSCreateDT;
                // these will be the same for all elements of the array
                bindingInfo.methodParms = methodParms;
                bindingInfo.AddressLocation = addressLocation;
                bindingInfo.OperationLocation = operationLocation;
                bindingInfo.ProtocolType = protocolType;
                bindingInfoList.add(bindingInfo);
            }
        }
        return bindingInfoList.toArray(new DisseminationBindingInfo[0]);
    }

    @Override
    public ObjectMethodsDef[] listMethods(Context context,
                                          String PID,
                                          Date asOfDateTime)
            throws ServerException {
        long startTime = logger.isDebugEnabled() ? System.currentTimeMillis() : 0;
        PID = Server.getPID(PID).toString();
        m_authorizationModule.enforceListMethods(context, PID, asOfDateTime);
        DOReader reader =
                m_manager.getReader(Server.USE_DEFINITIVE_STORE, context, PID);

        ObjectMethodsDef[] methodDefs = reader.listMethods(asOfDateTime);
        if (logger.isDebugEnabled()) {
            long stopTime = System.currentTimeMillis();
            long interval = stopTime - startTime;
            logger.debug("Roundtrip listMethods: {} milliseconds.", interval);
        }

        // DYNAMIC!! Grab any dynamic method definitions and merge them with
        // the statically bound method definitions
        ObjectMethodsDef[] dynamicMethodDefs =
                m_dynamicAccess.listMethods(context, PID, asOfDateTime);
        ObjectMethodsDef[] result =
                new ObjectMethodsDef[methodDefs.length + dynamicMethodDefs.length];
        System.arraycopy(methodDefs, 0, result, 0, methodDefs.length);
        System.arraycopy(dynamicMethodDefs, 0, result, methodDefs.length, dynamicMethodDefs.length);
        return result;
    }

    @Override
    public DatastreamDef[] listDatastreams(Context context,
                                           String PID,
                                           Date asOfDateTime)
            throws ServerException {
        long startTime = logger.isDebugEnabled() ? new Date().getTime() : 0;
        PID = Server.getPID(PID).toString();
        m_authorizationModule
                .enforceListDatastreams(context, PID, asOfDateTime);
        DOReader reader =
                m_manager.getReader(Server.USE_DEFINITIVE_STORE, context, PID);

        Datastream[] datastreams = reader.GetDatastreams(asOfDateTime, null);
        DatastreamDef[] dsDefs = new DatastreamDef[datastreams.length];
        for (int i = 0; i < datastreams.length; i++) {
            dsDefs[i] =
                    new DatastreamDef(datastreams[i].DatastreamID,
                                      datastreams[i].DSLabel,
                                      datastreams[i].DSMIME);
        }

        if (logger.isDebugEnabled()) {
            long stopTime = new Date().getTime();
            long interval = stopTime - startTime;
            logger.debug("Roundtrip listDatastreams: {} milliseconds.", interval);
        }
        return dsDefs;
    }

    @Override
    public ObjectProfile getObjectProfile(Context context,
                                          String PID,
                                          Date asOfDateTime)
            throws ServerException {
        PID = Server.getPID(PID).toString();
        m_authorizationModule.enforceGetObjectProfile(context,
                                                      PID,
                                                      asOfDateTime);
        DOReader reader =
                m_manager.getReader(asOfDateTime == null, context, PID);

        Date versDateTime = asOfDateTime;
        ObjectProfile profile = new ObjectProfile();
        profile.PID = reader.GetObjectPID();
        profile.objectLabel = reader.GetObjectLabel();
        profile.objectOwnerId = reader.getOwnerId();
        profile.objectModels = new HashSet<String>();
        profile.objectCreateDate = reader.getCreateDate();
        profile.objectLastModDate = reader.getLastModDate();
        profile.objectState = reader.GetObjectState();

        profile.objectModels.addAll(reader.getContentModels());

        // "bootstrap" context won't have the uri to determine security
        String securityUri = context
                .getEnvironmentValue(Constants.HTTP_REQUEST.SECURITY.attributeId);

        if (securityUri != null) {
            String reposBaseURL =
                    getReposBaseURL(securityUri
                                            .equals(Constants.HTTP_REQUEST.SECURE.uri) ? "https"
                                                                                       : "http",
                                    context
                                            .getEnvironmentValue(Constants.HTTP_REQUEST.SERVER_PORT.attributeId));
            profile.dissIndexViewURL =
                    getDissIndexViewURL(reposBaseURL,
                                        context
                                                .getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME),
                                        reader.GetObjectPID(),
                                        versDateTime);
            profile.itemIndexViewURL =
                    getItemIndexViewURL(reposBaseURL,
                                        context
                                                .getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME),
                                        reader.GetObjectPID(),
                                        versDateTime);
        }
        return profile;
    }

    /**
     * <p>
     * Lists the specified fields of each object matching the given criteria.
     * </p>
     *
     * @param context
     *        the context of this request
     * @param resultFields
     *        the names of the fields to return
     * @param maxResults
     *        the maximum number of results to return at a time
     * @param query
     *        the query
     * @return the results of te field search
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    @Override
    public FieldSearchResult findObjects(Context context,
                                         String[] resultFields,
                                         int maxResults,
                                         FieldSearchQuery query)
            throws ServerException {
        m_authorizationModule.enforceFindObjects(context);
        return m_manager.findObjects(context, resultFields, maxResults, query);
    }

    /**
     * <p>
     * Resumes an in-progress listing of object fields.
     * </p>
     *
     * @param context
     *        the context of this request
     * @param sessionToken
     *        the token of the session in which the remaining results can be
     *        obtained
     * @return the next set of results from the initial field search
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    @Override
    public FieldSearchResult resumeFindObjects(Context context,
                                               String sessionToken)
            throws ServerException {
        m_authorizationModule.enforceFindObjects(context);
        return m_manager.resumeFindObjects(context, sessionToken);
    }

    /**
     * <p>
     * Gets information that describes the repository.
     * </p>
     *
     * @param context
     *        the context of this request
     * @return information that describes the repository.
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    @Override
    public RepositoryInfo describeRepository(Context context)
            throws ServerException {
        m_authorizationModule.enforceDescribeRepository(context);
        RepositoryInfo repositoryInfo = new RepositoryInfo();
        repositoryInfo.repositoryName =
                getServer().getParameter("repositoryName");
        String reposBaseURL =
                getReposBaseURL(context
                                        .getEnvironmentValue(Constants.HTTP_REQUEST.SECURITY.attributeId)
                                        .equals(Constants.HTTP_REQUEST.SECURE.uri) ? "https"
                                                                                   : "http",
                                context
                                        .getEnvironmentValue(Constants.HTTP_REQUEST.SERVER_PORT.attributeId));
        repositoryInfo.repositoryBaseURL =
                reposBaseURL + "/" + context.getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME);

        repositoryInfo.repositoryVersion = Server.VERSION;
        Module domgr = getServer().getModule("org.fcrepo.server.storage.DOManager");
        repositoryInfo.repositoryPIDNamespace =
                domgr.getParameter("pidNamespace");
        repositoryInfo.defaultExportFormat =
                domgr.getParameter("defaultExportFormat");
        repositoryInfo.OAINamespace = m_repositoryDomainName;
        repositoryInfo.adminEmailList = getAdminEmails();
        repositoryInfo.samplePID =
                repositoryInfo.repositoryPIDNamespace + ":100";
        repositoryInfo.sampleOAIIdentifer =
                "oai:" + repositoryInfo.OAINamespace + ":"
                + repositoryInfo.samplePID;
        repositoryInfo.sampleSearchURL =
                repositoryInfo.repositoryBaseURL + "/objects";
        repositoryInfo.sampleAccessURL =
                repositoryInfo.repositoryBaseURL + "/objects/" + "demo:5"; // PID should be URL-encoded, but nothing to encode in this case
        repositoryInfo.sampleOAIURL =
                repositoryInfo.repositoryBaseURL + "/oai?verb=Identify";
        repositoryInfo.retainPIDs = getRetainPIDs();
        return repositoryInfo;
    }

    /**
     * <p>
     * Gets the change history of an object by returning a list of timestamps
     * that correspond to modification dates of components. This currently
     * includes changes to datastreams and disseminators.
     * </p>
     *
     * @param context
     *        The context of this request.
     * @param PID
     *        The persistent identifier of the digitla object.
     * @return An Array containing the list of timestamps indicating when
     *         changes were made to the object.
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    @Override
    public String[] getObjectHistory(Context context, String PID)
            throws ServerException {
        PID = Server.getPID(PID).toString();
        m_authorizationModule.enforceGetObjectHistory(context, PID);
        DOReader reader =
                m_manager.getReader(Server.USE_DEFINITIVE_STORE, context, PID);

        return reader.getObjectHistory(PID);
    }

    private String[] getAdminEmails() {
        String emailsCSV =
                convertToCSV(getServer().getParameter("adminEmailList"));
        Vector<Object> emails = new Vector<Object>();
        StringTokenizer st = new StringTokenizer(emailsCSV, ",");
        while (st.hasMoreElements()) {
            emails.add(st.nextElement());
        }
        return emails.toArray(EMPTY_STRING_ARRAY);
    }

    private String[] getRetainPIDs() {
        String retainPIDsCSV =
                convertToCSV(getServer()
                                     .getModule("org.fcrepo.server.storage.DOManager")
                                     .getParameter("retainPIDs"));
        Vector<Object> retainPIDs = new Vector<Object>();
        StringTokenizer st = new StringTokenizer(retainPIDsCSV, ",");
        while (st.hasMoreElements()) {
            retainPIDs.add(st.nextElement());
        }
        return retainPIDs.toArray(EMPTY_STRING_ARRAY);
    }

    private String convertToCSV(String list) {
        // make sure values in the list are comma delimited
        if (list == null) {
            return "*";
        }
        String original = list.trim();
        Pattern spaces = Pattern.compile(" ++");
        Matcher m = spaces.matcher(original);
        String interim = m.replaceAll(",");
        Pattern multcommas = Pattern.compile(",++");
        Matcher m2 = multcommas.matcher(interim);
        String csv = m2.replaceAll(",");
        return csv;
    }

    /**
     * <p>
     * Validates user-supplied method parameters against values in the
     * corresponding Service Definition object. The method will validate for:
     * </p>
     * <ol>
     * <li>Valid name - each name must match a valid method parameter name</li>
     * <li>DefaultValue - any specified parameters with valid default values
     * will have the default value substituted if the user-supplied value is
     * null</li>
     * <li>Required name - each required method parameter name must be present
     * </ol>
     *
     * @param context
     *        The context of this request.
     * @param PID
     *        The persistent identifier of the digital object.
     * @param sDefPID
     *        The persistent identifier of the Service Definition object.
     * @param methodName
     *        The name of the method.
     * @param h_userParms
     *        A hashtable of user-supplied method parameter name/value pairs.
     * @param versDateTime
     *        The version datetime stamp of the digital object.
     * @throws ServerException
     *         If any type of error occurred fulfilling the request.
     */
    private void validateUserParms(Context context,
                                   String PID,
                                   String sDefPID,
                                   ServiceDeploymentReader sdepreader,
                                   String methodName,
                                   Hashtable<String, String> h_userParms,
                                   Date versDateTime) throws ServerException {
        PID = Server.getPID(PID).toString();
        sDefPID = Server.getPID(sDefPID).toString();
        MethodParmDef[] methodParms = null;
        MethodParmDef methodParm = null;
        StringBuffer sb = new StringBuffer();
        Hashtable<String, MethodParmDef> h_validParms =
                new Hashtable<String, MethodParmDef>();
        boolean isValid = true;

        if (sdepreader != null) // this code will be used for the CMDA example
        {
            MethodDef[] methods = sdepreader.getServiceMethods(versDateTime);
            // Filter out parms that are internal to the mechanism and not part
            // of the abstract method definition.  We just want user parms.
            for (MethodDef element : methods) {
                if (element.methodName.equalsIgnoreCase(methodName)) {
                    ArrayList<MethodParmDef> filteredParms =
                            new ArrayList<MethodParmDef>();
                    MethodParmDef[] parms = element.methodParms;
                    for (MethodParmDef element2 : parms) {
                        if (element2.parmType
                                .equalsIgnoreCase(MethodParmDef.USER_INPUT)) {
                            filteredParms.add(element2);
                        }
                    }
                    methodParms = filteredParms.toArray(METHOD_PARM_DEF_TYPE);
                }
            }
        } else {
            String message =
                    "[DefaultAccess] Old-style disseminators are no longer supported ";
            throw new DisseminatorNotFoundException(message);
            //        reader = m_manager.getReader(Server.GLOBAL_CHOICE, context, PID);
            //        methodParms = reader.getObjectMethodParms(sDefPID, methodName, versDateTime);
        }

        // Put valid method parameters and their attributes into hashtable
        if (methodParms != null) {
            for (int i = 0; i < methodParms.length; i++) {
                methodParm = methodParms[i];
                h_validParms.put(methodParm.parmName, methodParm);
                if (logger.isDebugEnabled()) {
                    logger.debug("methodParms[" + i + "]: "
                                 + methodParms[i].parmName + "\nlabel: "
                                 + methodParms[i].parmLabel + "\ndefault: "
                                 + methodParms[i].parmDefaultValue + "\nrequired: "
                                 + methodParms[i].parmRequired + "\ntype: "
                                 + methodParms[i].parmType);
                    for (String element : methodParms[i].parmDomainValues) {
                        logger.debug("domainValue: {}", element);
                    }
                }
            }
        }

        if (!h_validParms.isEmpty()) {
            // Iterate over valid parmameters to check for any missing required parms.
            Enumeration<String> e = h_validParms.keys();
            while (e.hasMoreElements()) {
                String validName = e.nextElement();
                MethodParmDef mp = h_validParms.get(validName);
                if (mp.parmRequired && h_userParms.get(validName) == null) {
                    // This is a fatal error. A required method parameter does not
                    // appear in the list of user supplied parameters.
                    sb.append("The required parameter \"" + validName
                              + "\" was not found in the "
                              + "user-supplied parameter list.");
                    throw new InvalidUserParmException("[Invalid User Parameters] "
                                                       + sb.toString());
                }
            }

            // Iterate over each user supplied parameter name
            Enumeration<String> parmNames = h_userParms.keys();
            while (parmNames.hasMoreElements()) {
                String parmName = parmNames.nextElement();
                methodParm = h_validParms.get(parmName);
                if (methodParm != null && methodParm.parmName != null) {
                    // Method has one or more parameters defined
                    // Check for default value if user-supplied value is null or empty
                    String value = h_userParms.get(methodParm.parmName);
                    if (value == null || value.equalsIgnoreCase("")) {
                        // Value of user-supplied parameter is  null or empty
                        if (methodParm.parmDefaultValue != null) {
                            // Default value is specified for this parameter.
                            // Substitute default value.
                            h_userParms.put(methodParm.parmName,
                                            methodParm.parmDefaultValue);
                        } else {
                            // This is a non-fatal error. There is no default specified
                            // for this parameter and the user has supplied no value for
                            // the parameter. The value of the empty string will be used
                            // as the value of the parameter.
                            logger.warn("The method parameter \""
                                        + methodParm.parmName
                                        + "\" has no default value and no "
                                        + "value was specified by the user.  "
                                        + "The value of the empty string has "
                                        + "been assigned to this parameter.");
                        }
                    } else {
                        // Value of user-supplied parameter contains a value.
                        // Validate the supplied value against the parmDomainValues list.
                        String[] parmDomainValues = methodParm.parmDomainValues;
                        if (parmDomainValues.length > 0) {
                            if (!parmDomainValues[0].equalsIgnoreCase("null")) {
                                boolean isValidValue = false;
                                String userValue =
                                        h_userParms.get(methodParm.parmName);
                                for (String element : parmDomainValues) {
                                    if (userValue.equalsIgnoreCase(element)
                                        || element.equalsIgnoreCase("null")) {
                                        isValidValue = true;
                                    }
                                }
                                if (!isValidValue) {
                                    for (int i = 0; i < parmDomainValues.length; i++) {
                                        sb.append(parmDomainValues[i]);
                                        if (i != parmDomainValues.length - 1) {
                                            sb.append(", ");
                                        }
                                    }
                                    sb
                                            .append("The method parameter \""
                                                    + methodParm.parmName
                                                    + "\" with a value of \""
                                                    + h_userParms
                                                    .get(methodParm.parmName)
                                                    + "\" is not allowed for the method \""
                                                    + methodName
                                                    + "\". Allowed values for this "
                                                    + "method include \""
                                                    + sb.toString() + "\".");
                                    isValid = false;
                                }
                            }
                        }
                    }
                } else {
                    // This is a fatal error. A user-supplied parameter name does
                    // not match any valid parameter names for this method.
                    sb.append("The method parameter \"" + parmName
                              + "\" is not valid for the method \"" + methodName
                              + "\".");
                    isValid = false;
                }
            }
        } else {
            // There are no method parameters define for this method.
            if (!h_userParms.isEmpty()) {
                // This is an error. There are no method parameters defined for
                // this method and user parameters are specified in the
                // dissemination request.
                Enumeration<String> e = h_userParms.keys();
                while (e.hasMoreElements()) {
                    sb.append("The method parameter \"" + e.nextElement()
                              + "\" is not valid for the method \"" + methodName
                              + "\"." + "The method \"" + methodName
                              + "\" defines no method parameters.");
                }
                throw new InvalidUserParmException("[Invalid User Parameters] "
                                                   + sb.toString());
            }
        }
        if (!isValid) {
            throw new InvalidUserParmException("[Invalid User Parameter] "
                                               + sb.toString());
        }
        return;
    }

    private String getDissIndexViewURL(String reposBaseURL,
                                       String fedoraContext,
                                       String PID,
                                       Date versDateTime) {
        String dissIndexURL = null;

        try {
            if (versDateTime == null) {
                dissIndexURL =
                        reposBaseURL + "/" + fedoraContext + "/objects/" + URLEncoder.encode(PID, "UTF-8")
                        + "/methods/" + URLEncoder.encode("fedora-system:3", "UTF-8") + "/viewMethodIndex";
            } else {
                dissIndexURL =
                        reposBaseURL + "/" + fedoraContext + "/objects/" + URLEncoder.encode(PID, "UTF-8")
                        + "/methods" + URLEncoder.encode("fedora-system:3", "UTF-8") + "/viewMethodIndex?asOfDateTime="
                        + DateUtility.convertDateToString(versDateTime);
            }
        } catch (UnsupportedEncodingException e) {
            // should never happen...
            throw new RuntimeException(e);
        }
        return dissIndexURL;
    }

    // FIXIT!! Consider implications of hard-coding the default dissemination
    // aspects of the URL (e.g. fedora-system3 as the PID and viewItemIndex.
    private String getItemIndexViewURL(String reposBaseURL,
                                       String fedoraContext,
                                       String PID,
                                       Date versDateTime) {
        String itemIndexURL = null;

        try {
            if (versDateTime == null) {
                itemIndexURL =
                        reposBaseURL + "/" + fedoraContext + "/objects/" + URLEncoder.encode(PID, "UTF-8")
                        + "/methods/" + URLEncoder.encode("fedora-system:3", "UTF-8") + "/viewItemIndex";
            } else {
                itemIndexURL =
                        reposBaseURL + "/" + fedoraContext + "/objects/" + URLEncoder.encode(PID, "UTF-8")
                        + "/methods/" + URLEncoder.encode("fedora-system:3", "UTF-8") + "/viewItemIndex?asOfDateTime="
                        + DateUtility.convertDateToString(versDateTime);
            }
        } catch (UnsupportedEncodingException e) {
            // should never happen...
            throw new RuntimeException(e);
        }
        return itemIndexURL;
    }

    private String getReposBaseURL(String protocol, String port) {
        String reposBaseURL = null;
        String fedoraServerHost = getServer().getParameter("fedoraServerHost");
        if (fedoraServerHost == null || fedoraServerHost.isEmpty()) {
            logger.warn("Configuration parameter fedoraServerHost is empty.");
            try {
                InetAddress hostIP = InetAddress.getLocalHost();
                fedoraServerHost = hostIP.getHostName();
            } catch (UnknownHostException e) {
                logger.error("Unable to resolve host of Fedora server", e);
                fedoraServerHost = "localhost";
            }
        }
        reposBaseURL = protocol + "://" + fedoraServerHost + ":" + port;
        return reposBaseURL;
    }

    @Override
    public MIMETypedStream getDatastreamDissemination(Context context,
                                                      String PID,
                                                      String dsID,
                                                      Date asOfDateTime)
            throws ServerException {
        PID = Server.getPID(PID).toString();
        m_authorizationModule.enforceGetDatastreamDissemination(context,
                                                                PID,
                                                                dsID,
                                                                asOfDateTime);
        MIMETypedStream mimeTypedStream = null;
        long startTime = (logger.isDebugEnabled()) ? new Date().getTime() : 0;
        DOReader reader =
                m_manager.getReader(Server.USE_DEFINITIVE_STORE, context, PID);

        Datastream ds = reader.GetDatastream(dsID, asOfDateTime);
        if (ds == null) {
            String message =
                    "[DefaulAccess] No datastream could be returned. "
                    + "Either there is no datastream for the digital "
                    + "object \""
                    + PID
                    + "\" with datastream ID of \""
                    + dsID
                    + " \"  OR  there are no datastreams that match the specified "
                    + "date/time value of \""
                    + DateUtility.convertDateToString(asOfDateTime)
                    + " \"  .";
            throw new DatastreamNotFoundException(message);
        }

        if (ds.DSControlGrp.equalsIgnoreCase("E")) {
            DatastreamReferencedContent drc =
                    (DatastreamReferencedContent) reader
                            .GetDatastream(dsID, asOfDateTime);
            ContentManagerParams params = new ContentManagerParams(drc.DSLocation,
                                                                   drc.DSMIME, null, null);
            params.setContext(context);
            mimeTypedStream = m_externalContentManager.getExternalContent(params);
        } else if (ds.DSControlGrp.equalsIgnoreCase("M")) {
            DatastreamManagedContent dmc =
                    (DatastreamManagedContent) reader
                            .GetDatastream(dsID, asOfDateTime);

            mimeTypedStream = new MIMETypedStream(ds.DSMIME, dmc.getContentStream(context), null,ds.DSSize);
        } else if (ds.DSControlGrp.equalsIgnoreCase("X")) {
            DatastreamXMLMetadata dxm =
                    (DatastreamXMLMetadata) reader.GetDatastream(dsID,
                                                                 asOfDateTime);
            mimeTypedStream = new MIMETypedStream(ds.DSMIME, dxm.getContentStream(context), null, ds.DSSize);
        } else if (ds.DSControlGrp.equalsIgnoreCase("R")) {
            DatastreamReferencedContent drc =
                    (DatastreamReferencedContent) reader
                            .GetDatastream(dsID, asOfDateTime);
            // The dsControlGroupType of Redirect("R") is a special control type
            // used primarily for streaming media. Datastreams of this type are
            // not mediated (proxied by Fedora) and their physical dsLocation is
            // simply redirected back to the client. Therefore, the contents
            // of the MIMETypedStream returned for dissemination requests will
            // contain the raw URL of the dsLocation and will be assigned a
            // special fedora-specific MIME type to identify the stream as
            // a MIMETypedStream whose contents contain a URL to which the client
            // should be redirected.
            InputStream inStream =
                    new ByteArrayInputStream(drc.DSLocation
                            .getBytes(Charset.forName("UTF-8")));
            mimeTypedStream =
                    new MIMETypedStream("application/fedora-redirect",
                            inStream,
                            null);
        }
        if (logger.isDebugEnabled()) {
            long stopTime = new Date().getTime();
            long interval = stopTime - startTime;
            logger.debug("Roundtrip getDatastreamDissemination: {} milliseconds.", interval);
        }
        return mimeTypedStream;
    }

}
