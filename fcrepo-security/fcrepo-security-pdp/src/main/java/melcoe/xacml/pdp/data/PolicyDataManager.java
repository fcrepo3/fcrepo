/*
 * File: PolicyDataManager.java
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

package melcoe.xacml.pdp.data;

import java.io.File;
import java.util.List;
import java.util.Map;

import melcoe.xacml.util.AttributeBean;

import com.sun.xacml.EvaluationCtx;

/**
 * This class provides an interface for Policy Stores. Policy Stores can be
 * implemented in whatever way they wish using any kind of backend from database
 * to filesystem as long as they adhere to this interface.
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public interface PolicyDataManager {

    /**
     * Extracts a list of policies from the Policy Store that are relevant to
     * the given Evaluation Context. Note that this returns a set that still has
     * to be further filtered by the PolicyFinder to find policies that match
     * 100%. This merely eliminates the need to match every policy, just the
     * most likely ones to apply.
     * 
     * @param eval
     *        the Evaluation Context from which to match policies against
     * @return the List of potential policies
     * @throws PolicyDataManagerException
     */
    public Map<String, byte[]> getPolicies(EvaluationCtx eval)
            throws PolicyDataManagerException;

    /**
     * Obtains the policy with the provided name from the Policy Store.
     * 
     * @param name
     *        the name of the policy to return
     * @return the policy as an array of bytes
     * @throws PolicyDataManagerException
     */
    public byte[] getPolicy(String name) throws PolicyDataManagerException;

    /**
     * Generates a policy using the {@link File} and name provided and adds it
     * to the Policy Store.
     * 
     * @param f
     *        the policy as a {@link File}
     * @param name
     *        the name to assign the policy
     * @return the name of the policy
     * @throws {@link PolicyDataManagerException}
     */
    public String addPolicy(File f, String name)
            throws PolicyDataManagerException;

    /**
     * Generates a policy using the {@link File} provided. The name is
     * automatically generated based on the PolicyId attribute of the Policy.
     * The policy is then added to the Policy Store.
     * 
     * @param f
     *        the policy as a {@link File}
     * @return the name of the added policy
     * @throws {@link PolicyDataManagerException}
     */
    public String addPolicy(File f) throws PolicyDataManagerException;

    /**
     * Generates a policy based on the string data provided and the name. The
     * policy is then added to the Policy Store.
     * 
     * @param document
     *        the policy as a {@link String}
     * @param name
     * @return the name of the added policy
     * @throws {@link PolicyDataManagerException}
     */
    public String addPolicy(String document, String name)
            throws PolicyDataManagerException;

    /**
     * Generates a policy based on the string data provided. The name is
     * automatically generated based on the PolicyId attribute of the Policy.
     * The policy is then added to the Policy Store.
     * 
     * @param document
     *        the policy as a {@link String}
     * @return the name of the added policy
     * @throws {@link PolicyDataManagerException}
     */
    public String addPolicy(String document) throws PolicyDataManagerException;

    /**
     * Removes the policy given by name from the data store.
     * 
     * @param name
     *        the name of the policy
     * @return true if policy was deleted
     * @throws PolicyDataManagerException
     */
    public boolean deletePolicy(String name) throws PolicyDataManagerException;

    /**
     * Generates a new policy based for the given policy name and replaces the
     * old policy in the Policy Store with it. An exception is thrown if the
     * policy with the given name cannot be found.
     * 
     * @param name
     *        the name of the policy to update
     * @param newDocument
     *        the new policy as a {@link String}
     * @return true if policy was updated
     * @throws PolicyDataManagerException
     */
    public boolean updatePolicy(String name, String newDocument)
            throws PolicyDataManagerException;

    /**
     * Obtains a list of stored policies.
     * 
     * @return a list containing the names of all the policies in the store.
     * @throws PolicyDataManagerException
     */
    public List<String> listPolicies() throws PolicyDataManagerException;

    /**
     * Obtains a time at which the database was last updated.
     * 
     * @return a long integer representing the last update time relative to the
     *         server.
     */
    public long getLastUpdate();

    /**
     * Searches through the policy store for policies that match the search
     * criteria. Search criteria is passed in through the AttributeBean class.
     * For a policy to match it has to contain all the attribute values in each
     * of the AttributeBean elements.
     * 
     * @param attributes
     *        an array of AttributeBean classes
     * @return map of policies keyed by the policies name and contain a byte
     *         array of the policy itself
     * @throws PolicyDataManagerException
     */
    public Map<String, byte[]> findPolicies(AttributeBean[] attributes)
            throws PolicyDataManagerException;
}
