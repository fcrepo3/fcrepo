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

package melcoe.xacml.test;

import melcoe.xacml.pdp.MelcoePDP;
import melcoe.xacml.pdp.MelcoePDPImpl;
import melcoe.xacml.util.ContextUtil;

import org.apache.log4j.Logger;

import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;

/**
 * Borrowed <b>heavily</b> from sunxacml samples SampleRequestBuilder.java
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public class ContextHandler {

    private static Logger log =
            Logger.getLogger(ContextHandler.class.getName());

    private static final ContextHandler contextHandler;

    private static final ContextUtil contextUtil = ContextUtil.getInstance();

    private static MelcoePDP melcoePDPImpl;

    static {
        contextHandler = new ContextHandler();
    }

    private ContextHandler() {
        try {
            melcoePDPImpl = new MelcoePDPImpl();
            log.debug("created new PDP");
        } catch (Exception e) {
            // test code...
        }
    }

    public static ContextHandler getInstance() {
        return contextHandler;
    }

    /**
     * @param reqCtx
     *        an XACML request context for resolution.
     * @return an XACML response context based on the evaluation of the request
     *         context.
     */
    public ResponseCtx evaluate(RequestCtx reqCtx) throws Exception {
        log.debug("Resolving RequestCtx request!");

        String request = contextUtil.makeRequestCtx(reqCtx);
        String response = evaluate(request);
        ResponseCtx resCtx = contextUtil.makeResponseCtx(response);

        return resCtx;
    }

    /**
     * @param req
     *        an XACML request context for resolution.
     * @return an XACML response context based on the evaluation of the request
     *         context.
     */
    public String evaluate(String req) throws Exception {
        String res = melcoePDPImpl.evaluate(req);
        return res;
    }
}
