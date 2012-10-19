package org.fcrepo.common.policy;



public class XACML1ResourceNamespace extends XacmlNamespace {

    public final XacmlName ID;

    private XACML1ResourceNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        ID = new XacmlName(this,"resource-id");
    }

    public static XACML1ResourceNamespace onlyInstance = new XACML1ResourceNamespace(null, "resource");
    static {
        onlyInstance.addNamespace(XACML1Namespace.getInstance());
    }

    public static final XACML1ResourceNamespace getInstance() {
        return onlyInstance;
    }

}
