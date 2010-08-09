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

import java.util.List;

/**
 * This class provides a CRUD interface for Policy Stores. Policy Stores can be
 * implemented in whatever way they wish using any kind of backend from database
 * to filesystem as long as they adhere to this interface.
 *
 * See PolicyIndex for a query interface to policies.
 *
 * @author nishen@melcoe.mq.edu.au
 */
public interface PolicyStore {


    /**
     * Obtains the policy with the provided name from the Policy Store.
     *
     * @param name
     *        the name of the policy to return
     * @return the policy as an array of bytes
     * @throws PolicyStoreException
     */
    byte[] getPolicy(String name) throws PolicyStoreException;

    /**
     * Generates a policy using the {@link File} and name provided and adds it
     * to the Policy Store.
     *
     * @param f
     *        the policy as a {@link File}
     * @param name
     *        the name to assign the policy
     * @return the name of the policy
     * @throws {@link PolicyStoreException}
     */
    String addPolicy(File f, String name)
            throws PolicyStoreException;

    /**
     * Generates a policy using the {@link File} provided. The name is
     * automatically generated based on the PolicyId attribute of the Policy.
     * The policy is then added to the Policy Store.
     *
     * @param f
     *        the policy as a {@link File}
     * @return the name of the added policy
     * @throws {@link PolicyStoreException}
     */
    String addPolicy(File f) throws PolicyStoreException;

    /**
     * Generates a policy based on the string data provided and the name. The
     * policy is then added to the Policy Store.
     *
     * @param document
     *        the policy as a {@link String}
     * @param name
     * @return the name of the added policy
     * @throws {@link PolicyStoreException}
     */
    String addPolicy(String document, String name)
            throws PolicyStoreException;

    /**
     * Generates a policy based on the string data provided. The name is
     * automatically generated based on the PolicyId attribute of the Policy.
     * The policy is then added to the Policy Store.
     *
     * @param document
     *        the policy as a {@link String}
     * @return the name of the added policy
     * @throws {@link PolicyStoreException}
     */
    String addPolicy(String document) throws PolicyStoreException;

    /**
     * Removes the policy given by name from the data store.
     *
     * @param name
     *        the name of the policy
     * @return true if policy was deleted
     * @throws PolicyStoreException
     */
    boolean deletePolicy(String name) throws PolicyStoreException;

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
     * @throws PolicyStoreException
     */
    boolean updatePolicy(String name, String newDocument)
            throws PolicyStoreException;

    /**
     * Obtains a list of stored policies.
     *
     * @return a list containing the names of all the policies in the store.
     * @throws PolicyStoreException
     */
    List<String> listPolicies() throws PolicyStoreException;

    /**
     * Check if the policy identified by policyName exists.
     *
     * @param policy
     * @return true iff the policy store contains a policy with the same
     *         PolicyId
     * @throws PolicyStoreException
     */
    boolean contains(String policy) throws PolicyStoreException;

    /**
     * Check if the policy in the file exists in the policy store.
     *
     * @param policy
     * @return true iff the policy store contains a policy with the same
     *         PolicyId
     * @throws PolicyStoreException
     */
    boolean contains(File policy) throws PolicyStoreException;

}
