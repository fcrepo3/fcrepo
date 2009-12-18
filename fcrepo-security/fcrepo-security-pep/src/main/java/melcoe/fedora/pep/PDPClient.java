/*
 * File: PDPClient.java
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

/**
 * @author nishen@melcoe.mq.edu.au
 */
public interface PDPClient {

    /**
     * Sends an XACML request for evaluation to the PDP.
     * 
     * @param request
     *        an XACML request as a String
     * @return an XACML reponse as a String
     * @throws PEPException
     */
    public String evaluate(String request) throws PEPException;

    /**
     * Sends a String array of XACML requests for evaluation to the PDP. A
     * single resposne with the results of all requests is returned.
     * 
     * @param request
     *        a String array of XACML requests
     * @return an XACML reponse as a String containing results for all requests
     * @throws PEPException
     */
    public String evaluateBatch(String[] request) throws PEPException;
}
