/*
 * File: FieldSearchResultHandler.java
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

package org.fcrepo.server.security.xacml.pep.ws.operations;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.fcrepo.common.Constants;
import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.fcrepo.server.security.xacml.pep.ContextHandler;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.pep.ResourceAttributes;
import org.fcrepo.server.security.xacml.util.ContextUtil;
import org.fcrepo.server.types.gen.FieldSearchResult;
import org.fcrepo.server.types.gen.ObjectFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Status;


/**
 * This class handles the filtering of search results for both the findObjects
 * and resumeFindObjects operations and is called by each of them.
 *
 * @author nishen@melcoe.mq.edu.au
 */
public class FieldSearchResultHandler
        extends AbstractOperationHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(FieldSearchResultHandler.class);

    private ContextUtil m_contextUtil = null;

    /**
     * Default constructor.
     *
     * @throws PEPException
     */
    public FieldSearchResultHandler(ContextHandler contextHandler)
            throws PEPException {
        super(contextHandler);
    }

    public void setContextUtil(ContextUtil contextUtil) {
        m_contextUtil = contextUtil;
    }

    /**
     * Removes non-permissable objects from the result list.
     *
     * @param context
     *        the message context
     * @param result
     *        the search result object
     * @return the new search result object without non-permissable items
     * @throws PEPException
     */
    public FieldSearchResult filter(SOAPMessageContext context,
                                    FieldSearchResult result)
            throws PEPException {
        if (result == null || result.getResultList() == null || result.getResultList().getObjectFields() == null || result.getResultList().getObjectFields().isEmpty()) {
            return result;
        }
        List<ObjectFields> objs = result.getResultList().getObjectFields();
        List<String> requests = new ArrayList<String>();
        Map<String, ObjectFields> objects = new HashMap<String, ObjectFields>();



        for (ObjectFields o : objs) {
            if (logger.isDebugEnabled()) {
                logger.debug("Checking: " + o.getPid());
            }

            Map<URI, AttributeValue> actions =
                    new HashMap<URI, AttributeValue>();
            Map<URI, AttributeValue> resAttr;

            String pid = o.getPid() != null ? o.getPid().getValue() : null;
            if (pid != null && !pid.isEmpty()) {
                objects.put(pid, o);

                try {
                    actions
                            .put(Constants.ACTION.ID.getURI(),
                                 new StringAttribute(Constants.ACTION.LIST_OBJECT_IN_FIELD_SEARCH_RESULTS
                                         .getURI().toASCIIString()));
                    actions.put(Constants.ACTION.API.getURI(),
                                new StringAttribute(Constants.ACTION.APIA.getURI()
                                        .toASCIIString()));

                    resAttr = ResourceAttributes.getResources(pid);

                    RequestCtx req =
                            getContextHandler()
                                    .buildRequest(getSubjects(context),
                                                  actions,
                                                  resAttr,
                                                  getEnvironment(context));

                    requests.add(m_contextUtil.makeRequestCtx(req));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    throw new OperationHandlerException(e.getMessage(), e);
                }
            }
        }

        String response =
                getContextHandler().evaluateBatch(requests
                        .toArray(new String[requests.size()]));
        ResponseCtx resCtx;
        try {
            resCtx = m_contextUtil.makeResponseCtx(response);
        } catch (MelcoeXacmlException e) {
            throw new PEPException(e);
        }

        @SuppressWarnings("unchecked")
        Set<Result> results = resCtx.getResults();

        List<ObjectFields> resultObjects = new ArrayList<ObjectFields>();
        for (Result r : results) {
            String resource = r.getResource();
            if (resource == null || resource.isEmpty()) {
                logger.warn("This resource has no resource identifier in the xacml response results!");
            } else {
                logger.debug("Checking: {}", resource);
            }

            int lastSlash = resource.lastIndexOf('/');
            String rid = resource.substring(lastSlash + 1);

            if (r.getStatus().getCode().contains(Status.STATUS_OK)
                    && r.getDecision() == Result.DECISION_PERMIT) {
                ObjectFields tmp = objects.get(rid);
                if (tmp != null) {
                    resultObjects.add(tmp);
                    logger.debug("Adding: {}[{}]", resource, rid);
                } else {
                    logger.warn("Not adding this object as no object found for this key: {}[{}]",
                                    resource, rid);
                }
            }
        }
        FieldSearchResult.ResultList rl = new FieldSearchResult.ResultList();
        rl.getObjectFields().addAll(resultObjects);
        result.setResultList(rl);
        return result;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.ws.operations.OperationHandler#handleRequest(SOAPMessageContext)
     */
    @Override
    public RequestCtx handleRequest(SOAPMessageContext context)
            throws OperationHandlerException {
        RequestCtx req = null;

        Map<URI, AttributeValue> resAttr;
        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();

        try {
            resAttr = ResourceAttributes.getRepositoryResources();

            actions.put(Constants.ACTION.ID.getURI(),
                        new StringAttribute(Constants.ACTION.FIND_OBJECTS
                                .getURI().toASCIIString()));
            actions.put(Constants.ACTION.API.getURI(),
                        new StringAttribute(Constants.ACTION.APIA.getURI()
                                .toASCIIString()));

            req =
                    getContextHandler().buildRequest(getSubjects(context),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(context));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new OperationHandlerException(e.getMessage(), e);
        }

        return req;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.ws.operations.OperationHandler#handleResponse(SOAPMessageContext)
     */
    @Override
    public RequestCtx handleResponse(SOAPMessageContext context)
            throws OperationHandlerException {
        try {
            Object response = getSOAPResponseObject(context);
            org.fcrepo.server.types.gen.FieldSearchResult result =
                (org.fcrepo.server.types.gen.FieldSearchResult) callGetter("getResult",response);
            result =
                filter(context, result);
            Method setter = response.getClass().getDeclaredMethod("setResult",
                    org.fcrepo.server.types.gen.FieldSearchResult.class);
            setter.invoke(response, result);
            setSOAPResponseObject(context, response);
        } catch (Exception e) {
            logger.error("Error filtering Objects", e);
            throw new OperationHandlerException("Error filtering Objects", e);
        }

        return null;
    }
}
