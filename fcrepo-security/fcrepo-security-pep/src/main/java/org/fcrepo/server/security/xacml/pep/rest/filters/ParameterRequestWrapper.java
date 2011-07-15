/*
 * File: ParameterRequestWrapper.java
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

package org.fcrepo.server.security.xacml.pep.rest.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.server.security.servletfilters.ExtendedHttpServletRequestWrapper;

/**
 * Request wrapper that forces the addition of specified parameters.
 *
 * @author nishen@melcoe.mq.edu.au
 */

public class ParameterRequestWrapper
        extends ExtendedHttpServletRequestWrapper {

    private static final Logger logger =
            LoggerFactory.getLogger(ParameterRequestWrapper.class);

    private Map<String, String[]> params = null;

    private List<String> localParams = null;

    private final String[] format = null;

    /**
     * Default constructor that duplicates a request.
     *
     * @param request
     *        the request to duplicate
     */
    @SuppressWarnings("unchecked")
    public ParameterRequestWrapper(HttpServletRequest request)
            throws Exception {
        super(request);
        params = new HashMap<String, String[]>(request.getParameterMap());
        localParams = new ArrayList<String>();

        if (request.getRequestURI() != null
                && request.getRequestURI().endsWith("/search")) {
            if (params.size() > 0 && params.get("pid") == null) {
                params.put("pid", new String[] {"true"});
                localParams.add("pid");
            }
        } else if (request.getRequestURI() != null
                && request.getRequestURI().endsWith("/objects")) {
            if (params.size() > 0 && params.get("pid") == null) {
                params.put("pid", new String[] {"true"});
                localParams.add("pid");
            }
        }

        // this was handling format parameter rewriting to force RDF/XML output, which is then
        // parsed in RISearchFilter, which subsequently transforms to the actual format requested
        // RISearchFilter is currently disabled, so this removed as it also prevents "count" reponses being processed correctly.
        // See: FCREPO-600

        /*else if (request.getRequestURI() != null
                && request.getRequestURI().endsWith("/risearch")) {
            if (logger.isDebugEnabled()) {
                logger.debug("Entered format check");
            }

            format = params.get("format");

            if (logger.isDebugEnabled()) {
                if (format != null) {
                    for (String f : format) {
                        logger.debug("Format: " + f);
                    }
                }
            }

            if (format != null && !Arrays.asList(format).contains("RDF/XML")) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Setting format to: RDF/XML");
                }

                params.put("format", new String[] {"RDF/XML"});
            }
        } */
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.ServletRequestWrapper#getParameter(java.lang.String)
     */
    @Override
    public String getParameter(String name) {
        String[] param = params.get(name);

        if (param == null || param.length == 0) {
            return null;
        }

        return param[0];
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.ServletRequestWrapper#getParameterMap()
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        return Collections.unmodifiableMap(params);
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.ServletRequestWrapper#getParameterNames()
     */
    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(params.keySet());
    }

    /*
     * (non-Javadoc)
     * @see
     * javax.servlet.ServletRequestWrapper#getParameterValues(java.lang.String)
     */
    @Override
    public String[] getParameterValues(String name) {
        return params.get(name);
    }

    /**
     * @return the format
     */
    public String[] getFormat() {
        return format;
    }
}
