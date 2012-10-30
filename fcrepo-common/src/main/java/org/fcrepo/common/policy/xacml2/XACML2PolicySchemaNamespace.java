package org.fcrepo.common.policy.xacml2;
import org.fcrepo.common.policy.XacmlName;
import org.fcrepo.common.policy.XacmlNamespace;



public class XACML2PolicySchemaNamespace extends XacmlNamespace {

    public final XacmlName OS;
    private XACML2PolicySchemaNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
        OS = new XacmlName(this,"os");
    }

    public static XACML2PolicySchemaNamespace onlyInstance =
            new XACML2PolicySchemaNamespace(null,
                                            "urn:oasis:names:tc:xacml:2.0:policy:schema");

    public static final XACML2PolicySchemaNamespace getInstance() {
        return onlyInstance;
    }

}
