/*
 * File: WebServicesPDPClient.java
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

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.server.security.xacml.pdp.MelcoePDP;
import org.fcrepo.server.security.xacml.pdp.MelcoePDPImpl;

/**
 * This is the Web Services based client for the MelcoePDP. It uses the classes
 * in org.fcrepo.server.security.xacml.pdp.client which are the Web Service client stubs.
 *
 * @author nishen@melcoe.mq.edu.au
 */
public class DirectPDPClient
        implements PDPClient {

    private static final Logger logger =
            LoggerFactory.getLogger(DirectPDPClient.class);

    private MelcoePDP client = null;

    /**
     * Initialises the DirectPDPClient class.
     *
     * @param options
     *        a Map of options for this class
     * @throws PEPException
     */
    public DirectPDPClient(Map<String, String> options)
            throws PEPException {
        // FIXME: this constructor is not required - no options - but is called by pep ContextHandlerImpl
        /*
        try {
            client = new MelcoePDPImpl();
        } catch (Exception e) {
            logger.error("Could not initialise the PEP Client.");
            throw new PEPException("Could not initialise the PEP Client.", e);
        }
        */
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.PEPClient#evaluate(java.lang.String)
     */
    @Override
    public String evaluate(String request) throws PEPException {
        if (logger.isDebugEnabled()) {
            logger.debug("Resolving String request:\n" + request);
        }

        String response = null;
        try {
            response = getClient().evaluate(request);
        } catch (Exception e) {
            logger.error("Error evaluating request.", e);
            throw new PEPException("Error evaluating request", e);
        }

        return response;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.PEPClient#evaluateBatch(java.lang.String[])
     */
    @Override
    public String evaluateBatch(List<String> request) throws PEPException {
        if (request == null) {
            throw new NullPointerException("evaluateBatch(request=null)");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Resolving request batch (" + request.size()
                    + " requests)");
        }

        String response = null;
        try {
            response = getClient().evaluateBatch(request.toArray(new String[]{}));
        } catch (Exception e) {
            logger.error("Error evaluating request.", e);
            throw new PEPException("Error evaluating request", e);
        }

        return response;
    }
    private MelcoePDP getClient() throws PEPException {
        // lazy instantiation - as MelcoePDPImpl attempts to load policies when it is constructed,
        // if we are using Fedora as a policy store the server won't be running at this point and therefore the load will fail
        if (client == null) {
            try {
                client = new MelcoePDPImpl();
            } catch (Exception e) {
                logger.error("Could not initialise the PEP Client.");
                throw new PEPException("Could not initialise the PEP Client.", e);
            }
        }
        return client;

    }
}
