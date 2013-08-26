/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.xacml.pep.rest.objectshandlers;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fcrepo.common.Constants;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.pep.ResourceAttributes;
import org.fcrepo.server.security.xacml.pep.rest.filters.AbstractFilter;
import org.fcrepo.server.security.xacml.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.RequestCtx;

/**
* Handles REST API method getDissemination
*
* @author Stephen Bayliss
* @version $$Id$$
*/
public class GetDissemination
    extends AbstractFilter {

    private static final Logger logger =
        LoggerFactory.getLogger(GetDissemination.class);

        public GetDissemination()
            throws PEPException {
        super();
    }

        @Override
        public RequestCtx handleRequest(HttpServletRequest request,
                                        HttpServletResponse response)
                throws IOException, ServletException {
            if (logger.isDebugEnabled()) {
                logger.debug("{}/handleRequest!", this.getClass().getName());
            }

            String[] parts = getPathParts(request);
            if (parts.length < 5) {
                logger.error("Not enough path components on the URI: {}", request.getRequestURI());
                throw new ServletException("Not enough path components on the URI: " + request.getRequestURI());
            }
            
            String pid = parts[1];

            String asOfDateTime = request.getParameter("asOfDateTime");
            if (!isDate(asOfDateTime)) {
                asOfDateTime = null;
            }

            RequestCtx req = null;
            Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
            Map<URI, AttributeValue> resAttr;
            try {
                resAttr = ResourceAttributes.getResources(pid);

                // - /objects/[pid]/methods/[sdef]/[method]
                if ("methods".equals(parts[2])) {
                    String sDefPid = parts[3];
                    String methodName = parts[4];
                    if (sDefPid != null && !sDefPid.isEmpty()) {
                        resAttr.put(Constants.SDEF.PID.getURI(),
                                new StringAttribute(sDefPid));
                    }
                    if (methodName != null && !methodName.isEmpty()) {
                        resAttr.put(Constants.DISSEMINATOR.METHOD.getURI(),
                                new StringAttribute(methodName));
                    }
                }

                if (asOfDateTime != null && !asOfDateTime.isEmpty()) {
                    resAttr.put(Constants.DATASTREAM.AS_OF_DATETIME.getURI(),
                                DateTimeAttribute.getInstance(asOfDateTime));
                }

                actions.put(Constants.ACTION.ID.getURI(),
                            Constants.ACTION.GET_DISSEMINATION
                                    .getStringAttribute());
                actions.put(Constants.ACTION.API.getURI(),
                            Constants.ACTION.APIA.getStringAttribute());

                req =
                        getContextHandler().buildRequest(getSubjects(request),
                                                         actions,
                                                         resAttr,
                                                         getEnvironment(request));

                LogUtil.statLog(request.getRemoteUser(),
                                Constants.ACTION.GET_DISSEMINATION.uri,
                                pid,
                                null);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new ServletException(e.getMessage(), e);
            }

            return req;
        }

}
