/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.xacml.pdp.data;



/**
 * A factory for a PolicyStore.  Used to get a PolicyStore
 * instance based on configuration file.
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class PolicyStoreFactory {

    /**
     * Generate a PolicyStore instance based on config file
     * @return
     * @throws PolicyStoreException
     */
    public PolicyStore newPolicyStore() throws PolicyStoreException {

        // TODO: should we be supplying a classloader?
        PolicyStore policyStore;
        String policyStoreClassName;
        try {
            policyStoreClassName = Config.policyStoreClassName();
        } catch (PolicyConfigException e) {
            throw new PolicyStoreException("Error reading config for PolicyStore", e);
        }

        try {
            policyStore = (PolicyStore) Class.forName(policyStoreClassName).newInstance();
        } catch (Exception e) {
            throw new PolicyStoreException("Error instantiating PolicyStore " + policyStoreClassName, e);
        }

        return policyStore;
    }


}
