/*
 * File: ContextUtil.java
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

package org.fcrepo.server.security.xacml.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.fcrepo.common.Constants;
import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.Indenter;
import com.sun.xacml.ParsingException;
import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Subject;


/**
 * Utility class that provides various methods for creating/converting contexts.
 * This class can convert requests and responses from their string
 * representations to their object representations and vice versa as well as a
 * few utility methods for getting information from the contexts. It also
 * contains methods for constructing requests.
 *
 * @author nishen@melcoe.mq.edu.au
 */

public class ContextUtil {

    private static final Logger logger =
            LoggerFactory.getLogger(ContextUtil.class);

    private static final URI XACML_RESOURCE_ID =
            Constants.XACML1_RESOURCE.ID.getURI();

    private final Map<URI, URI> actionMap =
            new ConcurrentHashMap<URI, URI>();

    private final Map<String, String> actionValueMap =
            new ConcurrentHashMap<String, String>();

    public ContextUtil() {
        logger.info("created");
    }

    public void setActionMap(Map<String,String> actions) {
        for (String from: actions.keySet()){
            URI key;
            String to = actions.get(from);
            try {
                key = new URI(from);
                URI value = new URI(actions.get(from));
                actionMap.put(key, value);
            } catch (URISyntaxException e) {
                logger.warn("Mapping contained invalid URI: ["
                        + from + "] / [" + to + "]");
            }
        }
    }

    public void setActionValueMap(Map<String,String> values) {
        actionValueMap.putAll(values);
    }

    /**
     * Sets up the Subject section of the request. Takes a list of Maps of
     * URI/AttributeValue pairs. Each list element represents one subject which
     * contains a map of URI/AttributeValues.
     *
     * @return a Set of Subject instances for inclusion in a Request
     */
    public Set<Subject> setupSubjects(List<Map<URI, List<AttributeValue>>> subjs) {
        Set<Subject> subjects = new HashSet<Subject>();

        if (subjs == null || subjs.size() == 0) {
            subjects.add(new Subject(new HashSet<Attribute>()));
            return subjects;
        }

        // Go through each of the subjects
        for (Map<URI, List<AttributeValue>> s : subjs) {
            Set<Attribute> attributes = new HashSet<Attribute>();

            // Extract and create the attributes for this subject and add them
            // to the set
            for (URI uri : s.keySet()) {
                List<AttributeValue> attributeValues = s.get(uri);
                for (AttributeValue attributeValue : attributeValues) {
                    attributes.add(new Attribute(uri,
                                                 null,
                                                 null,
                                                 attributeValue));
                }
            }

            // Create a new subject and add the attributes for this subject
            subjects.add(new Subject(attributes));
        }

        return subjects;
    }

    /**
     * Creates a Resource specifying the resource-id, a required attribute.
     *
     * @return a Set of Attributes for inclusion in a Request
     */
    public Set<Attribute> setupResources(Map<URI, AttributeValue> res, RelationshipResolver relationshipResolver)
            throws MelcoeXacmlException {
        Set<Attribute> attributes = new HashSet<Attribute>();

        if (res == null || res.size() == 0) {
            return attributes;
        }

        try {
            String pid = null;
            AttributeValue pidAttr = res.get(XACML_RESOURCE_ID);
            if (pidAttr != null) {
                pid = pidAttr.encode();
                pid = relationshipResolver.buildRESTParentHierarchy(pid);

                String dsid = null;
                AttributeValue dsidAttr =
                        res.get(Constants.DATASTREAM.ID.getURI());
                if (dsidAttr != null) {
                    dsid = dsidAttr.encode();
                    if (!dsid.isEmpty()) {
                        pid += "/" + dsid;
                    }
                }

                res.put(XACML_RESOURCE_ID, new AnyURIAttribute(new URI(pid)));
            }
        } catch (Exception e) {
            logger.error("Error finding parents.", e);
            throw new MelcoeXacmlException("Error finding parents.", e);
        }

        for (URI uri : res.keySet()) {
            attributes.add(new Attribute(uri, null, null, res.get(uri)));
        }

        return attributes;
    }

    /**
     * Creates an Action specifying the action-id, an optional attribute.
     *
     * @return a Set of Attributes for inclusion in a Request
     */
    public Set<Attribute> setupAction(Map<URI, AttributeValue> a) {
        Set<Attribute> actions = new HashSet<Attribute>();

        if (a == null || a.size() == 0) {
            return actions;
        }

        Map<URI, AttributeValue> newActions =
                new HashMap<URI, AttributeValue>();
        for (URI uri : a.keySet()) {
            URI newUri = null;
            AttributeValue newValue = null;

            if (actionMap != null && actionMap.size() > 0) {
                newUri = actionMap.get(uri);
            }

            if (actionValueMap != null && actionValueMap.size() > 0) {
                String tmpValue = actionValueMap.get(a.get(uri).encode());
                if (tmpValue != null) {
                    newValue = new StringAttribute(tmpValue);
                }
            }

            newUri = newUri == null ? uri : newUri;
            newValue = newValue == null ? a.get(uri) : newValue;
            newActions.put(newUri, newValue);
        }

        for (URI uri : newActions.keySet()) {
            actions.add(new Attribute(uri, null, null, newActions.get(uri)));
        }

        return actions;
    }

