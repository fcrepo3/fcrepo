
package org.fcrepo.server.security;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.fcrepo.server.errors.authorization.AuthzException;

public interface PolicyEnforcementPoint {

    public static final String SUBACTION_SEPARATOR = "//";

    public static final String SUBRESOURCE_SEPARATOR = "//";

    public static final String XACML_SUBJECT_ID =
            Constants.XACML1_SUBJECT.ID.toString();

    public static final String XACML_ACTION_ID =
            Constants.XACML1_ACTION.ID.toString();

    public static final String XACML_RESOURCE_ID =
            Constants.XACML1_RESOURCE.ID.toString();

    public void newPdp() throws Exception;

    public abstract void inactivate();

    public abstract void destroy();

    public abstract void enforce(String subjectId, String action, String api,
            String pid, String namespace, Context context)
            throws AuthzException;

}