package org.fcrepo.common.policy.xacml1;
import org.fcrepo.common.policy.XacmlName;
import org.fcrepo.common.policy.XacmlNamespace;


public class XACML1Namespace extends XacmlNamespace {

    private XACML1Namespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
    }

    public static XACML1Namespace onlyInstance = new XACML1Namespace(null, "urn:oasis:names:tc:xacml:1.0");
    
    static {
        onlyInstance.addNamespace(XACML1ActionNamespace.getInstance());
        onlyInstance.addNamespace(XACML1ActionCategoryNamespace.getInstance());
        onlyInstance.addNamespace(XACML1EnvironmentNamespace.getInstance());
        onlyInstance.addNamespace(XACML1EnvironmentCategoryNamespace.getInstance());
        onlyInstance.addNamespace(XACML1PolicyNamespace.getInstance());
        onlyInstance.addNamespace(XACML1ResourceNamespace.getInstance());
        onlyInstance.addNamespace(XACML1ResourceCategoryNamespace.getInstance());
        onlyInstance.addNamespace(XACML1SubjectNamespace.getInstance());
        onlyInstance.addNamespace(XACML1SubjectCategoryNamespace.getInstance());
    }

    public static final XACML1Namespace getInstance() {
        return onlyInstance;
    }

}
