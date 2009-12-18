/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.translation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.Parser;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fedora.common.Constants;
import fedora.common.Models;
import fedora.common.PID;

import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.DigitalObject;

import fedora.utilities.FileUtils;
import fedora.utilities.XmlTransformUtility;

import static fedora.common.Models.FEDORA_OBJECT_3_0;

import static fedora.server.storage.translation.DOTranslationUtility.DESERIALIZE_INSTANCE;
import static fedora.server.storage.translation.DOTranslationUtility.SERIALIZE_EXPORT_ARCHIVE;

/**
 * @author Edwin Shin
 */
public class TestAtomDOSerializer
        extends TestXMLDOSerializer {

    private static final String iso_tron =
            "src/main/resources/schematron/iso_schematron_skeleton.xsl";

    private static final String atom_tron = "src/main/resources/schematron/atom.sch";

    public TestAtomDOSerializer() {
        super(new AtomDOSerializer());
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    @Override
    public void setUp() {
        super.setUp();
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("fedora", "http://www.example.org");
        NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
        XMLUnit.setXpathNamespaceContext(ctx);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    @Override
    public void tearDown() throws Exception {
        XMLUnit.setXpathNamespaceContext(SimpleNamespaceContext.EMPTY_CONTEXT);
    }

    @Test
    public void testSerializeFromFOXML() throws Exception {
        String source =
                "src/main/resources/demo/demo-objects/foxml/local-server-demos/image-collection-demo/dataObjects/demo_SmileyBeerGlass.xml";
        source =
                "src/main/resources/demo/demo-objects/foxml/local-server-demos/formatting-objects-demo/obj_demo_26.xml";
        InputStream in = new FileInputStream(source);
        File f = File.createTempFile("test", null);
        OutputStream out = new FileOutputStream(f);

        DODeserializer deser = new FOXML1_1DODeserializer();
        DigitalObject obj = new BasicDigitalObject();
        deser.deserialize(in, obj, "UTF-8", DESERIALIZE_INSTANCE);

        // some sanity checks
        setObjectDefaults(obj);

        DOSerializer serializer = new AtomDOSerializer();
        serializer.serialize(obj, out, "UTF-8", SERIALIZE_EXPORT_ARCHIVE);
    }

    @Test
    public void testSerialize() throws Exception {
        DigitalObject obj = createTestObject(Models.FEDORA_OBJECT_3_0);
        obj.setLastModDate(new Date());
        DatastreamXMLMetadata ds1 = createXDatastream("DS1");
        ds1.DSCreateDT = new Date();
        obj.addDatastreamVersion(ds1, true);

        OutputStream out = new ByteArrayOutputStream();

        DOSerializer serializer = new AtomDOSerializer();
        serializer.serialize(obj, out, "UTF-8", SERIALIZE_EXPORT_ARCHIVE);
        // TODO
        //validateWithISOSchematron(out.toString());
    }

    @Test
    public void testAtomZip() throws Exception {
        DigitalObject obj = createTestObject(FEDORA_OBJECT_3_0);
        obj.setLastModDate(new Date());
        DatastreamXMLMetadata ds1 = createXDatastream("DS1");
        ds1.DSCreateDT = new Date();
        obj.addDatastreamVersion(ds1, true);

        File f = File.createTempFile("atom", ".zip");
        OutputStream out = new FileOutputStream(f);

        DOSerializer serializer = new AtomDOSerializer(Constants.ATOM_ZIP1_1);
        serializer.serialize(obj, out, "UTF-8", SERIALIZE_EXPORT_ARCHIVE);
        out.close();

        ZipInputStream zip = new ZipInputStream(new FileInputStream(f));
        ZipEntry entry;
        int count = 0;
        while ((entry = zip.getNextEntry()) != null) {
            if (entry.getName().equals("atommanifest.xml")) {
                count++;
                ByteArrayOutputStream manifest = new ByteArrayOutputStream();
                FileUtils.copy(zip, manifest);

                Abdera abdera = Abdera.getInstance();
                Parser parser = abdera.getParser();
                Document<Feed> feedDoc = parser.parse(new StringReader(manifest.toString("UTF-8")));
                Feed feed = feedDoc.getRoot();
                assertEquals(PID.getInstance(TEST_PID).toURI(), feed.getId().toString());
                // TODO other tests?
            }
        }
        assertEquals("Expected exactly 1 manifest file", 1, count);
        zip.close();

        //f.delete();
    }

    // TODO
    private void validateWithISOSchematron(String candidate)
            throws TransformerException, IOException {
        StreamSource skeleton = new StreamSource(new File(iso_tron));
        StreamSource schema = new StreamSource(new File(atom_tron));
        StringWriter temp = new StringWriter();
        StreamResult result = new StreamResult(temp);

        // generate the stylesheet
        TransformerFactory factory = XmlTransformUtility.getTransformerFactory();
        Transformer xform = factory.newTransformer(skeleton);
        xform.transform(schema, result);
        temp.flush();
        temp.close();
        String stylesheet = temp.toString();

        // now flip
        StringReader in = new StringReader(stylesheet);
        StreamSource sheet = new StreamSource(in);
        Transformer validator = factory.newTransformer(sheet);
        validator.setOutputProperty("method", "text");
        temp = new StringWriter();
        result = new StreamResult(temp);
        validator.transform(new StreamSource(new StringReader(candidate)),
                            result);
        temp.flush();
        String output = temp.toString();

        // Check for no output if all tests pass.
        assertEquals(output, "", output);
    }

    private void setObjectDefaults(DigitalObject obj) {
        if (obj.getCreateDate() == null) obj.setCreateDate(new Date());

        Iterator<String> dsIds = obj.datastreamIdIterator();
        while (dsIds.hasNext()) {
            String dsid = dsIds.next();
            for (Datastream ds : obj.datastreams(dsid)) {
                if (ds.DSCreateDT == null) {
                    ds.DSCreateDT = new Date();
                }
            }
        }
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestAtomDOSerializer.class);
    }

}
