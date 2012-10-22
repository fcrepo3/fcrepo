package org.fcrepo.common.policy.xacml1;
import org.fcrepo.common.policy.XacmlName;
import org.fcrepo.common.policy.XacmlNamespace;



public class XACML1ActionNamespace extends XacmlNamespace {

    public final XacmlName ID;

    private XACML1ActionNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        ID = new XacmlName(this,"action-id");
    }

    public static XACML1ActionNamespace onlyInstance =
            new XACML1ActionNamespace(XACML1Namespace.getInstance(), "action");

    public static final XACML1ActionNamespace getInstance() {
        return onlyInstance;
    }

}
