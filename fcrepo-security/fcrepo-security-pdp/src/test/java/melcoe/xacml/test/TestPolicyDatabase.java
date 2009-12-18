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

package melcoe.xacml.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import melcoe.xacml.pdp.data.DbXmlPolicyDataManager;
import melcoe.xacml.pdp.data.PolicyDataManagerException;
import melcoe.xacml.util.PolicyFileFilter;

import org.apache.log4j.Logger;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class TestPolicyDatabase {

    private static final Logger log =
            Logger.getLogger(TestPolicyDatabase.class);

    private static final String POLICY_HOME = "C:/Code/policies";

    private static DbXmlPolicyDataManager dbXmlPolicyDataManager;

    private static Set<String> policyNames = new HashSet<String>();

    public static void main(String[] args) throws PolicyDataManagerException,
            FileNotFoundException {
        dbXmlPolicyDataManager = new DbXmlPolicyDataManager();
        log.info("Adding");
        add();
        log.info("Listing");
        list();
        log.info("Deleting");
        delete("au:edu:mq:melcoe:ramp:fedora:xacml:2.0:policy:white-papers-read");
        log.info("Listing");
        list();
        log.info("Updating");
        update();
        log.info("Getting");
        get("au:edu:mq:melcoe:ramp:fedora:xacml:2.0:policy:admin-access");
        log.info("Cleaning out policies.");
        clean();
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
        log.info(new String(doc));
    }

    public static void addDocuments() throws PolicyDataManagerException,
            FileNotFoundException {
        File[] files = getPolicyFiles();
        for (File f : files) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] bytes = new byte[1024];
            FileInputStream fis = new FileInputStream(f);

            log.info("Adding: " + f.getName());

            try {
                int count = fis.read(bytes);
                while (count > -1) {
                    out.write(bytes, 0, count);
                    count = fis.read(bytes);
                }
            } catch (IOException e) {
                log.error("Error reading file: " + f.getName(), e);
            }

            policyNames.add(dbXmlPolicyDataManager.addPolicy(out.toString()));
        }
    }

    public static void list() throws PolicyDataManagerException {
        List<String> docNames = dbXmlPolicyDataManager.listPolicies();
        for (String s : docNames) {
            log.info("doc: " + s);
        }
    }

    public static void clean() throws PolicyDataManagerException {
        Set<String> pn = new HashSet<String>(policyNames);

        for (String s : pn) {
            log.info("removing: " + s);
            delete(new String(s));
        }
    }

    public static File[] getPolicyFiles() {
        File policyHome = new File(POLICY_HOME + "/policies");
        File[] policies = policyHome.listFiles(new PolicyFileFilter());
        return policies;
    }
}
