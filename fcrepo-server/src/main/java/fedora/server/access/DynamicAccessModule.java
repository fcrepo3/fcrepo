/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.access;

import java.io.File;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;

import fedora.server.Context;
import fedora.server.Module;
import fedora.server.Server;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ServerException;
import fedora.server.search.FieldSearchQuery;
import fedora.server.search.FieldSearchResult;
import fedora.server.storage.DOManager;
import fedora.server.storage.types.DatastreamDef;
import fedora.server.storage.types.MIMETypedStream;
import fedora.server.storage.types.MethodDef;
import fedora.server.storage.types.ObjectMethodsDef;
import fedora.server.storage.types.Property;

/**
 * Module Wrapper for DynamicAccessImpl.
 * 
 * <p>The Dynamic Access module will associate dynamic disseminators with
 * a digital object.  It will look to the Fedora repository configuration file 
 * to obtain a list of dynamic disseminators. Currently, the system supports 
 * two types of dynamic disseminators: - Default (SDefPID=fedora-system:3 and
 * SDepPID=fedora-system:4) - Bootstrap (SDefPID=fedora-system:1 and
 * SDepPID=fedora-system:2). The Default disseminator that is associated with
 * every object in the repository. The Default Disseminator endows the objects
 * with a set of basic generic behaviors that enable a simplistic view of the
 * object contents (the Item Index) and a list of all disseminations available
 * on the object (the Dissemination Index). The Bootstrap disseminator is
 * associated with every Service Definition and Service Deployment object. It
 * defines methods to get the special metadata datastreams out of them, and some
 * other methods. (NOTE: The Bootstrap Disseminator functionality is NOT YET
 * IMPLEMENTED.
 * 
 * @author Sandy Payette
 */
