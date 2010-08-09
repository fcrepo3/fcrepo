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
import java.io.File;

import java.util.HashSet;
import java.util.Set;

import com.sun.xacml.ConfigurationStore;
import com.sun.xacml.Indenter;
import com.sun.xacml.PDP;
import com.sun.xacml.ParsingException;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.server.security.xacml.util.PopulatePolicyDatabase;

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

    private PDP pdp;

    /**
     * The default constructor. This reads in the configuration file and
     * instantiates a PDP based on it.
     *
     * @throws MelcoePDPException
     */
    public MelcoePDPImpl()
            throws MelcoePDPException {
        ConfigurationStore config = null;
        try {
            String home = PDP_HOME.getAbsolutePath();
            File f = null;
            String filename = null;

            // Loads the policies in PDP_HOME/policies
            // Does not monitor the directory for changes, nor will
            // subsequently deleted policies be removed from the policy store
            try {
            PopulatePolicyDatabase.addDocuments();
            } catch (Exception e) {
                throw new MelcoePDPException("Error loading bootstrap FeSL policies", e);
            }
            //

            // Ensure we have the configuration file.
            filename = home + "/conf/config-pdp.xml";
            f = new File(filename);
            if (!f.exists()) {
                throw new MelcoePDPException("Could not locate config file: "
                        + f.getAbsolutePath());
            }

            logger.info("Loading config file: " + f.getAbsolutePath());

            config = new ConfigurationStore(f);
            pdp = new PDP(config.getDefaultPDPConfig());

            logger.info("PDP Instantiated and initialised!");
        } catch (Exception e) {
            logger.error("Could not initialise PDP: " + e.getMessage(), e);
            throw new MelcoePDPException("Could not initialise PDP: "
                    + e.getMessage(), e);
        }
    }


    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.MelcoePDP#evaluate(java.lang.String)
     */
    public String evaluate(String request) throws EvaluationException {
        logger.debug("evaluating request: {}", request);

        RequestCtx req = null;
        ByteArrayInputStream is = new ByteArrayInputStream(request.getBytes());

        try {
            req = RequestCtx.getInstance(is);
        } catch (ParsingException pe) {
            logger.error("Error parsing request:\n" + request, pe);
            throw new EvaluationException("Error parsing request:\n" + request);
        }

        ResponseCtx res = pdp.evaluate(req);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        res.encode(os, new Indenter());

        logger.debug("response is: {}", os.toString());

        return os.toString();
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.MelcoePDP#evaluateBatch(java.lang.String[])
     */
    public String evaluateBatch(String[] requests) throws EvaluationException {
            logger.debug("evaluating request batch");

        Set<Result> results = new HashSet<Result>();

        for (String req : requests) {
            ResponseCtx resCtx = null;
            String response = evaluate(req);
            ByteArrayInputStream is =
                    new ByteArrayInputStream(response.getBytes());
            try {
                resCtx = ResponseCtx.getInstance(is);
            } catch (ParsingException pe) {
                logger.error("Error parsing response:\n" + response, pe);
                throw new EvaluationException("Error parsing response:\n"
                        + response);
            }

            @SuppressWarnings("unchecked")
            Set<Result> r = resCtx.getResults();
            results.addAll(r);
        }

        ResponseCtx combinedResponse = new ResponseCtx(results);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        combinedResponse.encode(os, new Indenter());

        return os.toString();
    }
}
