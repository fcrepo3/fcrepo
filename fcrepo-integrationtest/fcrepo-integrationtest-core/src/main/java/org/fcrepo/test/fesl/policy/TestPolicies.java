
package org.fcrepo.test.fesl.policy;

import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.JUnit4TestAdapter;

import org.fcrepo.common.Constants;

import org.fcrepo.test.FedoraServerTestCase;
import org.fcrepo.test.fesl.util.AuthorizationDeniedException;
import org.fcrepo.test.fesl.util.FedoraUtil;
import org.fcrepo.test.fesl.util.HttpUtils;
import org.fcrepo.test.fesl.util.LoadDataset;
import org.fcrepo.test.fesl.util.PolicyUtils;
import org.fcrepo.test.fesl.util.RemoveDataset;


public class TestPolicies extends FedoraServerTestCase implements Constants {

    private static final Logger logger =
            LoggerFactory.getLogger(TestPolicies.class);

    private static final String PROPERTIES = "fedora";

    private static HttpUtils httpUtils = null;

    //private FedoraAPIM apim = null;
    private PolicyUtils policyUtils = null;

    //private static PolicyStoreService polMan = null; // was: PolicyStore

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestPolicies.class);
    }

    @Override
    public void setUp() {

        PropertyResourceBundle prop =
                (PropertyResourceBundle) ResourceBundle.getBundle(PROPERTIES);
        String username = prop.getString("fedora.admin.username");
        String password = prop.getString("fedora.admin.password");
        //String fedoraUrl = prop.getString("fedora.url");
        String fedoraUrl = FedoraUtil.getBaseURL();



        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Setting up...");
            }

            policyUtils = new PolicyUtils(getFedoraClient());


            //PolicyStoreFactory f = new PolicyStoreFactory();
            //polMan = f.newPolicyStore();
            //polMan = new PolicyStoreService();


            httpUtils = new HttpUtils(fedoraUrl, username, password);

            // Load the admin policy to give us rights to add objects
            // FIXME: redundant, bootstrap policies will allow this
            String policyId = policyUtils.addPolicy("test-access-admin.xml");

            LoadDataset.load("fesl", fedoraUrl, username, password);

            // httpUtils.get("/fedora/risearch?flush=true");

            // Now that objects are loaded, remove the policy
            policyUtils.delPolicy(policyId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    @Override
    @After
    public void tearDown() {
        PropertyResourceBundle prop =
            (PropertyResourceBundle) ResourceBundle.getBundle(PROPERTIES);
        String username = prop.getString("fedora.admin.username");
        String password = prop.getString("fedora.admin.password");
        //String fedoraUrl = prop.getString("fedora.url");
        String fedoraUrl = FedoraUtil.getBaseURL();

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Tearing down...");
            }
            //PolicyStoreFactory f = new PolicyStoreFactory();
            //polMan = f.newPolicyStore();
            //polMan = new PolicyStoreService();

            // Load the admin policy to give us rights to remove objects
            String policyId = policyUtils.addPolicy("test-access-admin.xml");

            RemoveDataset.remove("fesl", fedoraUrl, username, password);

            // Now that objects are loaded, remove the policy
            policyUtils.delPolicy(policyId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testAdminGetDeny() throws Exception {
        // getting object test:1000007 but applying policy
        // to parent object (test:1000006) first

        String policyId = policyUtils.addPolicy("test-policy-00.xml");

        try {
            String url = "/fedora/objects/test:1000007?format=xml";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            // If we get here, we fail... should have thrown exception
            // PEP caching must be disabled (previously cached results will invalidate test)
            Assert.fail("Access was permitted when it should have been denied.  (Check that PEP_NOCACHE env variable is set to true)");
        } catch (AuthorizationDeniedException e) {
            // expected

        } catch (Exception e) {
            throw e;
        } finally {
            policyUtils.delPolicy(policyId);
        }
    }

    @Test
    public void testAdminGetPermit() throws Exception {
        // getting object test:1000007 but applying policy
        // to parent object (test:1000006) first

        String policyId = policyUtils.addPolicy("test-policy-01.xml");

        try {
            String url = "/fedora/objects/test:1000007?format=xml";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            boolean check = response.contains("<objLabel>Dexter</objLabel>");
            Assert.assertTrue("Expected object data not found", check);
        } catch (AuthorizationDeniedException e) {
            // PEP caching must be disabled (previously cached results will invalidate test)
            Assert.fail("Authorization denied.  (Check that PEP_NOCACHE env variable is set to true)");
        } catch (Exception e) {
            throw e;
        } finally {
            policyUtils.delPolicy(policyId);
        }
    }
/* --> see PolicyUtils
    private static String getPolicyId(byte[] data) throws Exception {
        Document doc = DataUtils.getDocumentFromBytes(data);
        String pid = doc.getDocumentElement().getAttribute("PolicyId");

        return pid;
    }

    private String addPolicy(String policyName) throws Exception {
        byte[] policy =
                DataUtils.loadFile(RESOURCEBASE + "/xacml/" + policyName);

        String policyId = getPolicyId(policy);

        String policyFile = "file:///" + (new File(RESOURCEBASE + "/xacml/" + policyName)).getAbsolutePath();

        // escape any pid namespace character
        if (policyId.contains(":")) {
            policyId = policyId.replace(":", "%3A");
        }

        String pid = "demo:" + policyId;

        StringBuilder foxml = new StringBuilder();

        // basic empty object

        foxml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        foxml.append("<foxml:digitalObject VERSION=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        foxml.append("    xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\"\n");
        foxml.append("           xsi:schemaLocation=\"" + Constants.FOXML.uri
                     + " " + Constants.FOXML1_1.xsdLocation + "\"");
        foxml.append("\n           PID=\"" + StreamUtility.enc(pid)
                         + "\">\n");
        foxml.append("  <foxml:objectProperties>\n");
        foxml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"A\"/>\n");
        foxml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#label\" VALUE=\""
                + StreamUtility.enc("test policy object") + "\"/>\n");
        foxml.append("  </foxml:objectProperties>\n");

        foxml.append("<foxml:datastream ID=\"" + FedoraPolicyStore.POLICY_DATASTREAM
                     + "\" CONTROL_GROUP=\"M\">");
        foxml.append("<foxml:datastreamVersion ID=\"POLICY.0\" MIMETYPE=\"text/xml\" LABEL=\"XACML policy datastream\">");

        foxml.append("  <foxml:contentLocation REF=\"" + policyFile
                     + "\" TYPE=\"URL\"/>");


        //foxml.append("  <foxml:xmlContent>");
        //foxml.append(policy);
        //foxml.append("    </foxml:xmlContent>");

        foxml.append("  </foxml:datastreamVersion>");
        foxml.append("</foxml:datastream>");


        foxml.append("</foxml:digitalObject>");

        apim.ingest(foxml.toString().getBytes("UTF-8"), FOXML1_1.uri,
                    "ingesting new foxml object");

        return policyId;
    }

    private void delPolicy(String policyId) throws Exception {
        String pid = "demo:" + policyId;
        apim.purgeObject(pid, "removing test policy object", false);

    }
    */
}