public class DynamicAccessModule
        extends Module
        implements Access {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(DynamicAccessModule.class.getName());

    /**
     * An instance of the core implementation class for DynamicAccess. The
     * DynamicAccessModule acts as a wrapper to this class.
     */
    private DynamicAccessImpl da = null;;

    /** Current DOManager of the Fedora server. */
    private DOManager m_manager;

    /** Main Access module of the Fedora server. */
    private Access m_access;

    private Hashtable dynamicServiceToDeployment = null;

    private File reposHomeDir = null;

    /**
     * Creates and initializes the Dynmamic Access Module. When the server is
     * starting up, this is invoked as part of the initialization process.
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
    public DynamicAccessModule(Map moduleParameters, Server server, String role)
            throws ModuleInitializationException {
        super(moduleParameters, server, role);
    }

    @Override
    public void postInitModule() throws ModuleInitializationException {
        m_manager =
                (DOManager) getServer()
                        .getModule("fedora.server.storage.DOManager");
        if (m_manager == null) {
            throw new ModuleInitializationException("[DynamicAccessModule] "
                    + "Can't get a DOManager from Server.getModule", getRole());
        }
        m_access =
                (Access) getServer().getModule("fedora.server.access.Access");
        if (m_access == null) {
            throw new ModuleInitializationException("[DynamicAccessModule] "
                                                            + "Can't get a ref to Access from Server.getModule",
                                                    getRole());
        }
        // Get the repository Base URL
        InetAddress hostIP = null;
        try {
            hostIP = InetAddress.getLocalHost();
        } catch (UnknownHostException uhe) {
            LOG.error("Unable to resolve Fedora host", uhe);
        }
        String fedoraServerHost = getServer().getParameter("fedoraServerHost");
        if (fedoraServerHost == null || fedoraServerHost.equals("")) {
            fedoraServerHost = hostIP.getHostName();
        }
        reposHomeDir = getServer().getHomeDir();

        // FIXIT!! In the future, we want to read the repository configuration
        // file for the list of dynamic Service Definitions and their
        // associated internal service classes.  For now, we are explicitly
        // loading up the Default service def/dep since this is the only
        // thing supported in the system right now.
        dynamicServiceToDeployment = new Hashtable();
        try {
            dynamicServiceToDeployment.put("fedora-system:3", Class
                    .forName(getParameter("fedora-system:4")));
        } catch (Exception e) {
            throw new ModuleInitializationException(e.getMessage(),
                                                    "fedora.server.validation.DOValidatorModule");
        }

        // get ref to the Dynamic Access implementation class
        da = new DynamicAccessImpl(m_access, reposHomeDir, dynamicServiceToDeployment);
    }

    /**
     * Get a list of service definition identifiers for dynamic disseminators
     * associated with the digital object.
     * 
     * @param context
     * @param PID
     *        identifier of digital object being reflected upon
     * @param asOfDateTime
     * @return an array of service definition PIDs
     * @throws ServerException
     */
    public String[] getServiceDefinitions(Context context,
                                           String PID,
                                           Date asOfDateTime)
            throws ServerException {
        //m_ipRestriction.enforce(context);
        return da.getServiceDefinitions(context, PID, asOfDateTime);
    }

    /**
     * Get the method defintions for a given dynamic disseminator that
     * is associated with the digital object. The dynamic disseminator is
     * identified by the sDefPID.
     * 
     * @param context
     * @param PID
     *        identifier of digital object being reflected upon
     * @param sDefPID
     *        identifier of dynamic service definition
     * @param asOfDateTime
     * @return an array of method definitions
     * @throws ServerException
     */
    public MethodDef[] getMethods(Context context,
                                          String PID,
                                          String sDefPID,
                                          Date asOfDateTime)
            throws ServerException {
        //m_ipRestriction.enforce(context);
        return da.getMethods(context, PID, sDefPID, asOfDateTime);
    }

    /**
     * Get an XML encoding of the service defintions for a given dynamic
     * disseminator that is associated with the digital object. The dynamic
     * disseminator is identified by the sDefPID.
     * 
     * @param context
     * @param PID
     *        identifier of digital object being reflected upon
     * @param sDefPID
     *        identifier of dynamic service definition
     * @param asOfDateTime
     * @return MIME-typed stream containing XML-encoded method definitions
     * @throws ServerException
     */
    public MIMETypedStream getMethodsXML(Context context,
                                                 String PID,
                                                 String sDefPID,
                                                 Date asOfDateTime)
            throws ServerException {
        //m_ipRestriction.enforce(context);
        return da.getMethodsXML(context, PID, sDefPID, asOfDateTime);
    }

    public MIMETypedStream getDatastreamDissemination(Context context,
                                                      String PID,
                                                      String dsID,
                                                      Date asOfDateTime)
            throws ServerException {
        return da.getDatastreamDissemination(context, PID, dsID, asOfDateTime);
    }

    /**
     * Perform a dissemination for a method that belongs to a dynamic
     * disseminator that is associate with the digital object. The method
     * belongs to the dynamic service definition and is implemented by a
     * dynamic Service Deployment (which is an internal service in the
     * repository access subsystem).
     * 
     * @param context
     * @param PID
     *        identifier of the digital object being disseminated
     * @param sDefPID
     *        identifier of dynamic Service Definition
     * @param methodName
     * @param userParms
     * @param asOfDateTime
     * @return a MIME-typed stream containing the dissemination result
     * @throws ServerException
     */
    public MIMETypedStream getDissemination(Context context,
                                            String PID,
                                            String sDefPID,
                                            String methodName,
                                            Property[] userParms,
                                            Date asOfDateTime)
            throws ServerException {
        
        setParameter("useCachedObject", "" + false); //<<<STILL REQUIRED?

        return da
                .getDissemination(context,
                                  PID,
                                  sDefPID,
                                  methodName,
                                  userParms,
                                  asOfDateTime,
                                  m_manager
                                          .getReader(Server.USE_DEFINITIVE_STORE,
                                                     context,
                                                     PID));
    }

    /**
     * Get the definitions for all dynamic disseminations on the object.
     * 
     * <p>This will return the method definitions for all methods for all of 
     * the dynamic disseminators associated with the object.
     * 
     * @param context
     * @param PID
     *        identifier of digital object being reflected upon
     * @param asOfDateTime
     * @return an array of object method definitions
     * @throws ServerException
     */
    public ObjectMethodsDef[] listMethods(Context context,
                                          String PID,
                                          Date asOfDateTime)
            throws ServerException {
        return da.listMethods(context, PID, asOfDateTime);
    }

    /**
     * Get the profile information for the digital object. This contain key
     * metadata and URLs for the Dissemination Index and Item Index of the
     * object.
     * 
     * @param context
     * @param PID
     *        identifier of digital object being reflected upon
     * @param asOfDateTime
     * @return an object profile data structure
     * @throws ServerException
     */
    public ObjectProfile getObjectProfile(Context context,
                                          String PID,
                                          Date asOfDateTime)
            throws ServerException {
        return null;
    }

    // FIXIT: What do these mean in this context...anything?
    // Maybe these methods' exposure needs to be re-thought?
    public FieldSearchResult findObjects(Context context,
                                         String[] resultFields,
                                         int maxResults,
                                         FieldSearchQuery query)
            throws ServerException {
        return null;
    }

    // FIXIT: What do these mean in this context...anything?
    // Maybe these methods' exposure needs to be re-thought?
    public FieldSearchResult resumeFindObjects(Context context,
                                               String sessionToken)
            throws ServerException {
        return null;
    }

    // FIXIT: What do these mean in this context...anything?
    // Maybe these methods' exposure needs to be re-thought?
    public RepositoryInfo describeRepository(Context context)
            throws ServerException {
        return null;
    }

    // FIXIT: What do these mean in this context...anything?
    // Maybe these methods' exposure needs to be re-thought?
    public String[] getObjectHistory(Context context, String PID)
            throws ServerException {
        return da.getObjectHistory(context, PID);
    }

    protected boolean isDynamicService(Context context,
                                                  String PID,
                                                  String sDefPID)
            throws ServerException {
        return da.isDynamicDeployment(context, PID, sDefPID);
    }

    public DatastreamDef[] listDatastreams(Context context,
                                           String PID,
                                           Date asOfDateTime)
            throws ServerException {
        return da.listDatastreams(context, PID, asOfDateTime);
    }
}
