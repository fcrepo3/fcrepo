package org.fcrepo.common.policy.xacml1;
import org.fcrepo.common.policy.XacmlName;
import org.fcrepo.common.policy.XacmlNamespace;



public class XACML1ResourceCategoryNamespace extends XacmlNamespace {

    public final XacmlName ACCESS_RESOURCE;

    private XACML1ResourceCategoryNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        ACCESS_RESOURCE = new XacmlName(this,"access-resource");
    }

    public static XACML1ResourceCategoryNamespace onlyInstance =
            new XACML1ResourceCategoryNamespace(XACML1Namespace.getInstance(), "resource-category");

    public static final XACML1ResourceCategoryNamespace getInstance() {
        return onlyInstance;
    }

}
