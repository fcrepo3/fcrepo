package org.fcrepo.common.policy.xacml1;
import org.fcrepo.common.policy.XacmlName;
import org.fcrepo.common.policy.XacmlNamespace;



public class XACML1PolicyCombiningNamespace extends XacmlNamespace {
    
    public final XacmlName HIER_LOWEST_DENY_OVERRIDES;
    public final XacmlName HIER_LOWEST_PERMIT_OVERRIDES;

    private XACML1PolicyCombiningNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        HIER_LOWEST_DENY_OVERRIDES = new XacmlName(this,"hierarchical-lowest-child-deny-overrides");
        HIER_LOWEST_PERMIT_OVERRIDES = new XacmlName(this,"hierarchical-lowest-child-permit-overrides");
    }

    public static XACML1PolicyCombiningNamespace onlyInstance =
            new XACML1PolicyCombiningNamespace(XACML1Namespace.getInstance(),
                                               "policy-combining-algorithm");

    public static final XACML1PolicyCombiningNamespace getInstance() {
        return onlyInstance;
    }

}
