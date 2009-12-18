/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.access;

import java.io.File;

import java.rmi.RemoteException;

import org.apache.axis.AxisFault;
import org.apache.axis.types.NonNegativeInteger;

import org.apache.log4j.Logger;

import fedora.common.Constants;

import fedora.server.Context;
import fedora.server.ReadOnlyContext;
import fedora.server.Server;
import fedora.server.errors.InitializationException;
import fedora.server.errors.ServerInitializationException;
import fedora.server.types.gen.FieldSearchQuery;
import fedora.server.types.gen.FieldSearchResult;
import fedora.server.utilities.AxisUtility;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.TypeUtility;

/**
 * Implements the Fedora Access SOAP service.
 *
 * @author Ross Wayland
 */
public class FedoraAPIABindingSOAPHTTPImpl
        implements fedora.server.access.FedoraAPIA {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(FedoraAPIABindingSOAPHTTPImpl.class.getName());

    /** The Fedora Server instance. */
    private static Server s_server;

    /** Whether the service has initialized... true if initialized. */
    private static boolean s_initialized;

    /** The exception indicating that initialization failed. */
    private static InitializationException s_initException;

    /** Instance of the access subsystem */
    private static Access s_access;

    /** Context for cached objects. */
    //private static ReadOnlyContext context;
    /** Debug toggle for testing. */
    private static boolean debug = false;

    /** Before fulfilling any requests, make sure we have a server instance. */
    static {
        try {
            String fedoraHome = Constants.FEDORA_HOME;
            if (fedoraHome == null) {
                s_initialized = false;
                s_initException =
                        new ServerInitializationException("Server failed to initialize because FEDORA_HOME "
                                + "is undefined");
            } else {
                s_server = Server.getInstance(new File(fedoraHome));
                s_initialized = true;
                s_access =
                        (Access) s_server
                                .getModule("fedora.server.access.Access");
                Boolean B1 = new Boolean(s_server.getParameter("debug"));
                debug = B1.booleanValue();
            }
        } catch (InitializationException ie) {
            LOG.warn("Server initialization failed", ie);
            s_initialized = false;
            s_initException = ie;
        }
    }

    public java.lang.String[] getObjectHistory(java.lang.String PID)
            throws java.rmi.RemoteException {
        Context context = ReadOnlyContext.getSoapContext();
        assertInitialized();
        try {
            String[] sDefs = s_access.getObjectHistory(context, PID);
            if (sDefs != null && debug) {
                for (int i = 0; i < sDefs.length; i++) {
                    LOG.debug("sDef[" + i + "] = " + sDefs[i]);
                }
            }
            return sDefs;
        } catch (Throwable th) {
            LOG.error("Error getting object history", th);
            throw AxisUtility.getFault(th);
        }
    }

    /**
     * <p>
     * Gets a MIME-typed bytestream containing the result of a dissemination.
     * </p>
     *
     * @param PID
     *        The persistent identifier of the Digital Object.
     * @param sDefPID
     *        The persistent identifier of the Service Definition object.
     * @param methodName
     *        The name of the method.
     * @param asOfDateTime
     *        The version datetime stamp of the digital object.
     * @param userParms
     *        An array of user-supplied method parameters and values.
     * @return A MIME-typed stream containing the dissemination result.
     * @throws java.rmi.RemoteException
     */
    public fedora.server.types.gen.MIMETypedStream getDissemination(String PID,
                                                                    String sDefPID,
                                                                    String methodName,
                                                                    fedora.server.types.gen.Property[] userParms,
                                                                    String asOfDateTime)
            throws java.rmi.RemoteException {
        Context context = ReadOnlyContext.getSoapContext();
        assertInitialized();
        try {
            fedora.server.storage.types.Property[] properties =
                    TypeUtility
                            .convertGenPropertyArrayToPropertyArray(userParms);
            fedora.server.storage.types.MIMETypedStream mimeTypedStream =
                    s_access
                            .getDissemination(context,
                                              PID,
                                              sDefPID,
                                              methodName,
                                              properties,
                                              DateUtility
                                                      .convertStringToDate(asOfDateTime));
            fedora.server.types.gen.MIMETypedStream genMIMETypedStream =
                    TypeUtility
                            .convertMIMETypedStreamToGenMIMETypedStream(mimeTypedStream);
            return genMIMETypedStream;
        } catch (Throwable th) {
            LOG.error("Error getting dissemination", th);
            throw AxisUtility.getFault(th);
        }
    }

    public fedora.server.types.gen.MIMETypedStream getDatastreamDissemination(String PID,
                                                                              String dsID,
                                                                              String asOfDateTime)
            throws java.rmi.RemoteException {
        Context context = ReadOnlyContext.getSoapContext();
        assertInitialized();
        try {

            fedora.server.storage.types.MIMETypedStream mimeTypedStream =
                    s_access
                            .getDatastreamDissemination(context,
                                                        PID,
                                                        dsID,
                                                        DateUtility
                                                                .convertStringToDate(asOfDateTime));
            fedora.server.types.gen.MIMETypedStream genMIMETypedStream =
                    TypeUtility
                            .convertMIMETypedStreamToGenMIMETypedStream(mimeTypedStream);
            return genMIMETypedStream;
        } catch (OutOfMemoryError oome) {
            LOG.error("Out of memory error getting "+ dsID +
                      " datastream dissemination for " + PID);
            String exceptionText = "The datastream you are attempting to retrieve is too large " +
                                   "to transfer via getDatastreamDissemination (as determined " +
                                   "by the server memory allocation.) Consider retrieving this " +
                                   "datastream via REST at: ";
            String restURL = describeRepository().getRepositoryBaseURL() +
                             "/get/" + PID + "/" + dsID;
            throw AxisFault.makeFault(new Exception(exceptionText + restURL));
        } catch (Throwable th) {
            LOG.error("Error getting datastream dissemination", th);
            throw AxisUtility.getFault(th);
        }
    }

    public FieldSearchResult findObjects(String[] resultFields,
                                         NonNegativeInteger maxResults,
                                         FieldSearchQuery query)
            throws RemoteException {
        Context context = ReadOnlyContext.getSoapContext();
        assertInitialized();
        try {
            fedora.server.search.FieldSearchResult result =
                    s_access
                            .findObjects(context,
                                         resultFields,
                                         maxResults.intValue(),
                                         TypeUtility
                                                 .convertGenFieldSearchQueryToFieldSearchQuery(query));
            return TypeUtility
                    .convertFieldSearchResultToGenFieldSearchResult(result);
        } catch (Throwable th) {
            LOG.error("Error finding objects", th);
            throw AxisUtility.getFault(th);
        }
    }

    public FieldSearchResult resumeFindObjects(String sessionToken)
            throws java.rmi.RemoteException {
        Context context = ReadOnlyContext.getSoapContext();
        assertInitialized();
        try {
            fedora.server.search.FieldSearchResult result =
                    s_access.resumeFindObjects(context, sessionToken);
            return TypeUtility
                    .convertFieldSearchResultToGenFieldSearchResult(result);
        } catch (Throwable th) {
            LOG.error("Error resuming finding objects", th);
            throw AxisUtility.getFault(th);
        }
    }

    public fedora.server.types.gen.ObjectMethodsDef[] listMethods(String PID,
                                                                  String asOfDateTime)
            throws java.rmi.RemoteException {
        Context context = ReadOnlyContext.getSoapContext();
        assertInitialized();
        try {
            fedora.server.storage.types.ObjectMethodsDef[] objectMethodDefs =
                    s_access.listMethods(context, PID, DateUtility
                            .convertStringToDate(asOfDateTime));
            fedora.server.types.gen.ObjectMethodsDef[] genObjectMethodDefs =
                    TypeUtility
                            .convertObjectMethodsDefArrayToGenObjectMethodsDefArray(objectMethodDefs);
            return genObjectMethodDefs;
        } catch (Throwable th) {
            LOG.error("Error listing methods", th);
            throw AxisUtility.getFault(th);
        }
    }

    public fedora.server.types.gen.DatastreamDef[] listDatastreams(String PID,
                                                                   String asOfDateTime)
            throws java.rmi.RemoteException {
        Context context = ReadOnlyContext.getSoapContext();
        assertInitialized();
        try {
            fedora.server.storage.types.DatastreamDef[] datastreamDefs =
                    s_access.listDatastreams(context, PID, DateUtility
                            .convertStringToDate(asOfDateTime));
            fedora.server.types.gen.DatastreamDef[] genDatastreamDefs =
                    TypeUtility
                            .convertDatastreamDefArrayToGenDatastreamDefArray(datastreamDefs);
            return genDatastreamDefs;
        } catch (Throwable th) {
            LOG.error("Error listing datastreams", th);
            throw AxisUtility.getFault(th);
        }
    }

    /**
     * <p>
     * Gets the object profile which included key metadata about the object and
     * URLs for the Dissemination Index and Item Index of the object.
     * </p>
     *
     * @param PID
     *        The persistent identifier for the digital object.
     * @param asOfDateTime
     *        The versioning datetime stamp.
     * @return The object profile data structure.
     * @throws java.rmi.RemoteException
     */
    public fedora.server.types.gen.ObjectProfile getObjectProfile(String PID,
                                                                  String asOfDateTime)
            throws java.rmi.RemoteException {
        Context context = ReadOnlyContext.getSoapContext();
        assertInitialized();
        try {
            fedora.server.access.ObjectProfile objectProfile =
                    s_access.getObjectProfile(context, PID, DateUtility
                            .convertStringToDate(asOfDateTime));
            fedora.server.types.gen.ObjectProfile genObjectProfile =
                    TypeUtility
                            .convertObjectProfileToGenObjectProfile(objectProfile);
            return genObjectProfile;
        } catch (Throwable th) {
            LOG.error("Error getting object profile", th);
            throw AxisUtility.getFault(th);
        }
    }

    /**
     * <p>
     * Gets key information about the repository.
     * </p>
     *
     * @return The repository info data structure.
     * @throws java.rmi.RemoteException
     */
    public fedora.server.types.gen.RepositoryInfo describeRepository()
            throws java.rmi.RemoteException {
        Context context = ReadOnlyContext.getSoapContext();
        assertInitialized();
        try {
            fedora.server.access.RepositoryInfo repositoryInfo =
                    s_access.describeRepository(context);
            fedora.server.types.gen.RepositoryInfo genRepositoryInfo =
                    TypeUtility.convertReposInfoToGenReposInfo(repositoryInfo);
            return genRepositoryInfo;
        } catch (Throwable th) {
            LOG.error("Error describing repository", th);
            throw AxisUtility.getFault(th);
        }
    }

    private void assertInitialized() throws java.rmi.RemoteException {
        if (!s_initialized) {
            AxisUtility.throwFault(s_initException);
        }
    }
}
