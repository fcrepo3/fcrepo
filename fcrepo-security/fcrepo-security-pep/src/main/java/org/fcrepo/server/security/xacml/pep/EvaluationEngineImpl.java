/*
 * File: EvaluationEngineImpl.java
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

package org.fcrepo.server.security.xacml.pep;

import java.util.HashSet;
import java.util.Set;

import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.fcrepo.server.security.xacml.util.ContextUtil;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class EvaluationEngineImpl
        implements EvaluationEngine {

    private static final Logger logger =
            LoggerFactory.getLogger(EvaluationEngineImpl.class);

    private final ContextUtil contextUtil = new ContextUtil();

    private PDPClient client = null;

    private ResponseCache responseCache = null;

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.EvaluationEngine#evaluate(com.sun.xacml.ctx.RequestCtx)
     */
    public ResponseCtx evaluate(RequestCtx reqCtx) throws PEPException {
        if (logger.isDebugEnabled()) {
            logger.debug("evaluating RequestCtx request");
        }

        String request = contextUtil.makeRequestCtx(reqCtx);
        String response = evaluate(request);
        ResponseCtx resCtx;
        try {
            resCtx = contextUtil.makeResponseCtx(response);
        } catch (MelcoeXacmlException e) {
            throw new PEPException(e);
        }
        return resCtx;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.EvaluationEngine#evaluate(java.lang.String)
     */
    public String evaluate(String request) throws PEPException {
        if (logger.isDebugEnabled()) {
            logger.debug("evaluating String request");
        }

        String[] requests = new String[] {request};
        return evaluate(requests);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.EvaluationEngine#evaluate(java.lang.String[])
     */
    public String evaluate(String[] requests) throws PEPException {
        if (logger.isDebugEnabled()) {
            logger.debug("evaluating array of String requests");
        }

        long a, b;

        Set<Result> finalResults = new HashSet<Result>();

        for (String r : requests) {
            String response = null;

            a = System.currentTimeMillis();

            if (responseCache != null) {
                response = responseCache.getCacheItem(r);
            }

            if (response == null) {
                logger.debug("No item found in cache. Sending to PDP for evaluation.");

                response = client.evaluate(r);

                // Add this new result to the cache if caching is enabled
                if (logger.isDebugEnabled()) {
                    logger.debug("Adding PDP evaluation results to cache");
                }
                if (responseCache != null) {
                    responseCache.addCacheItem(r, response);
                }
            } else if (logger.isDebugEnabled()) {
                logger.debug("Item found in cache");

            }

            b = System.currentTimeMillis();
            if (logger.isDebugEnabled()) {
                logger.debug("Time taken for XACML Evaluation: " + (b - a) + "ms");
            }

            ResponseCtx resCtx;
            try {
                resCtx = contextUtil.makeResponseCtx(response);
            } catch (MelcoeXacmlException e) {
                throw new PEPException(e);
            }

            @SuppressWarnings("unchecked")
            Set<Result> results = resCtx.getResults();

            finalResults.addAll(results);
        }

        ResponseCtx resultCtx = new ResponseCtx(finalResults);

        return contextUtil.makeResponseCtx(resultCtx);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.EvaluationEngine#getClient()
     */
    public PDPClient getClient() {
        return client;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.EvaluationEngine#setClient(org.fcrepo.server.security.xacml.pep.PEPClient)
     */
    public void setClient(PDPClient client) {
        this.client = client;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.EvaluationEngine#getResponseCache()
     */
    public ResponseCache getResponseCache() {
        return responseCache;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.EvaluationEngine#setResponseCache(org.fcrepo.server.security.xacml.pep
     * .ResponseCache)
     */
    public void setResponseCache(ResponseCache responseCache) {
        this.responseCache = responseCache;
    }
}
