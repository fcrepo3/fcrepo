package org.fcrepo.common.policy.xacml1;
import org.fcrepo.common.policy.XacmlName;
import org.fcrepo.common.policy.XacmlNamespace;



public class XACML1PolicySchemaNamespace extends XacmlNamespace {

    public final XacmlName OS;
    private XACML1PolicySchemaNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        OS = new XacmlName(this,"os");
    }

    public static XACML1PolicySchemaNamespace onlyInstance = new XACML1PolicySchemaNamespace(XACML1PolicyNamespace.getInstance(), "schema");

    public static final XACML1PolicySchemaNamespace getInstance() {
        return onlyInstance;
    }

}
