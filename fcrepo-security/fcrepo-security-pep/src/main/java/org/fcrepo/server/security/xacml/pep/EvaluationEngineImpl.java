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

import org.fcrepo.server.security.RequestCtx;
import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.fcrepo.server.security.xacml.util.ContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.security.xacml.sunxacml.ctx.ResponseCtx;
import org.jboss.security.xacml.sunxacml.ctx.Result;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class EvaluationEngineImpl
        implements EvaluationEngine {

    private static final Logger logger =
            LoggerFactory.getLogger(EvaluationEngineImpl.class);

    private ContextUtil m_contextUtil = null;

    private PDPClient client = null;

    private ResponseCache responseCache = null;

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.EvaluationEngine#evaluate(org.jboss.security.xacml.sunxacml.ctx.RequestCtx)
     */
    @Override
    public ResponseCtx evaluate(RequestCtx reqCtx) throws PEPException {
        logger.debug("evaluating RequestCtx request");

        return evaluate(new RequestCtx[]{reqCtx});
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.EvaluationEngine#evaluate(java.lang.String)
     */
    @Override
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
    @Override
    public String evaluate(String[] requests) throws PEPException {
        logger.debug("evaluating array of String requests");

        long a, b;

        Set<Result> finalResults = new HashSet<Result>();
        for (int i = 0; i < requests.length; i++) {
            String r = requests[i];
            if (r == null) continue;
            ResponseCtx resCtx = null;

            a = System.currentTimeMillis();

            if (responseCache != null) {
                resCtx = responseCache.getCacheItem(r);
            }

            if (resCtx == null) {
                logger.debug("No item found in cache. Sending to PDP for evaluation.");

                org.fcrepo.server.security.RequestCtx req = null;
                try {
                    req = m_contextUtil.makeRequestCtx(r);
                } catch (MelcoeXacmlException e) {
                    throw new PEPException(e);
                }
                resCtx = client.evaluate(req);

                // Add this new result to the cache if caching is enabled
                logger.debug("Adding PDP evaluation results to cache");
                if (responseCache != null) {
                    responseCache.addCacheItem(r, resCtx);
                }
            } else {
                logger.debug("Item found in cache");
            }

            b = System.currentTimeMillis();
            logger.debug("Time taken for XACML Evaluation: {}ms", (b - a));

            @SuppressWarnings("unchecked")
            Set<Result> results = resCtx.getResults();

            finalResults.addAll(results);
        }

        ResponseCtx resultCtx = new ResponseCtx(finalResults);

        return m_contextUtil.makeResponseCtx(resultCtx);
    }

    @Override
    public ResponseCtx evaluate(RequestCtx[] requests) throws PEPException {
        logger.debug("evaluating array of requests");

        long a, b;

        Set<Result> finalResults = new HashSet<Result>();

        for (RequestCtx r : requests) {
            if (r == null) continue;

            ResponseCtx resCtx = null;

            a = System.currentTimeMillis();

            String rKey = m_contextUtil.makeRequestCtx(r);
            if (responseCache != null) {
                resCtx = responseCache.getCacheItem(rKey);
            }

            if (resCtx == null) {
                logger.debug("No item found in cache. Sending to PDP for evaluation.");

                resCtx = client.evaluate(r);

                // Add this new result to the cache if caching is enabled
                logger.debug("Adding PDP evaluation results to cache");
                if (responseCache != null) {
                    responseCache.addCacheItem(rKey, resCtx);
                }
            } else {
                logger.debug("Item found in cache");
            }

            b = System.currentTimeMillis();
            logger.debug("Time taken for XACML Evaluation: {}ms", (b - a));

            @SuppressWarnings("unchecked")
            Set<Result> results = resCtx.getResults();

            finalResults.addAll(results);
        }

        ResponseCtx resultCtx = new ResponseCtx(finalResults);

        return (resultCtx);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.EvaluationEngine#getClient()
     */
    @Override
    public PDPClient getClient() {
        return client;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.EvaluationEngine#setClient(org.fcrepo.server.security.xacml.pep.PEPClient)
     */
    @Override
    public void setClient(PDPClient client) {
        this.client = client;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.EvaluationEngine#getResponseCache()
     */
    @Override
    public ResponseCache getResponseCache() {
        return responseCache;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.EvaluationEngine#setResponseCache(org.fcrepo.server.security.xacml.pep
     * .ResponseCache)
     */
    @Override
    public void setResponseCache(ResponseCache responseCache) {
        this.responseCache = responseCache;
    }

    public void setContextUtil(ContextUtil contextUtil) {
        m_contextUtil = contextUtil;
    }
}