    /**
     * Creates the Environment attributes.
     *
     * @return a Set of Attributes for inclusion in a Request
     */
    public Set<Attribute> setupEnvironment(Map<URI, AttributeValue> e) {
        Set<Attribute> environment = new HashSet<Attribute>();

        if (e == null || e.size() == 0) {
            return environment;
        }

        for (URI uri : e.keySet()) {
            environment.add(new Attribute(uri, null, null, e.get(uri)));
        }

        return environment;
    }

    /**
     * Constructs a RequestCtx object.
     *
     * @param subjects
     *        list of Subjects
     * @param actions
     *        list of Action attributes
     * @param resources
     *        list of resource Attributes
     * @param environment
     *        list of environment Attributes
     * @return the RequestCtx object
     * @throws MelcoeXacmlException
     */
    public RequestCtx buildRequest(List<Map<URI, List<AttributeValue>>> subjects,
                                   Map<URI, AttributeValue> actions,
                                   Map<URI, AttributeValue> resources,
                                   Map<URI, AttributeValue> environment,
                                   RelationshipResolver relationshipResolver)
            throws MelcoeXacmlException {
        if (logger.isDebugEnabled()) {
            logger.debug("Building request!");
        }

        RequestCtx request = null;

        // Create the new Request.
        // Note that the Environment must be specified using a valid Set, even
        // if that Set is empty
        try {
            request =
                    new RequestCtx(setupSubjects(subjects),
                                   setupResources(resources, relationshipResolver),
                                   setupAction(actions),
                                   setupEnvironment(environment));
        } catch (Exception e) {
            logger.error("Error creating request.", e);
            throw new MelcoeXacmlException("Error creating request", e);
        }

        return request;
    }

    /**
     * Converts a string based response to a ResponseCtx obejct.
     *
     * @param response
     *        the string response
     * @return the ResponseCtx object
     * @throws MelcoeXacmlException
     */
    public ResponseCtx makeResponseCtx(String response)
            throws MelcoeXacmlException {
        ResponseCtx resCtx = null;
        try {
            // sunxacml 1.2 bug. ResponseCtx.getInstance looks for
            // ResourceId and creates ResourceID
            String newResponse =
                    response.replaceAll("ResourceID", "ResourceId");
            ByteArrayInputStream is =
                    new ByteArrayInputStream(newResponse.getBytes());
            resCtx = ResponseCtx.getInstance(is);
        } catch (ParsingException pe) {
            throw new MelcoeXacmlException("Error parsing response.", pe);
        }
        return resCtx;
    }

    /**
     * Converts a string based request to a RequestCtx obejct.
     *
     * @param request
     *        the string request
     * @return the RequestCtx object
     * @throws MelcoeXacmlException
     */
    public RequestCtx makeRequestCtx(String request)
            throws MelcoeXacmlException {
        RequestCtx reqCtx = null;
        try {
            ByteArrayInputStream is =
                    new ByteArrayInputStream(request.getBytes());
            reqCtx = RequestCtx.getInstance(is);
        } catch (ParsingException pe) {
            throw new MelcoeXacmlException("Error parsing response.", pe);
        }
        return reqCtx;
    }

    /**
     * Converts a RequestCtx object to its string representation.
     *
     * @param reqCtx
     *        the RequestCtx object
     * @return the String representation of the RequestCtx object
     */
    public String makeRequestCtx(RequestCtx reqCtx) {
        ByteArrayOutputStream request = new ByteArrayOutputStream();
        reqCtx.encode(request, new Indenter());
        return new String(request.toByteArray());
    }

    /**
     * Converst a ResponseCtx object to its string representation.
     *
     * @param resCtx
     *        the ResponseCtx object
     * @return the String representation of the ResponseCtx object
     */
    public String makeResponseCtx(ResponseCtx resCtx) {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        resCtx.encode(response, new Indenter());
        return new String(response.toByteArray());
    }

    /**
     * Returns a map of resource-id, result based on an XACML response.
     *
     * @param resCtx
     *        the XACML response
     * @return the Map of resource-id and result
     */
    public Map<String, Result> makeResultMap(ResponseCtx resCtx) {
        @SuppressWarnings("unchecked")
        Iterator<Result> i = resCtx.getResults().iterator();

        Map<String, Result> resultMap = new HashMap<String, Result>();

        while (i.hasNext()) {
            Result r = i.next();
            resultMap.put(r.getResource(), r);
        }

        return resultMap;
    }
}