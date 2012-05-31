/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.security.http;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class AuthZHttpRequestAttributesFilterTest extends TestCase {
	
	private static final Logger logger = LoggerFactory
			.getLogger(AuthZHttpRequestAttributesFilterTest.class);


	private MockHttpServletRequest request;
	private ServletResponse response;
	private AuthZHttpRequestAttributesFilter filter = new AuthZHttpRequestAttributesFilter();
	private MockFilterChain chain = new MockFilterChain();

	private final String header1 = "header1";
	private final String header2 = "header2";
	private final String principalHeader = "principalHeader";
	private final String attributesKey = AuthZHttpRequestAttributesFilter.FEDORA_ATTRIBUTES_KEY;

	@BeforeClass
	public void logMyRunning() {
		logger.info("Running AuthZHttpRequestAttributesFilterTest");
	}
	
	@Before
	public void setUp() {
		filter.setNames(header1 + " " + header2);
		filter.setPrincipalHeader(principalHeader);
		try {
			filter.init();
		} catch (ServletException e) {
			logger.error("Unexpected ServletException", e);
		}
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
	}
	

	/*
	 * A request without a principal header should not acquire any authz attributes from
	 * the headers.
	 */
	@Test
	public void testUnauthenticatedRequestHasNoAttributes() {
		logger.info("Running testUnauthenticatedRequestHasNoAttributes");
		try {
			filter.doFilter(request, response, chain);
		} catch (IOException e) {
			logger.error("Unexpected IOException", e);
		} catch (ServletException e) {
			logger.error("Unexpected ServletException", e);
		}
		assertNull("Found subject attributes where there should be none!",
				chain.getRequest().getAttribute(attributesKey));
	}
	
	/* A request with a principal header should be annotated with 
	 * authorization attributes. 
	 */
	@Test
	public void testAuthenticatedRequestHasAttributes() {
		logger.info("Running testAuthenticatedRequestHasAttributes");
		request.addHeader(principalHeader, "Hamfast Gamgee");
		try {
			filter.doFilter(request, response, chain);
		} catch (IOException e) {
			logger.error("Unexpected IOException", e);
		} catch (ServletException e) {
			logger.error("Unexpected ServletException", e);
		}
		Object attributes = chain.getRequest().getAttribute(attributesKey);
		logger.debug("Found attributes: {}", attributes);
		assertNotNull("Didn't find subject attributes where they should be!",
				attributes);
	}
	
	/* A request with a principal header should be authenticated.. 
	 */
	@Test
	public void testAuthenticatedRequestIsAuthenticated() {
		logger.info("Running testAuthenticatedRequestIsAuthenticated");
		request.addHeader(principalHeader, "Wiseman Gamwich");
		try {
			filter.doFilter(request, response, chain);
		} catch (IOException e) {
			logger.error("Unexpected IOException", e);
		} catch (ServletException e) {
			logger.error("Unexpected ServletException", e);
		}
		HttpServletRequest httprequest = (HttpServletRequest)chain.getRequest();
		assertNotNull("Didn't find authenticated principal!",
				httprequest.getUserPrincipal());
	}
	
	/* A request without a principal header should not be authenticated.. 
	 */
	@Test
	public void testUnauthenticatedRequestIsNotAuthenticated() {
		logger.info("Running testUnauthenticatedRequestIsNotAuthenticated");
		try {
			filter.doFilter(request, response, chain);
		} catch (IOException e) {
			logger.error("Unexpected IOException", e);
		} catch (ServletException e) {
			logger.error("Unexpected ServletException", e);
		}
		HttpServletRequest httprequest = (HttpServletRequest)chain.getRequest();
		assertNull("Found authenticated principal!",
				httprequest.getUserPrincipal());
	}
	
	/*
	 * A request with a principal header should pass authorization info through
	 * in the Fedora-expected request attribute
	 */
	@Test
	public void testAuthenticatedRequestHasAttributeValue() {
		logger.info("Running testAuthenticatedRequestHasAttributeValue"); 
		request.addHeader(principalHeader, "Hobson Gamgee");
		request.addHeader(header1, "Some value");
		try {
			filter.doFilter(request, response, chain);
		} catch (IOException e) {
			logger.error("Unexpected IOException", e);
		} catch (ServletException e) {
			logger.error("Unexpected ServletException", e);
		}
		Map<String,String[]>  attributes = (Map<String, String[]>) chain.getRequest().getAttribute(attributesKey);
		logger.debug("Found attributes: {}", attributes );
		String ourheader = attributes.get(header1)[0];
		assertEquals(ourheader,"Some value");
	}
	
	/*
	 * A header that is not called out for usage should not be passed on.
	 */
	@Test
	public void testAuthenticatedRequestLacksAttributeValue() {
		logger.info("Running testAuthenticatedRequestLacksAttributeValue"); 
		filter.setNames(header1);
		filter.setPrincipalHeader(principalHeader);
		try {
			filter.init();
		} catch (ServletException e1) {
			logger.error("Unexpected ServletException" ,e1);
		}
		request.addHeader(principalHeader, "Hob Gammidge.");
		request.addHeader(header2, "Some value");
		try {
			filter.doFilter(request, response, chain);
		} catch (IOException e) {
			logger.error("Unexpected IOException", e);
		} catch (ServletException e) {
			logger.error("Unexpected ServletException", e);
		}
		Map<String,String[]>  attributes = (Map<String, String[]>) chain.getRequest().getAttribute(attributesKey);
		logger.debug("Found attributes: {}", attributes );
		String[] ourheader = attributes.get(header2);
		assertNull(ourheader);
	}
	
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(AuthZHttpRequestAttributesFilterTest.class);
	}

}
