package org.fcrepo.common.policy.xacml1;
import org.fcrepo.common.policy.XacmlName;
import org.fcrepo.common.policy.XacmlNamespace;



public class XACML1SubjectCategoryNamespace extends XacmlNamespace {

    public final XacmlName ACCESS_SUBJECT;

    private XACML1SubjectCategoryNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        ACCESS_SUBJECT = new XacmlName(this, "access-subject");
    }

    public static XACML1SubjectCategoryNamespace onlyInstance = new XACML1SubjectCategoryNamespace(XACML1Namespace.getInstance(), "subject-category");

    public static final XACML1SubjectCategoryNamespace getInstance() {
        return onlyInstance;
    }

}
