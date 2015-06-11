/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage.translation;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.fcrepo.server.storage.types.AuditRecord;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.DatastreamXMLMetadata;
import org.fcrepo.utilities.DateUtility;









import static org.fcrepo.server.storage.translation.DOTranslationUtility.DESERIALIZE_INSTANCE;
import static org.fcrepo.server.storage.translation.DOTranslationUtility.SERIALIZE_EXPORT_ARCHIVE;
import static org.fcrepo.server.storage.translation.DOTranslationUtility.SERIALIZE_EXPORT_MIGRATE;
import static org.fcrepo.server.storage.translation.DOTranslationUtility.SERIALIZE_EXPORT_PUBLIC;
import static org.fcrepo.server.storage.translation.DOTranslationUtility.SERIALIZE_STORAGE_INTERNAL;
import static org.junit.Assert.assertEquals;


/**
 * @author Edwin Shin
 * @version $Id: TestDOTranslationUtility.java 6996 2008-04-18 18:46:06Z
 *          pangloss $
 */
public class TestDOTranslationUtility {
    private String appContext = null;
    private String baseURL = null;
    private DOTranslationUtility subject;
    
    @Before
    public void setUp() {
        Properties transProps = new Properties(System.getProperties());
        if (transProps.getProperty("fedora.hostname") == null) {
            transProps.setProperty("fedora.hostname","localhost");
        }
        if (transProps.getProperty("fedora.port") == null) {
            transProps.setProperty("fedora.port","1024");
        }
        if (transProps.getProperty("fedora.appServerContext") == null) {
            transProps.setProperty("fedora.appServerContext","fedora");
        }
        appContext = transProps.getProperty("fedora.appServerContext","fedora");
        subject = new DOTranslationUtility.Impl(transProps,true);
        String propertyUrl = "http://" + transProps.getProperty("fedora.hostname") + ":" + transProps.getProperty("fedora.port") + "/"
                + appContext;
        baseURL = System.getProperty("fedora.baseURL", propertyUrl);
    }
    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for
     * {@link org.fcrepo.server.storage.translation.DOTranslationUtility#normalizeDSLocationURLs(java.lang.String, org.fcrepo.server.storage.types.Datastream, int)}.
     */
    @Test
    public void testNormalizeDSLocationURLs() {
        Datastream ds, ds2;
        String pid = "demo:foo";

        ContextControlPair[] absoluteURLPairs =
                {new ContextControlPair(DESERIALIZE_INSTANCE, "E"),
                        new ContextControlPair(DESERIALIZE_INSTANCE, "R"),
                        new ContextControlPair(SERIALIZE_EXPORT_PUBLIC, "E"),
                        new ContextControlPair(SERIALIZE_EXPORT_PUBLIC, "R")};

        ContextControlPair[] dissemURLPairs =
                {new ContextControlPair(SERIALIZE_EXPORT_PUBLIC, "M"),
                        new ContextControlPair(SERIALIZE_EXPORT_MIGRATE, "M"),
                        new ContextControlPair(SERIALIZE_EXPORT_ARCHIVE, "M")};

        ContextControlPair[] localURLPairs =
                {
                        new ContextControlPair(SERIALIZE_EXPORT_MIGRATE, "E"),
                        new ContextControlPair(SERIALIZE_EXPORT_MIGRATE, "R"),
                        new ContextControlPair(SERIALIZE_STORAGE_INTERNAL, "E"),
                        new ContextControlPair(SERIALIZE_STORAGE_INTERNAL, "R"),
                        new ContextControlPair(SERIALIZE_EXPORT_ARCHIVE, "E"),
                        new ContextControlPair(SERIALIZE_EXPORT_ARCHIVE, "R")};

        // TODO also need one for internal

        ds = new DatastreamXMLMetadata();
        for (ContextControlPair pair : absoluteURLPairs) {
            ds.DSControlGrp = pair.getControlGroup();
            ds.DSLocation =
                    "http://localhost:8080/fedora-demo/simple-image-demo/coliseum-veryhigh.jpg";
            ds2 =
                    subject.normalizeDSLocationURLs(pid, ds, pair
                            .getContext());
            assertEquals(ds.DSLocation, ds2.DSLocation);

            ds.DSLocation = baseURL + "/get/demo:foo/DS1";
            ds2 =
                    subject.normalizeDSLocationURLs(pid, ds, pair
                            .getContext());
            assertEquals(ds.DSLocation, ds2.DSLocation);
        }

        ds = new DatastreamXMLMetadata();
        for (ContextControlPair pair : dissemURLPairs) {
            ds.DatastreamID = "DC";
            ds.DSControlGrp = pair.getControlGroup();
            ds.DSLocation =
                    String
                            .format("%s/get/%s/%s",
                                    baseURL,
                                    pid,
                                    ds.DatastreamID);
            System.setProperty("fedora.appServerContext", appContext);
            ds2 =
                    subject.normalizeDSLocationURLs(pid, ds, pair
                            .getContext());
            assertEquals(ds.DSLocation, ds2.DSLocation);
        }

        ds = new DatastreamXMLMetadata();
        for (ContextControlPair pair : localURLPairs) {
            ds.DatastreamID = "DC";
            ds.DSControlGrp = pair.getControlGroup();
            String url =
                    String
                            .format("%s/get/%s/%s",
                                    baseURL,
                                    pid,
                                    ds.DatastreamID);
            String localURL =
                    String
                            .format("http://local.fedora.server/" + appContext + "/get/%s/%s",
                                    pid,
                                    ds.DatastreamID);
            ds.DSLocation = url;
            ds2 =
                    subject.normalizeDSLocationURLs(pid, ds, pair
                            .getContext());
            assertEquals(localURL, ds2.DSLocation);

            ds.DSLocation = url;
            ds2 =
                    subject.normalizeDSLocationURLs(pid, ds, pair
                            .getContext());
            assertEquals(localURL, ds2.DSLocation);
        }
    }

    /**
     * Test method for
     * {@link org.fcrepo.server.storage.translation.DOTranslationUtility#normalizeInlineXML(java.lang.String, int)}.
     */
    @Test
    public void testNormalizeInlineXML() {
        String xml = "<foo/>";
        String result =
                subject.normalizeInlineXML(xml,
                                            DOTranslationUtility.SERIALIZE_EXPORT_PUBLIC);
        assertEquals(xml, result);
    }

    /**
     * Test method for
     * {@link org.fcrepo.server.storage.translation.DOTranslationUtility#getAuditRecords(java.io.InputStream)}.
     */
    @Test
    public void testGetAuditRecords() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb
                .append("<audit:auditTrail xmlns:audit=\"info:fedora/fedora-system:def/audit#\">");
        sb.append("  <audit:record audit:ID=\"AUDREC1\">");
        sb.append("      <audit:process audit:type=\"Fedora API-M\"/>");
        sb
                .append("      <audit:action>modifyDatastreamByReference</audit:action>");
        sb.append("      <audit:componentID>DRAWING-ICON</audit:componentID>");
        sb
                .append("      <audit:responsibility>fedoraAdmin</audit:responsibility>");
        sb.append("      <audit:date>2005-01-20T22:46:07.428Z</audit:date>");
        sb.append("      <audit:justification>spite</audit:justification>");
        sb.append("  </audit:record>");
        sb.append("  <audit:record audit:ID=\"AUDREC2\">");
        sb.append("      <audit:process audit:type=\"Fedora API-M\"/>");
        sb.append("      <audit:action>modifyDatastreamByValue</audit:action>");
        sb.append("      <audit:componentID>DC</audit:componentID>");
        sb
                .append("      <audit:responsibility>fedoraAdmin</audit:responsibility>");
        sb.append("      <audit:date>2008-01-20T22:46:07.001Z</audit:date>");
        sb.append("      <audit:justification>malice</audit:justification>");
        sb.append("  </audit:record>");
        sb.append("</audit:auditTrail>");
        String auditXML = sb.toString();

        List<AuditRecord> records =
                DOTranslationUtility.getAuditRecords(new ByteArrayInputStream(auditXML.getBytes("utf-8")));

        assertEquals(2, records.size());
        assertEquals("AUDREC1", records.get(0).id);
        assertEquals("modifyDatastreamByReference", records.get(0).action);
        assertEquals("DRAWING-ICON", records.get(0).componentID);
        assertEquals(DateUtility.convertStringToDate("2005-01-20T22:46:07.428Z"), records.get(0).date);
        assertEquals("spite", records.get(0).justification);
        assertEquals("Fedora API-M", records.get(0).processType);
        assertEquals("fedoraAdmin", records.get(0).responsibility);

        assertEquals("AUDREC2", records.get(1).id);
        assertEquals("modifyDatastreamByValue", records.get(1).action);
        assertEquals("DC", records.get(1).componentID);
        assertEquals(DateUtility.convertStringToDate("2008-01-20T22:46:07.001Z"),
                     records.get(1).date);
        assertEquals("malice", records.get(1).justification);
        assertEquals("Fedora API-M", records.get(1).processType);
        assertEquals("fedoraAdmin", records.get(1).responsibility);

    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestDOTranslationUtility.class);
    }

    class ContextControlPair {

        private final int context;

        private final String controlGroup;

        public ContextControlPair(int context, String controlGroup) {
            this.context = context;
            this.controlGroup = controlGroup;
        }

        public int getContext() {
            return context;
        }

        public String getControlGroup() {
            return controlGroup;
        }
    }
}
