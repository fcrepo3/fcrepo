/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.access.dissemination;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fcrepo.server.Context;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.DisseminationBindingInfoNotFoundException;
import org.fcrepo.server.errors.DisseminationException;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.security.Authorization;
import org.fcrepo.server.security.BackendPolicies;
import org.fcrepo.server.security.BackendSecurity;
import org.fcrepo.server.security.BackendSecuritySpec;
import org.fcrepo.server.storage.ContentManagerParams;
import org.fcrepo.server.storage.ExternalContentManager;
import org.fcrepo.server.storage.ServiceDeploymentReader;
import org.fcrepo.server.storage.types.DatastreamMediation;
import org.fcrepo.server.storage.types.DeploymentDSBindRule;
import org.fcrepo.server.storage.types.DeploymentDSBindSpec;
import org.fcrepo.server.storage.types.DisseminationBindingInfo;
import org.fcrepo.server.storage.types.MIMETypedStream;
import org.fcrepo.server.storage.types.MethodParmDef;
import org.fcrepo.server.utilities.ServerUtility;
import org.fcrepo.utilities.DateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service for executing a dissemination given its binding information.
 * 
 * @author Ross Wayland
 */
public class DisseminationService {

	private static final Logger logger = LoggerFactory
			.getLogger(DisseminationService.class);

	/**
	 * Signifies the special type of address location known as LOCAL. An address
	 * location of LOCAL implies that no remote host name is required for the
	 * address location and that the contents of the operation location are
	 * sufficient to execute the associated mechanism.
	 */
	private static final String LOCAL_ADDRESS_LOCATION = "LOCAL";

	/** The expiration limit in minutes for removing entries from the database. */
	private static int datastreamExpirationLimit = 0;

	/**
	 * An incremental counter used to insure uniqueness of tempIDs used for
	 * datastream mediation.
	 */
	private static int counter = 0;

	/** Datastream Mediation control flag. */
	private boolean m_doDatastreamMediation;
	private boolean m_useNewUrlEncodingTest;

	/** Configured Fedora server host */
	private String m_fedoraServerHost = null;

	/** Configured Fedora server port */
	private String m_fedoraServerPort = null;

	/** Configured Fedora application server context */
	private String m_fedoraAppServerContext = null;

	/** Configured Fedora redirect port */
	private String m_fedoraServerRedirectPort = null;

	private BackendSecuritySpec m_beSS = null;

	private final BackendSecurity m_beSecurity;

	private final ExternalContentManager m_ecm;

	private final Authorization m_authorization;

	/** The hashtable containing information required for datastream mediation. */
	protected static Hashtable<String, DatastreamMediation> dsRegistry = new Hashtable<String, DatastreamMediation>(
			1000);

	/**
	 * <p>
	 * Constructs an instance of DisseminationService. Initializes two class
	 * variables that contain the IP address and port number of the Fedora
	 * server. The port number is obtained from the Fedora server config file
	 * and the IP address of the server is obtained dynamically. These variables
	 * are needed to perform the datastream proxy service for datastream
	 * requests.
	 * </p>
	 */
	public DisseminationService(Server server) {
		m_fedoraServerHost = server.getParameter("fedoraServerHost");
		m_fedoraServerPort = server.getParameter("fedoraServerPort");
		m_fedoraAppServerContext = server
				.getParameter("fedoraAppServerContext");
		m_fedoraServerRedirectPort = server.getParameter("fedoraRedirectPort");
		m_beSecurity = (BackendSecurity) server
				.getModule("org.fcrepo.server.security.BackendSecurity");
		m_beSS = m_beSecurity.getBackendSecuritySpec();
		String expireLimit = server.getParameter("datastreamExpirationLimit");
		if (expireLimit == null || expireLimit.equalsIgnoreCase("")) {
			logger.info("datastreamExpirationLimit unspecified; defaulting to "
					+ "300 seconds");
			datastreamExpirationLimit = 300;
		} else {
			datastreamExpirationLimit = Integer.parseInt(expireLimit);
			logger.info("datastreamExpirationLimit={}",
					datastreamExpirationLimit);
		}
		String dsMediation = server
				.getModule("org.fcrepo.server.access.Access").getParameter(
						"doMediateDatastreams");
		if (dsMediation == null || dsMediation.equalsIgnoreCase("")) {
			logger.info("doMediateDatastreams unspecified; defaulting to false");
		} else {
			m_doDatastreamMediation = Boolean.parseBoolean(dsMediation);
		}

		String useNewUrlEncodingTest = server
				.getModule("org.fcrepo.server.access.Access").getParameter(
						"useNewUrlEncoding");
		if (useNewUrlEncodingTest == null || useNewUrlEncodingTest.equalsIgnoreCase("")) {
			logger.info("useNewUrlEncodingTest unspecified; defaulting to false");
		} else {
			m_useNewUrlEncodingTest = Boolean.parseBoolean(useNewUrlEncodingTest);
		}
		
		m_ecm = server.getBean(
				"org.fcrepo.server.storage.ExternalContentManager",
				ExternalContentManager.class);
		m_authorization = server
				.getBean("org.fcrepo.server.security.Authorization",
						Authorization.class);
	}

