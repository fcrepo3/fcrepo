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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.fcrepo.common.Constants;
import org.fcrepo.server.security.xacml.pep.ContextHandler;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.utilities.CXFUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;

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

    protected static final JAXBContext JAXB_CONTEXT = getJAXBContext();

    private final ContextHandler m_contextHandler;

    /**
     * Default constructor that obtains an instance of the ContextHandler.
     *
     * @throws PEPException
     */
    public AbstractOperationHandler(ContextHandler contextHandler)
            throws PEPException {
        m_contextHandler = contextHandler;
    }

    /**
     * Extracts the request as object from the context.
     *
     * @param context
     *        the message context.
     * @return Object
     * @throws SoapFault
     */
    protected Object getSOAPRequestObjects(SOAPMessageContext context) {
        // Obtain the operation details and message type
        // Extract the SOAP Message
        SOAPElement requestNode = getSOAPRequestNode(context);
        return unmarshall(requestNode);
    }

    private SOAPElement getSOAPNode(SOAPMessageContext context, QName operation) {
        SOAPMessage message = context.getMessage();
        SOAPBody body;
        try {
            body = message.getSOAPBody();
        } catch (SOAPException e) {
            throw CXFUtility.getFault(e);
        }
        SOAPElement bodyElement = body;
        if (bodyElement.getNamespaceURI().equals("http://schemas.xmlsoap.org/soap/envelope/")
                && bodyElement.getLocalName().equals("Body")){
            bodyElement = (SOAPElement)bodyElement.getElementsByTagNameNS(operation.getNamespaceURI(), operation.getLocalPart()).item(0);
        }
        if (logger.isDebugEnabled() && bodyElement != null) {
            logger.debug("Operation: {} {}", operation.getNamespaceURI(), operation.getLocalPart());
        }
        return bodyElement;
    }

    protected SOAPElement getSOAPRequestNode(SOAPMessageContext context) {
        QName operation =
                (QName) context.get(SOAPMessageContext.WSDL_OPERATION);
        SOAPElement bodyElement = getSOAPNode(context, operation);
        if (bodyElement == null){
            logger.error("Could not obtain " + operation.toString() + " Object from SOAP Request");
            logger.error(context.getMessage().toString());
            throw CXFUtility
                    .getFault(new Exception("Could not obtain Object from SOAP Request"));
        }
        return bodyElement;
    }

    protected SOAPElement getSOAPResponseNode(SOAPMessageContext context) {
        QName operation =
                (QName) context.get(SOAPMessageContext.WSDL_OPERATION);
        operation = new QName(operation.getNamespaceURI(),operation.getLocalPart() + "Response");
        SOAPElement bodyElement = getSOAPNode(context, operation);
        if (bodyElement == null){
            logger.error("Could not obtain " + operation.toString() + " Object from SOAP Response");
            logger.error(context.getMessage().toString());
            throw CXFUtility
                    .getFault(new Exception("Could not obtain Object from SOAP Response"));
        }
        return bodyElement;
    }

    protected Object getSOAPResponseObject(SOAPMessageContext context) {
        // Obtain the operation details and message type
        // Extract the SOAP Message
        SOAPElement responseNode = getSOAPResponseNode(context);
        return unmarshall(responseNode);
    }

    /**
     * Extracts the return object from the context.
     * @param <T>
     * @param <T>
     *
     * @param context
     *        the message context.
     * @return the return object for the message.
     * @throws SoapFault
     */
    protected <T> List<T>  getSOAPResponseObject(SOAPMessageContext context, Class<T> clazz) {
        // return result

        Object responseObject = getSOAPResponseObject(context);
        ArrayList<T> resultList = new ArrayList<T>();
        for (Method m:responseObject.getClass().getDeclaredMethods()){
            if (m.getReturnType() == clazz) {
                try {
                    resultList.add((T)m.invoke(responseObject, null));
                } catch (Exception e) {
                    logger.error(e.toString(),e);
                    throw CXFUtility.getFault(e);
                }
            }
        }

        return resultList;
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
                                         Object newResponse) {
        if (newResponse == null) {
            return;
        }
        // Extract the SOAP Message
        SOAPMessage message = context.getMessage();

        // Get the envelope body
        SOAPElement body;
        try {
            body = message.getSOAPBody();
            SOAPElement response = getSOAPResponseNode(context);
            body.removeChild(response);
            marshall(newResponse, body);
        } catch (SOAPException e) {
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
        return m_contextHandler;
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

    protected Object callGetter(String mname, Object context) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Class klass = context.getClass();
        Method getter = klass.getDeclaredMethod(mname, null);
        return getter.invoke(context);
    }

    private static JAXBContext getJAXBContext(){
        try {
            return JAXBContext.newInstance("org.fcrepo.server.types.gen:org.fcrepo.server.types.mtom.gen");
        } catch (JAXBException e) {
            logger.error(e.toString(),e);
            return null;
        }

    }

    protected static Object unmarshall(SOAPElement node) {
        if (node == null) return node;
        Unmarshaller um = null;
        try {
            um = JAXB_CONTEXT.createUnmarshaller();
        } catch (JAXBException e) {
            logger.error(e.toString(),e);
            throw CXFUtility.getFault(e);
        }
        Object result = null;
        try {
            result = um.unmarshal(node);
        } catch (Exception e) {
            logger.error("Problem obtaining params", e);
            throw CXFUtility.getFault(e);
        }
        return result;
    }

    protected static void marshall(Object unmarshalled, SOAPElement parent) {
        if (unmarshalled == null) return;
        Marshaller m = null;
        try {
            m = JAXB_CONTEXT.createMarshaller();
        } catch (Exception e) {
            logger.error(e.toString(),e);
            throw CXFUtility.getFault(e);
        }
        try {
            m.marshal(unmarshalled, parent);
        } catch (JAXBException e) {
            logger.error("Problem serializing params", e);
            throw CXFUtility.getFault(e);
        }
    }

}
