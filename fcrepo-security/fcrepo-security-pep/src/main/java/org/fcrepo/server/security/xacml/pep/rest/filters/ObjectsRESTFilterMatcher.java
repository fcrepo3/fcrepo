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

import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.pep.rest.objectshandlers.Handlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Formerly ObjectsFilter, this class has been reduced to mapping requests to named RESTFilters.
 * Named filters are mapped to classes in the handlers-objects portion of the config-melcoe-pep.xml file.
 *
 * @author nish.naidoo@gmail.com, count0@email.unc.edu
 */
public class ObjectsRESTFilterMatcher {

    private static final Logger logger =
            LoggerFactory.getLogger(ObjectsRESTFilterMatcher.class);

    private final Map<String, RESTFilter> m_objectsHandlers;

    private final NoopFilter m_noop;

    /**
     * Default constructor.
     *
     * @throws PEPException
     */
    public ObjectsRESTFilterMatcher(Map<String,RESTFilter> objectsHandlers, NoopFilter noop)
            throws PEPException {
        super();
        m_objectsHandlers = objectsHandlers;
        m_noop = noop;
    }

    public RESTFilter getObjectsHandler(HttpServletRequest request)
            throws ServletException {
        String uri = request.getRequestURI();
        String path = request.getPathInfo();


        // need to handle this special case due to the way the RestServlet is mapped
        // directly to /objects/nextPID and /objects/nextPID.xml
        if (uri.endsWith("/nextPID")) {
            path = "/nextPID";
        } else if (path == null) {
            path = "";
        }

        logger.debug("objectsHandler path: {}", path);

        // The method override header. Takes precedence over the HTTP method
        String method = request.getHeader("X-HTTP-Method-Override");
        if (method == null || method.isEmpty()) {
            method = request.getMethod();
        }

        if (method == null) {
            throw new ServletException("Request Method was NULL");
        }

        method = method.toUpperCase();

        logger.debug("objectsHandler method: {}", method);

        String[] parts = path.split("/");
        if (logger.isDebugEnabled()) {
            for (String p : parts) {
                logger.debug("objectsHandler part: {}", p);
            }
        }

        if (parts.length < 1) {
            logger.info("Not enough components on the URI.");
            throw new ServletException("Not enough components on the URI.");
        }

        if (parts.length == 2 && "application.wadl".equals(parts[1])) {
            return m_noop;
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


        RESTFilter handler = m_objectsHandlers.get(handlerName);
        if (handler != null) {
            logger.debug("activating handler: {}", handlerName);
            return handler;
        } else {
            // there must always be a handler
            throw new ServletException("No REST handler defined for method " + method + "(handler name: " + handlerName + ") path=" + path);
        }
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
}
