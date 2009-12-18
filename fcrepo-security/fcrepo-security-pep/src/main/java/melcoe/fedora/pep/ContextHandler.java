/*
 * File: ContextHandler.java
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

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;

/**
 * This interface represents the bridge between the PEP and the PDP. It is
 * responsible for building requests and passing them to the PDP. It then
 * receives the response and passes it to the PEP.
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public interface ContextHandler {

    /**
     * Creates a new Request.
     * 
     * @param subjects
     *        a list of Map<URI, AttributeValue> containing the attributes for a
     *        set of subjects.
     * @param actions
     *        the URI of the requested Action.
     * @param resources
     *        a Map<URI, AttributeValue> containing the attributes for a
     *        resource.
     * @param environment
     *        Map<URI, AttributeValue> containing the attributes for the
     *        environment.
     * @return a request context for a PDP to handle.
     * @throws PEPException
     */
    public RequestCtx buildRequest(List<Map<URI, List<AttributeValue>>> subjects,
                                   Map<URI, AttributeValue> actions,
                                   Map<URI, AttributeValue> resources,
                                   Map<URI, AttributeValue> environment)
            throws PEPException;

    /**
     * @param reqCtx
     *        an XACML request context for resolution.
     * @return an XACML response context based on the evaluation of the request
     *         context.
     * @throws PEPException
     */
    public ResponseCtx evaluate(RequestCtx reqCtx) throws PEPException;

    /**
     * @param req
     *        an XACML request as a string for resolution.
     * @return an XACML response as a string based on the evaluation of the
     *         request context.
     * @throws PEPException
     */
    public String evaluate(String req) throws PEPException;

    /**
     * @param requests
     *        an array of XACML requests as strings for resolution.
     * @return an XACML response as a string based on the evaluation of the
     *         request context.
     * @throws PEPException
     */
    public String evaluateBatch(String[] requests) throws PEPException;

    /**
     * @return a reference to the response cache.
     */
    public ResponseCache getResponseCache();
}
