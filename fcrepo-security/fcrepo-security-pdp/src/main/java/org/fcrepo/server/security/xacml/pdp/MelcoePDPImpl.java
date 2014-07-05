/*
 * File: MelcoePDPImpl.java
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

package org.fcrepo.server.security.xacml.pdp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.fcrepo.server.security.RequestCtx;
import org.fcrepo.server.security.impl.BasicEvaluationCtx;
import org.fcrepo.server.security.impl.BasicRequestCtx;
import org.jboss.security.xacml.sunxacml.Indenter;
import org.jboss.security.xacml.sunxacml.PDP;
import org.jboss.security.xacml.sunxacml.PDPConfig;
import org.jboss.security.xacml.sunxacml.ParsingException;
import org.jboss.security.xacml.sunxacml.ctx.ResponseCtx;
import org.jboss.security.xacml.sunxacml.ctx.Result;
import org.jboss.security.xacml.sunxacml.finder.AttributeFinder;

/**
 * This is an implementation of the MelcoePDP interface. It provides for the
 * evaluation of requests. It uses
 *
 * @author nishen@melcoe.mq.edu.au
 */
public class MelcoePDPImpl
        implements MelcoePDP {

    private static final Logger logger =
            LoggerFactory.getLogger(MelcoePDPImpl.class);

    private final PDP m_pdp;

    private final AttributeFinder m_finder;

    public MelcoePDPImpl(PDPConfig pdpConfig)
            throws MelcoePDPException {
        m_pdp = new PDP(pdpConfig);
        m_finder = pdpConfig.getAttributeFinder();
        logger.info("PDP Instantiated and initialised!");
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.MelcoePDP#evaluate(java.lang.String)
     */
    @Override
    public String evaluate(String request) throws EvaluationException {
        logger.debug("evaluating request: {}", request);

        RequestCtx req = null;
        ByteArrayInputStream is = new ByteArrayInputStream(request.getBytes());

        try {
            req = BasicRequestCtx.getInstance(is);
        } catch (ParsingException pe) {
            logger.error("Error parsing request:\n" + request, pe);
            throw new EvaluationException("Error parsing request:\n" + request);
        }

        ResponseCtx res = evaluate(req);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        res.encode(os, new Indenter());

        if (logger.isDebugEnabled()) {
            logger.debug("response is: {}", os.toString());
        }
        return os.toString();
    }

    @Override
    public ResponseCtx evaluate(RequestCtx request) throws EvaluationException {
        try {
            BasicEvaluationCtx evalCtx = new BasicEvaluationCtx(request, m_finder);
            // not necessary with local EvaluationCtx impl
            /**for (Object obj:req.getResourceAsList()) {
               Attribute att = (Attribute)obj;
               if (att.getId().equals(Constants.XACML1_RESOURCE.ID.attributeId)){
                   evalCtx.setResourceId(att.getValue());
               }
            }**/
            return m_pdp.evaluate(evalCtx);
        } catch (ParsingException pe) {
            logger.error("Error parsing request:\n" + request, pe);
            throw new EvaluationException("Error parsing request:\n" + request);
        }
    }
    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.MelcoePDP#evaluateBatch(java.lang.String[])
     */
    @Override
    public String evaluateBatch(String[] requests) throws EvaluationException {
        logger.debug("evaluating string request batch");

        RequestCtx[] requestCtxs = new RequestCtx[requests.length];
        for (int i=0; i< requests.length; i++) {
            String request = requests[i];
            ByteArrayInputStream is = new ByteArrayInputStream(request.getBytes());

            try {
                requestCtxs[i] =  BasicRequestCtx.getInstance(is);
            } catch (ParsingException pe) {
                logger.error("Error parsing request:\n" + request, pe);
                throw new EvaluationException("Error parsing request:\n" + request);
            }
        }

        ResponseCtx combinedResponse = evaluateBatch(requestCtxs);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        combinedResponse.encode(os, new Indenter());

        return os.toString();
    }

    @Override
    public ResponseCtx evaluateBatch(RequestCtx[] requests) throws EvaluationException {
        logger.debug("evaluating request batch");
        Set<Result> results = new HashSet<Result>();
        for (RequestCtx request: requests) {
            ResponseCtx response = evaluate(request);
            @SuppressWarnings("unchecked")
            Set<Result> r = response.getResults();
            results.addAll(r);
        }
        return new ResponseCtx(results);
    }
}
