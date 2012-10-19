/*
 * File: ListDatastreamsHandler.java
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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.cxf.binding.soap.SoapFault;
import org.fcrepo.common.Constants;
import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.fcrepo.server.security.xacml.pep.ContextHandler;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.util.ContextUtil;
import org.fcrepo.server.security.xacml.util.LogUtil;
import org.fcrepo.server.types.gen.DatastreamDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Status;


/**
 * @author nishen@melcoe.mq.edu.au
 */
public class ListDatastreamsHandler
        extends AbstractOperationHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(ListDatastreamsHandler.class);

    private ContextUtil m_contextUtil = null;

    public ListDatastreamsHandler(ContextHandler contextHandler)
            throws PEPException {
        super(contextHandler);
    }

    public void setContextUtil(ContextUtil contextUtil) {
        m_contextUtil = contextUtil;
    }

    @Override
    public RequestCtx handleResponse(SOAPMessageContext context)
            throws OperationHandlerException {
        if (logger.isDebugEnabled()) {
            logger.debug("ListDatastreamsHandler/handleResponse!");
        }

        try {
            List<DatastreamDef> dsDefs =
                getSOAPResponseObject(context, DatastreamDef.class);
            if (dsDefs == null || dsDefs.size() == 0) {
                return null;
            }

            Object oMap = getSOAPRequestObjects(context);
            String pid = (String) callGetter("getPid",oMap);
            if (oMap == null || pid == null) {
                logger.error("No request objects!");
                throw new OperationHandlerException("ListDatastream had no pid");
            }

            dsDefs = filter(context, dsDefs, pid);
//  todo: fix
//            RPCParam[] params = new RPCParam[dsDefs.length];
//            for (int x = 0; x < dsDefs.length; x++) {
//                params[x] =
//                        new RPCParam(context.getOperation().getReturnQName(),
//                                     dsDefs[x]);
//            }
//
//            setSOAPResponseObject(context, params);
        } catch (Exception e) {
            logger.error("Error filtering datastreams", e);
            throw new OperationHandlerException("Error filtering datastreams");
        }

        return null;
    }

    @Override
    public RequestCtx handleRequest(SOAPMessageContext context)
            throws OperationHandlerException {
        if (logger.isDebugEnabled()) {
            logger.debug("ListDatastreamsHandler/handleRequest!");
        }

        RequestCtx req = null;
        Object oMap = null;

        String pid = null;
        String asOfDateTime = null;

        try {
            oMap = getSOAPRequestObjects(context);
            logger.debug("Retrieved SOAP Request Objects");
        } catch (SoapFault af) {
            logger.error("Error obtaining SOAP Request Objects", af);
            throw new OperationHandlerException("Error obtaining SOAP Request Objects",
                                                af);
        }

        try {
            pid = (String) callGetter("getPid",oMap);
            asOfDateTime = (String) callGetter("getAsOfDateTime", oMap);
        } catch (Exception e) {
            logger.error("Error obtaining parameters", e);
            throw new OperationHandlerException("Error obtaining parameters.",
                                                e);
        }

        logger.debug("Extracted SOAP Request Objects");

        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr = new HashMap<URI, AttributeValue>();

        try {
            if (pid != null && !"".equals(pid)) {
                resAttr.put(Constants.OBJECT.PID.getURI(),
                            new StringAttribute(pid));
            }
            if (pid != null && !"".equals(pid)) {
                resAttr.put(Constants.XACML1_RESOURCE.ID.getURI(),
                            new AnyURIAttribute(new URI(pid)));
            }
            if (asOfDateTime != null && !"".equals(asOfDateTime)) {
                resAttr.put(Constants.DATASTREAM.AS_OF_DATETIME.getURI(),
                            DateTimeAttribute.getInstance(asOfDateTime));
            }

            actions.put(Constants.ACTION.ID.getURI(),
                        Constants.ACTION.LIST_DATASTREAMS
                                .getStringAttribute());
            actions.put(Constants.ACTION.API.getURI(),
                        Constants.ACTION.APIA
                                .getStringAttribute());

            req =
                    getContextHandler().buildRequest(getSubjects(context),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(context));

            LogUtil.statLog(getUser(context),
                            Constants.ACTION.LIST_DATASTREAMS.uri,
                            pid,
                            null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new OperationHandlerException(e.getMessage(), e);
        }

        return req;
    }

    public List<DatastreamDef> filter(SOAPMessageContext context,
                                  List<DatastreamDef> dsDefs,
                                  String pid) throws OperationHandlerException,
            PEPException {
        List<String> requests = new ArrayList<String>();
        Map<String, DatastreamDef> objects =
                new HashMap<String, DatastreamDef>();

        for (DatastreamDef dsDef : dsDefs) {
            if (logger.isDebugEnabled()) {
                logger.debug("Checking: " + dsDef.getID());
            }

            objects.put(dsDef.getID(), dsDef);

            Map<URI, AttributeValue> actions =
                    new HashMap<URI, AttributeValue>();
            Map<URI, AttributeValue> resAttr =
                    new HashMap<URI, AttributeValue>();

            try {
                actions.put(Constants.ACTION.ID.getURI(),
                            Constants.ACTION.GET_DATASTREAM
                                    .getStringAttribute());

                resAttr.put(Constants.OBJECT.PID.getURI(),
                            new StringAttribute(pid));
                resAttr.put(Constants.XACML1_RESOURCE.ID.getURI(),
                            new AnyURIAttribute(new URI(pid)));
                resAttr.put(Constants.DATASTREAM.ID.getURI(),
                            new StringAttribute(dsDef.getID()));

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

        List<DatastreamDef> resultObjects = new ArrayList<DatastreamDef>();
        for (Result r : results) {
            if (r.getResource() == null || "".equals(r.getResource())) {
                logger.warn("This resource has no resource identifier in the xacml response results!");
            } else if (logger.isDebugEnabled()) {
                logger.debug("Checking: " + r.getResource());
            }

            String[] ridComponents = r.getResource().split("\\/");
            String rid = ridComponents[ridComponents.length - 1];

            if (r.getStatus().getCode().contains(Status.STATUS_OK)
                    && r.getDecision() == Result.DECISION_PERMIT) {
                DatastreamDef tmp = objects.get(rid);
                if (tmp != null) {
                    resultObjects.add(tmp);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Adding: " + r.getResource() + "[" + rid
                                + "]");
                    }
                } else {
                    logger.warn("Not adding this object as no object found for this key: "
                                    + r.getResource() + "[" + rid + "]");
                }
            }
        }

        return resultObjects;
    }
}
