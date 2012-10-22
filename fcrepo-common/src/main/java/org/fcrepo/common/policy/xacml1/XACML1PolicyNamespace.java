package org.fcrepo.common.policy.xacml1;
import org.fcrepo.common.policy.XacmlNamespace;



public class XACML1PolicyNamespace extends XacmlNamespace {

    private XACML1PolicyNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
    }

    public static XACML1PolicyNamespace onlyInstance = new XACML1PolicyNamespace(XACML1Namespace.getInstance(), "policy");
    static {
        onlyInstance.addNamespace(XACML1PolicySchemaNamespace.getInstance());
    }
    public static final XACML1PolicyNamespace getInstance() {
        return onlyInstance;
    }

}
