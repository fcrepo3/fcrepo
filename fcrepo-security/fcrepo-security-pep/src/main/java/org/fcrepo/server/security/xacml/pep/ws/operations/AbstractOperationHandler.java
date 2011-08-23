/*
 * File: AbstractOperationHandler.java
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
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;

import org.fcrepo.server.security.xacml.pep.ContextHandler;
import org.fcrepo.server.security.xacml.pep.ContextHandlerImpl;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.utilities.CXFUtility;

/**
 * This is the AbstractHandler class which provides generic functionality for
 * all operation handlers. All operation handlers should extend this class.
 *
 * @author nishen@melcoe.mq.edu.au
 */
public abstract class AbstractOperationHandler
        implements OperationHandler {

    private static final Logger logger = LoggerFactory
            .getLogger(AbstractOperationHandler.class);

    protected static final String XACML_RESOURCE_ID =
            "urn:oasis:names:tc:xacml:1.0:resource:resource-id";

    protected static final String SUBJECT_ID =
            "urn:oasis:names:tc:xacml:1.0:subject:subject-id";

    protected static final String FEDORA_ROLE =
            "urn:fedora:names:fedora:2.1:subject:role";

    private static ContextHandler contextHandlerImpl;

    /**
     * Default constructor that obtains an instance of the ContextHandler.
     *
     * @throws PEPException
     */
    public AbstractOperationHandler()
            throws PEPException {
        contextHandlerImpl = ContextHandlerImpl.getInstance();
    }

    /**
     * Extracts the request parameters as objects from the context.
     *
     * @param context
     *        the message context.
     * @return list of Objects
     * @throws SoapFault
     */
    protected List<Object> getSOAPRequestObjects(SOAPMessageContext context) {
        // return result
        List<Object> result = new ArrayList<Object>();

        // Obtain the operation details and message type
        QName operation =
                (QName) context.get(SOAPMessageContext.WSDL_OPERATION);

        // Extract the SOAP Message
        SOAPMessage message = context.getMessage();

        // Extract the SOAP Envelope from the Message
        SOAPBody body;
        try {
            body = message.getSOAPBody();
        } catch (SOAPException e) {
            throw CXFUtility.getFault(e);
        }

        //        // Get the envelope body
        //        SOAPBodyElement body = envelope.getFirstBody();

        // Make sure that the body element is an RPCElement.
        //        if (body instanceof RPCElement) {
        // Get all the parameters from the Body Element.
        Iterator params = null;
        try {
            params = ((SOAPElement) body).getChildElements();
        } catch (Exception e) {
            logger.error("Problem obtaining params", e);
            throw CXFUtility.getFault(e);
        }
        int i = 0;
        if (params != null && params.hasNext()) {
            logger.info("Operation: " + operation.getNamespaceURI() + " "
                    + operation.getLocalPart());
            while (params.hasNext()) {
                SOAPElement param = (SOAPElement) params.next();
                result.add(param.getValue());
                logger.info("Obtained object: (" + i++ + ") "
                        + param.getElementQName().toString());
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Number of params: " + i);
            }
        }
        //        }

        return result;
    }

    /**
     * Extracts the return object from the context.
     *
     * @param context
     *        the message context.
     * @return the return object for the message.
     * @throws SoapFault
     */
    protected Object getSOAPResponseObject(SOAPMessageContext context, Class clazz) {
        // return result
        Object result = null;

        // Obtain the operation details and message type
        QName operation =
                (QName) context.get(SOAPMessageContext.WSDL_OPERATION);

        // Extract the SOAP Message
        SOAPMessage message = context.getMessage();

        // Get the envelope body
        SOAPBody body;
        try {
            body = message.getSOAPBody();
        } catch (SOAPException e) {
            throw CXFUtility.getFault(e);
        }

        // Make sure that the body element is an RPCElement.
        //        if (body instanceof RPCElement) {
        // Get all the parameters from the Body Element.
        Iterator params = null;
        try {
            params = ((SOAPElement) body).getChildElements();
        } catch (Exception e) {
            logger.error("Problem obtaining params", e);
            throw CXFUtility.getFault(e);
        }

        int i = 0;
        if (params != null && params.hasNext()) {
            logger.info("Operation: " + operation.getNamespaceURI() + " "
                    + operation.getLocalPart());

            while (params.hasNext() && result == null) {
                SOAPElement param = (SOAPElement) params.next();
                if (clazz.getName().equals(param.getElementQName().getLocalPart())) {
                    logger.info("Obtained object: (" + param + ") "
                            + param.getElementQName().toString());
                    result = param.getValue();
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Number of params: " + i);
        }
        //        }

        if (result == null) {
            throw CXFUtility
                    .getFault(new Exception("Could not obtain Object from SOAP Response"));
        }

        return result;
    }

    /**
     * Sets the request parameters for a request.
     *
     * @param context
     *        the message context
     * @param params
     *        list of parameters to set in order
     * @throws SoapFault
     */
    protected void setSOAPRequestObjects(SOAPMessageContext context,
                                         List<SOAPElement> params) {
        // Extract the SOAP Message
        SOAPMessage message = context.getMessage();

        // Get the envelope body
        SOAPBody body;
        try {
            body = message.getSOAPBody();
        } catch (SOAPException e) {
            throw CXFUtility.getFault(e);
        }

        try {
            body.removeContents();
            for (SOAPElement p : params) {
                body.addChildElement(p);
            }
        } catch (Exception e) {
            logger.error("Problem changing SOAP message contents", e);
            throw CXFUtility.getFault(e);
        }
    }

    /**
     * Sets the return object for a response as the param.
     *
     * @param context
     *        the message context
     * @param param
     *        the object to set as the return object
     * @throws SoapFault
     */
    protected void setSOAPResponseObject(SOAPMessageContext context,
                                         SOAPElement param) {
        // Extract the SOAP Message
        SOAPMessage message = context.getMessage();

        // Get the envelope body
        SOAPBody body;
        try {
            body = message.getSOAPBody();
        } catch (SOAPException e) {
            throw CXFUtility.getFault(e);
        }

        try {
            body.removeContents();
            body.addChildElement(param);
        } catch (Exception e) {
            logger.error("Problem changing SOAP message contents", e);
            throw CXFUtility.getFault(e);
        }
    }

    /**
     * Sets the return object for a response as a sequence of params.
     *
     * @param context
     *        the message context
     * @param param
     *        the object to set as the return object
     * @throws SoapFault
     */
    protected void setSOAPResponseObject(SOAPMessageContext context,
                                         SOAPElement[] params) {
        // Extract the SOAP Message
        SOAPMessage message = context.getMessage();

        // Get the envelope body
        SOAPBody body;
        try {
            body = message.getSOAPBody();
        } catch (SOAPException e) {
            throw CXFUtility.getFault(e);
        }

        try {
            body.removeContents();
            if (params != null) {
                for (SOAPElement param : params) {
                    body.addChildElement(param);
                }
            }
        } catch (Exception e) {
            logger.error("Problem changing SOAP message contents", e);
            throw CXFUtility.getFault(e);
        }
    }

    /**
     * Extracts the list of Subjects from the given context.
     *
     * @param context
     *        the message context
     * @return a list of Subjects
     * @throws SoapFault
     */
    protected List<Map<URI, List<AttributeValue>>> getSubjects(SOAPMessageContext context) {
        // setup the id and value for the requesting subject
        List<Map<URI, List<AttributeValue>>> subjects =
                new ArrayList<Map<URI, List<AttributeValue>>>();

        if (getUser(context) == null
                || "".equals(getUser(context).trim())) {
            return subjects;
        }

        String[] fedoraRole = getUserRoles(context);

        Map<URI, List<AttributeValue>> subAttr = null;
        List<AttributeValue> attrList = null;
        try {
            subAttr = new HashMap<URI, List<AttributeValue>>();
            attrList = new ArrayList<AttributeValue>();
            attrList.add(new StringAttribute(getUser(context)));
            subAttr.put(Constants.SUBJECT.LOGIN_ID.getURI(), attrList);
            if (fedoraRole != null && fedoraRole.length > 0) {
                attrList = new ArrayList<AttributeValue>();
                for (String r : fedoraRole) {
                    attrList.add(new StringAttribute(r));
                }
                subAttr.put(new URI(FEDORA_ROLE), attrList);
            }
            subjects.add(subAttr);

            subAttr = new HashMap<URI, List<AttributeValue>>();
            attrList = new ArrayList<AttributeValue>();
            attrList.add(new StringAttribute(getUser(context)));
            subAttr.put(Constants.SUBJECT.USER_REPRESENTED.getURI(), attrList);
            if (fedoraRole != null && fedoraRole.length > 0) {
                attrList = new ArrayList<AttributeValue>();
                for (String r : fedoraRole) {
                    attrList.add(new StringAttribute(r));
                }
                subAttr.put(new URI(FEDORA_ROLE), attrList);
            }
            subjects.add(subAttr);

            subAttr = new HashMap<URI, List<AttributeValue>>();
            attrList = new ArrayList<AttributeValue>();
            attrList.add(new StringAttribute(getUser(context)));
            subAttr.put(new URI(SUBJECT_ID), attrList);
            if (fedoraRole != null && fedoraRole.length > 0) {
                attrList = new ArrayList<AttributeValue>();
                for (String r : fedoraRole) {
                    attrList.add(new StringAttribute(r));
                }
                subAttr.put(new URI(FEDORA_ROLE), attrList);
            }
            subjects.add(subAttr);
        } catch (URISyntaxException use) {
            logger.error(use.getMessage(), use);
            throw CXFUtility.getFault(use);
        }

        return subjects;
    }

    /**
     * Obtains a list of environment Attributes.
     *
     * @param context
     *        the message context
     * @return list of environment Attributes
     */
    protected Map<URI, AttributeValue> getEnvironment(SOAPMessageContext context) {
        Map<URI, AttributeValue> envAttr = new HashMap<URI, AttributeValue>();

        HttpServletRequest request =
                (HttpServletRequest) context
                        .get(SOAPMessageContext.SERVLET_REQUEST);
        String ip = request.getRemoteAddr();

        if (ip != null && !"".equals(ip)) {
            envAttr.put(Constants.HTTP_REQUEST.CLIENT_IP_ADDRESS.getURI(),
                        new StringAttribute(ip));
        }

        return envAttr;
    }

    /**
     * @return the Context Handler
     */
    protected ContextHandler getContextHandler() {
        return contextHandlerImpl;
    }

    /**
     * Returns the roles that the user has.
     *
     * @param context
     *        the message context
     * @return a String array of roles
     */
    @SuppressWarnings("unchecked")
    protected String[] getUserRoles(SOAPMessageContext context) {
        HttpServletRequest request =
                (HttpServletRequest) context
                        .get(SOAPMessageContext.SERVLET_REQUEST);

        Map<String, Set<String>> reqAttr = null;
        reqAttr =
                (Map<String, Set<String>>) request
                        .getAttribute("FEDORA_AUX_SUBJECT_ATTRIBUTES");

        if (reqAttr == null) {
            return null;
        }

        Set<String> fedoraRoles = reqAttr.get("fedoraRole");
        if (fedoraRoles == null || fedoraRoles.size() == 0) {
            return null;
        }

        String[] fedoraRole =
                fedoraRoles.toArray(new String[fedoraRoles.size()]);

        return fedoraRole;
    }

    protected String getUser(SOAPMessageContext context){
//        String username = (String) context.get("Username");
        HttpServletRequest request =
            (HttpServletRequest) context
                    .get(SOAPMessageContext.SERVLET_REQUEST);
        return request.getRemoteUser();
    }
}
