package org.fcrepo.common.policy.xacml1;
import org.fcrepo.common.policy.XacmlName;
import org.fcrepo.common.policy.XacmlNamespace;



public class XACML1SubjectNamespace extends XacmlNamespace {

    public final XacmlName ID;

    private XACML1SubjectNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        ID = new XacmlName(this, "subject-id");
    }

    public static XACML1SubjectNamespace onlyInstance = new XACML1SubjectNamespace(XACML1Namespace.getInstance(), "subject");

    public static final XACML1SubjectNamespace getInstance() {
        return onlyInstance;
    }

}
