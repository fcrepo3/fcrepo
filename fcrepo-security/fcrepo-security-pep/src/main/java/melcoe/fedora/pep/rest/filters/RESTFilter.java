/*
 * File: RESTFilter.java
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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.xacml.ctx.RequestCtx;

/**
 * The interface for filters.
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public interface RESTFilter {

    /**
     * Handles the request path and returns a RequestCtx if necessary.
     * 
     * @param request
     *        the servlet request
     * @param response
     *        the servlet response
     * @return the RequestCtx if one is needed, or else null
     * @throws IOException
     * @throws ServletException
     */
    public RequestCtx handleRequest(HttpServletRequest request,
                                    HttpServletResponse response)
            throws IOException, ServletException;

    /**
     * Handles the response path and returns a RequestCtx if necessary.
     * 
     * @param request
     *        the servlet request
     * @param response
     *        the servlet response
     * @return the RequestCtx if one is needed, or else null
     * @throws IOException
     * @throws ServletException
     */
    public RequestCtx handleResponse(HttpServletRequest request,
                                     HttpServletResponse response)
            throws IOException, ServletException;
}
