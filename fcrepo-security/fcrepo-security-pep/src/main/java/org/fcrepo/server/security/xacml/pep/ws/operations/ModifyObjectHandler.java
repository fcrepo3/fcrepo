/*
 * File: ModifyObjectHandler.java
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
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.cxf.binding.soap.SoapFault;
import org.fcrepo.common.Constants;
import org.fcrepo.server.security.xacml.pep.ContextHandler;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.pep.ResourceAttributes;
import org.fcrepo.server.security.xacml.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.RequestCtx;


/**
 * @author nishen@melcoe.mq.edu.au
 */
public class ModifyObjectHandler
        extends AbstractOperationHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(ModifyObjectHandler.class);

    private static final StringAttribute PUBLISH = new StringAttribute("publish");

    private static final StringAttribute UNPUBLISH = new StringAttribute("unpublish");

    public ModifyObjectHandler(ContextHandler contextHandler)
            throws PEPException {
        super(contextHandler);
    }

    @Override
    public RequestCtx handleResponse(SOAPMessageContext context)
            throws OperationHandlerException {
        return null;
    }

    @Override
    public RequestCtx handleRequest(SOAPMessageContext context)
            throws OperationHandlerException {
        logger.debug("ModifyObjectHandler/handleRequest!");

        RequestCtx req = null;
        Object oMap = null;

        String pid = null;
        String state = null;
        String ownerId = null;

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
            state = (String) callGetter("getState",oMap);
            ownerId = (String) callGetter("getOwnerId",oMap);
        } catch (Exception e) {
            logger.error("Error obtaining parameters", e);
            throw new OperationHandlerException("Error obtaining parameters.",
                                                e);
        }

        logger.debug("Extracted SOAP Request Objects");

        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr;

        try {
            resAttr = ResourceAttributes.getResources(pid);
            if (state != null && !state.isEmpty()) {
                resAttr.put(Constants.OBJECT.STATE.getURI(),
                            new StringAttribute(state));
            }
            if (ownerId != null && !ownerId.isEmpty()) {
                resAttr.put(Constants.OBJECT.OWNER.getURI(),
                            new StringAttribute(state));
            }

            if (state != null && state.equals("A")) {
                actions.put(Constants.ACTION.ID.getURI(),
                            PUBLISH);
            } else if (state != null && state.equals("I")) {
                actions.put(Constants.ACTION.ID.getURI(),
                            UNPUBLISH);
            } else {
                actions.put(Constants.ACTION.ID.getURI(),
                            Constants.ACTION.MODIFY_OBJECT
                                    .getStringAttribute());
            }
            actions.put(Constants.ACTION.API.getURI(),
                        Constants.ACTION.APIM
                                .getStringAttribute());

            req =
                    getContextHandler().buildRequest(getSubjects(context),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(context));

            LogUtil.statLog(getUser(context),
                            Constants.ACTION.MODIFY_OBJECT.uri,
                            pid,
                            null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new OperationHandlerException(e.getMessage(), e);
        }

        return req;
    }
}
