package org.fcrepo.common.policy.xacml1;
import org.fcrepo.common.policy.XacmlNamespace;



public class XACML1EnvironmentNamespace extends XacmlNamespace {

    private XACML1EnvironmentNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
    }

    public static XACML1EnvironmentNamespace onlyInstance = new XACML1EnvironmentNamespace(XACML1Namespace.getInstance(), "environment");

    public static final XACML1EnvironmentNamespace getInstance() {
        return onlyInstance;
    }

}
