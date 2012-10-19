package org.fcrepo.common.policy;



public class XACML1ActionNamespace extends XacmlNamespace {

    public final XacmlName ID;

    private XACML1ActionNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        ID = new XacmlName(this,"action-id");
    }

    public static XACML1ActionNamespace onlyInstance = new XACML1ActionNamespace(null, "action");
    static {
        onlyInstance.addNamespace(XACML1Namespace.getInstance());
    }

    public static final XACML1ActionNamespace getInstance() {
        return onlyInstance;
    }

}