	/**
	 * <p>
	 * Assembles a dissemination given an instance of <code>
	 * DisseminationBindingInfo</code> which has the dissemination-related
	 * information from the digital object and its associated Service Deployment
	 * object.
	 * </p>
	 * 
	 * @param context
	 *            The current context.
	 * @param PID
	 *            The persistent identifier of the digital object.
	 * @param h_userParms
	 *            A hashtable of user-supplied method parameters.
	 * @param dissBindInfoArray
	 *            The associated dissemination binding information.
	 * @return A MIME-typed stream containing the result of the dissemination.
	 * @throws ServerException
	 *             If unable to assemble the dissemination for any reason.
	 */
	public MIMETypedStream assembleDissemination(Context context, String PID,
			Hashtable<String, String> h_userParms,
			DisseminationBindingInfo[] dissBindInfoArray, String deploymentPID,
			ServiceDeploymentReader bmReader, String methodName)
			throws ServerException {

		logger.debug("Started assembling dissemination");

		String dissURL = null;
		String protocolType = null;
		DisseminationBindingInfo dissBindInfo = null;
		MIMETypedStream dissemination = null;
		boolean isRedirect = false;

		if (logger.isDebugEnabled()) {
			printBindingInfo(dissBindInfoArray);
		}

		if (dissBindInfoArray != null && dissBindInfoArray.length > 0) {
			String replaceString = null;
			int numElements = dissBindInfoArray.length;

			// Get row(s) of binding info and perform string substitution
			// on DSBindingKey and method parameter values in WSDL
			// Note: In case where more than one datastream matches the
			// DSBindingKey or there are multiple DSBindingKeys for the
			// method, multiple rows will be present; otherwise there is only
			// a single row.
			for (int i = 0; i < dissBindInfoArray.length; i++) {
				m_authorization
						.enforce_Internal_DSState(context,
								dissBindInfoArray[i].dsID,
								dissBindInfoArray[i].dsState);
				dissBindInfo = dissBindInfoArray[i];

				// Before doing anything, check whether we can replace any
				// placeholders in the datastream url with parameter values from
				// the request. This supports the special case where a
				// datastream's URL is dependent on user parameters, such
				// as when the datastream is actually a dissemination that
				// takes parameters.
				if (dissBindInfo.dsLocation != null
						&& (dissBindInfo.dsLocation.startsWith("http://") || dissBindInfo.dsLocation
								.startsWith("https://"))) {
					String[] parts = dissBindInfo.dsLocation.split("=\\("); // regex
																			// for
																			// =(
					if (parts.length > 1) {
						StringBuffer replaced = new StringBuffer();
						replaced.append(parts[0]);
						for (int x = 1; x < parts.length; x++) {
							replaced.append('=');
							int rightParenPos = parts[x].indexOf(')');
							if (rightParenPos != -1 && rightParenPos > 0) {
								String key = parts[x].substring(0,
										rightParenPos);
								String val = h_userParms.get(key);
								if (val != null) {
									// We have a match... so insert the
									// urlencoded value.
									try {
										replaced.append(URLEncoder.encode(val,
												"UTF-8"));
									} catch (UnsupportedEncodingException uee) {
										// won't happen: java always supports
										// UTF-8
									}
									if (rightParenPos < parts[x].length()) {
										replaced.append(parts[x]
												.substring(rightParenPos + 1));
									}
								} else {
									replaced.append('(');
									replaced.append(parts[x]);
								}
							} else {
								replaced.append('(');
								replaced.append(parts[x]);
							}
						}
						dissBindInfo.dsLocation = replaced.toString();
					}
				}

				// Match DSBindingKey pattern in WSDL which is a string of the
				// form:
				// (DSBindingKey). Rows in DisseminationBindingInfo are sorted
				// alphabetically on binding key.
				String bindingKeyPattern = "\\(" + dissBindInfo.DSBindKey
						+ "\\)";
				if (i == 0) {
					// If addressLocation has a value of "LOCAL", this indicates
					// the associated operationLocation requires no
					// addressLocation.
					// i.e., the operationLocation contains all information
					// necessary
					// to perform the dissemination request. This is a special
					// case
					// used when the web services are generally mechanisms like
					// cgi-scripts,
					// java servlets, and simple HTTP GETs. Using the value of
					// LOCAL
					// in the address location also enables one to have
					// different methods
					// serviced by different hosts. In true web services like
					// SOAP, the
					// addressLocation specifies the host name of the service
					// and all
					// methods are served from that single host location.
					if (dissBindInfo.AddressLocation
							.equalsIgnoreCase(LOCAL_ADDRESS_LOCATION)) {
						dissURL = dissBindInfo.OperationLocation;
					} else {
						dissURL = dissBindInfo.AddressLocation
								+ dissBindInfo.OperationLocation;

						/*
						 * Substitute real app server context if we detect
						 * '/fedora'. This is necessary here because
						 * DOTranslator does not scrub URLs that result from
						 * concatenating fragments from different locations in
						 * the file
						 */
						dissURL = dissURL.replaceAll(m_fedoraServerHost + ":"
								+ m_fedoraServerPort + "/fedora/",
								m_fedoraServerHost + ":" + m_fedoraServerPort
										+ "/" + m_fedoraAppServerContext + "/");
					}
					protocolType = dissBindInfo.ProtocolType;
				}

				// Assess beSecurity for backend service and for datastreams
				// that may be parameters for the
				// backend service.
				//
				// dsMediatedCallbackHost - when dsMediation is in effect, all
				// M, X, and E type datastreams
				// are encoded as callbacks to the Fedora server to obtain the
				// datastream's contents. dsMediatedCallbackHost contains
				// protocol,
				// host, and port used for this type of backendservice-to-fedora
				// callback.
				// The specifics of protocol, host, and port are obtained from
				// the
				// beSecurity configuration file.
				// dsMediatedServletPath - when dsMediation is in effect, all M,
				// X, and E type datastreams
				// are encoded as callbacks to the Fedora server to obtain the
				// datastream's contents. dsMediatedServletPath contains the
				// servlet
				// path info for this type of backendservice-to-fedora callback.
				// The specifics of servlet path are obtained from the
				// beSecurity configuration
				// file and determines whether the backedservice-to-fedora
				// callback
				// will use authentication or not.
				// callbackRole - contains the role of the backend service (the
				// deploymentPID of the service).

				String callbackRole = deploymentPID;
				Hashtable<String, String> beHash = m_beSS.getSecuritySpec(
						callbackRole, methodName);
				boolean callbackBasicAuth = Boolean.parseBoolean(
						beHash.get("callbackBasicAuth"));
				boolean callbackSSL = Boolean.parseBoolean(beHash.get("callbackSSL"));
				String dsMediatedServletPath = null;
				if (callbackBasicAuth) {
					dsMediatedServletPath = "/" + m_fedoraAppServerContext
							+ "/getDSAuthenticated?id=";
				} else {
					dsMediatedServletPath = "/" + m_fedoraAppServerContext
							+ "/getDS?id=";
				}
				String dsMediatedCallbackHost = null;
				if (callbackSSL) {
					dsMediatedCallbackHost = "https://" + m_fedoraServerHost
							+ ":" + m_fedoraServerRedirectPort;
				} else {
					dsMediatedCallbackHost = "http://" + m_fedoraServerHost
							+ ":" + m_fedoraServerPort;
				}
				String datastreamResolverServletURL = dsMediatedCallbackHost
						+ dsMediatedServletPath;
				if (logger.isDebugEnabled()) {
					logger.debug(
							"******************Checking backend service dsLocation: {}",
							dissBindInfo.dsLocation);
					logger.debug(
							"******************Checking backend service dsControlGroupType: {}",
							dissBindInfo.dsControlGroupType);
					logger.debug(
							"******************Checking backend service callbackBasicAuth: {}",
							callbackBasicAuth);
					logger.debug(
							"******************Checking backend service callbackSSL: {}",
							callbackSSL);
					logger.debug(
							"******************Checking backend service callbackRole: {}",
							callbackRole);
					logger.debug(
							"******************DatastreamResolverServletURL: {}",
							datastreamResolverServletURL);
				}

				String currentKey = dissBindInfo.DSBindKey;
				String nextKey = "";
				if (i != numElements - 1) {
					// Except for last row, get the value of the next binding
					// key
					// to compare with the value of the current binding key.
					nextKey = dissBindInfoArray[i + 1].DSBindKey;
				}
				logger.debug("currentKey: '" + currentKey + "', nextKey: '"
						+ nextKey + "'");
				// In most cases, there is only a single datastream that matches
				// a
				// given DSBindingKey so the substitution process is to just
				// replace
				// the occurrence of (BINDING_KEY) with the value of the
				// datastream
				// location. However, when multiple datastreams match the same
				// DSBindingKey, the occurrence of (BINDING_KEY) is replaced
				// with the
				// value of the datastream location and the value +(BINDING_KEY)
				// is
				// appended so that subsequent datastreams matching the binding
				// key
				// will be substituted. The end result is that the binding key
				// will
				// be replaced by a series of datastream locations separated by
				// a
				// plus(+) sign. For example, in the case where 3 datastreams
				// match
				// the binding key for PHOTO:
				//
				// file=(PHOTO) becomes
				// file=dslocation1+dslocation2+dslocation3
				//
				// It is the responsibility of the Service Deployment to know
				// how to
				// handle an input parameter with multiple datastream locations.
				//
				// In the case of a method containing multiple binding keys,
				// substitutions are performed on each binding key. For example,
				// in
				// the case where there are 2 binding keys named PHOTO and
				// WATERMARK
				// where each matches a single datastream:
				//
				// image=(PHOTO)&watermark=(WATERMARK) becomes
				// image=dslocation1&watermark=dslocation2
				//
				// In the case with multiple binding keys and multiple
				// datastreams,
				// the substitution might appear like the following:
				//
				// image=(PHOTO)&watermark=(WATERMARK) becomes
				// image=dslocation1+dslocation2&watermark=dslocation3
				if (nextKey.equalsIgnoreCase(currentKey) & i != numElements) {
					// Case where binding keys are equal which means that
					// multiple
					// datastreams matched the same binding key.
					if (m_doDatastreamMediation
							&& !dissBindInfo.dsControlGroupType
									.equalsIgnoreCase("R")) {
						// Use Datastream Mediation (except for redirected
						// datastreams).

						replaceString = datastreamResolverServletURL
								+ registerDatastreamLocation(
										dissBindInfo.dsLocation,
										dissBindInfo.dsControlGroupType,
										callbackRole, methodName) + "+("
								+ dissBindInfo.DSBindKey + ")";
					} else {
						// Bypass Datastream Mediation.
						if (dissBindInfo.dsControlGroupType
								.equalsIgnoreCase("M")
								|| dissBindInfo.dsControlGroupType
										.equalsIgnoreCase("X")) {
							// Use the Default Disseminator syntax to resolve
							// the internal
							// datastream location for Managed and XML
							// datastreams.
							replaceString = resolveInternalDSLocation(context,
									dissBindInfo.dsLocation,
									dissBindInfo.dsCreateDT,
									dsMediatedCallbackHost)
									+ "+(" + dissBindInfo.DSBindKey + ")";
							;
						} else {
							replaceString = dissBindInfo.dsLocation + "+("
									+ dissBindInfo.DSBindKey + ")";
						}
						if (dissBindInfo.dsControlGroupType
								.equalsIgnoreCase("R")
								&& dissBindInfo.AddressLocation
										.equals(LOCAL_ADDRESS_LOCATION)) {
							isRedirect = true;
						}
					}
				} else {
					// Case where there are one or more binding keys.
					if (m_doDatastreamMediation
							&& !dissBindInfo.dsControlGroupType
									.equalsIgnoreCase("R")) {
						// Use Datastream Mediation (except for Redirected
						// datastreams)
						replaceString = datastreamResolverServletURL
								+ registerDatastreamLocation(
										dissBindInfo.dsLocation,
										dissBindInfo.dsControlGroupType,
										callbackRole, methodName); // this is
																	// generic,
																	// should be
																	// made
																	// specific
																	// per
																	// service
					} else {
						// Bypass Datastream Mediation.
						if (dissBindInfo.dsControlGroupType
								.equalsIgnoreCase("M")
								|| dissBindInfo.dsControlGroupType
										.equalsIgnoreCase("X")) {
							// Use the Default Disseminator syntax to resolve
							// the internal
							// datastream location for Managed and XML
							// datastreams.
							replaceString = resolveInternalDSLocation(context,
									dissBindInfo.dsLocation,
									dissBindInfo.dsCreateDT,
									dsMediatedCallbackHost);
						} else {
							replaceString = dissBindInfo.dsLocation;
						}
						if (dissBindInfo.dsControlGroupType
								.equalsIgnoreCase("R")
								&& dissBindInfo.AddressLocation
										.equals(LOCAL_ADDRESS_LOCATION)) {
							isRedirect = true;
						}
					}
				}
				try {
					
					// Here we choose between two different tests for deciding
					// whether to URL-encode the datastream URL:
					
					//Old method:
					// If the operationLocation contains datastreamInputParms and also
					// contains a "=(" sequence, then
                    // URLEncode each parameter before substitution. Otherwise, the
                    // operationLocation has no parameters (i.e., it is a simple URL )
                    // so bypass URLencoding.
					
					// New Method:
					// If the operationLocation contains datastreamInputParms
					// URLEncode each parameter before substitution, except when
					// the parameter comprises the first part of the the URL.

					boolean useUrlEncoding = m_useNewUrlEncodingTest ? dissURL.indexOf("("
							+ bindingKeyPattern + ")") > 0 : dissURL
							.indexOf("=(") != -1;

					if (useUrlEncoding) {
						dissURL = substituteString(dissURL, bindingKeyPattern,
								URLEncoder.encode(replaceString, "UTF-8"));
					} else {
						dissURL = substituteString(dissURL, bindingKeyPattern,
								replaceString);
					}
				} catch (UnsupportedEncodingException uee) {
					String message = "[DisseminationService] An error occured. The error "
							+ "was \""
							+ uee.getClass().getName()
							+ "\"  . The Reason was \""
							+ uee.getMessage()
							+ "\"  . String value: " + replaceString + "  . ";
					logger.error(message);
					throw new GeneralException(message);
				}
				logger.debug("Replaced dissURL: " + dissURL.toString()
						+ " DissBindingInfo index: " + i);
			}

			DeploymentDSBindSpec dsBindSpec = bmReader
					.getServiceDSInputSpec(null);
			DeploymentDSBindRule rules[] = dsBindSpec.dsBindRules;
			for (DeploymentDSBindRule element : rules) {
				String rulePattern = "(" + element.bindingKeyName + ")";
				if (dissURL.indexOf(rulePattern) != -1) {
					throw new DisseminationException(null, "Data Object " + PID
							+ " missing required datastream: "
							+ element.bindingKeyName, null, null, null);
				}
			}

			// Substitute method parameter values in dissemination URL
			Enumeration<String> e = h_userParms.keys();
			while (e.hasMoreElements()) {
				String name = null;
				String value = null;
				try {
					name = URLEncoder.encode(e.nextElement(), "UTF-8");
					value = URLEncoder.encode(h_userParms.get(name), "UTF-8");
				} catch (UnsupportedEncodingException uee) {
					String message = "[DisseminationService] An error occured. The error "
							+ "was \""
							+ uee.getClass().getName()
							+ "\"  . The Reason was \""
							+ uee.getMessage()
							+ "\"  . Parameter name: "
							+ name
							+ "  . "
							+ "Parameter value: " + value + "  .";
					logger.error(message);
					throw new GeneralException(message);
				}
				String pattern = "\\(" + name + "\\)";
				dissURL = substituteString(dissURL, pattern, value);
				logger.debug("User parm substituted in URL: " + dissURL);
			}

			// FIXME Need a more elegant means of handling optional
			// userInputParm
			// method parameters that are not supplied by the invoking client;
			// for now, any optional parms that were not supplied are removed
			// from
			// the outgoing URL. This works because parms are validated in
			// DefaultAccess to insure all required parms are present and all
			// parm
			// names match parm names defined for the specific method. The only
			// unsubstituted parms left in the operationLocation string at this
			// point
			// are those for optional parameters that the client omitted in the
			// initial request so they can safely be removed from the outgoing
			// dissemination URL. This step is only needed when optional
			// parameters
			// are not supplied by the client.
			if (dissURL.indexOf("(") != -1) {
				dissURL = stripParms(dissURL);
				logger.debug("Non-supplied optional userInputParm values removed "
						+ "from URL: " + dissURL);
			}

			if (dissURL.indexOf("(") != -1) {
				String datastreamName = dissURL.substring(
						dissURL.indexOf("(") + 1, dissURL.indexOf(")"));
				throw new DisseminationException(null, "Data Object " + PID
						+ " missing required datastream: " + datastreamName,
						null, null, null);
			}

			// Resolve content referenced by dissemination result.
			logger.debug("ProtocolType: " + protocolType);
			if (protocolType.equalsIgnoreCase("http")) {

				if (isRedirect) {
					// The dsControlGroupType of Redirect("R") is a special
					// control type
					// used primarily for streaming media. Datastreams of this
					// type are
					// not mediated (proxied by Fedora) and their physical
					// dsLocation is
					// simply redirected back to the client. Therefore, the
					// contents
					// of the MIMETypedStream returned for dissemination
					// requests will
					// contain the raw URL of the dsLocation and will be
					// assigned a
					// special fedora-specific MIME type to identify the stream
					// as
					// a MIMETypedStream whose contents contain a URL to which
					// the client
					// should be redirected.

					InputStream is = null;
					try {
						is = new ByteArrayInputStream(dissURL.getBytes("UTF-8"));
					} catch (UnsupportedEncodingException uee) {
						String message = "[DisseminationService] An error has occurred. "
								+ "The error was a \""
								+ uee.getClass().getName()
								+ "\"  . The "
								+ "Reason was \""
								+ uee.getMessage()
								+ "\"  . String value: " + dissURL + "  . ";
						logger.error(message);
						throw new GeneralException(message);
					}
					logger.debug("Finished assembling dissemination");
					dissemination = new MIMETypedStream(
							"application/fedora-redirect", is, null);
				} else {
					// For all non-redirected disseminations, Fedora captures
					// and returns
					// the MIMETypedStream resulting from the dissemination
					// request.
					logger.debug("Finished assembling dissemination, URL={}",
							dissURL);

					// See if backend service reference is to fedora server
					// itself or an external location.
					// We must examine URL to see if this is referencing a
					// remote backend service or is
					// simply a callback to the fedora server. If the reference
					// is remote, then use
					// the role of backend service deployment PID. If the
					// referenc is to the fedora server,
					// use the special role of "fedoraInternalCall-1" to denote
					// that the callback will come from the
					// fedora server itself.
					String beServiceRole = null;
					if (ServerUtility.isURLFedoraServer(dissURL)) {
						beServiceRole = BackendPolicies.FEDORA_INTERNAL_CALL;
					} else {
						beServiceRole = deploymentPID;
					}

					// Get basicAuth and SSL info about the backend service and
					// use this info to configure the
					// "call" to the backend service.
					Hashtable<String, String> beHash = m_beSS.getSecuritySpec(
							beServiceRole, methodName);
					boolean beServiceCallSSL = Boolean.parseBoolean(
							beHash.get("callSSL"));
					String beServiceCallUsername = "";
					String beServiceCallPassword = "";
					boolean beServiceCallBasicAuth = Boolean.parseBoolean(
							beHash.get("callBasicAuth"));
					if (beServiceCallBasicAuth) {
						beServiceCallUsername = beHash.get("callUsername");
						beServiceCallPassword = beHash.get("callPassword");
					}

					if (logger.isDebugEnabled()) {
						logger.debug(
								"******************getDisseminationContent beServiceRole: {}",
								beServiceRole);
						logger.debug(
								"******************getDisseminationContent beServiceCallBasicAuth: {}",
								beServiceCallBasicAuth);
						logger.debug(
								"******************getDisseminationContent beServiceCallSSL: {}",
								beServiceCallSSL);
						logger.debug(
								"******************getDisseminationContent beServiceCallUsername: {}",
								beServiceCallUsername);
						logger.debug(
								"******************getDisseminationContent beServiceCallPassword: {}",
								beServiceCallPassword);
						logger.debug(
								"******************getDisseminationContent dissURL: {}",
								dissURL);
					}

					// Dispatch backend service URL request authenticating as
					// necessary based on beSecurity configuration
					ContentManagerParams params = new ContentManagerParams(
							dissURL, null, beServiceCallUsername,
							beServiceCallPassword);
					params.setBypassBackend(true);
					params.setContext(context);
					dissemination = m_ecm.getExternalContent(params);
				}

			} else if (protocolType.equalsIgnoreCase("soap")) {
				// FIXME!! future handling of soap bindings.
				String message = "[DisseminationService] Protocol type: "
						+ protocolType + "NOT yet implemented";
				logger.error(message);
				throw new DisseminationException(message);

			} else if (protocolType.equalsIgnoreCase("file")) {
				ContentManagerParams params = new ContentManagerParams(dissURL);
				params.setContext(context);
				dissemination = m_ecm.getExternalContent(params);
			} else {
				String message = "[DisseminationService] Protocol type: "
						+ protocolType + "NOT supported.";
				logger.error(message);
				throw new DisseminationException(message);
			}

		} else {
			// DisseminationBindingInfo was empty so there was no information
			// provided to construct a dissemination.
			String message = "[DisseminationService] Dissemination Binding "
					+ "Info contained no data";
			logger.error(message);
			throw new DisseminationBindingInfoNotFoundException(message);
		}
		return dissemination;
	}

