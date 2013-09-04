/*
 * File: PEP.java
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

package org.fcrepo.server.security.xacml.pep.ws;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.fcrepo.server.security.xacml.pep.AuthzDeniedException;
import org.fcrepo.server.security.xacml.pep.ContextHandler;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.pep.ws.operations.OperationHandler;
import org.fcrepo.server.security.xacml.pep.ws.operations.OperationHandlerException;
import org.fcrepo.server.utilities.CXFUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;

/**
 * This class is an JAX-WS handler. It is used as a handler on both the request
 * and response. The handler examines the operation for the request and
 * retrieves an appropriate handler to manage the request.
 *
 * @author Jiri Kremser
 */
public class PEP
        implements javax.xml.ws.handler.soap.SOAPHandler<SOAPMessageContext> {


    private static final Logger logger = LoggerFactory.getLogger(PEP.class);

    /**
     * A list of instantiated handlers. As operations are invoked, handlers for
     * those operations are created and added to this list
     */
    private Map<String, Map<String, OperationHandler>> m_serviceHandlers = null;

    /**
     * The XACML context handler.
     */
    ContextHandler m_ctxHandler = null;

    private final boolean feslAuthZ;

    /**
     * A time stamp to note the time this AuthHandler was instantiated.
     */
    private Date m_ts = null;

    /**
     * Default constructor that initialises the handlers map and the
     * contextHandler.
     *
     * @throws PEPException
     */
    public PEP(boolean feslAuthZ)
            throws PEPException {
        super();
        this.feslAuthZ = feslAuthZ;
        logger.info("feslAuthZ = {}", feslAuthZ);
        if (feslAuthZ) {
            m_serviceHandlers = Collections.emptyMap();
            m_ts = new Date();
        }
    }

    public void setContextHandler(ContextHandler ctxHandler) {
        m_ctxHandler = ctxHandler;
    }

    public void setServiceHandlers(Map<String, Map<String,OperationHandler>> serviceHandlers) {
        m_serviceHandlers = serviceHandlers;
    }

    /*
     * (non-Javadoc)
     */
    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        if (!feslAuthZ) {
            return true;
        }
        String service =
                ((QName) context.get(SOAPMessageContext.WSDL_SERVICE))
                        .getLocalPart();
        String operation =
                ((QName) context.get(SOAPMessageContext.WSDL_OPERATION))
                        .getLocalPart();
        if (logger.isDebugEnabled()) {
            logger.debug("AuthHandler executed: " + service + "/" + operation
                    + " [" + m_ts + "]");
        }

        //        // Obtain the service details
        //        ServiceDesc service = context.getService().getServiceDescription();
        //        // Obtain the operation details and message type
        //        OperationDesc operation = context.getOperation();
        // Obtain a class to handle our request
        OperationHandler operationHandler = getHandler(service, operation);

        // there must always be a handler.
        if (operationHandler == null) {
            logger.error("Missing handler for service/operation: " + service
                    + "/" + operation);
            throw CXFUtility
                    .getFault(new PEPException("Missing handler for service/operation: "
                            + service + "/" + operation));
        }

        RequestCtx reqCtx = null;

        // if we are on the request pathway, outboundProperty == false. True on
        // response pathway
        Boolean outboundProperty =
                (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        try {
            if (outboundProperty) {
                reqCtx = operationHandler.handleResponse(context);
            } else {
                reqCtx = operationHandler.handleRequest(context);
            }
        } catch (OperationHandlerException ohe) {
            logger.error("Error handling operation: " + operation, ohe);
            throw CXFUtility
                    .getFault(new PEPException("Error handling operation: "
                            + operation, ohe));
        }

        // if handler returns null, then there is no work to do (would have
        // thrown exception if things went wrong).
        if (reqCtx == null) {
            return false;
        }

        // if we have received a requestContext, we need to hand it over to the
        // context handler for resolution.
        ResponseCtx resCtx = null;

        try {
            resCtx = m_ctxHandler.evaluate(reqCtx);
        } catch (PEPException pe) {
            logger.error("Error evaluating request", pe);
            throw CXFUtility
                    .getFault(new PEPException("Error evaluating request (operation: "
                                                       + operation + ")",
                                               pe));
        }

        // TODO: set obligations
        /*
         * Need to set obligations in some sort of map, with UserID/SessionID +
         * list of obligationIDs. Enforce will have to check that these
         * obligations are met before providing access. There will need to be an
         * external obligations service that this PEP can communicate with. Will
         * be working on that... This service will throw an 'Obligations need to
         * be met' exception for outstanding obligations
         */

        // TODO: enforce will need to ensure that obligations are met.
        enforce(resCtx);
        return true;
    }

    /**
     * Function to try and obtain a handler using the name of the current SOAP
     * service and operation.
     *
     * @param opName
     *        the name of the operation
     * @return OperationHandler to handle the operation
     */
    private OperationHandler getHandler(String serviceName, String operationName) {
        if (serviceName == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Service Name was null!");
            }

            return null;
        }

        if (operationName == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Operation Name was null!");
            }

            return null;
        }

        Map<String, OperationHandler> handlers =
                m_serviceHandlers.get(serviceName);
        if (handlers == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No Service Handlers found for: " + serviceName);
            }

            return null;
        }

        OperationHandler handler = handlers.get(operationName);
        if (handler == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Handler not found for: " + serviceName + "/"
                        + operationName);
            }
        }

        return handler;
    }

    /**
     * Method to check a response and enforce any denial. This is achieved by
     * throwing an SoapFault.
     *
     * @param res
     *        the ResponseCtx
     */
    private void enforce(ResponseCtx res) {
        @SuppressWarnings("unchecked")
        Set<Result> results = res.getResults();
        for (Result r : results) {
            if (r.getDecision() != Result.DECISION_PERMIT) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Denying access: " + r.getDecision());
                }
                switch (r.getDecision()) {
                    case Result.DECISION_DENY:
                        throw CXFUtility
                                .getFault(new AuthzDeniedException("Deny"));

                    case Result.DECISION_INDETERMINATE:
                        throw CXFUtility
                                .getFault(new AuthzDeniedException("Indeterminate"));
                    case Result.DECISION_NOT_APPLICABLE:
                        throw CXFUtility
                                .getFault(new AuthzDeniedException("NotApplicable"));
                    default:
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Permitting access!");
        }
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public void close(MessageContext arg0) {
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public boolean handleFault(SOAPMessageContext arg0) {
        return false;
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public Set<QName> getHeaders() {
        return null;

    }
}