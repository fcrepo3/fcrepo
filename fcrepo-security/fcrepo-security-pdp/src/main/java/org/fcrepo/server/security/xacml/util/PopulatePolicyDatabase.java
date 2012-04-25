/*
 * File: PopulatePolicyDatabase.java
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

package org.fcrepo.server.security.xacml.util;

import java.io.File;
import java.io.FileNotFoundException;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.server.security.xacml.pdp.MelcoePDP;
import org.fcrepo.server.security.xacml.pdp.MelcoePDPException;
import org.fcrepo.server.security.xacml.pdp.data.AbstractPolicyStore;
import org.fcrepo.server.security.xacml.pdp.data.FedoraPolicyStore;
import org.fcrepo.server.security.xacml.pdp.data.PolicyStore;
import org.fcrepo.server.security.xacml.pdp.data.PolicyStoreException;
import org.fcrepo.server.security.xacml.pdp.data.PolicyUtils;

/**
 * Populates the policy store from XACML files in the policies directory
 *
 * @author nishen@melcoe.mq.edu.au
 */
public class PopulatePolicyDatabase {

    private static final Logger logger =
            LoggerFactory.getLogger(PopulatePolicyDatabase.class);

    private static final String POLICY_HOME =
            MelcoePDP.PDP_HOME.getAbsolutePath() + "/policies";

    public static void add(PolicyStore policyStore) throws PolicyStoreException,
            FileNotFoundException {
        logger.info("Starting clock!");
        long time1 = System.nanoTime();
        AbstractPolicyStore.addDocuments((AbstractPolicyStore)policyStore);
        long time2 = System.nanoTime();
        logger.info("Stopping clock!");
        logger.info("Time taken: " + (time2 - time1));
    }

    /**
     * @deprecated Use {@link AbstractPolicyStore#addDocuments(PolicyStore)} instead
     */
    @Deprecated
    public static synchronized void addDocuments(PolicyStore policyStore)
            throws PolicyStoreException, FileNotFoundException {
        AbstractPolicyStore.addDocuments((AbstractPolicyStore)policyStore);
    }


    public static File[] getPolicyFiles() {
        File policyHome = new File(POLICY_HOME);
        File[] policies = policyHome.listFiles(new PolicyFileFilter());
        return policies;
    }
}
