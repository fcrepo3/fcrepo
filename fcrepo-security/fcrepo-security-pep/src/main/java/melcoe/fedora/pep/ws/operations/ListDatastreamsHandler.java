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

package melcoe.fedora.pep.ws.operations;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import melcoe.fedora.pep.PEPException;
import melcoe.fedora.util.LogUtil;
import melcoe.xacml.MelcoeXacmlException;
import melcoe.xacml.util.ContextUtil;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.axis.message.RPCParam;
import org.apache.log4j.Logger;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Status;

import fedora.common.Constants;
import fedora.server.types.gen.DatastreamDef;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class ListDatastreamsHandler
        extends AbstractOperationHandler {

    private static Logger log =
            Logger.getLogger(ListDatastreamsHandler.class.getName());

    private final ContextUtil contextUtil = new ContextUtil();

    public ListDatastreamsHandler()
            throws PEPException {
        super();
    }

    public RequestCtx handleResponse(MessageContext context)
            throws OperationHandlerException {
        if (log.isDebugEnabled()) {
            log.debug("ListDatastreamsHandler/handleResponse!");
        }

        try {
            DatastreamDef[] dsDefs =
                    (DatastreamDef[]) getSOAPResponseObject(context);
            if (dsDefs == null || dsDefs.length == 0) {
                return null;
            }

            List<Object> oMap = getSOAPRequestObjects(context);
            if (oMap == null || oMap.size() == 0) {
                log.error("No request objects!");
                throw new OperationHandlerException("ListDatastream had no pid");
            }
            String pid = (String) oMap.get(0);

            dsDefs = filter(context, dsDefs, pid);

            RPCParam[] params = new RPCParam[dsDefs.length];
            for (int x = 0; x < dsDefs.length; x++) {
                params[x] =
                        new RPCParam(context.getOperation().getReturnQName(),
                                     dsDefs[x]);
            }

            setSOAPResponseObject(context, params);
        } catch (Exception e) {
            log.error("Error filtering datastreams", e);
            throw new OperationHandlerException("Error filtering datastreams");
        }

        return null;
    }

    public RequestCtx handleRequest(MessageContext context)
            throws OperationHandlerException {
        if (log.isDebugEnabled()) {
            log.debug("ListDatastreamsHandler/handleRequest!");
        }

        RequestCtx req = null;
        List<Object> oMap = null;

        String pid = null;
        String asOfDateTime = null;

        try {
            oMap = getSOAPRequestObjects(context);
            log.debug("Retrieved SOAP Request Objects");
        } catch (AxisFault af) {
            log.error("Error obtaining SOAP Request Objects", af);
            throw new OperationHandlerException("Error obtaining SOAP Request Objects",
                                                af);
        }

        try {
            pid = (String) oMap.get(0);
            asOfDateTime = (String) oMap.get(1);
        } catch (Exception e) {
            log.error("Error obtaining parameters", e);
            throw new OperationHandlerException("Error obtaining parameters.",
                                                e);
        }

        log.debug("Extracted SOAP Request Objects");

        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr = new HashMap<URI, AttributeValue>();

        try {
            if (pid != null && !"".equals(pid)) {
                resAttr.put(Constants.OBJECT.PID.getURI(),
                            new StringAttribute(pid));
            }
            if (pid != null && !"".equals(pid)) {
                resAttr.put(new URI(XACML_RESOURCE_ID),
                            new AnyURIAttribute(new URI(pid)));
            }
            if (asOfDateTime != null && !"".equals(asOfDateTime)) {
                resAttr.put(Constants.DATASTREAM.AS_OF_DATETIME.getURI(),
                            DateTimeAttribute.getInstance(asOfDateTime));
            }

            actions.put(Constants.ACTION.ID.getURI(),
                        new StringAttribute(Constants.ACTION.LIST_DATASTREAMS
                                .getURI().toASCIIString()));
            actions.put(Constants.ACTION.API.getURI(),
                        new StringAttribute(Constants.ACTION.APIA.getURI()
                                .toASCIIString()));

            req =
                    getContextHandler().buildRequest(getSubjects(context),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(context));

            LogUtil.statLog(context.getUsername(),
                            Constants.ACTION.LIST_DATASTREAMS.getURI()
                                    .toASCIIString(),
                            pid,
                            null);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new OperationHandlerException(e.getMessage(), e);
        }

        return req;
    }

    public DatastreamDef[] filter(MessageContext context,
                                  DatastreamDef[] dsDefs,
                                  String pid) throws OperationHandlerException,
            PEPException {
        List<String> requests = new ArrayList<String>();
        Map<String, DatastreamDef> objects =
                new HashMap<String, DatastreamDef>();

        for (DatastreamDef dsDef : dsDefs) {
            if (log.isDebugEnabled()) {
                log.debug("Checking: " + dsDef.getID());
            }

            objects.put(dsDef.getID(), dsDef);

            Map<URI, AttributeValue> actions =
                    new HashMap<URI, AttributeValue>();
            Map<URI, AttributeValue> resAttr =
                    new HashMap<URI, AttributeValue>();

            try {
                actions.put(Constants.ACTION.ID.getURI(),
                            new StringAttribute(Constants.ACTION.GET_DATASTREAM
                                    .getURI().toASCIIString()));

                resAttr.put(Constants.OBJECT.PID.getURI(),
                            new StringAttribute(pid));
                resAttr.put(new URI(XACML_RESOURCE_ID),
                            new AnyURIAttribute(new URI(pid)));
                resAttr.put(Constants.DATASTREAM.ID.getURI(),
                            new StringAttribute(dsDef.getID()));

                RequestCtx req =
                        getContextHandler()
                                .buildRequest(getSubjects(context),
                                              actions,
                                              resAttr,
                                              getEnvironment(context));

                requests.add(contextUtil.makeRequestCtx(req));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new OperationHandlerException(e.getMessage(), e);
            }
        }

        String response =
                getContextHandler().evaluateBatch(requests
                        .toArray(new String[requests.size()]));
        ResponseCtx resCtx;
        try {
            resCtx = contextUtil.makeResponseCtx(response);
        } catch (MelcoeXacmlException e) {
            throw new PEPException(e);
        }

        @SuppressWarnings("unchecked")
        Set<Result> results = resCtx.getResults();

        List<DatastreamDef> resultObjects = new ArrayList<DatastreamDef>();
        for (Result r : results) {
            if (r.getResource() == null || "".equals(r.getResource())) {
                log
                        .warn("This resource has no resource identifier in the xacml response results!");
            } else if (log.isDebugEnabled()) {
                log.debug("Checking: " + r.getResource());
            }

            String[] ridComponents = r.getResource().split("\\/");
            String rid = ridComponents[ridComponents.length - 1];

            if (r.getStatus().getCode().contains(Status.STATUS_OK)
                    && r.getDecision() == Result.DECISION_PERMIT) {
                DatastreamDef tmp = objects.get(rid);
                if (tmp != null) {
                    resultObjects.add(tmp);
                    if (log.isDebugEnabled()) {
                        log.debug("Adding: " + r.getResource() + "[" + rid
                                + "]");
                    }
                } else {
                    log
                            .warn("Not adding this object as no object found for this key: "
                                    + r.getResource() + "[" + rid + "]");
                }
            }
        }

        return resultObjects.toArray(new DatastreamDef[resultObjects.size()]);
    }
}
