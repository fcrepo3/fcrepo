/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.translation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Test;

import fedora.common.Constants;
import fedora.common.Models;
import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.DigitalObject;
import fedora.common.FedoraTestConstants;

/**
 * @author Edwin Shin
 * @version $Id$
 */
public class TestAtomDODeserializer
        extends TestXMLDODeserializer
        implements FedoraTestConstants {

    public TestAtomDODeserializer() {
        super(new AtomDODeserializer(), new AtomDOSerializer());
    }

    public TestAtomDODeserializer(DODeserializer deserializer,
                                  DOSerializer serializer) {
        super(deserializer, serializer);
    }

    @Test
    public void testDeserializeSimpleCModelObject() {
        doSimpleTest(Models.CONTENT_MODEL_3_0);
    }

    @Test
    public void testDeserialize() throws Exception {
        // create a digital object
        DigitalObject original = createTestObject(Models.FEDORA_OBJECT_3_0);
        original.setLastModDate(new Date());
        DatastreamXMLMetadata ds1 = createXDatastream("DS1");
        ds1.DSCreateDT = new Date();
        original.addDatastreamVersion(ds1, true);

        // serialize the object as Atom
        DOSerializer serA = new AtomDOSerializer();
        File f = File.createTempFile("test", null);
        OutputStream out = new FileOutputStream(f);
        serA.serialize(original,
                       out,
                       "utf-8",
                       DOTranslationUtility.SERIALIZE_EXPORT_ARCHIVE);

        // deserialize the object
        DigitalObject candidate = new BasicDigitalObject();
        DODeserializer deserA = new AtomDODeserializer();
        InputStream in = new FileInputStream(f);
        deserA.deserialize(in,
                           candidate,
                           "utf-8",
                           DOTranslationUtility.DESERIALIZE_INSTANCE);

        // check the deserialization
        assertEquals(original.getLastModDate(), candidate.getLastModDate());
        DatastreamXMLMetadata candidateDS =
                (DatastreamXMLMetadata) candidate.datastreams("DS1").iterator()
                        .next();
        assertEquals(ds1.DatastreamID, candidateDS.DatastreamID);
        assertEquals(ds1.DSCreateDT, candidateDS.DSCreateDT);

        // FIXME dsSize tests omitted for now b/c of handling of closing tags
        //assertEquals(ds1.DSSize, candidateDS.DSSize);

        // also make sure we can serialize the object as foxml
        DOSerializer serF = new FOXML1_1DOSerializer();
        serF.serialize(candidate,
                       out,
                       "utf-8",
                       DOTranslationUtility.SERIALIZE_EXPORT_ARCHIVE);
    }

    public void testDeserializeFromDemoObjects() throws Exception {
        String[] demoSources =
                {"atom/local-server-demos/simple-image-demo/sdep_demo_2.xml",
                 "atom/local-server-demos/formatting-objects-demo/obj_demo_26.xml"};
        for (String source : demoSources) {
            File sourceFile = new File(DEMO_DIR_PREFIX + source);
            InputStream in = new FileInputStream(sourceFile);
            DigitalObject candidate = new BasicDigitalObject();
            DODeserializer deserA = new AtomDODeserializer();
            deserA.deserialize(in,
                               candidate,
                               "utf-8",
                               DOTranslationUtility.DESERIALIZE_INSTANCE);
        }
    }
    
    public void testDeserializeZip() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<feed xmlns=\"http://www.w3.org/2005/Atom\">");
        sb.append("  <id>info:fedora/demo:1001</id>");
        sb.append("  <title type=\"text\">Image of Coliseum in Rome</title>");
        sb.append("  <updated>2008-04-30T03:54:31.525Z</updated>");
        sb.append("  <author>");
        sb.append("    <name>fedoraAdmin</name>");
        sb.append("  </author>");
        sb.append("  <category term=\"Active\" scheme=\"info:fedora/fedora-system:def/model#state\"></category>");
        sb.append("  <category term=\"2008-04-30T03:54:31.525Z\" scheme=\"info:fedora/fedora-system:def/model#createdDate\"></category>");
        sb.append("  <icon>http://www.fedora-commons.org/images/logo_vertical_transparent_200_251.png</icon>");
        sb.append("  <entry>");
        sb.append("    <id>info:fedora/demo:1001/DC</id>");
        sb.append("    <title type=\"text\">DC</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <link href=\"info:fedora/demo:1001/DC/2008-04-30T03:54:31.459Z\" rel=\"alternate\"></link>");
        sb.append("    <category term=\"A\" scheme=\"info:fedora/fedora-system:def/model#state\"></category>");
        sb.append("    <category term=\"X\" scheme=\"info:fedora/fedora-system:def/model#controlGroup\"></category>");
        sb.append("    <category term=\"true\" scheme=\"info:fedora/fedora-system:def/model#versionable\"></category>");
        sb.append("  </entry>");
        sb.append("  <entry xmlns:thr=\"http://purl.org/syndication/thread/1.0\">");
        sb.append("    <id>info:fedora/demo:1001/DC/2008-04-30T03:54:31.459Z</id>");
        sb.append("    <title type=\"text\">DC1.0</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <thr:in-reply-to ref=\"info:fedora/demo:1001/DC\"></thr:in-reply-to>");
        sb.append("    <category term=\"DC Record for Coliseum image object\" scheme=\"info:fedora/fedora-system:def/model#label\"></category>");
        sb.append("    <category term=\"DISABLED\" scheme=\"info:fedora/fedora-system:def/model#digestType\"></category>");
        sb.append("    <category term=\"none\" scheme=\"info:fedora/fedora-system:def/model#digest\"></category>");
        sb.append("    <category term=\"491\" scheme=\"info:fedora/fedora-system:def/model#length\"></category>");
        sb.append("       <summary type=\"text\">DC1.0</summary>");
        sb.append("    <content type=\"text/xml\" src=\"DC1.0.xml\"/>");
        sb.append("  </entry>");
        sb.append("  <entry>");
        sb.append("    <id>info:fedora/demo:1001/RELS-EXT</id>");
        sb.append("    <title type=\"text\">RELS-EXT</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <link href=\"info:fedora/demo:1001/RELS-EXT/2008-04-30T03:54:31.459Z\" rel=\"alternate\"></link>");
        sb.append("    <category term=\"A\" scheme=\"info:fedora/fedora-system:def/model#state\"></category>");
        sb.append("    <category term=\"X\" scheme=\"info:fedora/fedora-system:def/model#controlGroup\"></category>");
        sb.append("    <category term=\"false\" scheme=\"info:fedora/fedora-system:def/model#versionable\"></category>");
        sb.append("  </entry>");
        sb.append("  <entry xmlns:thr=\"http://purl.org/syndication/thread/1.0\">");
        sb.append("    <id>info:fedora/demo:1001/RELS-EXT/2008-04-30T03:54:31.459Z</id>");
        sb.append("    <title type=\"text\">RELS-EXT1.0</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <thr:in-reply-to ref=\"info:fedora/demo:1001/RELS-EXT\"></thr:in-reply-to>");
        sb.append("    <category term=\"Relationships\" scheme=\"info:fedora/fedora-system:def/model#label\"></category>");
        sb.append("    <category term=\"DISABLED\" scheme=\"info:fedora/fedora-system:def/model#digestType\"></category>");
        sb.append("    <category term=\"none\" scheme=\"info:fedora/fedora-system:def/model#digest\"></category>");
        sb.append("    <category term=\"472\" scheme=\"info:fedora/fedora-system:def/model#length\"></category>");
        sb.append("    <content type=\"application/rdf+xml\" src=\"RELS-EXT1.0.xml\"/>");
        sb.append("    <summary type=\"text\">RELS-EXT1.0</summary>");
        sb.append("  </entry>");
        sb.append("</feed>");
        
        byte[] demo1001_manifest = sb.toString().getBytes("UTF-8");
        
        sb = new StringBuilder();
        sb.append("      <oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");                 
        sb.append("        <dc:title>Coliseum in Rome</dc:title>");
        sb.append("        <dc:creator>Thornton Staples</dc:creator>");
        sb.append("        <dc:subject>Architecture, Roman</dc:subject>");
        sb.append("        <dc:description>Image of Coliseum in Rome</dc:description>");
        sb.append("        <dc:publisher>University of Virginia Library</dc:publisher>");
        sb.append("        <dc:format>image/jpeg</dc:format>");
        sb.append("        <dc:identifier>demo:1001</dc:identifier>");
        sb.append("      </oai_dc:dc>");
        byte[] demo1001_dc = sb.toString().getBytes("UTF-8");
        
        sb = new StringBuilder();
        sb.append("      <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:fedora-model=\"info:fedora/fedora-system:def/model#\">");
        sb.append("        <rdf:Description rdf:about=\"info:fedora/demo:1001\">");
        sb.append("          <fedora-model:hasModel rdf:resource=\"info:fedora/demo:UVA_STD_IMAGE_1\"></fedora-model:hasModel>");
        sb.append("        </rdf:Description>");
        sb.append("      </rdf:RDF>");
        byte[] demo1001_relsext = sb.toString().getBytes("UTF-8");
        
        ZipEntry manifest = new ZipEntry("atommanifest.xml");
        ZipEntry dc = new ZipEntry("DC1.0.xml");
        ZipEntry relsext = new ZipEntry("RELS-EXT1.0.xml");
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bout);
        zip.putNextEntry(manifest);
        zip.write(demo1001_manifest);
        zip.putNextEntry(dc);
        zip.write(demo1001_dc);
        zip.putNextEntry(relsext);
        zip.write(demo1001_relsext);
        zip.flush();
        zip.close();
        byte[] demo1001ATOMZip = bout.toByteArray();

        InputStream in = new ByteArrayInputStream(demo1001ATOMZip);
        DigitalObject obj = new BasicDigitalObject();
        DODeserializer dser = new AtomDODeserializer(Constants.ATOM_ZIP1_1);
        dser.deserialize(in, obj, "UTF-8", DOTranslationUtility.DESERIALIZE_INSTANCE);
        assertEquals("demo:1001", obj.getPid());
        //TODO more tests
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestAtomDODeserializer.class);
    }

}
