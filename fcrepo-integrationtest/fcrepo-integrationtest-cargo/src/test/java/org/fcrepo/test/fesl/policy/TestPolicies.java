
package org.fcrepo.test.fesl.policy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

        // NOTE: system property fedora.fesl.pep_nocachevariable MUST be set to true to disable policy evaluation results caching

        try {
            // object
            try {
                String url = "/fedora/objects/test:1000007?format=xml";
                String response = httpUtils.get(url);
                logger.debug("http response:\n" + response);
                Assert.fail("Access was permitted when it should have been denied:  " + url);
            } catch (AuthorizationDeniedException e) { } // expected
            // list datastreams
            try {
                String url = "/fedora/objects/test:1000007/datastreams";
                String response = httpUtils.get(url);
                logger.debug("http response:\n" + response);
                Assert.fail("Access was permitted when it should have been denied:  " + url);
            } catch (AuthorizationDeniedException e) { } // expected

            // datastream profile
            try {
                String url = "/fedora/objects/test:1000007/datastreams/DC";
                String response = httpUtils.get(url);
                logger.debug("http response:\n" + response);
                Assert.fail("Access was permitted when it should have been denied:  " + url);
            } catch (AuthorizationDeniedException e) { } // expected

            // datastream content
            try {
                String url = "/fedora/objects/test:1000007/datastreams/DC/content";
                String response = httpUtils.get(url);
                logger.debug("http response:\n" + response);
                Assert.fail("Access was permitted when it should have been denied:  " + url);
            } catch (AuthorizationDeniedException e) { } // expected

            // list methods
            try {
                String url = "/fedora/objects/test:1000007/methods";
                String response = httpUtils.get(url);
                logger.debug("http response:\n" + response);
                Assert.fail("Access was permitted when it should have been denied:  " + url);
            } catch (AuthorizationDeniedException e) { } // expected

            // get method content
            try {
                String url = "/fedora/objects/test:1000007/methods/fedora-system:3/viewDublinCore";
                String response = httpUtils.get(url);
                logger.debug("http response:\n" + response);
                Assert.fail("Access was permitted when it should have been denied:  " + url);
            } catch (AuthorizationDeniedException e) { } // expected

            // TODO: extend for all REST methods

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
            Assert.fail("Authorization denied.  (Check that system property fedora.fesl.pep_nocache is set to true)");
        } catch (Exception e) {
            throw e;
        } finally {
            policyUtils.delPolicy(policyId);
        }
    }

    /*
     * Tests based on resource attributes sourced via the resource index.
     * Note the attributes must be defined in the pdp/config/config-attribute-finder.xml.
     *
     * Attributes tested for are
     * - dc:subject
     * - object/datastream state
     *
     * See the test-objects.txt in the same directory as the test objects to see which
     * objects have which attributes.
     *
     * Each pair of policies tested in the individual tests implement the same access conditions,
     * but do so through different implementations; ie using different xacml resource attributes
     * declared in the FedoraRIattribute finder configuration file.
     *
     * The different xacml resource id attribute declarations exercise sourcing attributes using:
     * - simple Fedora relationships
     * - TQL queries
     * - SPARQL queries
     * - SPO queries
     *
     */

    @Test
    public void testRIAttributesRels1() throws Exception {
        doAttributesTest("test-policy-state-rel1.xml", "test-policy-subject-rel1.xml");
    }

    @Test
    public void testRIAttributesRels2() throws Exception {
        doAttributesTest("test-policy-state-rel2.xml", "test-policy-subject-rel2.xml");
    }

    @Test
    public void testRIAttributesTQL() throws Exception {
        doAttributesTest("test-policy-state-itql.xml", "test-policy-subject-itql.xml");
    }

    @Test
    public void testRIAttributesSPARQL() throws Exception {
        doAttributesTest("test-policy-state-sparql.xml", "test-policy-subject-sparql.xml");
    }

    @Test
    public void testRIAttributesSPO() throws Exception {
        doAttributesTest("test-policy-state-spo.xml", "test-policy-subject-spo.xml");
    }

    private void doAttributesTest(String statePolicy, String subjectPolicy) throws Exception {

        String[] pidList;

        // A. object/datastream state

        // check permissions before adding policy
        PermissionTest perms = new PermissionTest(1000000, 1000012, "test", "DC");
        assertEquals("Allowed objects count (no policies)", perms.pidCount(), perms.object().allowed().size());
        assertEquals("Allowed DC datastreams count (no policies)", perms.pidCount(), perms.datastream().allowed().size());

        // load policy
        String policyId = policyUtils.addPolicy(statePolicy);
        try {
            perms = new PermissionTest(1000000, 1000012, "test", "DC");
            // objects that should be denied access
            pidList = new String[]{
                    "test:1000004",
                    "test:1000005",
                    "test:1000006",
                    "test:1000007",
                    "test:1000008",
                    "test:1000009"
            };
            assertEquals(statePolicy + ": Access denied for objects", "", perms.object().denied().mismatch(pidList));
            // datastreams that should be denied access
            pidList = new String[]{
                    "test:1000008",
                    "test:1000009",
                    "test:1000010",
                    "test:1000011",
                    "test:1000012"
            };
            assertEquals(statePolicy + ": Access denied for datastreams", "", perms.datastream().denied().mismatch(pidList));
        } finally {
            policyUtils.delPolicy(policyId);
        }

        // B. dc:subject attributes

        // check permissions before adding policy
        perms = new PermissionTest(1000000, 1000012, "test", "DC");
        assertEquals("Allowed objects count (no policies)", perms.pidCount(), perms.object().allowed().size());
        assertEquals("Allowed DC datastreams count (no policies)", perms.pidCount(), perms.datastream().allowed().size());

        // load policy
        policyId = policyUtils.addPolicy(subjectPolicy);
        try {
            perms = new PermissionTest(1000000, 1000012, "test", "DC");
            // objects that should be allowed access (deny=if pid divisible by two and/or three by using dc:subject attrs that indicate this)
            pidList = new String[]{
                    "test:1000001",
                    "test:1000003",
                    "test:1000007",
                    "test:1000009"
            };
            assertEquals(subjectPolicy + ": Access allowed for objects", "", perms.object().allowed().mismatch(pidList));
            // same for datastream access, as subject attribute is retrieved for the object, not for the datastream
            pidList = new String[]{
                    "test:1000001",
                    "test:1000003",
                    "test:1000007",
                    "test:1000009"
            };
            assertEquals(subjectPolicy + ": Access allowed for objects", "", perms.datastream().allowed().mismatch(pidList));
        } finally {
            policyUtils.delPolicy(policyId);
        }
    }


    // utility class for performing object and datastream access tests on a range of pids
    // representing the results in an [object | datastream] / [allowed | denied] / set of matching pids
    // structure
    class PermissionTest {
        private final EntityPerms m_object = new EntityPerms();
        private final EntityPerms m_datastream = new EntityPerms();
        private final int m_first;
        private final int m_last;
        private final String m_pidns;
        private final String m_dsid;

        PermissionTest(int first, int last, String pidNamespace, String dsid) throws Exception {
            m_first = first;
            m_last = last;
            m_pidns = pidNamespace;
            m_dsid = dsid;
            for (int i = m_first; i <= m_last; i++) {
                String pid = m_pidns + ":" + i;

                // test object access
                String url = "/fedora/objects/" + pid + "?format=xml";
                try {
                    String response = httpUtils.get(url);
                    // if we got here, it was allowed, so...
                    m_object.allowed().add(pid);
                } catch (AuthorizationDeniedException e) {
                    // access was denied
                    m_object.denied().add(pid);
                }
                // test datastream access
                if (!m_dsid.equals("")) {
                    url = "/fedora/objects/" + pid + "/datastreams/" + m_dsid + "?format=xml";
                    try {
                        String response = httpUtils.get(url);
                        // if we got here, it was allowed, so...
                        m_datastream.allowed().add(pid);
                    } catch (AuthorizationDeniedException e) {
                        // access was denied
                        m_datastream.denied().add(pid);
                    }
                }
            }
            // sanity check - allowed + denied = number of objects tested
            if (m_object.allowed().size() + m_object.denied().size() != pidCount()) {
                fail("Error in checking permissions - total of allowed and denied objects does not equal number of objects tested");
                throw new RuntimeException("Should not happen");
            }
            if (!m_dsid.equals("")) {
                if (m_datastream.allowed().size() + m_datastream.denied().size() != pidCount()) {
                    fail("Error in checking permissions - total of allowed and denied datastreams does not equal number of object datastreams tested");
                    throw new RuntimeException("Also should not happen");
                }
            }
        }

        public EntityPerms object() {
            return m_object;
        }
        public EntityPerms datastream() {
            return m_datastream;
        }
        public int pidCount() {
            return m_last - m_first + 1;
        }

        // holds two sets of pids, one for objects for which access was allowed, one for denied
        class EntityPerms {
            private final Perms m_allowed = new Perms();
            private final Perms m_denied = new Perms();

            public Perms allowed() {
                return m_allowed;
            }
            public Perms denied() {
                return m_denied;
            }

            // holds a set of pids with utility method for comparing to string array
            class Perms extends HashSet<String>{
                private static final long serialVersionUID =
                        3747931619024146008L;

                public boolean containsAll(String[] items) {
                    return this.containsAll(Arrays.asList(items));
                }
                public boolean containsOnly(String[] items) {
                    return ((this.size() == items.length) && containsAll(items));
                }
                /*
                 * returns a string representation of difference between the set members and the supplied array.
                 * Return empty string "" if items match
                 */
                public String mismatch(String[] items) {
                    String res = "";
                    if (containsOnly(items))
                        return res; // they match

                    // expected items not present in this set
                    res += "Expected permission not found for: [ ";
                    for (String item : items) {
                        if (!contains(item))
                            res += item + " ";
                    }
                    res += "]. ";

                    // items in set not present in supplied array
                    res += "Permission found but not expected for: [ ";
                    List<String> asList = Arrays.asList(items);
                    for (String item : this) {
                        if (!asList.contains(item))
                            res += item + " ";
                    }
                    res += "].";
                    return res;
                }
            }
        }
    }
}
