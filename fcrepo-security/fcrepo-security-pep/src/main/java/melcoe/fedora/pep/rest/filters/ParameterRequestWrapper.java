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

package melcoe.fedora.pep.rest.filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import fedora.server.security.servletfilters.ExtendedHttpServletRequestWrapper;

/**
 * Request wrapper that forces the addition of specified parameters.
 * 
 * @author nishen@melcoe.mq.edu.au
 */

public class ParameterRequestWrapper
        extends ExtendedHttpServletRequestWrapper {

    private static Logger log =
            Logger.getLogger(ParameterRequestWrapper.class.getName());

    private Map<String, String[]> params = null;

    private List<String> localParams = null;

    private String[] format = null;

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
        } else if (request.getRequestURI() != null
                && request.getRequestURI().endsWith("/risearch")) {
            if (log.isDebugEnabled()) {
                log.debug("Entered format check");
            }

            format = params.get("format");

            if (log.isDebugEnabled()) {
                if (format != null) {
                    for (String f : format) {
                        log.debug("Format: " + f);
                    }
                }
            }

            if (format != null && !Arrays.asList(format).contains("RDF/XML")) {
                if (log.isDebugEnabled()) {
                    log.debug("Setting format to: RDF/XML");
                }

                params.put("format", new String[] {"RDF/XML"});
            }
        }
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
    public Map<?, ?> getParameterMap() {
        return Collections.unmodifiableMap(params);
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.ServletRequestWrapper#getParameterNames()
     */
    @Override
    public Enumeration<?> getParameterNames() {
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
