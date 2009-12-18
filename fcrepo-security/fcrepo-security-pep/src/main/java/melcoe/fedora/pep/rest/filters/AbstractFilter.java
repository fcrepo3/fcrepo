/*
 * File: AbstractFilter.java
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import melcoe.fedora.pep.ContextHandler;
import melcoe.fedora.pep.ContextHandlerImpl;
import melcoe.fedora.pep.PEPException;

import org.apache.log4j.Logger;

import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;

import fedora.common.Constants;

/**
 * This is the AbstractFilter class which provides generic functionality for all
 * REST filters. All REST filters should extend this class.
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public abstract class AbstractFilter
        implements RESTFilter {

    private static Logger log =
            Logger.getLogger(AbstractFilter.class.getName());

    private static ContextHandler contextHandlerImpl;

    protected static final String XACML_RESOURCE_ID =
            "urn:oasis:names:tc:xacml:1.0:resource:resource-id";

    protected static final String SUBJECT_ID =
            "urn:oasis:names:tc:xacml:1.0:subject:subject-id";

    protected static final String FEDORA_ROLE =
            "urn:fedora:names:fedora:2.1:subject:role";

    /**
     * Default constructor obtains an instance of the Context Handler.
     * 
     * @throws PEPException
     */
    public AbstractFilter()
            throws PEPException {
        contextHandlerImpl = ContextHandlerImpl.getInstance();
    }

    /**
     * @return the ContextHandler instance
     */
    public ContextHandler getContextHandler() {
        return contextHandlerImpl;
    }

    /**
     * Creates a list of Subjects from a servlet request. If no subject is found
     * a subject called 'anonymous' is created.
     * 
     * @param request
     *        the servlet request
     * @return a list of Subjects
     * @throws ServletException
     */
    protected List<Map<URI, List<AttributeValue>>> getSubjects(HttpServletRequest request)
            throws ServletException {
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> reqAttr =
                (Map<String, Set<String>>) request
                        .getAttribute("FEDORA_AUX_SUBJECT_ATTRIBUTES");

        Set<String> fedoraRoles = null;
        if (reqAttr != null) {
            fedoraRoles = reqAttr.get("fedoraRole");
        }

        String[] fedoraRole = null;
        if (fedoraRoles != null && fedoraRoles.size() > 0) {
            fedoraRole = fedoraRoles.toArray(new String[fedoraRoles.size()]);
        }

        List<Map<URI, List<AttributeValue>>> subjects =
                new ArrayList<Map<URI, List<AttributeValue>>>();
        String user = request.getRemoteUser();

        if (user == null || "".equals(user)) {
            user = "anonymous";
        }

        // setup the id and value for the requesting subject
        try {
            Map<URI, List<AttributeValue>> subAttr =
                    new HashMap<URI, List<AttributeValue>>();
            List<AttributeValue> attrList = new ArrayList<AttributeValue>();
            attrList.add(new StringAttribute(user));
            subAttr.put(Constants.SUBJECT.LOGIN_ID.getURI(), attrList);
            if (fedoraRole != null && fedoraRole.length > 0) {
                attrList = new ArrayList<AttributeValue>();
                for (String f : fedoraRole) {
                    attrList.add(new StringAttribute(f));
                }
                subAttr.put(new URI(FEDORA_ROLE), attrList);
            }
            subjects.add(subAttr);

            subAttr = new HashMap<URI, List<AttributeValue>>();
            attrList = new ArrayList<AttributeValue>();
            attrList.add(new StringAttribute(user));
            subAttr.put(Constants.SUBJECT.USER_REPRESENTED.getURI(), attrList);
            if (fedoraRole != null && fedoraRole.length > 0) {
                attrList = new ArrayList<AttributeValue>();
                for (String f : fedoraRole) {
                    attrList.add(new StringAttribute(f));
                }
                subAttr.put(new URI(FEDORA_ROLE), attrList);
            }
            subjects.add(subAttr);

            subAttr = new HashMap<URI, List<AttributeValue>>();
            attrList = new ArrayList<AttributeValue>();
            attrList.add(new StringAttribute(user));
            subAttr.put(new URI(SUBJECT_ID), attrList);
            if (fedoraRole != null && fedoraRole.length > 0) {
                attrList = new ArrayList<AttributeValue>();
                for (String f : fedoraRole) {
                    attrList.add(new StringAttribute(f));
                }
                subAttr.put(new URI(FEDORA_ROLE), attrList);
            }
            subjects.add(subAttr);
        } catch (URISyntaxException use) {
            log.error(use.getMessage(), use);
            throw new ServletException(use);
        }

        return subjects;
    }

    /**
     * Returns a list of environment attributes.
     * 
     * @param request
     *        the servlet request from which to obtain the attributes
     * @return a list of environment attributes
     */
    protected Map<URI, AttributeValue> getEnvironment(HttpServletRequest request) {
        Map<URI, AttributeValue> envAttr = new HashMap<URI, AttributeValue>();
        String ip = request.getRemoteAddr();

        if (ip != null && !"".equals(ip)) {
            envAttr.put(Constants.HTTP_REQUEST.CLIENT_IP_ADDRESS.getURI(),
                        new StringAttribute(ip));
        }

        return envAttr;
    }

    /**
     * Function to determine whether a parameter is a PID.
     * 
     * @param item
     *        the uri parameter
     */
    protected boolean isPID(String item) {
        if (item == null) {
            return false;
        }

        // Currently, I am assuming that if it has a ':' in it, it's a
        // PID.
        return item.indexOf(':') > 0;
    }

    /**
     * Function to determine whether a parameter is a datastream or a
     * dissemination.
     * 
     * @param item
     *        the uri parameter
     */
    protected boolean isDatastream(String item) {
        if (item == null) {
            return false;
        }

        // Currently, I am assuming that if it has a ':' in it, it's a
        // dissemination. Otherwise it is a datastream.
        return item.indexOf(':') == -1;
    }

    /**
     * Checks whether a parameter fits the date pattern.
     * 
     * @param item
     *        the date
     * @return returns true if the string is a date or else false
     */
    protected boolean isDate(String item) {
        if (item == null) {
            return false;
        }

        if (item.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z$")) {
            return true;
        }

        if (item.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$")) {
            return true;
        }

        if (item.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            return true;
        }

        return false;
    }
}
