/*
 * File: DescribeFilter.java
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

import org.apache.axis.AxisFault;
import org.apache.log4j.Logger;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.RequestCtx;

import fedora.common.Constants;

/**
 * This class handles the describe operation.
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public class DescribeFilter
        extends AbstractFilter {

    private static Logger log =
            Logger.getLogger(DescribeFilter.class.getName());

    /**
     * Default constructor.
     * 
     * @throws PEPException
     */
    public DescribeFilter()
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
        RequestCtx req = null;

        Map<URI, AttributeValue> resAttr = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();

        try {
            resAttr.put(Constants.OBJECT.PID.getURI(),
                        new StringAttribute("FedoraRepository"));
            resAttr
                    .put(new URI("urn:oasis:names:tc:xacml:1.0:resource:resource-id"),
                         new AnyURIAttribute(new URI("FedoraRepository")));

            actions
                    .put(Constants.ACTION.ID.getURI(),
                         new StringAttribute(Constants.ACTION.DESCRIBE_REPOSITORY
                                 .getURI().toASCIIString()));
            actions.put(Constants.ACTION.API.getURI(),
                        new StringAttribute(Constants.ACTION.APIA.getURI()
                                .toASCIIString()));

            req =
                    getContextHandler().buildRequest(getSubjects(request),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(request));

            LogUtil.statLog(request.getRemoteUser(),
                            Constants.ACTION.DESCRIBE_REPOSITORY.getURI()
                                    .toASCIIString(),
                            "FedoraRepository",
                            null);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw AxisFault.makeFault(e);
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
