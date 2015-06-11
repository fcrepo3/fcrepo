/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.xacml.pdp.data;

import java.util.Map;

import org.fcrepo.common.Constants;

import org.jboss.security.xacml.sunxacml.AbstractPolicy;
import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.finder.PolicyFinder;


/**
 * This class provides an interface for a Policy Index. Policy Indexes can be
 * implemented in whatever way they wish using any kind of backend from database
 * to filesystem as long as they adhere to this interface.

 * A Policy Index is an index over a Policy Store (see PolicyStore).
 *
 * A PolicyIndex is used by the PolicyFinder to locate matching policies.
 *
 * Policy Indexes must be kept synchronised with Policy Stores.
 *
 * See org.fcrepo.server.security.xacml.pdp.decorator.DbXmlPolicyCacheInvocationHandler
 * for synchronisation of the dbxml cache with the Fedora policy store
 *
 * If the Policy Index does not contain the complete policy documents then the implementation
 * will need to include a PolicyStore instance for retrieving the actual matching policies.
 *
 * @author stephen.bayliss
 */

public interface PolicyIndex {

    public static final String XACML20_POLICY_NS = Constants.XACML2_POLICY_SCHEMA.OS.toString();
    public static final String XACML_RESOURCE_ID = Constants.XACML1_RESOURCE.ID.toString();

    /**
     * Extracts a list of policies from the Policy Index that are relevant to
     * the given Evaluation Context. Note that this returns a set that still has
     * to be further filtered by the PolicyFinder to find policies that match
     * 100%. This merely eliminates the need to match every policy, just the
     * most likely ones to apply.
     *
     * @param eval
     *        the Evaluation Context from which to match policies against
     * @return the List of potential policies
     * @throws PolicyIndexException
     */
    Map<String, AbstractPolicy> getPolicies(EvaluationCtx eval, PolicyFinder policyFinder)
            throws PolicyIndexException;



    /**
     * CRUD methods for the index
     */

    /**
     * Obtains the policy with the provided name from the Policy Store.
     *
     * @param name
     *        the name of the policy to return
     * @return the policy as an array of bytes
     * @throws PolicyIndexException
     */
    AbstractPolicy getPolicy(String name, PolicyFinder policyFinder) throws PolicyIndexException;


    /**
     * Generates a policy based on the string data provided and the name. The
     * policy is then added to the Policy Store.
     * @param name
     * @param document
     *        the policy as a {@link String}
     *
     * @return the name of the added policy
     * @throws PolicyIndexException
     */
    String addPolicy(String name, String document)
            throws PolicyIndexException;

    /**
     * Removes the policy given by name from the data store.
     *
     * @param name
     *        the name of the policy
     * @return true if policy was deleted
     * @throws PolicyIndexException
     */
    boolean deletePolicy(String name) throws PolicyIndexException;

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
     * @throws PolicyIndexException
     */
    boolean updatePolicy(String name, String newDocument)
            throws PolicyIndexException;

    /**
     * Check if the policy identified by policyName exists.
     *
     * @param policyName the PolicyId value
     * @return true iff the policy store contains a policy with the same
     *         PolicyId
     * @throws PolicyIndexException
     */
    boolean contains(String policyName) throws PolicyIndexException;

    /**
     * Clear the policy index completely
     * @return boolean
     * @throws PolicyIndexException
     */
    boolean clear() throws PolicyIndexException;

}