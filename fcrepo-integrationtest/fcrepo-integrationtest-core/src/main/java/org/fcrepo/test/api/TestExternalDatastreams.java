/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.security.MessageDigest;
import java.util.Date;

import javax.xml.ws.soap.SOAPFaultException;

import junit.framework.JUnit4TestAdapter;

import org.fcrepo.common.PID;
import org.fcrepo.server.access.FedoraAPIA;
import org.fcrepo.server.management.FedoraAPIM;
import org.fcrepo.server.utilities.StringUtility;
import org.fcrepo.server.utilities.TypeUtility;
import org.fcrepo.test.FedoraServerTestCase;
import org.fcrepo.utilities.Foxml11Document;
import org.fcrepo.utilities.Foxml11Document.ControlGroup;
import org.fcrepo.utilities.Foxml11Document.Property;
import org.fcrepo.utilities.Foxml11Document.State;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Edwin Shin, Benjamin Armintor
 * @version $Id$
 * @since 3.6
 */
public class TestExternalDatastreams
        extends FedoraServerTestCase {

    private FedoraAPIM apim;
    private FedoraAPIA apia;
    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        apim = getFedoraClient().getAPIM();
        apia = getFedoraClient().getAPIA();
        System.setProperty("fedoraServerHost", "localhost");
        System.setProperty("fedoraServerPort", "8080");
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testIngest() throws Exception {
        String pid = "demo:e_ds_test";
        File content = null;
        try {
            content = getTempFile(true);
            String dsLocation = content.getCanonicalFile().toURI().toString();
            System.out.println("Expect to succeed: " + dsLocation);
            apim.ingest(getFoxmlObject(pid, dsLocation), FOXML1_1.uri, null);
            apia.getDatastreamDissemination(pid, "DS", null);
        } finally {
            if (content != null) content.delete();
            apim.purgeObject(pid, "test", false);
        }
        try {
            content = getTempFile(false);
            String dsLocation = content.getCanonicalFile().toURI().toString();
            System.out.println("Expect to fail: " + dsLocation);
            apim.ingest(getFoxmlObject(pid, dsLocation), FOXML1_1.uri, null);
            apia.getDatastreamDissemination(pid, "DS", null);
            fail("Datastream Dissemination with unallowed dsLocation should have failed.");
        } catch (SOAPFaultException e) {
            System.out.println("Checking failure :\"" + e.getMessage() + "\" for \"Policy blocked datastream resolution\"");
            assertTrue(e.getMessage().contains("Policy blocked datastream resolution"));
        } finally {
            if (content != null) content.delete();
            apim.purgeObject(pid, "test", false);
        }

    }

    @Test
    public void testAddDatastream() throws Exception {
        String pid = "demo:e_ds_test_add";
        File content = null;

        apim.ingest(getFoxmlObject(pid, null), FOXML1_1.uri, null);
        try {
            content = getTempFile(true);
            String dsLocation = content.getCanonicalFile().toURI().toString();
            addDatastream(pid, dsLocation);
            apia.getDatastreamDissemination(pid, "DS", null);
        } finally {
            if (content != null) content.delete();
            apim.purgeObject(pid, "test", false);
        }

        apim.ingest(getFoxmlObject(pid, null), FOXML1_1.uri, null);
        try {
            content = getTempFile(false);
            String dsLocation = content.getCanonicalFile().toURI().toString();
            addDatastream(pid, dsLocation);
            apia.getDatastreamDissemination(pid, "DS", null);
            fail("Datastream Dissemination with unallowed dsLocation should have failed.");
        } catch (SOAPFaultException e) {
            System.out.println("Checking failure :\"" + e.getMessage() + "\" for \"Policy blocked datastream resolution\"");
            assertTrue(e.getMessage().contains("Policy blocked datastream resolution"));
        } finally {
            if (content != null) content.delete();
            apim.purgeObject(pid, "test", false);
        }
    }

    @Test
    public void testModifyDatastreamByReference() throws Exception {
        String pid = "demo:e_ds_test_add";

        String dsLocation = getBaseURL() + "/get/fedora-system:ContentModel-3.0/DC";
        apim.ingest(getFoxmlObject(pid, dsLocation), FOXML1_1.uri, null);
        apia.getDatastreamDissemination(pid, "DS", null);
        try {
            dsLocation = getBaseURL() + "/get/fedora-system:ContentModel-3.0/RELS-EXT";
            modifyDatastreamByReference(pid, dsLocation);
        } finally {
            apim.purgeObject(pid, "test", false);
        }

        File content = null;
        dsLocation = getBaseURL() + "/get/fedora-system:ContentModel-3.0/DC";
        apim.ingest(getFoxmlObject(pid, dsLocation), FOXML1_1.uri, null);
        try {
            content = getTempFile(false);
            dsLocation = content.getCanonicalFile().toURI().toString();
            modifyDatastreamByReference(pid, dsLocation);
            apia.getDatastreamDissemination(pid, "DS", null);
            fail("Pointing a datastream to an unallowed dsLocation should have failed.");
        } catch (SOAPFaultException e) {
            System.out.println("Checking failure :\"" + e.getMessage() + "\" for \"Policy blocked datastream resolution\"");
            assertTrue(e.getMessage().contains("Policy blocked datastream resolution"));
        } finally {
            if (content != null) content.delete();
            apim.purgeObject(pid, "test", false);
        }
    }

    @Test
    public void testAddDatastreamWithChecksum() throws Exception {
        String pid = "demo:e_ds_test_add";
        String checksumType = "MD5";
        apim.ingest(getFoxmlObject(pid, null), FOXML1_1.uri, null);
        File temp = null;

        try {
            temp = getTempFile(true);
            String dsLocation = temp.getCanonicalFile().toURI().toString();
            String checksum = computeChecksum(checksumType, new FileInputStream(temp));
            String dsId = addDatastream(pid, dsLocation, checksumType, checksum);
            assertEquals("DS", dsId);

            // Now ensure that bogus checksums do indeed fail
            apim.purgeDatastream(pid, dsId, null, null, null, false);
            addDatastream(pid, dsLocation, checksumType, checksum);

            apim.purgeDatastream(pid, dsId, null, null, null, false);
            checksum = "bogus";
            try {
                addDatastream(pid, dsLocation, checksumType, checksum);
                fail("Adding datastream with bogus checksum should have failed.");
            } catch (SOAPFaultException e) {
                assertTrue(e.getMessage().contains("Checksum Mismatch"));
            }
        } finally {
            apim.purgeObject(pid, "test", false);
            if (temp != null) temp.delete();
        }

        pid = "demo:e_ds_test_add_portable";
        apim.ingest(getFoxmlObject(pid, null), FOXML1_1.uri, null);
        try {
            String dsLocation = "http://local.fedora.server/fedora/objects/fedora-system:ContentModel-3.0/datastreams/DC/content";
            org.fcrepo.server.types.gen.MIMETypedStream dc_content = apia.getDatastreamDissemination("fedora-system:ContentModel-3.0", "DC", null);
            String checksum = computeChecksum(checksumType, new ByteArrayInputStream(dc_content.getStream()));
            String dsId = addDatastream(pid, dsLocation, checksumType, checksum);
            assertEquals("DS", dsId);
        } finally {
            apim.purgeObject(pid, "testAddDatastreamWithChecksumType", false);
            if (temp != null) temp.delete();
        }
    }

    @Test
    public void testAddDatastreamWithChecksumType() throws Exception {
        String pid = "demo:e_ds_test_add";
        String checksumType = "MD5";
        apim.ingest(getFoxmlObject(pid, null), FOXML1_1.uri, "testAddDatastreamWithChecksumType");
        File temp = null;

        try {
            temp = getTempFile(true);
            String dsLocation = temp.getCanonicalFile().toURI().toString();
            String dsId = addDatastream(pid, dsLocation, checksumType, null);
            assertEquals("DS", dsId);
            String checksum = computeChecksum(checksumType, new FileInputStream(temp));
            assertEquals(apim.getDatastream(pid, "DS", null).getChecksum(),checksum);
        } finally {
            apim.purgeObject(pid, "testAddDatastreamWithChecksumType", false);
            if (temp != null) temp.delete();
        }
        // Now test portable URLs
        pid = "demo:e_ds_test_portable";
        apim.ingest(getFoxmlObject(pid, null), FOXML1_1.uri, "testAddDatastreamWithChecksumType");
        try {
            String dsLocation = "http://local.fedora.server/fedora/objects/fedora-system:ContentModel-3.0/datastreams/DC/content";
            String dsId = addDatastream(pid, dsLocation, checksumType, null);
            assertEquals("DS", dsId);
        } finally {
            apim.purgeObject(pid, "testAddDatastreamWithChecksumType", false);
            if (temp != null) temp.delete();
        }
    }

    @Test
    public void testModifyDatastreamByReferenceWithChecksum() throws Exception {
        String pid = "file:e_ds_test_add"; // file pid namespace should work as well as demo
        String checksumType = "MD5";
        apim.ingest(getFoxmlObject(pid, null), FOXML1_1.uri, null);
        File temp = null;
        try {
            temp = getTempFile(true);
            String contentLocation = temp.getCanonicalFile().toURI().toString();
            String checksum = computeChecksum(checksumType, new FileInputStream(temp));
            String dsId = addDatastream(pid, contentLocation, checksumType, checksum);
            assertEquals("DS", dsId);

            FileOutputStream os = new FileOutputStream(temp);
            os.write("testModifyDatastreamByReferenceWithChecksum".getBytes());
            os.close();
            checksum = computeChecksum(checksumType, new FileInputStream(temp));
            modifyDatastreamByReference(pid, contentLocation, checksumType, checksum);

            // Now ensure that bogus checksums do indeed fail
            checksum = "bogus";
            try {
                modifyDatastreamByReference(pid, contentLocation, checksumType, checksum);
                fail("Modifying datastream with bogus checksum should have failed.");
            } catch (SOAPFaultException e) {
                assertTrue(e.getMessage().contains("Checksum Mismatch"));
            }
        } finally {
            apim.purgeObject(pid, "test", false);
            if (temp != null) temp.delete();
        }
    }

    private File getTempFile(boolean allowed) throws IOException {
        File temp = allowed? File.createTempFile("foo", "isallowed") : File.createTempFile("foo", "banned");
        Writer writer = new FileWriter(temp);
        writer.write("Neque porro quisquam est qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit");
        writer.flush();
        writer.close();
        return temp;
    }

    private byte[] getFoxmlObject(String pid, String contentLocation) throws Exception {
        Foxml11Document doc = createFoxmlObject(pid, contentLocation);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.serialize(out);
        return out.toByteArray();
    }

    private String addDatastream(String pid, String contentLocation)
            throws Exception {
        return addDatastream(pid, contentLocation, null, null);
    }

    private String addDatastream(String pid, String dsLocation, String checksumType, String checksum)
            throws Exception {
        return apim.addDatastream(pid,
                                  "DS",
                                  null,
                                  "testExternalDatastreams",
                                  true,
                                  "text/plain",
                                  "",
                                  dsLocation,
                                  "E",
                                  "A",
                                  checksumType,
                                  checksum,
                                  "testExternalDatastreams");
    }

    private String modifyDatastreamByReference(String pid,
                                               String contentLocation)
            throws Exception {
        return modifyDatastreamByReference(pid, contentLocation, null, null);
    }

    private String modifyDatastreamByReference(String pid,
                                               String contentLocation,
                                               String checksumType,
                                               String checksum)
            throws Exception {
        return apim.modifyDatastreamByReference(pid,
                                                "DS",
                                                TypeUtility.convertStringtoAOS(new String[]{}),
                                                "testExternalDatastreams",
                                                "text/plain",
                                                "",
                                                contentLocation,
                                                checksumType,
                                                checksum,
                                                "testExternalDatastreams",
                                                false);
    }


    private Foxml11Document createFoxmlObject(String spid, String contentLocation) throws Exception {
        PID pid = PID.getInstance(spid);
        Date date = new Date(1);

        Foxml11Document doc = new Foxml11Document(pid.toString());
        doc.addObjectProperty(Property.STATE, "A");

        if (contentLocation != null && contentLocation.length() > 0) {
            String ds = "DS";
            String dsv = "DS1.0";
            doc.addDatastream(ds, State.A, ControlGroup.E, true);
            doc.addDatastreamVersion(ds, dsv, "text/plain", "label", 1, date);
            doc.setContentLocation(dsv, contentLocation, org.fcrepo.server.storage.types.Datastream.DS_LOCATION_TYPE_URL);
        }
        return doc;
    }

    private String computeChecksum(String csType, InputStream is) throws Exception {
        MessageDigest md = MessageDigest.getInstance(csType);
        if (is == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        byte buffer[] = new byte[5000];
        int numread;
        while ((numread = is.read(buffer, 0, 5000)) > 0) {
            md.update(buffer, 0, numread);
        }
        return StringUtility.byteArraytoHexString(md.digest());
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestExternalDatastreams.class);
    }
}