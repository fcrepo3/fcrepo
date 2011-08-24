package org.fcrepo.test.fesl.policyindex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.util.ArrayList;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.apache.http.client.ClientProtocolException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.JUnit4TestAdapter;

import org.fcrepo.client.FedoraClient;

import org.fcrepo.common.Constants;

import org.fcrepo.server.management.FedoraAPIMMTOM;
import org.fcrepo.server.security.servletfilters.xmluserfile.FedoraUsers;
import org.fcrepo.server.security.xacml.pdp.data.FedoraPolicyStore;
import org.fcrepo.server.types.mtom.gen.Datastream;
import org.fcrepo.server.utilities.StreamUtility;
import org.fcrepo.server.utilities.TypeUtility;

import org.fcrepo.test.FedoraServerTestCase;
import org.fcrepo.test.fesl.util.AuthorizationDeniedException;
import org.fcrepo.test.fesl.util.HttpUtils;
import org.fcrepo.test.fesl.util.LoadDataset;
import org.fcrepo.test.fesl.util.RemoveDataset;

/**
 * Testing of the PolicyIndex
 *
 * Tests that modifications of policies as fedora objects with
 * FESLPOLICY datastreams are correctly propagated to the PolicyIndex
 *
 * Tests use various API methods to modify objects and datastreams, and verify
 * that the policy is correctly enforced (or not)
 *
 * Policies and objects, users and roles
 *
 * test-policy-A.xml gives access to members of collection test:1000001
 *   tests check for access to test:1000002
 *   user is testuser/testuser, role is testing
 *
 * test-policy-B.xml gives access to members of collection test:1000006
 *   tests check for access to test:1000007
 *   user is testuser/testuser, role is testing
 *
 * Note: as policies are included in foxml as inline (X) datastreams, don't include
 * any processing instructions (eg XML header) in the XACML policy files.
 *
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class TestPolicyIndex extends FedoraServerTestCase implements Constants {

    private static final Logger logger =
            LoggerFactory.getLogger(TestPolicyIndex.class);

    private static final String PROPERTIES = "fedora";

    // nb, for testing access, don't initiate with fedora admin credentials
    private static HttpUtils httpUtils = null;

    private FedoraAPIMMTOM apim = null;

    private PolicyIndexUtils policyIndexUtils = null;

    private static String POLICY_DATASTREAM = FedoraPolicyStore.FESL_POLICY_DATASTREAM;

    //private PolicyUtils policyUtils = null;

    private File fedoraUsersBackup = null;

    private String username = null;
    private String password = null;
    private String fedoraUrl = null;

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestPolicyIndex.class);
    }

    @Override
    public void setUp() {

        PropertyResourceBundle prop =
            (PropertyResourceBundle) ResourceBundle.getBundle(PROPERTIES);
        username = prop.getString("fedora.admin.username");
        password = prop.getString("fedora.admin.password");

        // this is the https URL, for REST API-M calls
        fedoraUrl = getProtocol() + "://" + getHost() + ":" + getPort() + "/" + getFedoraAppServerContext();

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Setting up...");
            }

            // create fedora users file with test user : testuser/testuser, role testing
            createFedoraUsersTestFile();

            //policyUtils = new PolicyUtils(getFedoraClient());

            // used for access testing, with test credentials which must match the modified fedora-users.xml file created in createFedoraUsersTestFile()
            // nb, uses getBaseURL(), ie http not https (ssl not required for ConfigC/API-A)
            httpUtils = new HttpUtils(getBaseURL(), "testuser", "testuser");

            FedoraClient client = getFedoraClient();
            assertNotNull("FedoraTestCase.getFedoraClient() returned NULL", client);
            apim = client.getAPIM();

            policyIndexUtils = new PolicyIndexUtils(apim);

            LoadDataset.load("fesl", fedoraUrl, username, password);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    @Override
    @After
    public void tearDown() {

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Tearing down...");
            }

            // restore the fedora users original from backup
            restoreFedoraUsersFile();

            RemoveDataset.remove("fesl", fedoraUrl, username, password);

            // policies are in demo namespace
            purgeDemoObjects();

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Overwrite existing fedora users file with one containing user testuser, role testing
     * backing up original fedora users file
     */
    private void createFedoraUsersTestFile() {
        System.out.println("Creating test Fedora Users file");
        // backup existing
        backupFedoraUsersFile();

        String sep = System.getProperty("line.seperator");
        if (sep == null) {
            sep = "\n";
        }

        if (!fedoraUsersBackup.exists()) {
            throw new RuntimeException("Fedora Users backup file expected, but none present");
        }

        // get existing file and add test user and role
        FileInputStream fis;
        try {
            // nb from backup file, which is only ever backed-up once, to avoid re-patching a patched users file
            fis = new FileInputStream(fedoraUsersBackup);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            boolean addedUser = false;
            String usersTag = "<users>";

            // read through each line in turn, when we find the <users> tag add the new user, then copy the rest of the file
            String inp;
            StringBuilder data = new StringBuilder();
            while ((inp = br.readLine()) != null) {
                if (!addedUser && inp.contains(usersTag)) {
                    // found start of users section
                    // add this...
                    data.append(inp + sep);
                    // and the test user
                    data.append("<user name=\"testuser\" password=\"testuser\">" + sep);
                    data.append("<attribute name=\"fedoraRole\">" + sep);
                    data.append("<value>testing</value>" + sep);
                    data.append("</attribute>" + sep);
                    data.append("</user>" + sep);
                    addedUser = true;

                } else {
                    data.append(inp + sep);
                }
            }

            // overwrite existing with new version
            FileOutputStream fu =
                new FileOutputStream(FedoraUsers.fedoraUsersXML);
            OutputStreamWriter pw = new OutputStreamWriter(fu);
            pw.write(data.toString());
            pw.close();
        } catch (IOException e) {
            System.out.println("Error generating test fedora-users.xml: " + e.getMessage());
            throw new RuntimeException(e);
        }

    }


    private void backupFedoraUsersFile() {
        fedoraUsersBackup =
                new File(FedoraUsers.fedoraUsersXML.getAbsolutePath()
                         + ".backup-fesl");
        if (!fedoraUsersBackup.exists()) {
            System.out.println("Backing Up Fedora Users");
            try {
                fedoraUsersBackup.createNewFile();
                copyFile(FedoraUsers.fedoraUsersXML, fedoraUsersBackup);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void restoreFedoraUsersFile() {
        if (!fedoraUsersBackup.exists()) {
            System.out.println("Error - Fedora Users backup file does not exist");
        } else {
            System.out.println("Restoring Fedora Users");
            copyFile(fedoraUsersBackup, FedoraUsers.fedoraUsersXML);
        }
    }

    private boolean copyFile(File src, File dest) {
        InputStream in;
        try {
            in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);
            StreamUtility.pipeStream(in, out, 1024);
            in.close();
            out.close();
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Test
    public void testObjectMethods() throws Exception {

        String pid, pidA, pidB;

        // add non-policy object
        pid = policyIndexUtils.addPolicyObject("X", "A", null);

        // check policies not in force
        assertFalse("authorization \"A\" PERMITTED, expected DENIED",checkPolicyEnforcement("A"));
        assertFalse("authorization \"B\" PERMITTED, expected DENIED",checkPolicyEnforcement("B"));

        // add policy objects
        pidA =  policyIndexUtils.addPolicyObject("A", "A", "A");
        pidB =  policyIndexUtils.addPolicyObject("B", "A", "A");

        // check policies in force
        assertTrue("authorization \"A\" DENIED, expected PERMITTED",checkPolicyEnforcement("A"));
        assertTrue("authorization \"B\" DENIED, expected PERMITTED",checkPolicyEnforcement("B"));

        // purge non-policy object
        apim.purgeObject(pid, "", false);

        // check policies in force
        assertTrue("authorization \"A\" DENIED, expected PERMITTED",checkPolicyEnforcement("A"));
        assertTrue("authorization \"B\" DENIED, expected PERMITTED",checkPolicyEnforcement("B"));

        // purge policy A object
        apim.purgeObject(pidA, "", false);

        // check policy A not in force
        assertFalse("authorization \"A\" PERMITTED, expected DENIED",checkPolicyEnforcement("A"));

        // check policy B in force
        assertTrue("authorization \"B\" DENIED, expected PERMITTED",checkPolicyEnforcement("B"));

        // purge policy B object
        apim.purgeObject(pidB, "", false);

        // check policies not in force
        assertFalse("authorization \"B\" PERMITTED, expected DENIED",checkPolicyEnforcement("B"));

    }

    @Test
    public void testStateChanges() throws Exception {

        String pidA, pidB;

        // add policy objects
        assertFalse(checkPolicyEnforcement("A"));
        assertFalse(checkPolicyEnforcement("B"));
        pidA =  policyIndexUtils.addPolicyObject("A", "A", "A");
        pidB =  policyIndexUtils.addPolicyObject("B", "A", "A");
        assertTrue(checkPolicyEnforcement("A"));
        assertTrue(checkPolicyEnforcement("B"));

        // set object state to NULL - FCREPO-820
        apim.modifyObject(pidA, null, "updated label", null, "updating label");

        // check policy A in force
        assertTrue(checkPolicyEnforcement("A"));
        // and B in force
        assertTrue(checkPolicyEnforcement("B"));

        // set object state to inactive
        apim.modifyObject(pidA, "I", null, null, "set inactive");

        // check policy A not in force
        assertFalse(checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue(checkPolicyEnforcement("B"));

        // set object state to NULL - FCREPO-820
        apim.modifyObject(pidA, null, "updated label", null, "updating label");

        // check policy not in force
        assertFalse(checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue(checkPolicyEnforcement("B"));

        // set objects state to deleted
        apim.modifyObject(pidA, "D", null , null, "set deleted");

        // check policy not in force
        assertFalse(checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue(checkPolicyEnforcement("B"));

        // set object state to active
        apim.modifyObject(pidA, "A", null , null, "set active");

        // check policy A in force
        assertTrue(checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue(checkPolicyEnforcement("B"));

        // set object state to deleted
        apim.modifyObject(pidA, "D", null , null, "set deleted");

        // check policyA not in force
        assertFalse(checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue(checkPolicyEnforcement("B"));

        // set object state to active
        apim.modifyObject(pidA, "A", null , null, "set active");

        // check policy A in force
        assertTrue(checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue(checkPolicyEnforcement("B"));


        // purge object A
        apim.purgeObject(pidA, "purging policy A", false);

        // check policy A not in force
        assertFalse(checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue(checkPolicyEnforcement("B"));

        // add policy A object with object state inactive
        pidA =  policyIndexUtils.addPolicyObject("A", "I", "A");

        // check policy A not in force
        assertFalse(checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue(checkPolicyEnforcement("B"));

        // set object state to NULL - FCREPO-820
        apim.modifyObject(pidA, null, "updated label", null, "updated label");

        // check policy A not in force
        assertFalse(checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue(checkPolicyEnforcement("B"));

        // set object state to active
        apim.modifyObject(pidA, "A", null, null, "set active");

        // check policy A in force
        assertTrue("authorization \"A\" DENIED, expected PERMITTED",checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue("authorization \"B\" DENIED, expected PERMITTED",checkPolicyEnforcement("B"));

        // purge A
        apim.purgeObject(pidA, "purging A", false);

        // add policy object with object state and datastream state inactive
        pidA =  policyIndexUtils.addPolicyObject("A", "I", "I");

        // check policy A not in force
        assertFalse("authorization \"A\" PERMITTED, expected DENIED",checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue("authorization \"B\" DENIED, expected PERMITTED",checkPolicyEnforcement("B"));

        // set object state active
        apim.modifyObject(pidA, "A", null, null, "set active");

        // check policy A not in force (datastream still inactive)
        assertFalse("authorization \"A\" PERMITTED, expected DENIED",checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue("authorization \"B\" DENIED, expected PERMITTED",checkPolicyEnforcement("B"));

        // set datastream state active
        apim.setDatastreamState(pidA, POLICY_DATASTREAM, "A", "datastream active");

        // check policy A in force
        assertTrue("authorization \"A\" DENIED, expected PERMITTED",checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue("authorization \"B\" DENIED, expected PERMITTED",checkPolicyEnforcement("B"));

        // set datastream state inactive
        apim.setDatastreamState(pidA, POLICY_DATASTREAM, "I", "datastream inactive");

        // check policy A not in force
        assertFalse(checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue("authorization \"B\" DENIED, expected PERMITTED",checkPolicyEnforcement("B"));

        // set datastream state deleted
        apim.setDatastreamState(pidA, POLICY_DATASTREAM, "D", "datastream deleted");

        // check policy A not in force
        assertFalse(checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue("authorization \"B\" DENIED, expected PERMITTED",checkPolicyEnforcement("B"));

        // set datastream state active
        apim.setDatastreamState(pidA, POLICY_DATASTREAM, "A", "datastream active");

        // check policy A in force
        assertTrue("authorization \"A\" DENIED, expected PERMITTED",checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue("authorization \"B\" DENIED, expected PERMITTED",checkPolicyEnforcement("B"));

        // set datastream state deleted
        apim.setDatastreamState(pidA, POLICY_DATASTREAM, "D", "datastream deleted");

        // check policy A not in force
        assertFalse(checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue("authorization \"B\" DENIED, expected PERMITTED",checkPolicyEnforcement("B"));

        // set object state deleted
        apim.modifyObject(pidA, "D", null, null, "set inactive");

        // check policy A not in force
        assertFalse(checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue("authorization \"B\" DENIED, expected PERMITTED",checkPolicyEnforcement("B"));

        // set datastream state active
        apim.setDatastreamState(pidA, POLICY_DATASTREAM, "A", "datastream active");

        // check policy A not in force
        assertFalse(checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue("authorization \"B\" DENIED, expected PERMITTED",checkPolicyEnforcement("B"));

        // set object state active
        apim.modifyObject(pidA, "A", null, null, "set inactive");

        // check policy A in force
        assertTrue("authorization \"A\" DENIED, expected PERMITTED",checkPolicyEnforcement("A"));
        // and B still in force
        assertTrue("authorization \"B\" DENIED, expected PERMITTED",checkPolicyEnforcement("B"));

        // purge both
        apim.purgeObject(pidA, "", false);
        apim.purgeObject(pidB, "", false);
        assertFalse(checkPolicyEnforcement("A"));
        assertFalse(checkPolicyEnforcement("B"));


    }

    @Test
    public void testDatastreamMethods()  throws Exception {
        String pid;

        // add policy A object
        pid =  policyIndexUtils.addPolicyObject("A", "A", "A");

        // check policy A in force
        assertTrue(checkPolicyEnforcement("A"));

        // modify by value to invalid XACML, ignore errors
        try {
            apim.modifyDatastreamByValue(pid, POLICY_DATASTREAM, null, "policy datastream", "text/xml", null, TypeUtility.convertBytesToDataHandler("<not><valid/></not>".getBytes("UTF-8")), null, null, "modify to policy B", false);
            Assert.fail("FeSL policy datastream validation failure - should have rejected update and thrown an exception");
        } catch (Exception e) {
            System.out.println("Expected error occurred from invalid XACML - " + e.getMessage());
        }

        // check policy A still in force
        assertTrue("authorization \"A\" DENIED, expected PERMITTED",checkPolicyEnforcement("A"));

        // modify by value to policy B
        apim.modifyDatastreamByValue(pid, POLICY_DATASTREAM, null, "policy datastream", "text/xml", null,  TypeUtility.convertBytesToDataHandler(PolicyIndexUtils.getPolicy("B").getBytes("UTF-8")), null, null, "modify to policy B", false);

        // check policy B in force
        assertTrue("authorization \"B\" DENIED, expected PERMITTED",checkPolicyEnforcement("B"));

        // check policy A not in force
        assertFalse(checkPolicyEnforcement("A"));

        // modify by value to policy A (needed for version purge test, or will revert back to the invalid version pending FCREPO-770)
        apim.modifyDatastreamByValue(pid, POLICY_DATASTREAM, null, "policy datastream", "text/xml", null,  TypeUtility.convertBytesToDataHandler(PolicyIndexUtils.getPolicy("A").getBytes("UTF-8")), null, null, "modify to policy B", false);
        // check
        assertTrue("authorization \"A\" DENIED, expected PERMITTED",checkPolicyEnforcement("A"));
        assertFalse(checkPolicyEnforcement("B"));

        // purge latest datastream version
        Datastream ds = apim.getDatastream(pid, POLICY_DATASTREAM, null);
        apim.purgeDatastream(pid, POLICY_DATASTREAM, ds.getCreateDate(), ds.getCreateDate(), "purging latest version", false);

        // check reverted back to B in force, A not in force (ie prior version)
        assertTrue("authorization \"B\" DENIED, expected PERMITTED",checkPolicyEnforcement("B"));
        assertFalse(checkPolicyEnforcement("A"));

        // purge datastream
        apim.purgeDatastream(pid, POLICY_DATASTREAM, null, null, "purge FESLPOLICY", false);

        // check policy B and A not in force
        assertFalse(checkPolicyEnforcement("A"));
        assertFalse(checkPolicyEnforcement("B"));

        // add datastream  state inactive, policy A
        String pidTemp =  policyIndexUtils.addPolicyObject("A", "A", "A");
        apim.addDatastream(pid, POLICY_DATASTREAM, null, "FESL policy datastream", true, "text/xml", null, fedoraUrl + "/objects/" + pidTemp + "/datastreams/FESLPOLICY/content", "M", "I", null, null, "add policy datastream by reference");
        apim.purgeObject(pidTemp, "removing temp object", false);

        // check policy not in force
        assertFalse(checkPolicyEnforcement("A"));

        // set datastream active
        apim.setDatastreamState(pid, POLICY_DATASTREAM, "A", "FESLPOLICY set to active");

        // check policy A in force
        assertTrue("authorization \"A\" DENIED, expected PERMITTED",checkPolicyEnforcement("A"));

        // check policy B not in force
        assertFalse("authorization \"B\" PERMITTED, expected DENIED",checkPolicyEnforcement("B"));

        // add unrelated datastream
        apim.addDatastream(pid, "UNRELATED", null, "some datastream", true, "text/xml", null, fedoraUrl+ "/objects/test:1000001/datastreams/DC/content", "M", "A", null, null, "adding UNRELATED");

        // check policy A in force
        assertTrue("authorization \"A\" DENIED, expected PERMITTED",checkPolicyEnforcement("A"));

        // purge unrelated datastream (FCREPO-820)
        apim.purgeDatastream(pid, "UNRELATED", null, null, "purge UNRELATED", false);

        // check policy A in force
        assertTrue("authorization \"A\" DENIED, expected PERMITTED",checkPolicyEnforcement("A"));

        // purge datastream
        apim.purgeDatastream(pid, POLICY_DATASTREAM,null, null, "purge FESLPOLICY", false);

        // check policy not in force
        assertFalse(checkPolicyEnforcement("B"));

        // add policy A datastream by reference:
        // add Policy A datastream object (for content)
        // add datastream by reference, from this new object
        // purge the new object
        pidTemp =  policyIndexUtils.addPolicyObject("A", "A", "A");
        apim.addDatastream(pid, POLICY_DATASTREAM, null, "FESL policy datastream", true, "text/xml", null, fedoraUrl + "/objects/" + pidTemp + "/datastreams/FESLPOLICY/content", "M", "A", null, null, "add policy datastream by reference");
        apim.purgeObject(pidTemp, "removing temp object", false);

        // check policy A in force
        assertTrue("authorization \"A\" DENIED, expected PERMITTED",checkPolicyEnforcement("A"));

        // modify datastream by reference to policy B (as above, by reference from a temp object created then purged afterwards)
        pidTemp =  policyIndexUtils.addPolicyObject("B", "A", "A");
        apim.modifyDatastreamByReference(pid, POLICY_DATASTREAM, null, "FESL policy datastream", "text/xml", null, fedoraUrl + "/objects/" + pidTemp + "/datastreams/FESLPOLICY/content", null, null, "modiy FESLPOLICY to policy B", false);
        apim.purgeObject(pidTemp, "removing temp object", false);

        // check policy B in force
        assertTrue(checkPolicyEnforcement("B"));

        // check policy A not in force
        assertFalse(checkPolicyEnforcement("A"));

        // set datastream versionable off
        apim.setDatastreamVersionable(pid, POLICY_DATASTREAM, false, "");

        // check policy B in force
        assertTrue(checkPolicyEnforcement("B"));

        // modify datastream by reference to policy A - as per above
        pidTemp =  policyIndexUtils.addPolicyObject("A", "A", "A");
        apim.modifyDatastreamByReference(pid, POLICY_DATASTREAM, null, "FESL policy datastream", "text/xml", null, fedoraUrl + "/objects/" + pidTemp + "/datastreams/FESLPOLICY/content", null, null, "modiy FESLPOLICY to policy B", false);
        apim.purgeObject(pidTemp, "removing temp object", false);

        // check policy A in force
        assertTrue(checkPolicyEnforcement("A"));

        // check policy B not in force
        assertFalse(checkPolicyEnforcement("B"));

        // set datastream versionable on
        apim.setDatastreamVersionable(pid, POLICY_DATASTREAM, true, "");

        // modify datastream by reference to policy B
        pidTemp =  policyIndexUtils.addPolicyObject("B", "A", "A");
        apim.modifyDatastreamByReference(pid, POLICY_DATASTREAM, null, "FESL policy datastream", "text/xml", null, fedoraUrl + "/objects/" + pidTemp + "/datastreams/FESLPOLICY/content", null, null, "modiy FESLPOLICY to policy B", false);
        apim.purgeObject(pidTemp, "removing temp object", false);

        // check policy B in force
        assertTrue(checkPolicyEnforcement("B"));

        // check policy A not in force
        assertFalse(checkPolicyEnforcement("A"));


    }

    // some stress testing - concurrent adds, reads, deletes

    @Test
    public void testManyModifications() throws Exception {

        final int updatersCount = 5; // number of concurrent update threads
        final int updatersPolicyCount = 10; // number of policies each update thread adds/reads/deletes
        final int readersCount = 10; // number of concurrent read threads


        ArrayList<PolicyIndexExerciser> updaters = new ArrayList<PolicyIndexExerciser>();
        ArrayList<PolicyIndexExerciser> readers = new ArrayList<PolicyIndexExerciser>();


        // construct the updaters
        for (int e = 0; e < updatersCount; e++) {
            // get the pids
            String[] pids =  policyIndexUtils.getNextPids(updatersPolicyCount);
            PolicyIndexExerciser ex = new PolicyIndexExerciser(getBaseURL(),
                                                                 "testuser",
                                                                 "testuser",
                                                                 fedoraUrl,
                                                                 username,
                                                                 password,
                                                                 pids);
            updaters.add(ex);
        }
        // and the readers
        for (int e = 0; e < readersCount; e++) {
            // get the pids
            PolicyIndexExerciser ex = new PolicyIndexExerciser(getBaseURL(),
                                                                 "testuser",
                                                                 "testuser"
                                                                 );
            readers.add(ex);
        }

        // kick them all off, readers first
        for (PolicyIndexExerciser ex : readers) {
            ex.start();
        }
        for (PolicyIndexExerciser ex : updaters) {
            ex.start();
        }

        // wait until at least one updater has actually started
        int maxSleepSeconds = 20;
        while (PolicyIndexExerciser.updaterRunningCount() == 0) {
            Thread.sleep(1000);
            maxSleepSeconds--;
            if (maxSleepSeconds == 0)
                // serious problem if none of the threads have started
                Assert.fail("No threads have started");
        }

        // wait until they have all finished
        maxSleepSeconds = 60 * 10; // try for 10 mins, probably way too long...
        while (PolicyIndexExerciser.updaterRunningCount() > 0) {
            Thread.sleep(1000); // wait a second
            maxSleepSeconds--;
            if (maxSleepSeconds == 0) {
                break;
            }
        }

        // stop the readers
        for (PolicyIndexExerciser ex : readers) {
            ex.stopit();
        }

        // wait until they have actually finished
        maxSleepSeconds = 60 * 5; // try for 5 mins, probably way too long...
        while (PolicyIndexExerciser.readerRunningCount() > 0) {
            Thread.sleep(1000); // wait a second
            maxSleepSeconds--;
            if (maxSleepSeconds == 0) {
                break;
            }
        }


        // report any failures
        for (PolicyIndexExerciser ex : updaters) {
            if (ex.failed()) {
                System.out.println("PolicyIndexExerciser failed.  Last URL was: " + ex.lastUrl());
                System.out.println("Error was: " + ex.failure().getMessage());
            }
        }
        // report any failures
        for (PolicyIndexExerciser ex : readers) {
            if (ex.failed()) {
                System.out.println("PolicyIndexExerciser failed.  Last URL was: " + ex.lastUrl());
                System.out.println("Error was: " + ex.failure().getMessage());
            }
        }

        // check for non-completed exercisers
        assertTrue("Some policy index exercisers did not complete", PolicyIndexExerciser.updaterRunningCount() == 0);
        assertTrue("Some policy index exercisers did not complete", PolicyIndexExerciser.readerRunningCount() == 0);

        // check for failures
        assertTrue("Some policy index operations reported errors", PolicyIndexExerciser.updaterPassedCount() == updatersCount);
        assertTrue("Some policy index operations reported errors", PolicyIndexExerciser.readerPassedCount() == readersCount);

        // check no policies active
        assertFalse(checkPolicyEnforcement("A"));
        assertFalse(checkPolicyEnforcement("B"));





    }

    /**
     * Check policy enforcement for specified policy
     * @param policy - the policy to check enforcement for
     * @param permit - true if we are checking policy is in force, false if we are checking policy not in force
     * @return - true if access allowed, false otherwise
     * @throws IOException
     * @throws ClientProtocolException
     */
    private boolean checkPolicyEnforcement(String policy) throws ClientProtocolException, IOException {
        String url;
        if (policy.equals("A")) {
            url = "/fedora/objects/test:1000002?format=xml";
        } else if (policy.equals("B")) {
            url = "/fedora/objects/test:1000007?format=xml";
        } else {
            throw new RuntimeException("Invalid policy, specify A or B");
        }
        boolean permitted;
        try {
            httpUtils.get(url);
            permitted = true;
        } catch (AuthorizationDeniedException e) {
            permitted = false;
        }
        return permitted;
    }


}
