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

import org.fcrepo.common.Constants;
import org.fcrepo.server.security.RequestCtx;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.pep.ResourceAttributes;
import org.fcrepo.server.security.xacml.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jboss.security.xacml.sunxacml.attr.AttributeValue;

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
    @Override
    public RequestCtx handleRequest(HttpServletRequest request,
                                    HttpServletResponse response)
    throws IOException, ServletException {

        RequestCtx req = null;

        String action = request.getParameter("action");

        if (action == null) {
            throw new ServletException("Invalid request, action parameter must be specified.");
        }


        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr;

        try {

            resAttr = ResourceAttributes.getRepositoryResources();

            // note - no API specified in legacy XACML, so none specified here
            /*
            actions.put(Constants.ACTION.API.getURI(),
                        Constants.ACTION.APIA.getStringAttribute()));
            */

            if (action.equals("status")) {
                actions.put(Constants.ACTION.ID.getURI(),
                            Constants.ACTION.SERVER_STATUS.getStringAttribute());
            } else if (action.equals("reloadPolicies")) {
                actions.put(Constants.ACTION.ID.getURI(),
                            Constants.ACTION.RELOAD_POLICIES.getStringAttribute());
            } else if (action.equals("modifyDatastreamControlGroup")) {
                // FCREPO-765, no specific URI defined for this operation
                actions.put(Constants.ACTION.ID.getURI(),
                            Constants.ACTION.RELOAD_POLICIES.getStringAttribute());
            } else {
                throw new ServletException("Invalid request, invalid action parameter specified:" + action);
            }


            req =
                getContextHandler().buildRequest(getSubjects(request),
                                                 actions,
                                                 resAttr,
                                                 getEnvironment(request));

            LogUtil.statLog(request.getRemoteUser(),
                            Constants.ACTION.LIST_METHODS.uri,
                            Constants.FEDORA_REPOSITORY_PID.uri,
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
