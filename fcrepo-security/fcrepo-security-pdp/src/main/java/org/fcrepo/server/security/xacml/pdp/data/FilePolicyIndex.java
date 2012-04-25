/*
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

package org.fcrepo.server.security.xacml.pdp.data;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.fcrepo.common.MalformedPIDException;
import org.fcrepo.common.PID;
import org.fcrepo.server.security.xacml.pdp.MelcoePDP;
import org.fcrepo.server.security.xacml.pdp.finder.policy.PolicyReader;
import org.fcrepo.server.security.xacml.util.DataFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.ParsingException;
import com.sun.xacml.finder.PolicyFinder;

/**
 * Implements PolicyIndex for a filesystem policy index, cached in memory
 *
 * FIXME: there is no indexing; all policies will be returned by getPolicies()
 *
 * @author nishen@melcoe.mq.edu.au
 */
class FilePolicyIndex
extends PolicyIndexBase
implements PolicyIndex {

    private static final Logger logger =
        LoggerFactory.getLogger(FilePolicyIndex.class.getName());


    private String DB_HOME = null;

    // contains the cached policies.  one and only one of these
    private static Map<String, byte[]> policies = null;
    // protects concurrent access to the policies (particularly the files in the cache directory)
    private static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    public static final Lock readLock = rwl.readLock();
    public static final Lock writeLock = rwl.writeLock();

    protected FilePolicyIndex(PolicyReader policyReader)
    throws PolicyIndexException {
        super(policyReader);
        indexed = false; // this implementation returns all policies when queried - no indexing/filtering

        logger.info("Starting FilePolicyIndex");

    }

    public void setPolicyDirectoryPath(String dbHome) throws PolicyIndexException {
        if (logger.isDebugEnabled()) {
            Runtime runtime = Runtime.getRuntime();
            logger.debug("Total memory: " + runtime.totalMemory() / 1024);
            logger.debug("Free memory: " + runtime.freeMemory() / 1024);
            logger.debug("Max memory: " + runtime.maxMemory() / 1024);
        }

        DB_HOME = MelcoePDP.PDP_HOME.getAbsolutePath() + dbHome;
        File db_home = new File(DB_HOME);
        if (!db_home.exists()) {
            try {
                db_home.mkdirs();
            } catch (Exception e) {
                throw new PolicyIndexException("Could not create DB directory: " + db_home.getAbsolutePath());
            }
        }
    }

    public void init() throws PolicyIndexException {
        loadPolicies(DB_HOME);
    }

    /*
     * (non-Javadoc)
     * @seemelcoe.xacml.pdp.data.Index#getPolicies(com.sun.xacml.
     * EvaluationCtx)
     */
    @Override
    public Map<String, AbstractPolicy> getPolicies(EvaluationCtx eval, PolicyFinder policyFinder)
    throws PolicyIndexException {
        // no indexing, return everything
        // return a copy, otherwise the map could change during evaluation if policies are added, deleted etc
        readLock.lock();
        try {
            Map<String, AbstractPolicy> result = new ConcurrentHashMap<String, AbstractPolicy>();
            for(String id:policies.keySet()){
                AbstractPolicy policy = handleDocument(m_policyReader.readPolicy(policies.get(id)),policyFinder);
                result.put(id, policy);
            }
            return result;
        }
        catch (ParsingException pe) {
            throw new PolicyIndexException(pe.getMessage(),pe);
        }finally {
            readLock.unlock();
        }
    }


    /**
     * Convert a policy name to a filename that can be used to persist the policy.
     * Policy names must be valid PIDs (the DO managing the policy)
     * @param policyName
     * @return
     * @throws PolicyIndexException
     */
    private File nameToFile(String policyName) throws PolicyIndexException {
        PID pid;
        try {
            pid = new PID(policyName);
        } catch (MalformedPIDException e) {
            throw new PolicyIndexException("Invalid policy name.  Policy name must be a valid PID - " + policyName);
        }
        return new File(DB_HOME + "/" + pid.toFilename() + ".xml");

    }
    /**
     * Determine name of policy from file.  .xml prefix is removed, and name is converted to a PID.
     * Policy names must be valid PIDs.
     * @param policyFile
     * @return
     * @throws PolicyIndexException
     */
    private String fileToName(File policyFile) throws PolicyIndexException {
        try {
            if (!policyFile.getName().endsWith(".xml"))
                throw new PolicyIndexException("Invalid policy file name.  Policy files must end in .xml - " + policyFile.getName());

            return PID.fromFilename(policyFile.getName().substring(0, policyFile.getName().lastIndexOf(".xml"))).toString();
        } catch (MalformedPIDException e) {
            throw new PolicyIndexException("Invalid policy file name.  Filename cannot be converted to a valid PID - " + policyFile.getName());
        }

    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.data.PolicyIndex#addPolicy(java.lang.String,
     * java.lang.String)
     */
    @Override
    public String addPolicy(String name, String document)
    throws PolicyIndexException {
        writeLock.lock();
        try {
            logger.debug("Adding policy named: " + name);
            return doAdd(name, document);
        } finally {
            writeLock.unlock();
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#deletePolicy(java.lang.String)
     */
    @Override
    public boolean deletePolicy(String name) throws PolicyIndexException {

        writeLock.lock();
        try {
            logger.debug("Deleting policy named: " + name);
            return doDelete(name);
        } finally {
            writeLock.unlock();
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyIndex#updatePolicy(java.lang.String,
     * java.lang.String)
     */
    @Override
    public boolean updatePolicy(String name, String newDocument)
    throws PolicyIndexException {

        writeLock.lock();
        try {
            logger.debug("Updating policy named: " + name);
            if (doDelete(name)) {
                doAdd(name, newDocument);
                return true;
            } else {
                // delete failed
                return false;
            }
        } finally {
            writeLock.unlock();
        }
    }

    // the actual add and delete methods; these are not protected by locks
    // as locking should take place on the public methods (especially we don't want
    // separate add/delete locks for an update
    private String doAdd(String name, String document) throws PolicyIndexException {
        String filename = nameToFile(name).getAbsolutePath();

        if (policies.put(name, document.getBytes()) != null) {
            throw new PolicyIndexException("Attempting to add policy " + name + " but it already exists");
        }

        try {
            logger.debug("Saving policy file in index: " + filename);
            DataFileUtils.saveDocument(filename, document.getBytes());
        } catch (Exception e) {
            throw new PolicyIndexException("Failed to save policy file " + filename);
        }

        return name;
    }


    private boolean doDelete(String name) throws PolicyIndexException {
        if (policies.remove(name) == null) {
            throw new PolicyIndexException("Attempting to delete non-existent policy " + name);
        }

        File policy = nameToFile(name);

        logger.debug("Removing policy file from index: " + policy);

        if (!policy.exists()) {
            logger.error("Policy " + name + " removed from cache, but no corresponding file found in policy cache directory");
        } else {
            // delete the file
            if (!policy.delete()) {
                policy.deleteOnExit(); // just in case
                logger.error("Failed to delete policy file " + policy.getName() + ".  Marked for deletion on VM exit");
            }
        }

        return true;

    }


    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#getPolicy(java.lang.String)
     */
    @Override
    public AbstractPolicy getPolicy(String name, PolicyFinder policyFinder) throws PolicyIndexException {
        readLock.lock();
        try {
            logger.debug("Getting policy named: " + name);
            if (policies.containsKey(name)) {
                return handleDocument(m_policyReader.readPolicy(policies.get(name)), policyFinder);
            } else {
                throw new PolicyIndexException("Attempting to get non-existent policy " + name);
            }
        } catch (ParsingException pe) {
            throw new PolicyIndexException(pe.getMessage(),pe);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean contains(String name) throws PolicyIndexException {
        readLock.lock();
        try {
            return policies.containsKey(name);
        } finally {
            readLock.unlock();
        }

    }

    @Override
    public boolean clear() throws PolicyIndexException {

        writeLock.lock();
        try {

            logger.debug("Clearing file policy index");
            // remove the policy files
            File policyDir = new File(DB_HOME);
            for (File policyFile :  policyDir.listFiles()) {
                if (!policyFile.delete()) {
                    throw new PolicyIndexException("Could not clear policy index.  Failed to delete policy file " + policyFile.getAbsolutePath());
                }
            }
            // clear the cache
            policies = new ConcurrentHashMap<String, byte[]>();

            return true;
        } finally {
            writeLock.unlock();
        }
    }

    private void loadPolicies(String policyDir)
    throws PolicyIndexException {

        // stop any reads whilst we're starting up
        writeLock.lock();
        try {

            if (policies == null) {

                logger.info("Populating FeSL File policy index cache");

                policies = new ConcurrentHashMap<String, byte[]>();

                File policyHome = new File(policyDir);
                if (!policyHome.exists()) {
                    throw new PolicyIndexException("Policy directory does not exist: "
                                                   + policyHome.getAbsolutePath());
                }

                File[] pf = policyHome.listFiles();
                for (File f : pf) {
                    if (!f.getName().endsWith(".xml")) {
                        // should not happen... all files should be .xml
                        throw new PolicyIndexException("Non .xml files found in policy index cache directory");
                    }

                    try {
                        logger.info("Loading FeSL policy from cache directory: " + f.getAbsolutePath());
                        byte[] doc = DataFileUtils.loadFile(f);

                        String policyName = fileToName(f);
                        logger.debug("Adding policy file to cache, policy name: " + policyName);
                        policies.put(policyName, doc);
                    } catch (Exception e) {
                        logger.error("Error loading document: " + f.getName(), e);
                        throw new PolicyIndexException("Error loading document: " + f.getName(),e);
                    }
                }
                logger.info("Populated cache with " + pf.length + " files");
            }
        } finally {
            writeLock.unlock();
        }
    }



}