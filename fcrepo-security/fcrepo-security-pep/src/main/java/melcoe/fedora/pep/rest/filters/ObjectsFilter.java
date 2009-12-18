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

package melcoe.fedora.pep.rest.filters;

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

import melcoe.fedora.pep.PEPException;
import melcoe.fedora.pep.rest.objectshandlers.Handlers;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.ctx.RequestCtx;

import fedora.common.Constants;

/**
 * Handles the get operations.
 * 
 * @author nish.naidoo@gmail.com
 */
public class ObjectsFilter
        extends AbstractFilter {

    private static Logger log = Logger.getLogger(ObjectsFilter.class.getName());

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
     * melcoe.fedora.pep.rest.filters.RESTFilter#handleRequest(javax.servlet
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
     * melcoe.fedora.pep.rest.filters.RESTFilter#handleResponse(javax.servlet
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

    public RESTFilter getObjectsHandler(HttpServletRequest request)
            throws ServletException {
        String uri = request.getRequestURI();
        String path = request.getPathInfo();
        // need to handle this special case due to the way the RestServlet is mapped.
        if (uri.endsWith("nextPID")) {
            path = "/nextPID";
        } else if (path == null) {
            path = "";
        }

        if (log.isDebugEnabled()) {
            log.debug("objectsHandler path: " + path);
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

        if (log.isDebugEnabled()) {
            log.debug("objectsHandler method: " + method);
        }

        String[] parts = path.split("/");
        if (log.isDebugEnabled()) {
            for (String p : parts) {
                log.debug("objectsHandler part: " + p);
            }
        }

        if (parts.length < 1) {
            log.info("Not enough components on the URI.");
            throw new ServletException("Not enough components on the URI.");
        }

        String handlerName = null;
        // ascertain the correct handler based on uri pattern.
        if (parts.length == 1) {
            if (request.getParameterMap().containsKey("sessionToken")) {
                handlerName = Handlers.RESUMEFINDOBJECTS;
            } else if (request.getParameterMap().containsKey("terms")
                    || request.getParameterMap().containsKey("query")) {
                handlerName = Handlers.FINDOBJECTS;
            }
        } else if (parts.length == 2
                && (isPID(parts[1]) || "new".equals(parts[1]))) {
            if ("GET".equals(method)) {
                handlerName = Handlers.GETOBJECTPROFILE;
            } else if ("PUT".equals(method)) {
                handlerName = Handlers.MODIFYOBJECT;
            } else if ("DELETE".equals(method)) {
                handlerName = Handlers.PURGEOBJECT;
            } else if ("POST".equals(method) && "new".equals(parts[1])) {
                handlerName = Handlers.INGEST;
            }
        } else if (parts.length == 2 && parts[1].equals("nextPID")) {
            handlerName = Handlers.GETNEXTPID;
        } else if (parts.length == 3 && isPID(parts[1]) && "GET".equals(method)) {
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
            }
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
        } else if (parts.length == 5 && isPID(parts[1])
                && "datastreams".equals(parts[2]) && isDatastream(parts[3])
                && "content".equals(parts[4])) {
            handlerName = Handlers.GETDATASTREAMDISSEMINATION;
        }

        if (log.isDebugEnabled()) {
            log.debug("activating handler: " + handlerName);
        }

        return objectsHandlers.get(handlerName);
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
                        if (log.isDebugEnabled()) {
                            log.debug("objects handler added to map: " + opn
                                    + "/" + cls);
                        }
                    } catch (ClassNotFoundException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("filterClass not found for: " + cls);
                        }
                    } catch (InstantiationException ie) {
                        log.error("Could not instantiate filter: " + cls);
                        throw new ServletException(ie.getMessage(), ie);
                    } catch (IllegalAccessException iae) {
                        log.error("Could not instantiate filter: " + cls);
                        throw new ServletException(iae.getMessage(), iae);
                    }
                }
            }
        } catch (Exception e) {
            log.fatal("Failed to initialse the PEP for REST");
            log.fatal(e.getMessage(), e);
            throw new ServletException(e.getMessage(), e);
        }
    }
}
