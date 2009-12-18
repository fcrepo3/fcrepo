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

package melcoe.fedora.pep.ws;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import melcoe.fedora.pep.AuthzDeniedException;
import melcoe.fedora.pep.ContextHandler;
import melcoe.fedora.pep.ContextHandlerImpl;
import melcoe.fedora.pep.PEPException;
import melcoe.fedora.pep.ws.operations.OperationHandler;
import melcoe.fedora.pep.ws.operations.OperationHandlerException;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.axis.description.OperationDesc;
import org.apache.axis.description.ServiceDesc;
import org.apache.axis.handlers.BasicHandler;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;

import fedora.common.Constants;

/**
 * This class is an Apache Axis handler. It is used as a handler on both the
 * request and response. The handler examines the operation for the request and
 * retrieves an appropriate handler to manage the request.
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public class PEP
        extends BasicHandler {

    private static final long serialVersionUID = -3435060948149239989L;

    private static Logger log = Logger.getLogger(PEP.class.getName());

    /**
     * A list of instantiated handlers. As operations are invoked, handlers for
     * those operations are created and added to this list
     */
    private Map<String, Map<String, OperationHandler>> serviceHandlers = null;

    /**
     * The XACML context handler.
     */
    ContextHandler ctxHandler = null;

    /**
     * A time stamp to note the time this AuthHandler was instantiated.
     */
    private Date ts = null;

    /**
     * Default constructor that initialises the handlers map and the
     * contextHandler.
     * 
     * @throws PEPException
     */
    public PEP()
            throws PEPException {
        super();
        loadHandlers();
        ctxHandler = ContextHandlerImpl.getInstance();
        ts = new Date();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.axis.Handler#invoke(org.apache.axis.MessageContext)
     */
    public void invoke(MessageContext context) throws AxisFault {
        if (log.isDebugEnabled()) {
            log.debug("AuthHandler executed: " + context.getTargetService()
                    + "/" + context.getOperation().getName() + " [" + ts + "]");
        }

        // Obtain the service details
        ServiceDesc service = context.getService().getServiceDescription();
        // Obtain the operation details and message type
        OperationDesc operation = context.getOperation();
        // Obtain a class to handle our request
        OperationHandler operationHandler =
                getHandler(service.getName(), operation.getName());

        // If we found no handler just exit
        if (operationHandler == null) {
            return;
        }

        RequestCtx reqCtx = null;

        // if we are on the request pathway, getPastPivot() == false. True on
        // response pathway
        try {
            if (context.getPastPivot()) {
                reqCtx = operationHandler.handleResponse(context);
            } else {
                reqCtx = operationHandler.handleRequest(context);
            }
        } catch (OperationHandlerException ohe) {
            log.error("Error handling operation: " + operation.getName(), ohe);
            throw AxisFault
                    .makeFault(new PEPException("Error handling operation: "
                            + operation.getName(), ohe));
        }

        // if handler returns null, then there is no work to do (would have
        // thrown exception if things went wrong).
        if (reqCtx == null) {
            return;
        }

        // if we have received a requestContext, we need to hand it over to the
        // context handler for resolution.
        ResponseCtx resCtx = null;

        try {
            resCtx = ctxHandler.evaluate(reqCtx);
        } catch (PEPException pe) {
            log.error("Error evaluating request", pe);
            throw AxisFault
                    .makeFault(new PEPException("Error evaluating request (operation: "
                                                        + operation.getName()
                                                        + ")",
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
    }

    /**
     * Reads the configuration file and loads up all the handlers listed within.
     * This method creates handlers for each of the services listed.
     * 
     * @throws PEPException
     */
    private void loadHandlers() throws PEPException {
        serviceHandlers = new HashMap<String, Map<String, OperationHandler>>();

        try {
            // get the PEP configuration
            File configPEPFile =
                    new File(Constants.FEDORA_HOME,
                             "server/config/config-melcoe-pep.xml");
            InputStream is = new FileInputStream(configPEPFile);
            if (is == null) {
                throw new PEPException("Could not locate config file: config-melcoe-pep.xml");
            }

            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(is);

            // to avoid having the same class instantiated multiple times for multiple services.
            Map<String, OperationHandler> handlerMap =
                    new HashMap<String, OperationHandler>();

            NodeList nodes = doc.getElementsByTagName("handlers-ws");
            for (int x = 0; x < nodes.getLength(); x++) {
                String service =
                        nodes.item(x).getAttributes().getNamedItem("service")
                                .getNodeValue();
                if (service == null || "".equals(service)) {
                    throw new PEPException("Error in config file: service name missing.");
                }

                Map<String, OperationHandler> handlers =
                        serviceHandlers.get(service);
                if (handlers == null) {
                    handlers = new HashMap<String, OperationHandler>();
                    serviceHandlers.put(service, handlers);
                }

                NodeList handlerNodes = nodes.item(x).getChildNodes();
                for (int y = 0; y < handlerNodes.getLength(); y++) {
                    if (handlerNodes.item(y).getNodeType() == Node.ELEMENT_NODE) {
                        String opn =
                                handlerNodes.item(y).getAttributes()
                                        .getNamedItem("operation")
                                        .getNodeValue();
                        String cls =
                                handlerNodes.item(y).getAttributes()
                                        .getNamedItem("class").getNodeValue();

                        if (opn == null || "".equals(opn)) {
                            throw new PEPException("Cannot have a missing or empty operation attribute");
                        }

                        if (cls == null || "".equals(cls)) {
                            throw new PEPException("Cannot have a missing or empty class attribute");
                        }

                        OperationHandler handler = handlerMap.get(cls);

                        if (handler == null) {
                            try {
                                Class<?> handlerClass = Class.forName(cls);
                                handler =
                                        (OperationHandler) handlerClass
                                                .newInstance();
                                handlerMap.put(cls, handler);
                            } catch (ClassNotFoundException e) {
                                log.debug("handlerClass not found: " + cls);
                            } catch (InstantiationException ie) {
                                log.error("Could not instantiate handler: "
                                        + cls);
                                throw new PEPException(ie);
                            } catch (IllegalAccessException iae) {
                                log.error("Could not instantiate handler: "
                                        + cls);
                                throw new PEPException(iae);
                            }
                        }

                        handlers.put(opn, handler);

                        if (log.isDebugEnabled()) {
                            log.debug("handler added to handler map: "
                                    + service + "/" + opn + "/" + cls);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.fatal("Failed to initialse the PEP for WS");
            log.fatal(e.getMessage(), e);
            throw new PEPException(e.getMessage(), e);
        }
    }

    /**
     * Function to try and obtain a handler using the name of the current SOAP
     * service and operation.
     * 
     * @param opName
     *        the name of the operation
     * @return OperationHandler to handle the operation
     * @throws AxisFault
     */
    private OperationHandler getHandler(String serviceName, String operationName) {
        if (serviceName == null) {
            if (log.isDebugEnabled()) {
                log.debug("Service Name was null!");
            }

            return null;
        }

        if (operationName == null) {
            if (log.isDebugEnabled()) {
                log.debug("Operation Name was null!");
            }

            return null;
        }

        Map<String, OperationHandler> handlers =
                serviceHandlers.get(serviceName);
        if (handlers == null) {
            if (log.isDebugEnabled()) {
                log.debug("No Service Handlers found for: " + serviceName);
            }

            return null;
        }

        OperationHandler handler = handlers.get(operationName);
        if (handler == null) {
            if (log.isDebugEnabled()) {
                log.debug("Handler not found for: " + serviceName + "/"
                        + operationName);
            }
        }

        return handler;
    }

    /**
     * Method to check a response and enforce any denial. This is achieved by
     * throwing an AxisFault.
     * 
     * @param res
     *        the ResponseCtx
     * @throws AxisFault
     */
    private void enforce(ResponseCtx res) throws AxisFault {
        @SuppressWarnings("unchecked")
        Set<Result> results = res.getResults();
        for (Result r : results) {
            if (r.getDecision() != Result.DECISION_PERMIT) {
                if (log.isDebugEnabled()) {
                    log.debug("Denying access: " + r.getDecision());
                }
                switch (r.getDecision()) {
                    case Result.DECISION_DENY:
                        throw AxisFault
                                .makeFault(new AuthzDeniedException("Deny"));
                    case Result.DECISION_INDETERMINATE:
                        throw AxisFault
                                .makeFault(new AuthzDeniedException("Indeterminate"));
                    case Result.DECISION_NOT_APPLICABLE:
                        throw AxisFault
                                .makeFault(new AuthzDeniedException("NotApplicable"));
                    default:
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Permitting access!");
        }
    }
}
