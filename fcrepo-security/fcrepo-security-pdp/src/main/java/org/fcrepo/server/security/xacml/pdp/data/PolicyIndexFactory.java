/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.xacml.pdp.data;




/**
 * A factory for a PolicyIndex.  Used to get a PolicyIndex
 * instance based on configuration file.
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class PolicyIndexFactory {

    /**
     * Generate a PolicyStore instance based on config file
     * @return
     * @throws PolicyStoreException
     */
    public PolicyIndex newPolicyIndex() throws PolicyIndexException {

        // TODO: should we be supplying a classloader?
        PolicyIndex policyIndex;
        String policyIndexClassName;
        try {
            policyIndexClassName = Config.policyIndexClassName();
        } catch (PolicyConfigException e) {
            throw new PolicyIndexException("Error reading config for PolicyIndex", e);
        }

        try {
            policyIndex = (PolicyIndex) Class.forName(policyIndexClassName).newInstance();
        } catch (Exception e) {
            throw new PolicyIndexException("Error instantiating PolicyIndex " + policyIndexClassName, e);
        }

        return policyIndex;
    }


}
