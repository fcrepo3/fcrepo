/*
 * File: ObjectsFilter.java
 *
 * Copyright 2009 2DC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.fcrepo.server.security.xacml.pep.rest.filters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.sun.xacml.ctx.RequestCtx;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;

import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.pep.rest.objectshandlers.Handlers;

/**
 * Handles the get operations.
 *
 * @author nish.naidoo@gmail.com
 */
public class ObjectsFilter
        extends AbstractFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(ObjectsFilter.class);

    private Map<String, RESTFilter> objectsHandlers = null;

    /**
     * Default constructor.
     *
     * @throws PEPException
     */
    public ObjectsFilter()
            throws PEPException {
        super();
        try {
            loadObjectsHandlers();
        } catch (ServletException se) {
            throw new PEPException(se);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.rest.filters.RESTFilter#handleRequest(javax.servlet
     * .http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public RequestCtx handleRequest(HttpServletRequest request,
                                    HttpServletResponse response)
            throws IOException, ServletException {
        RESTFilter objectsHandler = getObjectsHandler(request);

        if (objectsHandler == null) {
            return null;
        }

        return objectsHandler.handleRequest(request, response);
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.rest.filters.RESTFilter#handleResponse(javax.servlet
     * .http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public RequestCtx handleResponse(HttpServletRequest request,
                                     HttpServletResponse response)
            throws IOException, ServletException {
        RESTFilter objectsHandler = getObjectsHandler(request);

        if (objectsHandler == null) {
            return null;
        }

        return objectsHandler.handleResponse(request, response);
    }

    protected RESTFilter getObjectsHandler(HttpServletRequest request)
            throws ServletException {

        // IMPORTANT:
        // Do not return null unless you are sure that the endpoint
        // requires no authorisation.  The only current case for this
        // is returning the WADL.  All other REST endpoints MUST have handlers.


        String uri = request.getRequestURI();
        String path = request.getPathInfo();


        // need to handle this special case due to the way the RestServlet is mapped
        // directly to /objects/nextPID and /objects/nextPID.xml
        if (uri.endsWith("/nextPID")) {
            path = "/nextPID";
        } else if (path == null) {
            path = "";
        }

        if (logger.isDebugEnabled()) {
            logger.debug("objectsHandler path: " + path);
        }

        // The method override header. Takes precedence over the HTTP method
        String method = request.getHeader("X-HTTP-Method-Override");
        if (method == null || "".equals(method)) {
            method = request.getMethod();
        }

        if (method == null) {
            throw new ServletException("Request Method was NULL");
        }

        method = method.toUpperCase();

        if (logger.isDebugEnabled()) {
            logger.debug("objectsHandler method: " + method);
        }

        String[] parts = path.split("/");
        if (logger.isDebugEnabled()) {
            for (String p : parts) {
                logger.debug("objectsHandler part: " + p);
            }
        }

        if (parts.length < 1) {
            logger.info("Not enough components on the URI.");
            throw new ServletException("Not enough components on the URI.");
        }

        // IMPORTANT:
        // this is the only case we return null.  No authz on the WADL.
        // Every other endpoint MUST have a handler.
        if (parts.length == 2 && "application.wadl".equals(parts[1])) {
            return null;
        }

        // FIXME: tests below could do with tidying up
        // particularly wrt checking for valid pid and ds IDs.
        // if the tests are done in the correct order this should not be necessary
        // (and the REST API mappings/annotations do not use this form of syntax checking,
        // so they will allow pass-through of invalid PIDs and DSIDs, which will be checked later in the code -
        // the stuff below will actually result in not finding a handler if a bogus PID/DSID is found)

        String handlerName = "no-handler-name-determined-from-request-path";
        // ascertain the correct handler based on uri pattern.

        // - /objects
        if (parts.length < 2) {
            if ("GET".equals(method)) {
                if (request.getParameterMap().containsKey("sessionToken")) {
                    handlerName = Handlers.RESUMEFINDOBJECTS;
                } else {
                    handlerName = Handlers.FINDOBJECTS;
                }
            } else if ("POST".equals(method)) {
                handlerName = Handlers.INGEST;
            }
            // - /objects/nextPID
        } else if (parts.length == 2 && parts[1].equals("nextPID")) {
            handlerName = Handlers.GETNEXTPID;
            // - /objects/[pid]
        } else if (parts.length == 2) {
            if ("GET".equals(method)) {
                handlerName = Handlers.GETOBJECTPROFILE;
            } else if ("PUT".equals(method)) {
                handlerName = Handlers.MODIFYOBJECT;
            } else if ("DELETE".equals(method)) {
                handlerName = Handlers.PURGEOBJECT;
            } else if ("POST".equals(method)) {
                handlerName = Handlers.INGEST;
            }
            // - /objects/[pid]/...  (except relationships - handled later)
        } else if (parts.length == 3 && isPID(parts[1]) && "GET".equals(method)  && !"relationships".equals(parts[2])) {
            if ("datastreams".equals(parts[2])) {
                handlerName = Handlers.LISTDATASTREAMS;
            } else if ("export".equals(parts[2])) {
                handlerName = Handlers.EXPORT;
            } else if ("methods".equals(parts[2])) {
                handlerName = Handlers.LISTMETHODS;
            } else if ("objectXML".equals(parts[2])) {
                handlerName = Handlers.GETOBJECTXML;
            } else if ("versions".equals(parts[2])) {
                handlerName = Handlers.GETOBJECTHISTORY;
            } else if ("validate".equals(parts[2])) {
                handlerName = Handlers.VALIDATE;
            }

            // - /objects/[pid]/datastreams/[dsid]
        } else if (parts.length == 4 && isPID(parts[1])
                && "datastreams".equals(parts[2]) && isDatastream(parts[3])) {
            if ("PUT".equals(method)
                    && request.getParameterMap().containsKey("dsState")) {
                handlerName = Handlers.SETDATASTREAMSTATE;
            } else if ("PUT".equals(method)
                    && request.getParameterMap().containsKey("versionable")) {
                handlerName = Handlers.SETDATASTREAMVERSIONABLE;
            } else if ("PUT".equals(method)) {
                handlerName = Handlers.MODIFYDATASTREAM;
            } else if ("POST".equals(method)) {
                handlerName = Handlers.ADDDATASTREAM;
            } else if ("GET".equals(method)) {
                handlerName = Handlers.GETDATASTREAM;
            } else if ("DELETE".equals(method)) {
                handlerName = Handlers.PURGEDATASTREAM;
            }
            // - /objects/[pid]/datastreams/[dsid]/content
        } else if (parts.length == 5 && isPID(parts[1])
                && "datastreams".equals(parts[2]) && isDatastream(parts[3])
                && "content".equals(parts[4])) {
            handlerName = Handlers.GETDATASTREAMDISSEMINATION;

            // - /objects/[pid]/datastreams/[dsid]/history
        } else if (parts.length == 5 && isPID(parts[1]) && "datastreams".equals(parts[2]) && isDatastream(parts[3]) && "history".equals(parts[4])) {
            handlerName = Handlers.GETDATASTREAMHISTORY;

            // - /objects/[pid]/methods/[sdef]/method
        } else if (parts.length == 5 && isPID(parts[1])
                && "methods".equals(parts[2]) && isPID(parts[3]) && "GET".equals(method)) {
            handlerName = Handlers.GETDISSEMINATION;

            // - /objects/[pid]/methods/[sdef]
        } else if (parts.length == 4 && isPID(parts[1]) && "GET".equals(method) && "methods".equals(parts[2]) && isPID(parts[3])) {
            handlerName = Handlers.LISTMETHODS;

            // - /objects/[pid/relationships[/...]
        } else if (isPID(parts[1]) && "relationships".equals(parts[2])) {
            // add
            if ("POST".equals(method)) {
                handlerName = Handlers.ADDRELATIONSHIP;
                // get
            } else if ("GET".equals(method)) {
                handlerName = Handlers.GETRELATIONSHIPS;
                // purge
            } else if ("DELETE".equals(method)) {
                handlerName = Handlers.PURGERELATIONSHIP;
            }
        }


        RESTFilter handler = objectsHandlers.get(handlerName);
        if (handler != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("activating handler: " + handlerName);
            }
            return handler;
        } else {
            // there must always be a handler
            throw new ServletException("No REST handler defined for method " + method + "(handler name: " + handlerName + ") path=" + path);
        }

    }

    private void loadObjectsHandlers() throws ServletException {
        objectsHandlers = new HashMap<String, RESTFilter>();

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

            Node node = doc.getElementsByTagName("handlers-objects").item(0);
            NodeList nodes = node.getChildNodes();
            for (int x = 0; x < nodes.getLength(); x++) {
                Node n = nodes.item(x);
                if (n.getNodeType() == Node.ELEMENT_NODE
                        && "handler".equals(n.getNodeName())) {
                    String opn =
                            n.getAttributes().getNamedItem("operation")
                                    .getNodeValue();
                    String cls =
                            n.getAttributes().getNamedItem("class")
                                    .getNodeValue();

                    if (opn == null || "".equals(opn)) {
                        throw new PEPException("Cannot have a missing or empty operation attribute");
                    }

                    if (cls == null || "".equals(cls)) {
                        throw new PEPException("Cannot have a missing or empty class attribute");
                    }

                    try {
                        Class<?> filterClass = Class.forName(cls);
                        RESTFilter filter =
                                (RESTFilter) filterClass.newInstance();
                        objectsHandlers.put(opn, filter);
                        if (logger.isDebugEnabled()) {
                            logger.debug("objects handler added to map: " + opn
                                    + "/" + cls);
                        }
                    } catch (ClassNotFoundException e) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("filterClass not found for: " + cls);
                        }
                    } catch (InstantiationException ie) {
                        logger.error("Could not instantiate filter: " + cls);
                        throw new ServletException(ie.getMessage(), ie);
                    } catch (IllegalAccessException iae) {
                        logger.error("Could not instantiate filter: " + cls);
                        throw new ServletException(iae.getMessage(), iae);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to initialse the PEP for REST", e);
            throw new ServletException(e.getMessage(), e);
        }
    }
}
