package org.fcrepo.server.security.xacml.pdp.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import org.fcrepo.server.security.xacml.pdp.MelcoePDPException;
import org.fcrepo.server.security.xacml.util.PopulatePolicyDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractPolicyStore implements PolicyStore {

    public static final Logger LOGGER =
            LoggerFactory.getLogger(PopulatePolicyDatabase.class);

    public static Set<String> policyNames = new HashSet<String>();

    protected boolean policiesLoaded = false;

    @Override
    public void init() throws FileNotFoundException, PolicyStoreException{
        AbstractPolicyStore.addDocuments(this);
    }

    public void reloadPolicies() throws FileNotFoundException, PolicyStoreException {
        synchronized(AbstractPolicyStore.class){
            this.policiesLoaded = false;
            AbstractPolicyStore.addDocuments(this, true);
        }
    }

    public static synchronized void addDocuments(AbstractPolicyStore policyStore) throws PolicyStoreException,
    FileNotFoundException {
        addDocuments(policyStore, false);
    }

    public static synchronized void addDocuments(AbstractPolicyStore policyStore, boolean reload) throws PolicyStoreException,
    FileNotFoundException {

        if (policyStore.policiesLoaded)
            return;

        File[] files = PopulatePolicyDatabase.getPolicyFiles();
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

                if (policyStore.contains(policyID) && !reload) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Policy database already contains " + policyID + " (" + f.getName()+ ")"
                                     + ". Skipping.");
                    }
                } else {
                    AbstractPolicyStore.policyNames.add(policyStore.addPolicy(f, policyID));
                }
            } catch (MelcoePDPException e){
                LOGGER.warn("Failed to add bootstrap policy " + f.getName() + " - " + e.getMessage());
                failedPolicies.append(f.getName() + "\n");
            }

        }
        if (failedPolicies.length() != 0) {
            throw new PolicyStoreException("Failed to load some bootstrap policies: " + failedPolicies.toString());
        }
        policyStore.policiesLoaded = true;
    }


}