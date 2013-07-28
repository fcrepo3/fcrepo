
package org.fcrepo.test.fesl.policy;

import static junit.framework.Assert.assertTrue;

import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.JUnit4TestAdapter;

import org.fcrepo.client.FedoraClient;
import org.fcrepo.common.Constants;

import org.fcrepo.test.FedoraServerTestCase;
import org.fcrepo.test.fesl.util.AuthorizationDeniedException;
import org.fcrepo.test.fesl.util.FedoraUtil;
import org.fcrepo.test.fesl.util.HttpUtils;
import org.fcrepo.test.fesl.util.LoadDataset;
import org.fcrepo.test.fesl.util.PolicyUtils;
import org.fcrepo.test.fesl.util.RemoveDataset;

/**
 * Tests hierarchical path-based resource-id (XACML hierarchical resource profile)
 *
 * Objects are related through a series of parent-child and child-parent relationships
 *
 * Test policy is set at the bottom object level using a full path to that object, ie
 * /test:1000000/test:1000001/test:1000002/test:1000003/test:1000004
 *
 * So policy will only apply correctly if the full hierarchical resource id is
 * correctly constructed from the relationships.
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class TestHierarchy extends FedoraServerTestCase implements Constants {

    private static final Logger logger =
            LoggerFactory.getLogger(TestHierarchy.class);

    private static final String PROPERTIES = "fedora";

    private HttpUtils httpUtils = null;

    //private FedoraAPIM apim = null;
    private PolicyUtils policyUtils = null;

    //private static PolicyStoreService polMan = null; // was: PolicyStore

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestHierarchy.class);
    }

    @Before
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

            FedoraClient client = getFedoraClient();
            policyUtils = new PolicyUtils(client);
            client.shutdown();


            //PolicyStoreFactory f = new PolicyStoreFactory();
            //polMan = f.newPolicyStore();
            //polMan = new PolicyStoreService();


            httpUtils = new HttpUtils(fedoraUrl, username, password);

            // Load the admin policy to give us rights to add objects
            // FIXME: redundant, bootstrap policies will allow this
            String policyId = policyUtils.addPolicy("test-access-admin.xml");

            LoadDataset.load("fesl-hierarchy", fedoraUrl, username, password);

            // httpUtils.get("/fedora/risearch?flush=true");

            // Now that objects are loaded, remove the policy
            policyUtils.delPolicy(policyId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

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

            RemoveDataset.remove("fesl-hierarchy", fedoraUrl, username, password);

            // Now that objects are loaded, remove the policy
            policyUtils.delPolicy(policyId);
            httpUtils.shutdown();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testHierarchicalResourceID() throws Exception {

        // first check we have access to test:1000004
        String url = "/fedora/objects/test:1000004?format=xml";
        String response = httpUtils.get(url);
        assertTrue("No access to test:1000004", response.contains("Chuck Versus the First Date"));

        // policy denies access using full resource-id hierarchical path
        String policyId = policyUtils.addPolicy("test-policy-H.xml");

        // check no access
        url = "/fedora/objects/test:1000004?format=xml";
        try {
            response = httpUtils.get(url);
            // should have thrown auth exception
            Assert.fail("Access was permitted to test:1000004 when it should have been denied");

        } catch (AuthorizationDeniedException e) {
            // expected
        } finally {
            policyUtils.delPolicy(policyId);
        }
    }

}