	/**
	 * <p>
	 * Datastream locations are considered privileged information by the Fedora
	 * repository. To prevent disclosing physical datastream locations to
	 * external mechanism services, a proxy is used to disguise the datastream
	 * locations. This method generates a temporary ID that maps to the physical
	 * datastream location and registers this information in a memory resident
	 * hashtable for subsequent resolution of the physical datastream location.
	 * The servlet <code>DatastreamResolverServlet</code> provides the proxy
	 * resolution service for datastreams.
	 * </p>
	 * <p>
	 * </p>
	 * <p>
	 * The format of the tempID is derived from <code>java.sql.Timestamp</code>
	 * with an arbitrary counter appended to the end to insure uniqueness. The
	 * syntax is of the form:
	 * <ul>
	 * <p>
	 * YYYY-MM-DD HH:mm:ss.mmm:dddddd where
	 * </p>
	 * <ul>
	 * <li>YYYY - year (1900-8099)</li>
	 * <li>MM - month (01-12)</li>
	 * <li>DD - day (01-31)</li>
	 * <li>hh - hours (0-23)</li>
	 * <li>mm - minutes (0-59)</li>
	 * <li>ss - seconds (0-59)</li>
	 * <li>mmm - milliseconds (0-999)</li>
	 * <li>dddddd - incremental counter (0-999999)</li>
	 * </ul>
	 * </ul>
	 * 
	 * @param dsLocation
	 *            The physical location of the datastream.
	 * @param dsControlGroupType
	 *            The type of the datastream.
	 * @return A temporary ID used to reference the physical location of the
	 *         specified datastream
	 * @throws ServerException
	 *             If an error occurs in registering a datastream location.
	 */
	public String registerDatastreamLocation(String dsLocation,
			String dsControlGroupType, String beServiceCallbackRole,
			String methodName) throws ServerException {

		String tempID = null;
		Timestamp timeStamp = null;
		if (counter > 999999) {
			counter = 0;
		}
		long currentTime = new Timestamp(new Date().getTime()).getTime();
		long expireLimit = currentTime - (long) datastreamExpirationLimit
				* 1000;

		try {

			// Remove any datastream registrations that have expired.
			// The expiration limit can be adjusted using the Fedora config
			// parameter
			// named "datastreamExpirationLimit" which is in seconds.
			for (Enumeration<String> e = dsRegistry.keys(); e.hasMoreElements();) {
				String key = e.nextElement();
				timeStamp = Timestamp.valueOf(extractTimestamp(key));
				if (expireLimit > timeStamp.getTime()) {
					dsRegistry.remove(key);
					logger.debug("DatastreamMediationKey removed from Hash: {}",
							key);
				}
			}

			// Register datastream.
			if (tempID == null) {
				timeStamp = new Timestamp(new Date().getTime());
				tempID = timeStamp.toString() + ":" + counter++;
				DatastreamMediation dm = new DatastreamMediation();
				dm.mediatedDatastreamID = tempID;
				dm.dsLocation = dsLocation;
				dm.dsControlGroupType = dsControlGroupType;
				dm.methodName = methodName;

				// See if datastream reference is to fedora server itself or an
				// external location.
				// M and X type datastreams always reference fedora server. With
				// E type datastreams
				// we must examine URL to see if this is referencing a remote
				// datastream or is
				// simply a callback to the fedora server. If the reference is
				// remote, then use
				// the role of the backend service that will make a callback for
				// this datastream.
				// If the referenc s to the fedora server, use the special role
				// of "fedoraInternalCall-1" to
				// denote that the callback will come from the fedora server
				// itself.
				String beServiceRole = null;
				if (ServerUtility.isURLFedoraServer(dsLocation)
						|| dsControlGroupType.equals("M")
						|| dsControlGroupType.equals("X")) {
					beServiceRole = BackendPolicies.FEDORA_INTERNAL_CALL;
				} else {
					beServiceRole = beServiceCallbackRole;
				}

				// Store beSecurity info in hash
				Hashtable<String, String> beHash = m_beSS.getSecuritySpec(
						beServiceRole, methodName);
				boolean beServiceCallbackBasicAuth = Boolean.parseBoolean(
						beHash.get("callbackBasicAuth"));
				boolean beServiceCallBasicAuth = Boolean.parseBoolean(
						beHash.get("callBasicAuth"));
				boolean beServiceCallbackSSL = Boolean.parseBoolean(
						beHash.get("callbackSSL"));
				boolean beServiceCallSSL = Boolean.parseBoolean(
				        beHash.get("callSSL"));
				String beServiceCallUsername = beHash.get("callUsername");
				String beServiceCallPassword = beHash.get("callPassword");
				if (logger.isDebugEnabled()) {
					logger.debug(
							"******************Registering datastream dsLocation: {}",
							dsLocation);
					logger.debug(
							"******************Registering datastream dsControlGroupType: {}",
							dsControlGroupType);
					logger.debug(
							"******************Registering datastream beServiceRole: {}",
							beServiceRole);
					logger.debug(
							"******************Registering datastream beServiceCallbackBasicAuth: {}",
							beServiceCallbackBasicAuth);
					logger.debug(
							"******************Registering datastream beServiceCallBasicAuth: {}",
							beServiceCallBasicAuth);
					logger.debug(
							"******************Registering datastream beServiceCallbackSSL: {}",
							beServiceCallbackSSL);
					logger.debug(
							"******************Registering datastream beServiceCallSSL: {}",
							beServiceCallSSL);
					logger.debug(
							"******************Registering datastream beServiceCallUsername: {}",
							beServiceCallUsername);
					logger.debug(
							"******************Registering datastream beServiceCallPassword: {}",
							beServiceCallPassword);
				}
				dm.callbackRole = beServiceRole;
				dm.callUsername = beServiceCallUsername;
				dm.callPassword = beServiceCallPassword;
				dm.callbackBasicAuth = beServiceCallbackBasicAuth;
				dm.callBasicAuth = beServiceCallBasicAuth;
				dm.callbackSSL = beServiceCallbackSSL;
				dm.callSSL = beServiceCallSSL;
				dsRegistry.put(tempID, dm);
				logger.debug("DatastreammediationKey added to Hash: " + tempID);
			}

		} catch (Throwable th) {
			throw new DisseminationException("[DisseminationService] register"
					+ "DatastreamLocation: "
					+ "returned an error. The underlying error was a "
					+ th.getClass().getName() + " The message " + "was \""
					+ th.getMessage() + "\" .");
		}

		// Replace the blank between date and time with the character "T".
		return tempID.replaceAll(" ", "T");
	}

