package org.fcrepo.common.policy;



public class XACML1SubjectNamespace extends XacmlNamespace {

    public final XacmlName ID;

    private XACML1SubjectNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        ID = new XacmlName(this, "subject-id");
    }

    public static XACML1SubjectNamespace onlyInstance = new XACML1SubjectNamespace(null, "subject");
    static {
        onlyInstance.addNamespace(XACML1Namespace.getInstance());
    }

    public static final XACML1SubjectNamespace getInstance() {
        return onlyInstance;
    }

}
