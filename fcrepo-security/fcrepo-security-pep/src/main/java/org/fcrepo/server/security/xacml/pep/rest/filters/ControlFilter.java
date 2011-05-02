/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.xacml.pep.rest.filters;

import java.io.IOException;

import java.net.URI;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.RequestCtx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;

import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.util.LogUtil;

/**
* Filter for ServerController operations
*
* @author Stephen Bayliss
* @version $$Id$$
*/
public class ControlFilter
        extends AbstractFilter {


    private static final Logger logger =
        LoggerFactory.getLogger(ControlFilter.class);

    /**
     * Default constructor.
     *
     * @throws PEPException
     */
    public ControlFilter()
    throws PEPException {
        super();
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.rest.filters.RESTFilter#handleRequest(javax.servlet
     * .http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public RequestCtx handleRequest(HttpServletRequest request,
                                    HttpServletResponse response)
    throws IOException, ServletException {

        RequestCtx req = null;

        String action = request.getParameter("action");

        if (action == null) {
            throw new ServletException("Invalid request, action parameter must be specified.");
        }


        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr = new HashMap<URI, AttributeValue>();

        try {

            String pid = "FedoraRepository";

            resAttr.put(Constants.OBJECT.PID.getURI(),
                        new StringAttribute(pid));
            resAttr
            .put(new URI("urn:oasis:names:tc:xacml:1.0:resource:resource-id"),
                 new AnyURIAttribute(new URI(pid)));

            // note - no API specified in legacy XACML, so none specified here
            /*
            actions.put(Constants.ACTION.API.getURI(),
                        new StringAttribute(Constants.ACTION.APIA.getURI()
                                            .toASCIIString()));
            */

            if (action.equals("status")) {
                actions.put(Constants.ACTION.ID.getURI(),
                    new StringAttribute(Constants.ACTION.SERVER_STATUS.getURI().toASCIIString()));
            } else if (action.equals("reloadPolicies")) {
                actions.put(Constants.ACTION.ID.getURI(),
                            new StringAttribute(Constants.ACTION.RELOAD_POLICIES.getURI().toASCIIString()));
            } else if (action.equals("modifyDatastreamControlGroup")) {
                // FCREPO-765, no specific URI defined for this operation
                actions.put(Constants.ACTION.ID.getURI(),
                            new StringAttribute(Constants.ACTION.RELOAD_POLICIES.getURI().toASCIIString()));
            } else {
                throw new ServletException("Invalid request, invalid action parameter specified:" + action);
            }


            req =
                getContextHandler().buildRequest(getSubjects(request),
                                                 actions,
                                                 resAttr,
                                                 getEnvironment(request));

            LogUtil.statLog(request.getRemoteUser(),
                            Constants.ACTION.LIST_METHODS.getURI()
                            .toASCIIString(),
                            pid,
                            null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ServletException(e);
        }

        return req;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.rest.filters.RESTFilter#handleResponse(javax.servlet
     * .http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public RequestCtx handleResponse(HttpServletRequest request,
                                     HttpServletResponse response)
    throws IOException, ServletException {
        return null;
    }


}
