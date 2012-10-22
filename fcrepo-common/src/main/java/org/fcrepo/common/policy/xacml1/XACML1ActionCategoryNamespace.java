package org.fcrepo.common.policy.xacml1;
import org.fcrepo.common.policy.XacmlName;
import org.fcrepo.common.policy.XacmlNamespace;



public class XACML1ActionCategoryNamespace extends XacmlNamespace {

    public final XacmlName ACCESS_ACTION;

    private XACML1ActionCategoryNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        ACCESS_ACTION = new XacmlName(this,"access-action");
    }

    public static XACML1ActionCategoryNamespace onlyInstance = new XACML1ActionCategoryNamespace(XACML1Namespace.getInstance(), "action-category");

    public static final XACML1ActionCategoryNamespace getInstance() {
        return onlyInstance;
    }

}
