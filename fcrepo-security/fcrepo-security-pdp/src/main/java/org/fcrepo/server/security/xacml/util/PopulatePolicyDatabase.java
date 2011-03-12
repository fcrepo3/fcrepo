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

    private static boolean policiesLoaded = false;

    private static final Logger logger =
            LoggerFactory.getLogger(PopulatePolicyDatabase.class);

    private static final String POLICY_HOME =
            MelcoePDP.PDP_HOME.getAbsolutePath() + "/policies";

    private static Set<String> policyNames = new HashSet<String>();


    public static void add(PolicyStore policyStore) throws PolicyStoreException,
            FileNotFoundException {
        logger.info("Starting clock!");
        long time1 = System.nanoTime();
        addDocuments(policyStore);
        long time2 = System.nanoTime();
        logger.info("Stopping clock!");
        logger.info("Time taken: " + (time2 - time1));
    }

    public static synchronized void addDocuments(PolicyStore policyStore) throws PolicyStoreException,
    FileNotFoundException {

        if (policiesLoaded)
            return;

        File[] files = getPolicyFiles();
        if (files.length == 0) {
            return;
        }
        PolicyUtils utils = new PolicyUtils();

        // don't fail if a single policy fails, instead continue and list failed policies when done
        StringBuilder failedPolicies = new StringBuilder();

        for (File f : files) {
            try {
                String policyID = utils.getPolicyName(f);

                // TODO: name mangling only if Fedora policy store; use consts for ns from that
                if (policyStore instanceof FedoraPolicyStore) {

                    // get the policy ID - note that adding a policy with no name will generate a PID from
                    // the policy ID, but using the default PID namespace; we want specific namespace for bootstrap policies hence doing this here
                    // if XACML policy ID contains a pid separator, escape it
                    if (policyID.contains(":")) {
                        policyID = policyID.replace(":", "%3A");
                    }
                    policyID = FedoraPolicyStore.FESL_BOOTSTRAP_POLICY_NAMESPACE + ":" + policyID;
                }

                if (policyStore.contains(policyID)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Policy database already contains " + policyID + " (" + f.getName()+ ")"
                                     + ". Skipping.");
                    }
                } else {
                    policyNames.add(policyStore.addPolicy(f, policyID));
                }
            } catch (MelcoePDPException e){
                logger.warn("Failed to add bootstrap policy " + f.getName() + " - " + e.getMessage());
                failedPolicies.append(f.getName() + "\n");
            }

        }
        if (failedPolicies.length() != 0) {
            throw new PolicyStoreException("Failed to load some bootstrap policies: " + failedPolicies.toString());
        }
        policiesLoaded = true;
    }


    public static File[] getPolicyFiles() {
        File policyHome = new File(POLICY_HOME);
        File[] policies = policyHome.listFiles(new PolicyFileFilter());
        return policies;
    }
}
