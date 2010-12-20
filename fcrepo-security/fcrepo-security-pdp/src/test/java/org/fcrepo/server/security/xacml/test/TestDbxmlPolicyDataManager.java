
package org.fcrepo.server.security.xacml.test;

import org.fcrepo.server.security.xacml.pdp.data.PolicyStore;
import org.fcrepo.server.security.xacml.pdp.data.PolicyStoreException;
import org.fcrepo.server.security.xacml.pdp.data.PolicyStoreFactory;

public class TestDbxmlPolicyDataManager {

    private static PolicyStore pdm = null;

    /**
     * @param args
     */
    public static void main(String[] args) throws PolicyStoreException {
        String str = "<test/>";
        String str2 = "<testing/>";

        PolicyStoreFactory f = new PolicyStoreFactory();
        pdm = f.newPolicyStore();

        pdm.addPolicy(str, "test-nish");
        byte[] docb = pdm.getPolicy("test-nish");
        System.out.println(new String(docb));

        pdm.updatePolicy("test-nish", str2);
        docb = pdm.getPolicy("test-nish");
        System.out.println(new String(docb));

        pdm.deletePolicy("test-nish");
    }
}
