/*
 * File: MelcoePDP.java
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

package melcoe.xacml.pdp;

import java.io.File;

import fedora.common.Constants;

/**
 * The interface for the MelcoePDP. The PDP simply evaluates requests and
 * returns responses.
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public interface MelcoePDP {

    public static File PDP_HOME = new File(Constants.FEDORA_HOME, "pdp/");

    /**
     * This method evaluates an XACML request and returns an XACML response.
     * 
     * @param request
     *        the XACML request
     * @return the XACML response
     * @throws EvaluationException
     */
    public String evaluate(String request) throws EvaluationException;

    /**
     * A convenience function designed for reducing the number of WS calls made.
     * This function takes an array of requests and evaluates them and returns a
     * single response that contains all the resource id's and results.
     * 
     * @param requests
     *        the String array of XACML requests
     * @return the XACML response
     * @throws EvaluationException
     */
    public String evaluateBatch(String[] requests) throws EvaluationException;
}
