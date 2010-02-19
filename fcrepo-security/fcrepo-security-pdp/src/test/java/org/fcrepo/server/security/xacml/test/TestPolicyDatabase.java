/*
 * File: TestPolicyDatabase.java
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

package org.fcrepo.server.security.xacml.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.server.security.xacml.pdp.data.DbXmlPolicyDataManager;
import org.fcrepo.server.security.xacml.pdp.data.PolicyDataManagerException;
import org.fcrepo.server.security.xacml.util.PolicyFileFilter;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class TestPolicyDatabase {

    private static final Logger logger =
            LoggerFactory.getLogger(TestPolicyDatabase.class);

    private static final String POLICY_HOME = "C:/Code/policies";

    private static DbXmlPolicyDataManager dbXmlPolicyDataManager;

    private static Set<String> policyNames = new HashSet<String>();

    public static void main(String[] args) throws PolicyDataManagerException,
            FileNotFoundException {
        dbXmlPolicyDataManager = new DbXmlPolicyDataManager();
        logger.info("Adding");
        add();
        logger.info("Listing");
        list();
        logger.info("Deleting");
        delete("au:edu:mq:melcoe:ramp:fedora:xacml:2.0:policy:white-papers-read");
        logger.info("Listing");
        list();
        logger.info("Updating");
        update();
        logger.info("Getting");
        get("au:edu:mq:melcoe:ramp:fedora:xacml:2.0:policy:admin-access");
        logger.info("Cleaning out policies.");
        clean();
        logger.info("Listing");
        list();
    }

    public static void add() throws PolicyDataManagerException,
            FileNotFoundException {
        logger.info("Starting clock!");
        long time1 = System.nanoTime();
        addDocuments();
        long time2 = System.nanoTime();
        logger.info("Stopping clock!");
        logger.info("Time taken: " + (time2 - time1));
    }

    public static void delete(String name) throws PolicyDataManagerException {
        dbXmlPolicyDataManager.deletePolicy(name);
        policyNames.remove(name);
    }

    public static void update() throws PolicyDataManagerException {
        String name = "au:edu:mq:melcoe:ramp:fedora:xacml:2.0:policy:3";
        byte[] doc = dbXmlPolicyDataManager.getPolicy(name);
        dbXmlPolicyDataManager.updatePolicy(name, new String(doc));
    }

    public static void get(String name) throws PolicyDataManagerException {
        byte[] doc = dbXmlPolicyDataManager.getPolicy(name);
        logger.info(new String(doc));
    }

    public static void addDocuments() throws PolicyDataManagerException,
            FileNotFoundException {
        File[] files = getPolicyFiles();
        for (File f : files) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] bytes = new byte[1024];
            FileInputStream fis = new FileInputStream(f);

            logger.info("Adding: " + f.getName());

            try {
                int count = fis.read(bytes);
                while (count > -1) {
                    out.write(bytes, 0, count);
                    count = fis.read(bytes);
                }
            } catch (IOException e) {
                logger.error("Error reading file: " + f.getName(), e);
            }

            policyNames.add(dbXmlPolicyDataManager.addPolicy(out.toString()));
        }
    }

    public static void list() throws PolicyDataManagerException {
        List<String> docNames = dbXmlPolicyDataManager.listPolicies();
        for (String s : docNames) {
            logger.info("doc: " + s);
        }
    }

    public static void clean() throws PolicyDataManagerException {
        Set<String> pn = new HashSet<String>(policyNames);

        for (String s : pn) {
            logger.info("removing: " + s);
            delete(new String(s));
        }
    }

    public static File[] getPolicyFiles() {
        File policyHome = new File(POLICY_HOME + "/policies");
        File[] policies = policyHome.listFiles(new PolicyFileFilter());
        return policies;
    }
}
