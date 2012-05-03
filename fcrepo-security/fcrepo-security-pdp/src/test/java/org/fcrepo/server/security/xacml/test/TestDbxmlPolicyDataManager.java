
package org.fcrepo.server.security.xacml.test;

import org.fcrepo.server.security.xacml.pdp.data.DbXmlPolicyIndex;
import org.fcrepo.server.security.xacml.pdp.data.PolicyIndex;
import org.fcrepo.server.security.xacml.pdp.data.PolicyIndexException;

import com.sun.xacml.AbstractPolicy;

public class TestDbxmlPolicyDataManager {

    private static PolicyIndex pdm = null;

    /**
     * @param args
     * @throws PolicyIndexException
     */
    public static void main(String[] args) throws PolicyIndexException {
        String str = "<test/>";
        String str2 = "<testing/>";

        pdm = new DbXmlPolicyIndex(null);

        pdm.addPolicy(str, "test-nish");
        AbstractPolicy docb = pdm.getPolicy("test-nish", null);
        docb.encode(System.out);

        pdm.updatePolicy("test-nish", str2);
        docb = pdm.getPolicy("test-nish", null);
        docb.encode(System.out);

        pdm.deletePolicy("test-nish");
    }
}
