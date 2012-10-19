package org.fcrepo.common.policy;


public class XACML1Namespace extends XacmlNamespace {

    public final XacmlName POLICY;

    private XACML1Namespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        POLICY = new XacmlName(this,"policy");
    }

    public static XACML1Namespace onlyInstance = new XACML1Namespace(null, "urn:oasis:names:tc:xacml:1.0");
    static {
        onlyInstance.addNamespace(FedoraAsOrganizationNamespace.getInstance());
    }

    public static final XACML1Namespace getInstance() {
        return onlyInstance;
    }

}
