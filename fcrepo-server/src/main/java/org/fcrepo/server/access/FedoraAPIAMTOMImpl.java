/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.access;

import java.io.File;

import java.util.Arrays;
import java.util.List;

import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import javax.annotation.Resource;

import org.apache.cxf.binding.soap.SoapFault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;

import org.fcrepo.server.Context;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.InitializationException;
import org.fcrepo.server.errors.ServerInitializationException;
import org.fcrepo.server.utilities.CXFUtility;
import org.fcrepo.server.utilities.TypeUtility;

import org.fcrepo.utilities.DateUtility;

/**
 * @author Jiri Kremser
 */

public class FedoraAPIAMTOMImpl
        implements FedoraAPIAMTOM {

    private static final Logger LOG = LoggerFactory
            .getLogger(FedoraAPIAMTOMImpl.class);

    @Resource
    private WebServiceContext context;

    /** The Fedora Server instance. */
    private static Server s_server;

    /** Whether the service has initialized... true if initialized. */
    private static boolean s_initialized;

    /** The exception indicating that initialization failed. */
    private static InitializationException s_initException;

    /** Instance of the access subsystem */
    private static Access s_access;

    /** Context for cached objects. */
    // private static ReadOnlyContext context;
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
                                .getModule("org.fcrepo.server.access.Access");
                Boolean debugBool = new Boolean(s_server.getParameter("debug"));
                debug = debugBool.booleanValue();
            }
        } catch (InitializationException ie) {
            LOG.warn("Server initialization failed", ie);
            s_initialized = false;
            s_initException = ie;
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.access.FedoraAPIAMTOM#getDatastreamDissemination(java
     * .lang.String pid ,)java.lang.String dsID ,)java.lang.String asOfDateTime
     * )*
     */
    @Override
    public org.fcrepo.server.types.mtom.gen.MIMETypedStream getDatastreamDissemination(java.lang.String pid,
                                                                                       java.lang.String dsID,
                                                                                       java.lang.String asOfDateTime) {
        MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            org.fcrepo.server.storage.types.MIMETypedStream mimeTypedStream =
                    s_access.getDatastreamDissemination(context,
                                                        pid,
                                                        dsID,
                                                        DateUtility
                                                                .parseDateOrNull(asOfDateTime));
            org.fcrepo.server.types.mtom.gen.MIMETypedStream genMIMETypedStream =
                    TypeUtility
                            .convertMIMETypedStreamToGenMIMETypedStreamMTOM(mimeTypedStream);
            return genMIMETypedStream;
        } catch (OutOfMemoryError oome) {
            LOG.error("Out of memory error getting " + dsID
                    + " datastream dissemination for " + pid);
            String exceptionText =
                    "The datastream you are attempting to retrieve is too large "
                            + "to transfer via getDatastreamDissemination (as determined "
                            + "by the server memory allocation.) Consider retrieving this "
                            + "datastream via REST at: ";
            String restURL =
                    describeRepository().getRepositoryBaseURL() + "/get/" + pid
                            + "/" + dsID;
            throw CXFUtility.getFault(new Exception(exceptionText + restURL));
        } catch (Throwable th) {
            LOG.error("Error getting datastream dissemination", th);
            throw CXFUtility.getFault(th);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.access.FedoraAPIA#getDissemination(String pid
     * ,)String serviceDefinitionPid ,)String methodName
     * ,)org.fcrepo.server.types.gen.GetDissemination.Parameters parameters
     * ,)String asOfDateTime )*
     */
    @Override
    public org.fcrepo.server.types.mtom.gen.MIMETypedStream getDissemination(String pid,
                                                                             String serviceDefinitionPid,
                                                                             String methodName,
                                                                             org.fcrepo.server.types.mtom.gen.GetDissemination.Parameters parameters,
                                                                             String asOfDateTime) {
        MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            org.fcrepo.server.storage.types.Property[] properties =
                    TypeUtility
                            .convertGenPropertyArrayToPropertyArrayMTOM(parameters);
            org.fcrepo.server.storage.types.MIMETypedStream mimeTypedStream =
                    s_access.getDissemination(context,
                                              pid,
                                              serviceDefinitionPid,
                                              methodName,
                                              properties,
                                              DateUtility
                                                      .parseDateOrNull(asOfDateTime));
            org.fcrepo.server.types.mtom.gen.MIMETypedStream genMIMETypedStream =
                    TypeUtility
                            .convertMIMETypedStreamToGenMIMETypedStreamMTOM(mimeTypedStream);
            return genMIMETypedStream;
        } catch (Throwable th) {
            th.printStackTrace();
            LOG.error("Error getting dissemination", th);
            throw CXFUtility.getFault(th);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.access.FedoraAPIA#getObjectProfile(String pid
     * ,)String asOfDateTime )*
     */
    @Override
    public org.fcrepo.server.types.mtom.gen.ObjectProfile getObjectProfile(String pid,
                                                                           String asOfDateTime) {
        MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            org.fcrepo.server.access.ObjectProfile objectProfile =
                    s_access.getObjectProfile(context, pid, DateUtility
                            .parseDateOrNull(asOfDateTime));
            org.fcrepo.server.types.mtom.gen.ObjectProfile genObjectProfile =
                    TypeUtility
                            .convertObjectProfileToGenObjectProfileMTOM(objectProfile);
            return genObjectProfile;
        } catch (Throwable th) {
            LOG.error("Error getting object profile", th);
            throw CXFUtility.getFault(th);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.access.FedoraAPIA#findObjects(org.fcrepo.server.types
     * .gen.ArrayOfString resultFields ,)java.math.BigInteger maxResults
     * ,)org.fcrepo.server.types.gen.FieldSearchQuery query )*
     */
    @Override
    public org.fcrepo.server.types.mtom.gen.FieldSearchResult findObjects(org.fcrepo.server.types.mtom.gen.ArrayOfString resultFields,
                                                                          java.math.BigInteger maxResults,
                                                                          org.fcrepo.server.types.mtom.gen.FieldSearchQuery query) {
        MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            String[] resultFieldsArray =
                    resultFields.getItem().toArray(new String[0]);

            org.fcrepo.server.search.FieldSearchResult result =
                    s_access.findObjects(context,
                                         resultFieldsArray,
                                         maxResults.intValue(),
                                         TypeUtility
                                                 .convertGenFieldSearchQueryToFieldSearchQueryMTOM(query));
            return TypeUtility
                    .convertFieldSearchResultToGenFieldSearchResultMTOM(result);
        } catch (Throwable th) {
            LOG.error("Error finding objects", th);
            throw CXFUtility.getFault(th);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.access.FedoraAPIA#getObjectHistory(String pid )*
     */
    @Override
    public List<String> getObjectHistory(String pid) {
        MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            String[] sDefs = s_access.getObjectHistory(context, pid);
            if (sDefs != null && debug) {
                for (int i = 0; i < sDefs.length; i++) {
                    LOG.debug("sDef[" + i + "] = " + sDefs[i]);
                }
            }
            return sDefs == null ? null : Arrays.asList(sDefs);
        } catch (Throwable th) {
            LOG.error("Error getting object history", th);
            throw CXFUtility.getFault(th);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.access.FedoraAPIA#describeRepository(*
     */
    @Override
    public org.fcrepo.server.types.mtom.gen.RepositoryInfo describeRepository() {
        MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            org.fcrepo.server.access.RepositoryInfo repositoryInfo =
                    s_access.describeRepository(context);
            org.fcrepo.server.types.mtom.gen.RepositoryInfo genRepositoryInfo =
                    TypeUtility
                            .convertReposInfoToGenReposInfoMTOM(repositoryInfo);
            return genRepositoryInfo;
        } catch (Throwable th) {
            LOG.error("Error describing repository", th);
            throw CXFUtility.getFault(th);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.access.FedoraAPIA#listMethods(String pid ,)String
     * asOfDateTime )*
     */
    @Override
    public List<org.fcrepo.server.types.mtom.gen.ObjectMethodsDef> listMethods(String pid,
                                                                               String asOfDateTime) {
        MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            org.fcrepo.server.storage.types.ObjectMethodsDef[] objectMethodDefs =
                    s_access.listMethods(context, pid, DateUtility
                            .parseDateOrNull(asOfDateTime));
            return TypeUtility
                    .convertObjectMethodsDefArrayToGenObjectMethodsDefListMTOM(objectMethodDefs);
        } catch (Throwable th) {
            LOG.error("Error listing methods", th);
            throw CXFUtility.getFault(th);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.access.FedoraAPIA#resumeFindObjects(String
     * sessionToken )*
     */
    @Override
    public org.fcrepo.server.types.mtom.gen.FieldSearchResult resumeFindObjects(String sessionToken) {
        MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            org.fcrepo.server.search.FieldSearchResult result =
                    s_access.resumeFindObjects(context, sessionToken);
            return TypeUtility
                    .convertFieldSearchResultToGenFieldSearchResultMTOM(result);
        } catch (Throwable th) {
            LOG.error("Error resuming finding objects", th);
            throw CXFUtility.getFault(th);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.access.FedoraAPIA#listDatastreams(String pid
     * ,)String asOfDateTime )*
     */
    @Override
    public List<org.fcrepo.server.types.mtom.gen.DatastreamDef> listDatastreams(String pid,
                                                                                String asOfDateTime) {
        MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            org.fcrepo.server.storage.types.DatastreamDef[] datastreamDefs =
                    s_access.listDatastreams(context, pid, DateUtility
                            .parseDateOrNull(asOfDateTime));
            return TypeUtility
                    .convertDatastreamDefArrayToGenDatastreamDefListMTOM(datastreamDefs);
        } catch (Throwable th) {
            LOG.error("Error listing datastreams", th);
            throw CXFUtility.getFault(th);
        }
    }

    private void assertInitialized() throws SoapFault {
        if (!s_initialized) {
            CXFUtility.throwFault(s_initException);
        }
    }
}
