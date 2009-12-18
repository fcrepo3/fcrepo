/*
 * File: EvaluationEngine.java
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

package melcoe.fedora.pep;

import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public interface EvaluationEngine {

    /**
     * Evaluates an XACML request and returns an XACML response.
     * 
     * @param request
     *        an XACML request as a RequestCtx object
     * @return and XACML response as a ResponseCtx object
     * @throws PEPException
     */
    public ResponseCtx evaluate(RequestCtx reqCtx) throws PEPException;

    /**
     * Evaluates an XACML request and returns an XACML response.
     * 
     * @param request
     *        an XACML request as a String
     * @return and XACML response as a String
     * @throws PEPException
     */
    public String evaluate(String request) throws PEPException;

    /**
     * Evaluates a String array of XACML requests. The responses are combined
     * into a single response and returned as an XACML response.
     * 
     * @param requests
     *        a String array of XACML requests
     * @return an XACML response as a String containing results for each request
     * @throws PEPException
     */
    public String evaluate(String[] requests) throws PEPException;

    /**
     * @return the client
     */
    public PDPClient getClient();

    /**
     * @param client
     *        the client to set
     */
    public void setClient(PDPClient client);

    /**
     * @return the responseCache
     */
    public ResponseCache getResponseCache();

    /**
     * @param responseCache
     *        the responseCache to set
     */
    public void setResponseCache(ResponseCache responseCache);
}
