
package org.fcrepo.server.security.xacml.test;

import org.fcrepo.server.security.xacml.pdp.data.DbXmlPolicyDataManager;
import org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager;
import org.fcrepo.server.security.xacml.pdp.data.PolicyDataManagerException;

public class TestDbxmlPolicyDataManager {

    private static PolicyDataManager pdm = null;

    /**
     * @param args
     */
    public static void main(String[] args) throws PolicyDataManagerException {
        String str = "<test/>";
        String str2 = "<testing/>";

        pdm = new DbXmlPolicyDataManager();

        pdm.addPolicy(str, "test-nish");
        byte[] docb = pdm.getPolicy("test-nish");
        System.out.println(new String(docb));

        pdm.updatePolicy("test-nish", str2);
        docb = pdm.getPolicy("test-nish");
        System.out.println(new String(docb));

        pdm.deletePolicy("test-nish");
    }
}
