package org.fcrepo.common.policy;



public class XACML1PolicyNamespace extends XacmlNamespace {

    private XACML1PolicyNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
    }

    public static XACML1PolicyNamespace onlyInstance = new XACML1PolicyNamespace(null, "policy");
    static {
        onlyInstance.addNamespace(XACML1Namespace.getInstance());
    }

    public static final XACML1PolicyNamespace getInstance() {
        return onlyInstance;
    }

}