	/**
	 * <p>
	 * The tempID that is used for datastream mediation consists of a <code>
	 * Timestamp</code> plus a counter appended to the end to insure uniqueness.
	 * This method is a utility method used to extract the Timestamp portion
	 * from the tempID by stripping off the arbitrary counter at the end of the
	 * string.
	 * </p>
	 * 
	 * @param tempID
	 *            The tempID to be extracted.
	 * @return The extracted Timestamp value as a string.
	 */
	public String extractTimestamp(String tempID) {
//		StringBuffer sb = new StringBuffer();
//		sb.append(tempID);
//		sb.replace(tempID.lastIndexOf(":"), tempID.length(), "");
		return tempID.substring(0, tempID.lastIndexOf(":"));
	}

	/**
	 * <p>
	 * Performs simple string replacement using regular expressions. All
	 * matching occurrences of the pattern string will be replaced in the input
	 * string by the replacement string.
	 * 
	 * @param inputString
	 *            The source string.
	 * @param patternString
	 *            The regular expression pattern.
	 * @param replaceString
	 *            The replacement string.
	 * @return The source string with substitutions.
	 */
	private String substituteString(String inputString, String patternString,
			String replaceString) {
		Pattern pattern = Pattern.compile(patternString);
		Matcher m = pattern.matcher(inputString);
		return m.replaceAll(replaceString);
	}

