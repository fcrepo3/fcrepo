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

package melcoe.xacml.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import melcoe.xacml.pdp.MelcoePDP;
import melcoe.xacml.pdp.data.DbXmlPolicyDataManager;
import melcoe.xacml.pdp.data.PolicyDataManagerException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class PopulatePolicyDatabase {

    private static final Logger log =
            Logger.getLogger(PopulatePolicyDatabase.class);

    private static final String POLICY_HOME =
            MelcoePDP.PDP_HOME.getAbsolutePath() + "/policies";

    private static DbXmlPolicyDataManager dbXmlPolicyDataManager;

    private static Set<String> policyNames = new HashSet<String>();

    static {
        try {
            dbXmlPolicyDataManager = new DbXmlPolicyDataManager();
        } catch (PolicyDataManagerException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws PolicyDataManagerException,
            FileNotFoundException {
        BasicConfigurator.configure();
        dbXmlPolicyDataManager = new DbXmlPolicyDataManager();
        log.info("Adding");
        add();
        log.info("Listing");
        list();
    }

    public static void add() throws PolicyDataManagerException,
            FileNotFoundException {
        log.info("Starting clock!");
        long time1 = System.nanoTime();
        addDocuments();
        long time2 = System.nanoTime();
        log.info("Stopping clock!");
        log.info("Time taken: " + (time2 - time1));
    }

    public static void addDocuments() throws PolicyDataManagerException,
            FileNotFoundException {
        File[] files = getPolicyFiles();
        if (files.length == 0) {
            return;
        }

        for (File f : files) {
            if (dbXmlPolicyDataManager.contains(f)) {
                if (log.isDebugEnabled()) {
                    log.debug("Policy database already contains " + f.getName()
                            + ". Skipping.");
                }
            } else {
                policyNames.add(dbXmlPolicyDataManager.addPolicy(f));
            }
        }
    }

    public static void list() throws PolicyDataManagerException {
        List<String> docNames = dbXmlPolicyDataManager.listPolicies();
        for (String s : docNames) {
            log.info("doc: " + s);
        }
    }

    public static File[] getPolicyFiles() {
        File policyHome = new File(POLICY_HOME);
        File[] policies = policyHome.listFiles(new PolicyFileFilter());
        return policies;
    }
}
