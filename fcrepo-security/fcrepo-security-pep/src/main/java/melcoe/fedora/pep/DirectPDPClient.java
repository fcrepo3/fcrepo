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

package melcoe.fedora.pep;

import java.util.Map;

import melcoe.xacml.pdp.MelcoePDP;
import melcoe.xacml.pdp.MelcoePDPImpl;

import org.apache.log4j.Logger;

/**
 * This is the Web Services based client for the MelcoePDP. It uses the classes
 * in melcoe.fedora.pdp.client which are the Web Service client stubs.
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public class DirectPDPClient
        implements PDPClient {

    private static Logger log =
            Logger.getLogger(DirectPDPClient.class.getName());

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
        try {
            client = new MelcoePDPImpl();
        } catch (Exception e) {
            log.error("Could not initialise the PEP Client.");
            throw new PEPException("Could not initialise the PEP Client.", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see melcoe.fedora.pep.PEPClient#evaluate(java.lang.String)
     */
    public String evaluate(String request) throws PEPException {
        if (log.isDebugEnabled()) {
            log.debug("Resolving String request:\n" + request);
        }

        String response = null;
        try {
            response = client.evaluate(request);
        } catch (Exception e) {
            log.error("Error evaluating request.", e);
            throw new PEPException("Error evaluating request", e);
        }

        return response;
    }

    /*
     * (non-Javadoc)
     * @see melcoe.fedora.pep.PEPClient#evaluateBatch(java.lang.String[])
     */
    public String evaluateBatch(String[] request) throws PEPException {
        if (log.isDebugEnabled()) {
            log.debug("Resolving request batch (" + request.length
                    + " requests)");
        }

        String response = null;
        try {
            response = client.evaluateBatch(request);
        } catch (Exception e) {
            log.error("Error evaluating request.", e);
            throw new PEPException("Error evaluating request", e);
        }

        return response;
    }
}
