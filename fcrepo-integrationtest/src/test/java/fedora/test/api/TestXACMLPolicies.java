/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.Test;
import junit.framework.TestSuite;

import fedora.client.FedoraClient;

import fedora.server.access.FedoraAPIA;
import fedora.server.management.FedoraAPIM;
import fedora.server.security.servletfilters.xmluserfile.FedoraUsers;
import fedora.server.types.gen.Property;
import fedora.server.utilities.ServerUtility;
import fedora.server.utilities.StreamUtility;

import fedora.test.DemoObjectTestSetup;
import fedora.test.FedoraServerTestCase;

/**
 * Tests involving XACML policies, for API-A and API-M.
 *
 * Note: Although these tests can run when API-A AuthN is off, for the best
 * coverage, make sure the server is configured to authenticate for API-A
 * access.
 *
 * @author Edwin Shin
 */
public class TestXACMLPolicies
        extends FedoraServerTestCase {

    private FedoraClient admin;

    private FedoraClient testuser1;

    private FedoraClient testuserroleA;

    private FedoraClient testuser2;

    private FedoraClient testuser3;

    private FedoraClient testuserroleB;

    private FedoraClient testuserroleC;

    private FedoraClient testuserroleC2;

    private FedoraClient testuser4;

    private File fedoraUsersBackup = null;

    public static Test suite() {
        TestSuite suite = new TestSuite("XACML Policy TestSuite");
        suite.addTestSuite(TestXACMLPolicies.class);
        return new DemoObjectTestSetup(suite);
    }

    public void testXACMLMultiOwnerAccess() throws Exception {
        // demo:MultiOwnerObject is owned by fedoraAdmin and testuser1
        final String pid = "test:MultiOwnerObject";
        final String owners = "fedoraAdmin,testuser1";
        addTestObject(pid, owners, null);

        // should only be modifiable by an owner
        try {
            assertTrue(canWrite(admin, pid));
            assertTrue(canWrite(testuser1, pid));
            assertFalse(canWrite(testuserroleA, pid));
        } finally {
            removeTestObject(pid);
        }
    }

    public void testXACMLUnmodifiableContentModel() throws Exception {

        // test policy disallows modifyObject for test:RestrictedCModel
        final String unrestrictedCModel = "test:UnrestrictedCModel";
        final String restrictedCModel = "test:RestrictedCModel";

        final String hasUnrestricted = "test:HasUnrestrictedCModel";
        addTestObject(hasUnrestricted, null, unrestrictedCModel);

        final String hasRestricted = "test:HasRestrictedCModel";
        addTestObject(hasRestricted, null, restrictedCModel);

        final String hasUnrestrictedAndRestricted = "test:HasUnrestrictedAndRestrictedCModel";
        addTestObject(hasUnrestrictedAndRestricted, null, unrestrictedCModel, restrictedCModel);

        final String hasRestrictedAndUnrestricted = "test:HasRestrictedAndUnrestrictedCModel";
        addTestObject(hasRestrictedAndUnrestricted, null, restrictedCModel, unrestrictedCModel);

        try {
            assertTrue(canWrite(admin, hasUnrestricted));
            assertFalse(canWrite(admin, hasRestricted));
            assertFalse(canWrite(admin, hasUnrestrictedAndRestricted));
            assertFalse(canWrite(admin, hasRestrictedAndUnrestricted));
        } finally {
            removeTestObject(hasUnrestricted);
            removeTestObject(hasRestricted);
            removeTestObject(hasUnrestrictedAndRestricted);
            removeTestObject(hasRestrictedAndUnrestricted);
        }
    }

    private boolean canWrite(FedoraClient client, String pid)
            throws Exception {
        FedoraAPIM apim = client.getAPIM();
        try {
            apim.modifyObject(pid, null, null, null, "log message");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void testXACMLAPIMAccess() throws Exception {
        String dateOfFirstSuccess = null;
        String dateOfSecondSuccess = null;
        String dateOfThirdSuccess = null;
        String dateOfFourthSuccess = null;
        String URL1 = getDemoBaseURL() + "/simple-image-demo/col1.jpg";
        String URL2 = getDemoBaseURL() + "/simple-image-demo/col2.jpg";
        String URL3 = getDemoBaseURL() + "/simple-image-demo/col3.jpg";
        Class modDSArgs[] =
                {String.class, String.class, String[].class, String.class,
                        String.class, String.class, String.class, String.class,
                        String.class, String.class, Boolean.TYPE};
        Object modDSParms1[] =
                {"demo:5", "THUMBRES_IMG", null, null, null, null, null, null,
                        null, null, Boolean.FALSE};
        Class purgeDSArgs[] =
                {String.class, String.class, String.class, String.class,
                        String.class, Boolean.TYPE};
        Object purgeDSParms1[] =
                {"demo:5", "THUMBRES_IMG", null, null, null, Boolean.FALSE};
        Class setVersionableArgs[] =
                {String.class, String.class, Boolean.TYPE, String.class};
        Object setVersionableFalse[] =
                {"demo:5", "THUMBRES_IMG", Boolean.FALSE, null};
        Object setVersionableTrue[] =
                {"demo:5", "THUMBRES_IMG", Boolean.TRUE, null};

        // APIM access by user without access- should fail
        // testuserroleA does not have permission to modify a datastream, so this should fail
        invokeAPIMFailure(testuserroleA,
                          "testuserroleA",
                          "modifyDatastreamByReference",
                          modDSArgs,
                          modDSParms1);

        //APIM accesses by users with access- should succeed
        modDSParms1[6] = URL1;
        dateOfFirstSuccess =
                invokeAPIMSuccessString(testuser1,
                                        "testuser1",
                                        "modifyDatastreamByReference",
                                        modDSArgs,
                                        modDSParms1);
        System.out.println("    URL = " + modDSParms1[6]);
        assertTrue(dateOfFirstSuccess != null);
        System.out.println("  Modify datastream from testuser1 succeeded.");

        System.out.println("Disabling versioning.");
        invokeAPIMSuccess(admin,
                          "admin",
                          "setDatastreamVersionable",
                          setVersionableArgs,
                          setVersionableFalse);

        modDSParms1[6] = URL2;
        System.out
                .println("Testing modify datastream from admin with versioning off.");
        dateOfSecondSuccess =
                invokeAPIMSuccessString(admin,
                                        "admin",
                                        "modifyDatastreamByReference",
                                        modDSArgs,
                                        modDSParms1);
        System.out.println("    URL = " + modDSParms1[6]);
        assertTrue(dateOfSecondSuccess != null);
        System.out.println("  Modify datastream from admin succeeded.");

        modDSParms1[6] = null;
        modDSParms1[3] = "The Colliseum with Graffiti";
        System.out
                .println("Testing modify datastream from admin with versioning off just changing label.");
        dateOfThirdSuccess =
                invokeAPIMSuccessString(admin,
                                        "admin",
                                        "modifyDatastreamByReference",
                                        modDSArgs,
                                        modDSParms1);
        System.out.println("    Label = " + modDSParms1[3]);
        assertTrue(dateOfThirdSuccess != null);
        System.out.println("  Modify datastream from admin succeeded.");

        System.out.println("Re-enabling versioning.");
        invokeAPIMSuccess(admin,
                          "admin",
                          "setDatastreamVersionable",
                          setVersionableArgs,
                          setVersionableTrue);

        modDSParms1[6] = URL3;
        modDSParms1[3] = null;
        dateOfFourthSuccess =
                invokeAPIMSuccessString(testuser1,
                                        "testuser1",
                                        "modifyDatastreamByReference",
                                        modDSArgs,
                                        modDSParms1);
        System.out.println("    URL = " + modDSParms1[6]);
        assertTrue(dateOfFourthSuccess != null);
        System.out.println("  Modify datastream from testuser1 succeeded.");

        // APIM access by user without access- should fail
        purgeDSParms1[2] = dateOfFirstSuccess;
        purgeDSParms1[3] = dateOfFourthSuccess;
        // testuser1 does not have permission to purge a datastream, so this should fail
        invokeAPIMFailure(testuser1,
                          "testuser1",
                          "purgeDatastream",
                          purgeDSArgs,
                          purgeDSParms1);

        //APIM access by user without access- should fail
        // testuserroleA does have permission to to purge a datastream, but only if
        // datastream is in Deleted(D) state. Datastream here is still in Active(A) state
        // so this should fail
        invokeAPIMFailure(testuserroleA,
                          "testuserroleA",
                          "purgeDatastream",
                          purgeDSArgs,
                          purgeDSParms1);

        //APIM access by user with access- should succeed
        // fedoraAdmin does have permission to purge a datastream regardless of the
        // datastream state. Datastream here is in Acive(A) state so purge should still suceed.
        String purged[] =
                invokeAPIMSuccessStringArray(admin,
                                             "admin",
                                             "purgeDatastream",
                                             purgeDSArgs,
                                             purgeDSParms1);
        System.out.println("    Checking number of versions purged.");
        assertEquals(purged.length, 2);
        System.out.println("    Checking dates of versions purged.");
        assertEquals(purged[0], dateOfThirdSuccess);
        assertEquals(purged[1], dateOfFourthSuccess);
        System.out.println("Purge Datastreams successful.");
    }

    public void testXACMLAPIAAccess() throws Exception {
        if (isAPIAAuthzOn()) {
            Class getDDArgs[] = {String.class, String.class, String.class};
            Object getDDParms[] = {"demo:5", "THUMBRES_IMG", null};
            Object getDDParms2[] = {"demo:29", "url", null};
            Object getDDParms3[] = {"demo:31", "DS1", null};
            Object getDDParms4[] = {"demo:ObjSpecificTest", "DC", null};

            Class getDissArgs[] =
                    {String.class, String.class, String.class,
                            Property[].class, String.class};
            Object getDissParms[] = {"demo:5", "demo:1", "getHigh", null, null};
            Object getDissParms2[] =
                    {"demo:29", "demo:27", "grayscaleImage", null, null};
            Class modObjArgs[] =
                    {String.class, String.class, String.class, String.class,
                            String.class};
            Object modObjParms[] = {"demo:31", null, null, null, null};

            // APIA access by user without access- should fail
            // testuser2 does not have permission to access api-a at all, so this should fail
            invokeAPIAFailure(testuser2, "testuser2", "getDatastreamDissemination", getDDArgs, getDDParms);

            // APIA access by user without access- should fail
            // testuser3 does not have permission to access Datastreams named THUMBRES_IMG, so this should fail
            invokeAPIAFailure(testuser3,
                              "testuser3",
                              "getDatastreamDissemination",
                              getDDArgs,
                              getDDParms);

            // APIA access by user without access- should fail
            // testuserroleB does not have permission to access HighRes Dissemenations, so this should fail
            invokeAPIAFailure(testuserroleB,
                              "testuserroleB",
                              "getDissemination",
                              getDissArgs,
                              getDissParms);

            // APIA access by user without access- should fail
            // testuser4 does not have permission to access demo:29 at all, so this should fail
            invokeAPIAFailure(testuser4,
                              "testuser4",
                              "getDatastreamDissemination",
                              getDDArgs,
                              getDDParms2);

            // APIA access by user without access- should fail
            // testuser4 does not have permission to access demo:29 at all, so this should fail
            invokeAPIAFailure(testuser4,
                              "testuser4",
                              "getDissemination",
                              getDissArgs,
                              getDissParms2);

            // APIA access by user without access- should fail
            // testuser1 does not have permission to access demo:29 datastreams, so this should fail
            invokeAPIAFailure(testuser1,
                              "testuser1",
                              "getDatastreamDissemination",
                              getDDArgs,
                              getDDParms2);

            // APIA access by user with access- should succeed
            // testuserroleC does have permission to access demo:29 datastreams, so this should succeed
            invokeAPIASuccess(testuserroleC,
                              "testuserroleC",
                              "getDatastreamDissemination",
                              getDDArgs,
                              getDDParms2);

            // Make sure object-specific policies in the POLICY datastream work
            addObjectSpecificPolicies();
            try {
                // APIA access by user with access- should succeed
                // testuserroleC does have permission to access demo:ObjSpecificTest datastreams, so this should succeed
                invokeAPIASuccess(testuserroleC,
                                  "testuserroleC",
                                  "getDatastreamDissemination",
                                  getDDArgs,
                                  getDDParms4);

                // APIA access by user without access- should fail
                // demo:ObjSpecificTest's object-specific policy explicitly denies access
                // to user with role roleUntrusted
                invokeAPIAFailure(testuserroleC2,
                                  "testuserroleC2",
                                  "getDatastreamDissemination",
                                  getDDArgs,
                                  getDDParms4);
            } finally {
                removeObjectSpecificPolicies();
            }

            // APIA access by user with access- should succeed
            // testuser1 does have permission to access demo:5 datastreams, so this should succeed
            invokeAPIASuccess(testuser1,
                              "testuser1",
                              "getDatastreamDissemination",
                              getDDArgs,
                              getDDParms);

            // APIA access by user who is not owner should fail
            // testuser1 is not currently owner of demo:31, so this should fail
            invokeAPIAFailure(testuser1,
                              "testuser1",
                              "getDatastreamDissemination",
                              getDDArgs,
                              getDDParms3);

            modObjParms[3] = "testuser1";
            String dateOfSuccess =
                    invokeAPIMSuccessString(admin,
                                            "fedoraAdmin",
                                            "modifyObject",
                                            modObjArgs,
                                            modObjParms);
            assertTrue(dateOfSuccess != null);
            System.out.println("  Modify Object from admin succeeded.");

            // APIA access by user who is now the owner, should succeed
            // testuser1 is now currently owner of demo:31, so this should succeed
            invokeAPIASuccess(testuser1,
                              "testuser1",
                              "getDatastreamDissemination",
                              getDDArgs,
                              getDDParms3);

            modObjParms[3] = "fedoraAdmin";
            dateOfSuccess =
                    invokeAPIMSuccessString(admin,
                                            "fedoraAdmin",
                                            "modifyObject",
                                            modObjArgs,
                                            modObjParms);
            assertTrue(dateOfSuccess != null);
            System.out.println("  Modify Object from admin succeeded.");
        } else {
            System.out.println("Authorization is not enabled for APIA");
            System.out
                    .println("Testing Policies for APIA access will not work.");
        }
    }

    public void invokeAPIMFailure(FedoraClient user,
                                  String username,
                                  String functionToTest,
                                  Class args[],
                                  Object parms[]) {
        // APIA access by user without access- should fail
        try {
            System.out.println("Testing " + functionToTest
                    + " from invalid user: " + username);

            FedoraAPIM apim1 = user.getAPIM();
            Method func = apim1.getClass().getMethod(functionToTest, args);
            Object result = func.invoke(apim1, parms);
            fail("Illegal access allowed");
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof org.apache.axis.AxisFault) {
                org.apache.axis.AxisFault af =
                        (org.apache.axis.AxisFault) cause;
                System.out.println("    Reason = "
                        + af.getFaultReason().substring(af.getFaultReason()
                                .lastIndexOf(".") + 1));
                assertTrue(af.getFaultReason().contains("AuthzDeniedException"));
                System.out.println("Access denied correctly");
            } else {
                System.out.println("Got exception: "
                        + cause.getClass().getName());
                fail("Illegal access dis-allowed for some other reason");
            }
        } catch (IOException ioe) {
            System.out
                    .println("    Reason = " + ioe.getMessage()/* .substring(ioe.getMessage().lastIndexOf("[")) */);
            assertTrue(ioe.getMessage().contains("[403 Forbidden]"));
            System.out.println("Access denied correctly");
            // exception was expected, all is A-OK
        } catch (Exception ae) {
            System.out.println("Some other exception: "
                    + ae.getClass().getName());
            fail("Some other exception");
        }
    }

    public String invokeAPIMSuccessString(FedoraClient user,
                                          String username,
                                          String functionToTest,
                                          Class args[],
                                          Object parms[]) {
        Object result =
                invokeAPIMSuccess(user, username, functionToTest, args, parms);
        return (String) result;
    }

    public String[] invokeAPIMSuccessStringArray(FedoraClient user,
                                                 String username,
                                                 String functionToTest,
                                                 Class args[],
                                                 Object parms[]) {
        Object result =
                invokeAPIMSuccess(user, username, functionToTest, args, parms);
        return (String[]) result;
    }

    public Object invokeAPIMSuccess(FedoraClient user,
                                    String username,
                                    String functionToTest,
                                    Class args[],
                                    Object parms[]) {
        // APIA access by user with access- should succeed
        try {
            // testuser1 does have permission to access demo:5 datastreams, so this should succeed
            System.out.println("Testing " + functionToTest
                    + " from valid user: " + username);
            FedoraAPIM apim1 = user.getAPIM();
            Method func = apim1.getClass().getMethod(functionToTest, args);
            Object result = func.invoke(apim1, parms);
            assertTrue(result != null);
            return result;
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof org.apache.axis.AxisFault) {
                org.apache.axis.AxisFault af =
                        (org.apache.axis.AxisFault) cause;
                System.out.println("Got exception: " + af.getClass().getName());
                System.out.println("Reason = " + af.getFaultReason());
                System.out.println("Message = " + af.getMessage());
                fail("Legal access dis-allowed");
            } else {
                System.out.println("Got exception: "
                        + cause.getClass().getName());
                fail("Legal access dis-allowed");
            }
        } catch (Exception e) {
            System.out.println("Got exception: " + e.getClass().getName());
            fail("Legal access dis-allowed");
        }

        return null;
    }

    public void invokeAPIAFailure(FedoraClient user,
                                  String username,
                                  String functionToTest,
                                  Class args[],
                                  Object parms[]) {
        // APIA access by user without access- should fail
        try {
            System.out.println("Testing " + functionToTest
                    + " from invalid user: " + username);

            FedoraAPIA apia1 = user.getAPIA();
            Method func = apia1.getClass().getMethod(functionToTest, args);
            Object result = func.invoke(apia1, parms);
            fail("Illegal access allowed");
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof org.apache.axis.AxisFault) {
                org.apache.axis.AxisFault af =
                        (org.apache.axis.AxisFault) cause;
                System.out.println("    Reason = "
                        + af.getFaultReason().substring(af.getFaultReason()
                                .lastIndexOf(".") + 1));
                assertTrue(af.getFaultReason().contains("AuthzDeniedException"));
                System.out.println("Access denied correctly");
            } else {
                System.out.println("Got exception: "
                        + cause.getClass().getName());
                fail("Illegal access dis-allowed for some other reason");
            }
        } catch (IOException ioe) {
            System.out.println("    Reason = "
                    + ioe.getMessage().substring(ioe.getMessage()
                            .lastIndexOf("[")));
            assertTrue(ioe.getMessage().contains("[403 Forbidden]"));
            System.out.println("Access denied correctly");
            // exception was expected, all is A-OK
        } catch (Exception ae) {
            System.out.println("Some other exception: "
                    + ae.getClass().getName());
            fail("Illegal access dis-allowed for some other reason");
        }
    }

    public Object invokeAPIASuccess(FedoraClient user,
                                    String username,
                                    String functionToTest,
                                    Class args[],
                                    Object parms[]) {
        // APIA access by user with access- should succeed
        try {
            // testuser1 does have permission to access demo:5 datastreams, so this should succeed
            System.out.println("Testing " + functionToTest
                    + " from valid user: " + username);
            FedoraAPIA apia1 = user.getAPIA();
            Method func = apia1.getClass().getMethod(functionToTest, args);
            Object result = func.invoke(apia1, parms);
            assertTrue(result != null);
            System.out.println("Access succeeded");
            return result;
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof org.apache.axis.AxisFault) {
                org.apache.axis.AxisFault af =
                        (org.apache.axis.AxisFault) cause;
                System.out.println("Got exception: " + af.getClass().getName());
                System.out.println("Reason = " + af.getFaultReason());
                System.out.println("Message = " + af.getMessage());
                fail("Legal access dis-allowed");
            } else {
                System.out.println("Got exception: "
                        + cause.getClass().getName());
                fail("Legal access dis-allowed");
            }
        } catch (Exception e) {
            System.out.println("Got exception: " + e.getClass().getName());
            fail("Legal access dis-allowed");
        }

        return null;
    }

    public boolean isAPIAAuthzOn() throws IOException {
        File installProperties =
                new File(FEDORA_HOME, "install/install.properties");
        BufferedReader prop = null;
        try {
            prop = new BufferedReader(new FileReader(installProperties));
            String line = null;
            while ((line = prop.readLine()) != null) {
                if (line.startsWith("apia.auth.required")) {
                    if (line.equals("apia.auth.required=true")) {
                        return true;
                    }
                    if (line.equals("apia.auth.required=false")) {
                        return false;
                    }
                }
            }
            return false;
        } finally {
            if (prop != null) {
                prop.close();
            }
        }
    }

    public void installJunitPolicies() {
        System.out.println("Copying Policies For Testing");
        File junitDir = new File("src/test/resources/XACMLTestPolicies/junit");
        File junitsaveDir =
                new File(FEDORA_HOME,
                         "data/fedora-xacml-policies/repository-policies/junit");
        if (!junitsaveDir.exists()) {
            junitsaveDir.mkdir();
        }
        File list[] = getFilesInDir(junitDir);
        traverseAndCopy(list, junitsaveDir);

        System.out.println("Copying Policies succeeded");
    }

    private void deleteJunitPolicies() {
        System.out.println("Removing Policies For Testing");
        File junitsaveDir =
                new File(FEDORA_HOME,
                         "data/fedora-xacml-policies/repository-policies/junit");
        if (junitsaveDir.exists()) {
            File list[] = getFilesInDir(junitsaveDir);
            traverseAndDelete(list);
            junitsaveDir.delete();
        }
    }

    private File[] getFilesInDir(File dir) {
        File srcFiles[] = dir.listFiles(new java.io.FilenameFilter() {

            public boolean accept(File dir, String name) {
                if ((name.toLowerCase().startsWith("permit") || name
                        .toLowerCase().startsWith("deny"))
                        && name.endsWith(".xml")) {
                    return true;
                }
                return false;
            }
        });

        return srcFiles;
    }

    private void traverseAndCopy(File srcFiles[], File destDir) {
        for (File element : srcFiles) {
            File destFile = new File(destDir, element.getName());
            System.out.println("Copying policy: " + element.getName());
            if (!destFile.exists()) {
                try {
                    destFile.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            copyFile(element, destFile);
        }
    }

    private void traverseAndDelete(File newFiles[]) {
        for (File element : newFiles) {
            System.out.println("Deleting policy: " + element.getName());
            element.delete();
        }
    }

    private boolean copyFile(File src, File dest) {
        InputStream in;
        try {
            in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);
            StreamUtility.pipeStream(in, out, 1024);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void reloadPolicies() {
        System.out.println("Reloading Policies...");
        try {
            FedoraClient client =
                    new FedoraClient(ServerUtility.getBaseURL(getProtocol()),
                                     getUsername(),
                                     getPassword());
            client.reloadPolicies();
            System.out.println("  Done Reloading Policies");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void backupFedoraUsersFile() {
        fedoraUsersBackup =
                new File(FedoraUsers.fedoraUsersXML.getAbsolutePath()
                        + ".backup");
        System.out.println("Backing Up Fedora Users");
        if (!fedoraUsersBackup.exists()) {
            try {
                fedoraUsersBackup.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        copyFile(FedoraUsers.fedoraUsersXML, fedoraUsersBackup);
    }

    private void restoreFedoraUsersFile() {
        System.out.println("Restoring Fedora Users");
        if (!fedoraUsersBackup.exists()) {
            return;
        }
        copyFile(fedoraUsersBackup, FedoraUsers.fedoraUsersXML);
    }

    private void createNewFedoraUsersFileWithTestUsers() {
        String sep = System.getProperty("line.seperator");
        if (sep == null) {
            sep = "\n";
        }
        String data = "<?xml version='1.0' ?>  " + sep
                + "<fedora-users>" + sep
                + "    <user name=\"" + getUsername() + "\" password=\"" + getPassword() + "\">" + sep
                + "      <attribute name=\"fedoraRole\">" + sep
                + "        <value>administrator</value>" + sep
                + "      </attribute>" + sep
                + "    </user>" + sep
                + "    <user name=\"fedoraIntCallUser\" password=\"changeme\">" + sep
                + "      <attribute name=\"fedoraRole\">" + sep
                + "        <value>fedoraInternalCall-1</value>" + sep
                + "        <value>fedoraInternalCall-2</value>" + sep
                + "      </attribute>" + sep
                + "    </user>" + sep
                + "    <user name=\"testuser1\" password=\"testuser1\"/>" + sep
                + "    <user name=\"testuser2\" password=\"testuser2\"/>" + sep
                + "    <user name=\"testuser3\" password=\"testuser3\"/>" + sep
                + "    <user name=\"testuser4\" password=\"testuser4\"/>" + sep
                + "    <user name=\"testuserroleA\" password=\"testuserroleA\">" + sep
                + "      <attribute name=\"fedoraRole\">" + sep
                + "        <value>roleA</value>" + sep
                + "      </attribute>" + sep
                + "    </user>" + sep
                + "    <user name=\"testuserroleB\" password=\"testuserroleB\">" + sep
                + "      <attribute name=\"fedoraRole\">" + sep
                + "        <value>roleB</value>" + sep
                + "      </attribute>" + sep
                + "    </user>" + sep
                + "    <user name=\"testuserroleC\" password=\"testuserroleC\">" + sep
                + "      <attribute name=\"fedoraRole\">" + sep
                + "        <value>roleC</value>" + sep
                + "      </attribute>" + sep
                + "    </user>" + sep
                + "    <user name=\"testuserroleC2\" password=\"testuserroleC2\">" + sep
                + "      <attribute name=\"fedoraRole\">" + sep
                + "        <value>roleC</value>" + sep
                + "        <value>roleUntrusted</value>" + sep
                + "      </attribute>" + sep
                + "    </user>" + sep
                + "  </fedora-users>";
        try {
            FileOutputStream fu =
                    new FileOutputStream(FedoraUsers.fedoraUsersXML);
            OutputStreamWriter pw = new OutputStreamWriter(fu);
            pw.write(data);
            pw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setUp() throws Exception {

        System.out.println("setting Up XACML test");
        admin = getFedoraClient();

        backupFedoraUsersFile();
        createNewFedoraUsersFileWithTestUsers();
        installJunitPolicies();
        reloadPolicies();
        System.out.println("creating alternate users");
        testuser1 = new FedoraClient(getBaseURL(), "testuser1", "testuser1");
        testuserroleA =
                new FedoraClient(getBaseURL(), "testuserroleA", "testuserroleA");
        testuser2 = new FedoraClient(getBaseURL(), "testuser2", "testuser2");
        testuser3 = new FedoraClient(getBaseURL(), "testuser3", "testuser3");
        testuserroleB =
                new FedoraClient(getBaseURL(), "testuserroleB", "testuserroleB");
        testuserroleC =
                new FedoraClient(getBaseURL(), "testuserroleC", "testuserroleC");
        testuserroleC2 =
                new FedoraClient(getBaseURL(),
                                 "testuserroleC2",
                                 "testuserroleC2");
        testuser4 = new FedoraClient(getBaseURL(), "testuser4", "testuser4");
        System.out.println("done setting up");
    }

    private void addObjectSpecificPolicies() {
        try {
            StringBuffer xml = new StringBuffer();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            xml.append("<foxml:digitalObject VERSION=\"1.1\" PID=\"demo:ObjSpecificTest\" xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\">");
            xml.append("  <foxml:objectProperties>");
            xml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"A\"/>");
            xml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#label\" VALUE=\"ObjSpecificTest\"/>");
            xml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#createdDate\" VALUE=\"2004-12-10T00:21:57Z\"/>");
            xml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/view#lastModifiedDate\" VALUE=\"2004-12-10T00:21:57Z\"/>");
            xml.append("  </foxml:objectProperties>");
            xml.append("  <foxml:datastream ID=\"POLICY\" CONTROL_GROUP=\"X\" STATE=\"A\">");
            xml.append("    <foxml:datastreamVersion FORMAT_URI=\"" + XACML_POLICY1_0.uri + "\" ID=\"POLICY1.0\" MIMETYPE=\"text/xml\" LABEL=\"Policy\">");
            xml.append("         <foxml:xmlContent>");
            xml.append("<Policy PolicyId=\"POLICY\" RuleCombiningAlgId=\"urn:oasis:names:tc:xacml:1.0:rule-combining-algorithm:first-applicable\"");
            xml.append("  xmlns=\"urn:oasis:names:tc:xacml:1.0:policy\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
            xml.append("  <Description>");
            xml.append("    Denies all to user with id testuserroleC2");
            xml.append("  </Description>");
            xml.append("  <Target>");
            xml.append("    <Subjects>");
            xml.append("      <AnySubject/>");
            xml.append("    </Subjects>");
            xml.append("    <Resources>");
            xml.append("      <AnyResource/>");
            xml.append("    </Resources>");
            xml.append("    <Actions>");
            xml.append("      <AnyAction/>");
            xml.append("    </Actions>");
            xml.append("  </Target>");
            xml.append("  <Rule Effect=\"Deny\" RuleId=\"1\">");
            xml.append("    <Condition FunctionId=\"urn:oasis:names:tc:xacml:1.0:function:string-is-in\">");
            xml.append("      <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">roleUntrusted</AttributeValue>");
            xml.append("      <SubjectAttributeDesignator AttributeId=\"fedoraRole\"");
            xml.append("        DataType=\"http://www.w3.org/2001/XMLSchema#string\" MustBePresent=\"false\"/>");
            xml.append("    </Condition>");
            xml.append("  </Rule>");
            xml.append("</Policy>");
            xml.append("      </foxml:xmlContent>");
            xml.append("    </foxml:datastreamVersion>");
            xml.append("  </foxml:datastream>");
            xml.append("</foxml:digitalObject>");
            admin.getAPIM().ingest(xml.toString().getBytes("UTF-8"),
                                   FOXML1_1.uri,
                                   "");
        } catch (Exception e) {
            throw new RuntimeException("Failure adding object-specific "
                    + "policies", e);
        }
    }

    private void removeObjectSpecificPolicies() {
        try {
            admin.getAPIM().purgeObject("demo:ObjSpecificTest", "", false);
        } catch (Exception e) {
            throw new RuntimeException("Failure removing object-specific "
                    + "policies", e);
        }
    }

    private void addTestObject(String pid, String ownerId, String... cModelPIDs) {
        try {
            StringBuffer xml = new StringBuffer();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            xml.append("<foxml:digitalObject VERSION=\"1.1\" PID=\"" + pid + "\" xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\">");
            xml.append("  <foxml:objectProperties>");
            if (ownerId != null && ownerId.trim().length() > 0) {
                xml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#ownerId\" VALUE=\"");
                xml.append(ownerId.trim());
                xml.append("\"/>");
            }
            xml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"A\"/>");
            xml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#label\" VALUE=\"MultiOwnerObject\"/>");
            xml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#createdDate\" VALUE=\"2004-12-10T00:21:57Z\"/>");
            xml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/view#lastModifiedDate\" VALUE=\"2004-12-10T00:21:57Z\"/>");
            xml.append("  </foxml:objectProperties>");
            if (cModelPIDs != null) {
                xml.append("<foxml:datastream CONTROL_GROUP=\"X\" ID=\"RELS-EXT\">");
                xml.append("  <foxml:datastreamVersion CREATED=\"2008-07-02T05:09:43.375Z\" FORMAT_URI=\"info:fedora/fedora-system:FedoraRELSExt-1.0\" ID=\"RELS-EXT1.0\" LABEL=\"RDF Statements about this object\" MIMETYPE=\"application/rdf+xml\">");
                xml.append("    <foxml:xmlContent>");
                xml.append("      <rdf:RDF xmlns:fedora-model=\"info:fedora/fedora-system:def/model#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">");
                xml.append("        <rdf:Description rdf:about=\"info:fedora/" + pid + "\">");
                for (String cModelPID: cModelPIDs) {
                    xml.append("          <fedora-model:hasModel rdf:resource=\"info:fedora/" + cModelPID + "\"/>");
                }
                xml.append("        </rdf:Description>");
                xml.append("      </rdf:RDF>");
                xml.append("    </foxml:xmlContent>");
                xml.append("  </foxml:datastreamVersion>");
                xml.append("</foxml:datastream>");
            }
            xml.append("</foxml:digitalObject>");
            admin.getAPIM().ingest(xml.toString().getBytes("UTF-8"),
                    FOXML1_1.uri, "");
        } catch (Exception e) {
            throw new RuntimeException("Failure adding test object: " + pid, e);
        }
    }

    private void removeTestObject(String pid) {
        try {
            admin.getAPIM().purgeObject(pid, "", false);
        } catch (Exception e) {
            throw new RuntimeException("Failure removing test object: " + pid, e);
        }
    }

    @Override
    public void tearDown() {
        restoreFedoraUsersFile();
        deleteJunitPolicies();
        reloadPolicies();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestXACMLPolicies.class);
    }

}
