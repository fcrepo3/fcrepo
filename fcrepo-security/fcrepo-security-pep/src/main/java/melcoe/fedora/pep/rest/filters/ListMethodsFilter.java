/*
 * File: ListMethodsFilter.java
 *
 * Copyright 2007 Macquarie E-Learning Centre Of Excellence
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package melcoe.fedora.pep.rest.filters;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import melcoe.fedora.pep.PEPException;
import melcoe.fedora.util.LogUtil;

import org.apache.log4j.Logger;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.RequestCtx;

import fedora.common.Constants;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class ListMethodsFilter
        extends AbstractFilter {

    private static Logger log =
            Logger.getLogger(ListMethodsFilter.class.getName());

    /**
     * Default constructor.
     * 
     * @throws PEPException
     */
    public ListMethodsFilter()
            throws PEPException {
        super();
    }

    /*
     * (non-Javadoc)
     * @see
     * melcoe.fedora.pep.rest.filters.RESTFilter#handleRequest(javax.servlet
     * .http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public RequestCtx handleRequest(HttpServletRequest request,
                                    HttpServletResponse response)
            throws IOException, ServletException {
        if (request.getPathInfo() == null) {
            log.error("Bad request: " + request.getRequestURI());
            throw new ServletException("Bad request: "
                    + request.getRequestURI());
        }

        String[] parts = request.getPathInfo().split("/");
        if (parts.length < 2) {
            log.info("Not enough path components on the URI.");
            throw new ServletException("Not enough path components on the URI.");
        }

        RequestCtx req = null;

        String pid = null;
        String dateTime = null;

        // Starting assumption
        pid = parts[1];

        if (parts.length > 2) {
            if (isDate(parts[2])) {
                dateTime = parts[2];
            }
        }

        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr = new HashMap<URI, AttributeValue>();

        try {
            if (pid != null && !"".equals(pid)) {
                resAttr.put(Constants.OBJECT.PID.getURI(),
                            new StringAttribute(pid));
            }
            // XACML 1.0 conformance. resource-id is mandatory. Remove when
            // switching to 2.0
            if (pid != null && !"".equals(pid)) {
                resAttr
                        .put(new URI("urn:oasis:names:tc:xacml:1.0:resource:resource-id"),
                             new AnyURIAttribute(new URI(pid)));
            }
            if (dateTime != null && !"".equals(dateTime)) {
                resAttr.put(Constants.DATASTREAM.AS_OF_DATETIME.getURI(),
                            DateTimeAttribute.getInstance(dateTime));
            }

            actions.put(Constants.ACTION.API.getURI(),
                        new StringAttribute(Constants.ACTION.APIA.getURI()
                                .toASCIIString()));
            actions.put(Constants.ACTION.ID.getURI(),
                        new StringAttribute(Constants.ACTION.LIST_METHODS
                                .getURI().toASCIIString()));

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
            log.error(e.getMessage(), e);
            throw new ServletException(e);
        }

        return req;
    }

    /*
     * (non-Javadoc)
     * @see
     * melcoe.fedora.pep.rest.filters.RESTFilter#handleResponse(javax.servlet
     * .http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public RequestCtx handleResponse(HttpServletRequest request,
                                     HttpServletResponse response)
            throws IOException, ServletException {
        return null;
    }
}
