package org.fcrepo.security.http;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.fcrepo.server.security.jaas.auth.AuthHttpServletRequestWrapper;
import org.fcrepo.server.security.jaas.auth.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ajs6f
 * @author cs2@st-andrews.ac.uk
 */
public class AuthZHttpRequestAttributesFilter implements Filter {

	// names of HTTP headers to aggregate HTTP attribute
	private Set<String> names;

	// name of the Shibboleth header that contains the principal's name
	private String principalHeader;

	// attribute in which to store authN data
	static final String FEDORA_ATTRIBUTES_KEY = "FEDORA_AUX_SUBJECT_ATTRIBUTES";

	private static final Logger logger = LoggerFactory
			.getLogger(AuthZHttpRequestAttributesFilter.class);

	/**
	 * setNames set the list of attribute names to look for
	 *
	 * @param names
	 *            contains space separated attribute names
	 */
	public void setNames(String names) {
		this.names = new HashSet<String>(Arrays.asList(names.split(" ")));
	}

	/**
	 * setPrincipalHeader set the name of the Shibboleth header whgich contains
	 * the principal's name
	 * 
	 * @param principalHeader
	 *            contains name of header containing principal's name
	 */
	public void setPrincipalHeader(String principalHeader) {
		this.principalHeader = principalHeader;
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;

		// this is the map we will actually store into the request attribute
		Map<String, String[]> subjectAttributes = new HashMap<String, String[]>();

		// we use two loops to populate attributes in case we have multiple
		// homonymic headers in addition to multivalued headers
		for (String name : names) {
			logger.debug("Looking for header with name: {}", name);
			Set<String> headervalues = new HashSet<String>();
			for (Enumeration<String> retrievedheadervalues = req
					.getHeaders(name); retrievedheadervalues.hasMoreElements();) {
				logger.debug("Retrieved header with name: {}", name);
				String value = retrievedheadervalues.nextElement();
				logger.debug("Now adding value: " + value + " to field " + name);

				headervalues.add(value);
			}
			// convert headervalues to a String[] and store it
			subjectAttributes.put(name, headervalues.toArray(new String[0]));
		}

		String[] principalNames = subjectAttributes.get(principalHeader);

		if (isValid(principalNames)) {

			logger.debug("Trying to set principal to new Principal with name="
					+ principalNames[0]);

			// get name of principal and inject it into request
			// we use here a FeSL implementation of java.security.Principal
			UserPrincipal principal = new UserPrincipal(principalNames[0]);
			AuthHttpServletRequestWrapper authRequest = new AuthHttpServletRequestWrapper(
					req);
			authRequest.setUserPrincipal(principal);

			logger.debug("Principal has been set to " + principal);

			// can inject other attributes
			authRequest.setAttribute(FEDORA_ATTRIBUTES_KEY, subjectAttributes);
			if (logger.isDebugEnabled()) {
				logger.debug("Added " + subjectAttributes + " to "
						+ FEDORA_ATTRIBUTES_KEY);
			}
			if (chain != null)
				chain.doFilter(authRequest, response);

		} else {
			// not authenticated, so just pass original request through
			// to rest of chain
			if (chain != null)
				chain.doFilter(request, response);
		}
	}

	/**
	 * @param principalnames
	 *            array of Strings containing possible names for the security
	 *            Principal
	 * @return whether or not the array contains exactly one name of more than 0
	 *         length
	 */
	private Boolean isValid(String[] principalnames) {
		logger.debug("Checking potential principal name.");
		// no principal name found
		if (principalnames == null) {
			logger.debug("Principal name was null!");
			return false;
		}
		// no principal name found, but there was some empty header
		if (0 == principalnames.length) {
			logger.debug("Principal name was an array of zero length!");
			return false;
		}

		// more than one principal name found
		if (principalnames.length > 1) {
			logger.error(new Exception(
					"More than one principal for authentication found in HTTP request!")
					.toString());
			return false;
		}

		// make sure that first principal name is not empty
		if (principalnames[0] == null || principalnames[0].length() == 0) {
			return false;
		}
		// we have a valid Principal name!
		logger.debug("Principal name {} was valid!", principalnames[0]);
		return true;
	}

	/**
	 * init initialise filter
	 * 
	 * @param config
	 *            not used
	 */
	@Override
	public void init(FilterConfig config) throws ServletException {
		this.init();
	}

	public void init() throws ServletException // {{{
	{
		// add principal header to list of names to look for
		names.add(principalHeader);

		// log initialisation
		logger.info("Initializing {}", this.getClass().getName());
	}

	@Override
	public void destroy() {
		logger.info("Destroying {}", this.getClass().getName());
	}

	public String getNames() {
		// we use a loop-and-test to ensure that the results of getNames() could be used for setNames()
		StringBuffer result = new StringBuffer();
		Iterator<String> iter = names.iterator();
		while (iter.hasNext()) {
			result.append(iter.next());
			if (iter.hasNext()) {
				result.append("");
			}
		}
		return result.toString();
	}

	public String getPrincipalHeader() {
		return principalHeader;
	}

}
