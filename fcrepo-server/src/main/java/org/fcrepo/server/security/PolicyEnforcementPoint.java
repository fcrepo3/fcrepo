
package org.fcrepo.server.security;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.authorization.AuthzException;

public interface PolicyEnforcementPoint {

    public static final String SUBACTION_SEPARATOR = "//";

    public static final String SUBRESOURCE_SEPARATOR = "//";

    public static final String XACML_SUBJECT_ID =
            "urn:oasis:names:tc:xacml:1.0:subject:subject-id";

    public static final String XACML_ACTION_ID =
            "urn:oasis:names:tc:xacml:1.0:action:action-id";

    public static final String XACML_RESOURCE_ID =
            "urn:oasis:names:tc:xacml:1.0:resource:resource-id";

    public void newPdp() throws Exception;

    public abstract void inactivate();

    public abstract void destroy();

    public abstract void enforce(String subjectId, String action, String api,
            String pid, String namespace, Context context)
            throws AuthzException;

}