	/**
	 * <p>
	 * Removes any optional userInputParms which remain in the dissemination
	 * URL. This occurs when a method has optional parameters and the user does
	 * not supply a value for one or more of the optional parameters. The result
	 * is a syntax similar to "parm=(PARM_BIND_KEY)". This method removes these
	 * non-supplied optional parameters from the string.
	 * </p>
	 * 
	 * @param dissURL
	 *            String to be processed.
	 * @return An edited string with parameters removed where no value was
	 *         specified for any optional parameters.
	 */
	private String stripParms(String dissURL) {
		// if no parameters, simply return passed in string.
		if (dissURL.indexOf('?') == -1) {
			return dissURL;
		}
		String parmString = dissURL.substring(dissURL.indexOf('?') + 1,
				dissURL.length());
		String[] parms = parmString.split("&");
		StringBuffer sb = new StringBuffer();
		sb.append(dissURL, 0, dissURL.indexOf('?') + 1);
		int index = -1;
		for (String element : parms) {
			int len = element.length() - 1;
			if (element.lastIndexOf(')') != len) {
				sb.append(element + "&");
				index = sb.length() - 1;
			}
		}
		if (index != -1 && index + 1 == sb.length()) {
			sb.setLength(index);
		}
		return sb.toString();
	}

	/**
	 * <p>
	 * Converts the internal dsLocation used by managed and XML type datastreams
	 * to the corresponding Default Dissemination request that will return the
	 * datastream contents.
	 * </p>
	 * 
	 * @param internalDSLocation
	 *            - dsLocation of the Managed or XML type datastream.
	 * @param PID
	 *            - the persistent identifier of the digital object.
	 * @return - A URL corresponding to the Default Dissemination request for
	 *         the specified datastream.
	 * @throws ServerException
	 *             - If anything goes wrong during the conversion attempt.
	 */
	private String resolveInternalDSLocation(Context context,
			String internalDSLocation, Date dsCreateDT, String callbackHost)
			throws ServerException {

		if (callbackHost == null || callbackHost.isEmpty()) {
			throw new DisseminationException(
					"[DisseminationService] was unable to "
							+ "resolve the base URL of the Fedora Server. The URL specified was: \""
							+ callbackHost
							+ "\". This information is required by the Dissemination Service.");
		}

		String[] s = internalDSLocation.split("\\+");
		String dsLocation = null;
		if (s.length == 3) {
			dsLocation = callbackHost + "/" + m_fedoraAppServerContext
					+ "/get/" + s[0] + "/" + s[1] + "/"
					+ DateUtility.convertDateToString(dsCreateDT);

		} else {
			String message = "[DisseminationService] An error has occurred. "
					+ "The internal dsLocation: \"" + internalDSLocation
					+ "\" is " + "not in the required format of: "
					+ "\"doPID+DSID+DSVERSIONID\" .";
			logger.error(message);
			throw new GeneralException(message);
		}
		logger.debug("********** Resolving Internal Datastream dsLocation: {}",
				dsLocation);
		return dsLocation;
	}

