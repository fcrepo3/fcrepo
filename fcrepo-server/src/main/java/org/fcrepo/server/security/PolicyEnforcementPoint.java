
package org.fcrepo.server.security;

import java.util.List;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.storage.DOManager;

public interface PolicyEnforcementPoint {

    public static final String SUBACTION_SEPARATOR = "//";

    public static final String SUBRESOURCE_SEPARATOR = "//";

    public static final String XACML_SUBJECT_ID =
            "urn:oasis:names:tc:xacml:1.0:subject:subject-id";

    public static final String XACML_ACTION_ID =
            "urn:oasis:names:tc:xacml:1.0:action:action-id";

    public static final String XACML_RESOURCE_ID =
            "urn:oasis:names:tc:xacml:1.0:resource:resource-id";

    public abstract void setAttributeFinderModules(
            List<com.sun.xacml.finder.AttributeFinderModule> attrFinderModules);

    public abstract void initPep(String enforceMode, String combiningAlgorithm,
            String globalPolicyConfig, String globalBackendPolicyConfig,
            String globalPolicyGuiToolConfig, DOManager manager,
            boolean validateRepositoryPolicies,
            boolean validateObjectPoliciesFromDatastream,
            PolicyParser policyParser, String ownerIdSeparator)
            throws Exception;

    public void newPdp() throws Exception;

    public abstract void inactivate();

    public abstract void destroy();

    public abstract void enforce(String subjectId, String action, String api,
            String pid, String namespace, Context context)
            throws AuthzException;

}