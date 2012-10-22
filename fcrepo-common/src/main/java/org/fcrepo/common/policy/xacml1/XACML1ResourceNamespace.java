package org.fcrepo.common.policy.xacml1;
import org.fcrepo.common.policy.XacmlName;
import org.fcrepo.common.policy.XacmlNamespace;



public class XACML1ResourceNamespace extends XacmlNamespace {

    public final XacmlName ID;

    private XACML1ResourceNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        ID = new XacmlName(this,"resource-id");
    }

    public static XACML1ResourceNamespace onlyInstance = new XACML1ResourceNamespace(XACML1Namespace.getInstance(), "resource");

    public static final XACML1ResourceNamespace getInstance() {
        return onlyInstance;
    }

}
