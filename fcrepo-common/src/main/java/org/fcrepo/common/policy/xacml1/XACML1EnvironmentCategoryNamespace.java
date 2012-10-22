package org.fcrepo.common.policy.xacml1;
import org.fcrepo.common.policy.XacmlName;
import org.fcrepo.common.policy.XacmlNamespace;



public class XACML1EnvironmentCategoryNamespace extends XacmlNamespace {

    public final XacmlName ACCESS_ENVIRONMENT;

    private XACML1EnvironmentCategoryNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        ACCESS_ENVIRONMENT = new XacmlName(this,"access-environment");
    }

    public static XACML1EnvironmentCategoryNamespace onlyInstance =
            new XACML1EnvironmentCategoryNamespace(XACML1Namespace.getInstance(), "environment-category");

    public static final XACML1EnvironmentCategoryNamespace getInstance() {
        return onlyInstance;
    }

}
