/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.access;

import java.io.File;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

import fedora.common.Constants;

import fedora.server.Context;
import fedora.server.Module;
import fedora.server.access.defaultdisseminator.DefaultDisseminatorImpl;
import fedora.server.access.defaultdisseminator.ServiceMethodDispatcher;
import fedora.server.errors.GeneralException;
import fedora.server.errors.MethodNotFoundException;
import fedora.server.errors.ServerException;
import fedora.server.search.FieldSearchQuery;
import fedora.server.search.FieldSearchResult;
import fedora.server.storage.DOReader;
import fedora.server.storage.types.DatastreamDef;
import fedora.server.storage.types.MIMETypedStream;
import fedora.server.storage.types.MethodDef;
import fedora.server.storage.types.ObjectMethodsDef;
import fedora.server.storage.types.Property;

/**
 * The implementation of the Dynamic Access module.
 *
 * <p>The Dynamic Access module will associate dynamic disseminators with a 
 * digital object. It will look to the Fedora repository configuration file 
 * to obtain a list of dynamic disseminators.  Currently, the system supports 
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
public class DynamicAccessImpl {

    private final Access m_access;

    private final ServiceMethodDispatcher dispatcher;

    private File reposHomeDir = null;

    private Hashtable dynamicServiceToDeployment = null;

    public DynamicAccessImpl(Access m_access,
                             File reposHomeDir,
                             Hashtable dynamicSDefToDep) {
        dispatcher = new ServiceMethodDispatcher();
        this.m_access = m_access;
        this.reposHomeDir = reposHomeDir;
        this.dynamicServiceToDeployment = dynamicSDefToDep;
    }

    /**
     * Get a list of Service Definition identifiers for dynamic disseminators
     * associated with the digital object.
     * 
     * @param context
     * @param PID
     *        identifier of digital object being reflected upon
     * @param asOfDateTime
     * @return an array of Service Definition PIDs
     * @throws ServerException
     */
    public String[] getServiceDefinitions(Context context,
                                           String PID,
                                           Date asOfDateTime)
            throws ServerException {
        // FIXIT! In FUTURE this method might consult some source that tells
        // what Service Definitions are appropriate to dynamically associate
        // with the object.  The rules for association might be based on the
        // context or based on something about the particular object (PID).
        // There is one rule that is always true - associate the Default
        // Service Definition with EVERY object. For now we will just take the
        // dynamic Service Definitions that were loaded by DynamicAccessModule.
        // NOTE: AT THIS TIME THERE THERE IS JUST ONE LOADED, NAMELY,
        // THE DEFAULT DISSEMINATOR SDEF (sDefPID = fedora-system:3)

        ArrayList sdefs = new ArrayList();
        Iterator iter = dynamicServiceToDeployment.keySet().iterator();
        while (iter.hasNext()) {
            sdefs.add(iter.next());
        }
        return (String[]) sdefs.toArray(new String[0]);
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
     *        identifier of dynamic Service Definition
     * @param asOfDateTime
     * @return an array of method definitions
     * @throws ServerException
     */
    public MethodDef[] getMethods(Context context,
                                          String PID,
                                          String sDefPID,
                                          Date asOfDateTime)
            throws ServerException {
        Class deploymentClass = (Class) dynamicServiceToDeployment.get(sDefPID);
        if (deploymentClass != null) {
            try {
                Method method =
                        deploymentClass.getMethod("reflectMethods", (Class[]) null);
                return (MethodDef[]) method.invoke(null, (Object[]) null);
            } catch (Exception e) {
                throw new GeneralException("[DynamicAccessImpl] returned error when "
                        + "attempting to get dynamic method definitions. "
                        + "The underlying error class was: "
                        + e.getClass().getName()
                        + ". The message "
                        + "was \""
                        + e.getMessage() + "\"");
            }
        }
        throw new MethodNotFoundException("[DynamicAccessImpl] The object, "
                + PID + " does not have the dynamic Service Definition "
                + sDefPID);
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
     *        identifier of dynamic Service Definition
     * @param asOfDateTime
     * @return MIME-typed stream containing XML-encoded method definitions
     * @throws ServerException
     */
    public MIMETypedStream getMethodsXML(Context context,
                                                 String PID,
                                                 String sDefPID,
                                                 Date asOfDateTime)
            throws ServerException {
        return null;
    }

    private String getReposBaseURL(String protocol, String port) {
        String reposBaseURL = null;
        String fedoraServerHost =
                ((Module) m_access).getServer()
                        .getParameter("fedoraServerHost");
        reposBaseURL = protocol + "://" + fedoraServerHost + ":" + port;
        return reposBaseURL;
    }

    /**
     * Perform a dissemination for a method that belongs to a dynamic
     * disseminator that is associate with the digital object. The method
     * belongs to the dynamic Service Definition and is implemented by a
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
                                            Date asOfDateTime,
                                            DOReader reader)
            throws ServerException {
        if (sDefPID.equalsIgnoreCase("fedora-system:3")) {
            // FIXIT!! Use lookup to dynamicSDefToDep table to get class for
            // DefaultDisseminatorImpl and construct via Java reflection.

            String reposBaseURL =
                    getReposBaseURL(context
                                            .getEnvironmentValue(Constants.HTTP_REQUEST.SECURITY.uri)
                                            .equals(Constants.HTTP_REQUEST.SECURE.uri) ? "https"
                                            : "http",
                                    context
                                            .getEnvironmentValue(Constants.HTTP_REQUEST.SERVER_PORT.uri));

            Object result =
                    dispatcher
                            .invokeMethod(new DefaultDisseminatorImpl(context,
                                                                      asOfDateTime,
                                                                      reader,
                                                                      m_access,
                                                                      reposBaseURL,
                                                                      reposHomeDir),
                                          methodName,
                                          userParms);
            if (result
                    .getClass()
                    .getName()
                    .equalsIgnoreCase("fedora.server.storage.types.MIMETypedStream")) {
                return (MIMETypedStream) result;
            } else {
                throw new GeneralException("[DynamicAccessImpl] returned error. "
                        + "Internal service must return a MIME typed stream. "
                        + "(see fedora.server.storage.types.MIMETypedStream)");
            }
        } else {
            // FIXIT! (FUTURE) Open up the possibility of there being other
            // kinds of dynamic behaviors.  Use the sDefPID to locate the
            // appropriate deployment for the dynamic behavior.  In future
            // we want the deployment for a dynamic service defintion to
            // be able to be either an internal services, a local services,
            // or a distributed service.  We'll have to rework some things to
            // be able to see what kind of deployment we have, and to do the
            // request dispatching appropriately.
        }
        return null;
    }

    /**
     * Get the definitions for all dynamic disseminations on the object. This
     * will return the method definitions for all methods for all of the dynamic
     * disseminators associated with the object.
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
        String[] sDefPIDs = getServiceDefinitions(context, PID, asOfDateTime);
        Date versDateTime = asOfDateTime;
        ArrayList objectMethods = new ArrayList();
        for (String element : sDefPIDs) {
            MethodDef[] methodDefs =
                    getMethods(context, PID, element, asOfDateTime);
            for (MethodDef element2 : methodDefs) {
                ObjectMethodsDef method = new ObjectMethodsDef();
                method.PID = PID;
                method.asOfDate = versDateTime;
                method.sDefPID = element;
                method.methodName = element2.methodName;
                method.methodParmDefs = element2.methodParms;
                objectMethods.add(method);
            }
        }
        return (ObjectMethodsDef[]) objectMethods
                .toArray(new ObjectMethodsDef[0]);
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
        // FIXIT! Return something here.
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
        return null;
    }

    public boolean isDynamicDeployment(Context context,
                                               String PID,
                                               String sDefPID)
            throws ServerException {
        if (dynamicServiceToDeployment.containsKey(sDefPID)) {
            return true;
        }
        return false;
    }

    // FIXIT: What do these mean in this context...anything?
    // Maybe these methods' exposure needs to be re-thought?
    public MIMETypedStream getDatastreamDissemination(Context context,
                                                      String PID,
                                                      String dsID,
                                                      Date asOfDateTime)
            throws ServerException {
        return null;
    }

    public DatastreamDef[] listDatastreams(Context context,
                                           String PID,
                                           Date asOfDateTime)
            throws ServerException {
        return null;
    }
}
