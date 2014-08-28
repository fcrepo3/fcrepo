
package org.fcrepo.test.fesl.policy;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TestPolicies extends FedoraServerTestCase implements Constants {

    private static final Logger logger =
            LoggerFactory.getLogger(TestPolicies.class);

    private static final String PROPERTIES = "fedora";
    
    private static FedoraClient s_client;

    private HttpUtils httpUtils = null;
    
    private static String ri_impl;
    
    //private FedoraAPIM apim = null;
    private PolicyUtils policyUtils = null;

    //private static PolicyStoreService polMan = null; // was: PolicyStore

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestPolicies.class);
    }
    
    @BeforeClass
    public static void bootStrap() throws Exception {
        s_client = getFedoraClient();
        ri_impl = getRIImplementation();
    }
    
    @AfterClass
    public static void cleanUp() {
        s_client.shutdown();
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

            policyUtils = new PolicyUtils(s_client);


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
            fail(e.getMessage());
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
            httpUtils.shutdown();
            //PolicyStoreFactory f = new PolicyStoreFactory();
            //polMan = f.newPolicyStore();
            //polMan = new PolicyStoreService();

            // Load the admin policy to give us rights to remove objects
            String policyId = policyUtils.addPolicy("test-access-admin.xml");

            RemoveDataset.remove("fesl", fedoraUrl, username, password);

            // Now that objects are loaded, remove the policy
            policyUtils.delPolicy(policyId);
            httpUtils.shutdown();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
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
                fail("Access was permitted when it should have been denied:  " + url);
            } catch (AuthorizationDeniedException e) { } // expected
            // list datastreams
            try {
                String url = "/fedora/objects/test:1000007/datastreams";
                String response = httpUtils.get(url);
                logger.debug("http response:\n" + response);
                fail("Access was permitted when it should have been denied:  " + url);
            } catch (AuthorizationDeniedException e) { } // expected

            // datastream profile
            try {
                String url = "/fedora/objects/test:1000007/datastreams/DC";
                String response = httpUtils.get(url);
                logger.debug("http response:\n" + response);
                fail("Access was permitted when it should have been denied:  " + url);
            } catch (AuthorizationDeniedException e) { } // expected

            // datastream content
            try {
                String url = "/fedora/objects/test:1000007/datastreams/DC/content";
                String response = httpUtils.get(url);
                logger.debug("http response:\n" + response);
                fail("Access was permitted when it should have been denied:  " + url);
            } catch (AuthorizationDeniedException e) { } // expected

            // list methods
            try {
                String url = "/fedora/objects/test:1000007/methods";
                String response = httpUtils.get(url);
                logger.debug("http response:\n" + response);
                fail("Access was permitted when it should have been denied:  " + url);
            } catch (AuthorizationDeniedException e) { } // expected

            // get method content
            try {
                String url = "/fedora/objects/test:1000007/methods/fedora-system:3/viewDublinCore";
                String response = httpUtils.get(url);
                logger.debug("http response:\n" + response);
                fail("Access was permitted when it should have been denied:  " + url);
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
            assertTrue("Expected object data not found", check);
        } catch (AuthorizationDeniedException e) {
            // PEP caching must be disabled (previously cached results will invalidate test)
            fail("Authorization denied.  (Check that system property fedora.fesl.pep_nocache is set to true)");
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
        /* skip if MPTTriplestore implementation */
        Assume.assumeTrue(! "localPostgresMPTTriplestore".equals(ri_impl));
        
        doAttributesTest("test-policy-state-itql.xml", "test-policy-subject-itql.xml");
    }

    @Test
    public void testRIAttributesSPARQL() throws Exception {
        /* skip if MPTTriplestore implementation */
        Assume.assumeTrue(! "localPostgresMPTTriplestore".equals(ri_impl));
        
        doAttributesTest("test-policy-state-sparql.xml", "test-policy-subject-sparql.xml");
    }

    @Test
    public void testRIAttributesSPO() throws Exception {
        doAttributesTest("test-policy-state-spo.xml", "test-policy-subject-spo.xml");
    }

    private void doAttributesTest(String statePolicy, String subjectPolicy) throws Exception {


        // A. object/datastream state
        String [] pids = new String[]{
                "test:1000000",
                "test:1000001",
                "test:1000002",
                "test:1000003",
                "test:1000004",
                "test:1000005",
                "test:1000006",
                "test:1000007",
                "test:1000008",
                "test:1000009",
                "test:1000010",
                "test:1000011",
                "test:1000012"
        };
        Perms allPids = new Perms();
        allPids.addAll(Arrays.asList(pids));

        // check permissions before adding policy
        PermissionTest perms = new PermissionTest(1000000, 1000012, "test", "DC");
        assertEquals("Allowed objects count (no policies)", 0, perms.object().allowed().mismatch(pids,true).length);
        assertEquals("Allowed DC datastreams count (no policies)", 0, perms.datastream().allowed().mismatch(pids,true).length);

        // load policy
        String policyId = policyUtils.addPolicy(statePolicy);
        try {
            perms = new PermissionTest(1000000, 1000012, "test", "DC");
            // objects that should be denied access
            String [] denied = new String[]{
                    "test:1000004",
                    "test:1000005",
                    "test:1000006",
                    "test:1000007",
                    "test:1000008",
                    "test:1000009"
            };
            String [] deniedVideos = new String[]{
                    "test:1000000", // not a video
                    "test:1000004",
                    "test:1000005",
                    "test:1000006",
                    "test:1000007",
                    "test:1000008",
                    "test:1000009"
            };
            String [] allowed = allPids.mismatch(denied, false);
            String [] mismatches = perms.object().denied().mismatch(denied, false);
            assertEquals(getAccessErrorMessage(subjectPolicy, "objects", "denied", mismatches), 0, mismatches.length);
            allowed = allPids.mismatch(deniedVideos, false);
            mismatches = perms.object().listed().mismatch(allowed, true);
            assertEquals(getAccessErrorMessage(subjectPolicy, "objects", "listed", mismatches), 0, mismatches.length);
            // datastreams that should be denied access
            denied = new String[]{
                    "test:1000008",
                    "test:1000009",
                    "test:1000010",
                    "test:1000011",
                    "test:1000012"
            };
            mismatches = perms.datastream().denied().mismatch(denied, true);
            assertEquals(getAccessErrorMessage(subjectPolicy, "datastreams", "denied", mismatches), 0, mismatches.length);
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
            String [] allowed = new String[]{
                    "test:1000001",
                    "test:1000003",
                    "test:1000007",
                    "test:1000009"
            };
            String [] mismatches = perms.object().allowed().mismatch(allowed, true);
            assertEquals(getAccessErrorMessage(subjectPolicy, "objects", "allowed", mismatches), 0, mismatches.length);
            mismatches = perms.object().listed().mismatch(allowed, true);
            assertEquals(getAccessErrorMessage(subjectPolicy, "objects", "listed", mismatches), 0, mismatches.length);
            // same for datastream access, as subject attribute is retrieved for the object, not for the datastream
            mismatches = perms.datastream().allowed().mismatch(allowed, true);
            assertEquals(getAccessErrorMessage(subjectPolicy, "datastreams", "allowed", mismatches), 0, mismatches.length);
        } finally {
            policyUtils.delPolicy(policyId);
        }
    }

    private static String getAccessErrorMessage(String subjectPolicy, String type, String verb, String[] mismatches){
        StringBuilder sb = new StringBuilder();
        sb.append(subjectPolicy).append(": Access ").append(verb).append(" for ").append(type).append("[");
        for (int i=0; i<mismatches.length; i++){
            sb.append(mismatches[i]);
            if (i < mismatches.length - 1) sb.append(',');
        }
        sb.append(']');
        return sb.toString();

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
                    httpUtils.get(url);
                    // if we got here, it was allowed, so...
                    m_object.allowed().add(pid);
                } catch (AuthorizationDeniedException e) {
                    // access was denied
                    m_object.denied().add(pid);
                }
                // test datastream access
                if (!m_dsid.isEmpty()) {
                    url = "/fedora/objects/" + pid + "/datastreams/" + m_dsid + "?format=xml";
                    try {
                        httpUtils.get(url);
                        // if we got here, it was allowed, so...
                        m_datastream.allowed().add(pid);
                    } catch (AuthorizationDeniedException e) {
                        // access was denied
                        m_datastream.denied().add(pid);
                    }
                    // Now check that the datastreams are being filtered correctly from listing
                    url = "/fedora/objects?resultFormat=xml&query=type%7Evideo";
                    String response = httpUtils.get(url);
                    Matcher matcher = Pattern.compile("<pid>(.*)<\\/pid>").matcher(response);
                    while(matcher.find()){
                        m_object.listed().add(matcher.group(1));
                    }
                }
            }
            // sanity check - allowed + denied = number of objects tested
            if (m_object.allowed().size() + m_object.denied().size() != pidCount()) {
                fail("Error in checking permissions - total of allowed and denied objects does not equal number of objects tested");
                throw new RuntimeException("Should not happen");
            }
            if (!m_dsid.isEmpty()) {
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
    }

    // holds two sets of pids, one for objects for which access was allowed, one for denied
    class EntityPerms {
        private final Perms m_allowed = new Perms();
        private final Perms m_denied = new Perms();
        private final Perms m_listed = new Perms();

        public Perms allowed() {
            return m_allowed;
        }
        public Perms denied() {
            return m_denied;
        }
        public Perms listed() {
            return m_listed;
        }
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
        public boolean containsAny(String [] items) {
            boolean result = false;
            for (String item:items) { result |= contains(item);}
            return result;
        }
        /*
         * returns a string representation of difference between the set members and the supplied array.
         * Return empty string "" if items match
         */
        public String[] mismatch(String[] items, boolean indicate) {
            ArrayList<String> res = new ArrayList<String>();
            if (containsOnly(items))
                return res.toArray(new String[0]); // they match

            // expected items not present in this set
            for (String item : items) {
                if (!contains(item)){
                    if (indicate) res.add("-" + item);
                    else res.add(item);
                }
            }

            // items in set not present in supplied array
            List<String> asList = Arrays.asList(items);
            for (String item : this) {
                if (!asList.contains(item)){
                    if (indicate) res.add("+" + item);
                    else res.add(item);
                }
            }
            return res.toArray(new String[0]);
        }
    }
}
