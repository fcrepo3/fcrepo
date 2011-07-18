/**
 * Please modify this class to meet your needs
 * This class is not complete
 */

package org.fcrepo.server.access2;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.access.Access;
import org.fcrepo.server.errors.InitializationException;
import org.fcrepo.server.errors.ServerInitializationException;
import org.fcrepo.server.utilities.TypeUtility;
import org.fcrepo.utilities.DateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jiri Kremser
 */

public class FedoraAPIAImpl implements FedoraAPIA {

	private static final Logger LOG = LoggerFactory
			.getLogger(FedoraAPIAImpl.class);

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
				s_initException = new ServerInitializationException(
						"Server failed to initialize because FEDORA_HOME "
								+ "is undefined");
			} else {
				s_server = Server.getInstance(new File(fedoraHome));
				s_initialized = true;
				s_access = (Access) s_server
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
	 * 
	 * @see org.fcrepo.server.access2.FedoraAPIA#getDissemination(String pid
	 * ,)String serviceDefinitionPid ,)String methodName
	 * ,)org.fcrepo.server.types2.gen.GetDissemination.Parameters parameters
	 * ,)String asOfDateTime )*
	 */
	public org.fcrepo.server.types2.gen.MIMETypedStream getDissemination(
			String pid,
			String serviceDefinitionPid,
			String methodName,
			org.fcrepo.server.types2.gen.GetDissemination.Parameters parameters,
			String asOfDateTime) {
		MessageContext ctx = context.getMessageContext();
		Context context = ReadOnlyContext.getSoapContext(ctx);
		assertInitialized();
		try {
			org.fcrepo.server.storage.types.Property[] properties = TypeUtility
					.convertGenPropertyArrayToPropertyArray2(parameters);
			org.fcrepo.server.storage.types.MIMETypedStream mimeTypedStream = s_access
					.getDissemination(context, pid, serviceDefinitionPid,
							methodName, properties,
							DateUtility.parseDateOrNull(asOfDateTime));
			org.fcrepo.server.types2.gen.MIMETypedStream genMIMETypedStream = TypeUtility
					.convertMIMETypedStreamToGenMIMETypedStream2(mimeTypedStream);
			return genMIMETypedStream;
		} catch (Throwable th) {
			LOG.error("Error getting dissemination", th);
			// throw AxisUtility.getFault(th);
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fcrepo.server.access2.FedoraAPIA#getObjectProfile(String pid
	 * ,)String asOfDateTime )*
	 */
	public org.fcrepo.server.types2.gen.ObjectProfile getObjectProfile(
			String pid, String asOfDateTime) {
		MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            org.fcrepo.server.access.ObjectProfile objectProfile =
                    s_access.getObjectProfile(context, pid, DateUtility
                            .parseDateOrNull(asOfDateTime));
            org.fcrepo.server.types2.gen.ObjectProfile genObjectProfile =
                    TypeUtility
                            .convertObjectProfileToGenObjectProfile2(objectProfile);
            return genObjectProfile;
        } catch (Throwable th) {
            LOG.error("Error getting object profile", th);
//            throw AxisUtility.getFault(th);
            return null;
        }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fcrepo.server.access2.FedoraAPIA#findObjects(org.fcrepo.server.types2
	 * .gen.ArrayOfString resultFields ,)java.math.BigInteger maxResults
	 * ,)org.fcrepo.server.types2.gen.FieldSearchQuery query )*
	 */
	public org.fcrepo.server.types2.gen.FieldSearchResult findObjects(
			org.fcrepo.server.types2.gen.ArrayOfString resultFields,
			java.math.BigInteger maxResults,
			org.fcrepo.server.types2.gen.FieldSearchQuery query) {
		MessageContext ctx = context.getMessageContext();
		Context context = ReadOnlyContext.getSoapContext(ctx);
		assertInitialized();
		try {
			String[] resultFieldsArray = resultFields.getItem().toArray(
					new String[0]);

			org.fcrepo.server.search.FieldSearchResult result = s_access
					.findObjects(
							context,
							resultFieldsArray,
							maxResults.intValue(),
							TypeUtility
									.convertGenFieldSearchQueryToFieldSearchQuery2(query));
			return TypeUtility
					.convertFieldSearchResultToGenFieldSearchResult2(result);
		} catch (Throwable th) {
			LOG.error("Error finding objects", th);
			// throw AxisUtility.getFault(th);
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fcrepo.server.access2.FedoraAPIA#getObjectHistory(String pid )*
	 */
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
			return sDefs == null ? null : Arrays.asList(new String[0]);
		} catch (Throwable th) {
			LOG.error("Error getting object history", th);
			// throw AxisUtility.getFault(th);
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.fcrepo.server.access2.FedoraAPIA#getDatastreamDissemination(String
	 * pid ,)String dsID ,)String asOfDateTime )*
	 */
	public org.fcrepo.server.types2.gen.MIMETypedStream getDatastreamDissemination(
			String pid, String dsID, String asOfDateTime) {
		MessageContext ctx = context.getMessageContext();
		Context context = ReadOnlyContext.getSoapContext(ctx);
		assertInitialized();
		try {
			org.fcrepo.server.storage.types.MIMETypedStream mimeTypedStream = s_access
					.getDatastreamDissemination(context, pid, dsID,
							DateUtility.parseDateOrNull(asOfDateTime));
			org.fcrepo.server.types2.gen.MIMETypedStream genMIMETypedStream = TypeUtility
					.convertMIMETypedStreamToGenMIMETypedStream2(mimeTypedStream);
			return genMIMETypedStream;
		} catch (OutOfMemoryError oome) {
			LOG.error("Out of memory error getting " + dsID
					+ " datastream dissemination for " + pid);
			String exceptionText = "The datastream you are attempting to retrieve is too large "
					+ "to transfer via getDatastreamDissemination (as determined "
					+ "by the server memory allocation.) Consider retrieving this "
					+ "datastream via REST at: ";
			String restURL = describeRepository().getRepositoryBaseURL()
					+ "/get/" + pid + "/" + dsID;
			// throw AxisFault.makeFault(new Exception(exceptionText +
			// restURL));
			return null;
		} catch (Throwable th) {
			LOG.error("Error getting datastream dissemination", th);
			// throw AxisUtility.getFault(th);
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fcrepo.server.access2.FedoraAPIA#describeRepository(*
	 */
	public org.fcrepo.server.types2.gen.RepositoryInfo describeRepository() {
		MessageContext ctx = context.getMessageContext();
        Context context = ReadOnlyContext.getSoapContext(ctx);
        assertInitialized();
        try {
            org.fcrepo.server.access.RepositoryInfo repositoryInfo =
                    s_access.describeRepository(context);
            org.fcrepo.server.types2.gen.RepositoryInfo genRepositoryInfo =
                    TypeUtility.convertReposInfoToGenReposInfo2(repositoryInfo);
            return genRepositoryInfo;
        } catch (Throwable th) {
            LOG.error("Error describing repository", th);
//            throw AxisUtility.getFault(th);
            return null;
        }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fcrepo.server.access2.FedoraAPIA#listMethods(String pid ,)String
	 * asOfDateTime )*
	 */
	public List<org.fcrepo.server.types2.gen.ObjectMethodsDef> listMethods(
			String pid, String asOfDateTime) {
		MessageContext ctx = context.getMessageContext();
		Context context = ReadOnlyContext.getSoapContext(ctx);
		assertInitialized();
		try {
			org.fcrepo.server.storage.types.ObjectMethodsDef[] objectMethodDefs = s_access
					.listMethods(context, pid,
							DateUtility.parseDateOrNull(asOfDateTime));
			return TypeUtility
					.convertObjectMethodsDefArrayToGenObjectMethodsDefList(objectMethodDefs);
		} catch (Throwable th) {
			LOG.error("Error listing methods", th);
			// throw AxisUtility.getFault(th);
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fcrepo.server.access2.FedoraAPIA#resumeFindObjects(String
	 * sessionToken )*
	 */
	public org.fcrepo.server.types2.gen.FieldSearchResult resumeFindObjects(
			String sessionToken) {
		MessageContext ctx = context.getMessageContext();
		Context context = ReadOnlyContext.getSoapContext(ctx);
		assertInitialized();
		try {
			org.fcrepo.server.search.FieldSearchResult result = s_access
					.resumeFindObjects(context, sessionToken);
			return TypeUtility
					.convertFieldSearchResultToGenFieldSearchResult2(result);
		} catch (Throwable th) {
			LOG.error("Error resuming finding objects", th);
			// throw AxisUtility.getFault(th);
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fcrepo.server.access2.FedoraAPIA#listDatastreams(String pid
	 * ,)String asOfDateTime )*
	 */
	public List<org.fcrepo.server.types2.gen.DatastreamDef> listDatastreams(
			String pid, String asOfDateTime) {
		MessageContext ctx = context.getMessageContext();
		Context context = ReadOnlyContext.getSoapContext(ctx);
		assertInitialized();
		try {
			org.fcrepo.server.storage.types.DatastreamDef[] datastreamDefs = s_access
					.listDatastreams(context, pid,
							DateUtility.parseDateOrNull(asOfDateTime));
			return TypeUtility
					.convertDatastreamDefArrayToGenDatastreamDefList(datastreamDefs);
		} catch (Throwable th) {
			LOG.error("Error listing datastreams", th);
			// throw AxisUtility.getFault(th);
			return null;
		}
	}

	private void assertInitialized()/* throws java.rmi.RemoteException */{
		if (!s_initialized) {
			// AxisUtility.throwFault(s_initException);
		}
	}

}
