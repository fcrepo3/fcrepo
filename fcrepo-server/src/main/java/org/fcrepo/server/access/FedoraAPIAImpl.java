/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.access;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.binding.soap.SoapFault;
import org.fcrepo.server.Context;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.utilities.CXFUtility;
import org.fcrepo.server.utilities.TypeUtility;
import org.fcrepo.utilities.DateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jiri Kremser
 */

public class FedoraAPIAImpl
        implements FedoraAPIA {

    private static final Logger LOG = LoggerFactory
            .getLogger(FedoraAPIAImpl.class);

    @Resource
    private WebServiceContext context;

    /** The Fedora Server instance. */
    private final Server m_server;

    /** Instance of the access subsystem */
    private final Access m_access;

    /** Context for cached objects. */
    // private static ReadOnlyContext context;
    /** Debug toggle for testing. */
    private boolean debug = false;

    /** Before fulfilling any requests, make sure we have a server instance. */
    public FedoraAPIAImpl(Server server) {
        m_server = server;
        m_access =
                (Access) m_server
                .getModule("org.fcrepo.server.access.Access");
        debug = Boolean.parseBoolean(m_server.getParameter("debug"));
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.access.FedoraAPIA#getDissemination(String pid
     * ,)String serviceDefinitionPid ,)String methodName
     * ,)org.fcrepo.server.types.gen.GetDissemination.Parameters parameters
     * ,)String asOfDateTime )*
     */
    @Override
    public org.fcrepo.server.types.gen.MIMETypedStream getDissemination(String pid,
                                                                        String serviceDefinitionPid,
                                                                        String methodName,
                                                                        org.fcrepo.server.types.gen.GetDissemination.Parameters parameters,
                                                                        String asOfDateTime) {
        MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            org.fcrepo.server.storage.types.Property[] properties =
                    TypeUtility
                            .convertGenPropertyArrayToPropertyArray(parameters);
            org.fcrepo.server.storage.types.MIMETypedStream mimeTypedStream =
                    m_access.getDissemination(context,
                                              pid,
                                              serviceDefinitionPid,
                                              methodName,
                                              properties,
                                              DateUtility
                                                      .parseDateOrNull(asOfDateTime));
            org.fcrepo.server.types.gen.MIMETypedStream genMIMETypedStream =
                    TypeUtility
                            .convertMIMETypedStreamToGenMIMETypedStream(mimeTypedStream);
            return genMIMETypedStream;
        } catch (Throwable th) {
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
    public org.fcrepo.server.types.gen.ObjectProfile getObjectProfile(String pid,
                                                                      String asOfDateTime) {
        MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            org.fcrepo.server.access.ObjectProfile objectProfile =
                    m_access.getObjectProfile(context, pid, DateUtility
                            .parseDateOrNull(asOfDateTime));
            org.fcrepo.server.types.gen.ObjectProfile genObjectProfile =
                    TypeUtility
                            .convertObjectProfileToGenObjectProfile(objectProfile);
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
    public org.fcrepo.server.types.gen.FieldSearchResult findObjects(org.fcrepo.server.types.gen.ArrayOfString resultFields,
                                                                     java.math.BigInteger maxResults,
                                                                     org.fcrepo.server.types.gen.FieldSearchQuery query) {
        MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            String[] resultFieldsArray =
                    resultFields.getItem().toArray(new String[0]);

            org.fcrepo.server.search.FieldSearchResult result =
                    m_access.findObjects(context,
                                         resultFieldsArray,
                                         maxResults.intValue(),
                                         TypeUtility
                                                 .convertGenFieldSearchQueryToFieldSearchQuery(query));
            return TypeUtility
                    .convertFieldSearchResultToGenFieldSearchResult(result);
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
            String[] sDefs = m_access.getObjectHistory(context, pid);
            if (sDefs != null && debug) {
                for (int i = 0; i < sDefs.length; i++) {
                    LOG.debug("sDef[{}] = {}", i, sDefs[i]);
                }
            }
            return sDefs == null ? null : Arrays.asList(new String[0]);
        } catch (Throwable th) {
            LOG.error("Error getting object history", th);
            throw CXFUtility.getFault(th);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.access.FedoraAPIA#getDatastreamDissemination(String pid
     * ,)String dsID ,)String asOfDateTime )*
     */
    @Override
    public org.fcrepo.server.types.gen.MIMETypedStream getDatastreamDissemination(String pid,
                                                                                  String dsID,
                                                                                  String asOfDateTime) {
        MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            org.fcrepo.server.storage.types.MIMETypedStream mimeTypedStream =
                    m_access.getDatastreamDissemination(context,
                                                        pid,
                                                        dsID,
                                                        DateUtility
                                                                .parseDateOrNull(asOfDateTime));
            org.fcrepo.server.types.gen.MIMETypedStream genMIMETypedStream =
                    TypeUtility
                            .convertMIMETypedStreamToGenMIMETypedStream(mimeTypedStream);
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
     * @see org.fcrepo.server.access.FedoraAPIA#describeRepository(*
     */
    @Override
    public org.fcrepo.server.types.gen.RepositoryInfo describeRepository() {
        MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            org.fcrepo.server.access.RepositoryInfo repositoryInfo =
                    m_access.describeRepository(context);
            org.fcrepo.server.types.gen.RepositoryInfo genRepositoryInfo =
                    TypeUtility.convertReposInfoToGenReposInfo(repositoryInfo);
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
    public List<org.fcrepo.server.types.gen.ObjectMethodsDef> listMethods(String pid,
                                                                          String asOfDateTime) {
        MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            org.fcrepo.server.storage.types.ObjectMethodsDef[] objectMethodDefs =
                    m_access.listMethods(context, pid, DateUtility
                            .parseDateOrNull(asOfDateTime));
            return TypeUtility
                    .convertObjectMethodsDefArrayToGenObjectMethodsDefList(objectMethodDefs);
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
    public org.fcrepo.server.types.gen.FieldSearchResult resumeFindObjects(String sessionToken) {
        MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            org.fcrepo.server.search.FieldSearchResult result =
                    m_access.resumeFindObjects(context, sessionToken);
            return TypeUtility
                    .convertFieldSearchResultToGenFieldSearchResult(result);
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
    public List<org.fcrepo.server.types.gen.DatastreamDef> listDatastreams(String pid,
                                                                           String asOfDateTime) {
        MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            org.fcrepo.server.storage.types.DatastreamDef[] datastreamDefs =
                    m_access.listDatastreams(context, pid, DateUtility
                            .parseDateOrNull(asOfDateTime));
            return TypeUtility
                    .convertDatastreamDefArrayToGenDatastreamDefList(datastreamDefs);
        } catch (Throwable th) {
            LOG.error("Error listing datastreams", th);
            throw CXFUtility.getFault(th);
        }
    }

    private void assertInitialized() throws SoapFault {
        if (m_server == null) {
            CXFUtility.throwFault(new ModuleInitializationException("Null was injected for Server to WS implementor",
                    "org.fcrepo.server.access.FedoraAPIA"));
        }
        if (m_access == null) {
            CXFUtility.throwFault(new ModuleInitializationException("No Access module found for WS implementor",
                    "org.fcrepo.server.access.FedoraAPIA"));
        }
    }

}