	public static void printBindingInfo(DisseminationBindingInfo[] info) {
		for (int i = 0; i < info.length; i++) {
			logger.debug("DisseminationBindingInfo[" + i + "]:");
			logger.debug("  DSBindKey          : " + info[i].DSBindKey);
			logger.debug("  dsLocation         : " + info[i].dsLocation);
			logger.debug("  dsControlGroupType : " + info[i].dsControlGroupType);
			logger.debug("  dsID               : " + info[i].dsID);
			logger.debug("  dsVersionID        : " + info[i].dsVersionID);
			logger.debug("  AddressLocation    : " + info[i].AddressLocation);
			logger.debug("  OperationLocation  : " + info[i].OperationLocation);
			logger.debug("  ProtocolType       : " + info[i].ProtocolType);
			logger.debug("  dsState            : " + info[i].dsState);
			logger.debug("  dsCreateDT         : " + info[i].dsCreateDT);
			for (int j = 0; j < info[i].methodParms.length; j++) {
				MethodParmDef def = info[i].methodParms[j];
				logger.debug("  MethodParamDef[" + j + "]:");
				logger.debug("    parmName         : " + def.parmName);
				logger.debug("    parmDefaultValue : " + def.parmDefaultValue);
				logger.debug("    parmRequired     : " + def.parmRequired);
				logger.debug("    parmLabel        : " + def.parmLabel);
				logger.debug("    parmPassBy       : " + def.parmPassBy);
				for (String element : def.parmDomainValues) {
					logger.debug("    parmDomainValue  : " + element);
				}
			}
		}
	}
}
