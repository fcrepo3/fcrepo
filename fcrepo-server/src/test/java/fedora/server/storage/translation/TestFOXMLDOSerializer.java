/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.TransformerException;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;

import org.jrdf.graph.Literal;
import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.PredicateNode;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fedora.common.rdf.RDFName;

import fedora.server.storage.RDFRelationshipReader;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.types.RelationshipTuple;

import static fedora.common.Constants.FOXML;
import static fedora.common.Constants.MODEL;
import static fedora.common.Constants.RDF;
import static fedora.common.Models.CONTENT_MODEL_3_0;
import static fedora.common.Models.FEDORA_OBJECT_3_0;
import static fedora.common.Models.SERVICE_DEFINITION_3_0;
import static fedora.common.Models.SERVICE_DEPLOYMENT_3_0;

/**
 * Common unit tests for FOXML serializers.
 *
 * @author Chris Wilper
 */
public abstract class TestFOXMLDOSerializer
        extends TestXMLDOSerializer {

    protected static final String ROOT_PATH = "/" + FOXML.DIGITAL_OBJECT.qName;

    protected static final String PROPERTIES_PATH =
            ROOT_PATH + "/" + FOXML.OBJECT_PROPERTIES.qName;

    protected static final String PROPERTY_PATH =
            PROPERTIES_PATH + "/" + FOXML.PROPERTY.qName;

    protected static final String DATASTREAM_PATH =
            ROOT_PATH + "/" + FOXML.DATASTREAM.qName;

    TestFOXMLDOSerializer(DOSerializer serializer) {
        super(serializer);
    }

    //---
    // Setup/Teardown
    //---

    @Before
    @Override
    public void setUp() {
        super.setUp();
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put(FOXML.prefix, FOXML.uri);
        NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
        XMLUnit.setXpathNamespaceContext(ctx);
    }

    @Override
    @After
    public void tearDown() {
        XMLUnit.setXpathNamespaceContext(SimpleNamespaceContext.EMPTY_CONTEXT);
    }

    //---
    // Tests
    //---

    @Test
    public void testPIDAttribute() throws TransformerException, XpathException {
        DigitalObject obj = createTestObject(FEDORA_OBJECT_3_0);
        Document xml = doSerializeOrFail(obj);
        assertXpathExists(ROOT_PATH + "[@PID='" + TEST_PID + "']", xml);
    }

    @Test
    public void testCommonFedoraObjectTypes() throws TransformerException {
        DigitalObject obj;
        Document xml;

        obj = createTestObject(FEDORA_OBJECT_3_0);
        xml = doSerializeOrFail(obj);
        checkRelationships(xml, MODEL.HAS_MODEL, FEDORA_OBJECT_3_0);

        obj = createTestObject(CONTENT_MODEL_3_0);
        xml = doSerializeOrFail(obj);
        checkRelationships(xml,
                           MODEL.HAS_MODEL,
                           CONTENT_MODEL_3_0);

        obj = createTestObject(SERVICE_DEFINITION_3_0);
        xml = doSerializeOrFail(obj);
        checkRelationships(xml,
                           MODEL.HAS_MODEL,
                           SERVICE_DEFINITION_3_0);

        obj = createTestObject(SERVICE_DEPLOYMENT_3_0);
        xml = doSerializeOrFail(obj);
        checkRelationships(xml,
                           MODEL.HAS_MODEL,
                           SERVICE_DEPLOYMENT_3_0);
    }

    @Test
    public void testTwoInlineDatastreams() throws TransformerException, XpathException {
        DigitalObject obj = createTestObject(FEDORA_OBJECT_3_0);

        final String dsID1 = "DS1";
        DatastreamXMLMetadata ds1 = createXDatastream(dsID1);

        final String dsID2 = "DS2";
        DatastreamXMLMetadata ds2 = createXDatastream(dsID2);

        obj.addDatastreamVersion(ds1, true);
        obj.addDatastreamVersion(ds2, true);
        Document xml = doSerializeOrFail(obj);

        /* 3 datastreams: rels-ext, ds1, and ds2 */
        assertXpathEvaluatesTo("3", "count(" + DATASTREAM_PATH + ")", xml);
    }

    //---
    // Instance helpers
    //---

    protected void checkProperty(Document xml, RDFName name, String value)
            throws TransformerException, XpathException {
        assertXpathExists(PROPERTY_PATH + "[@NAME='" + name.uri + "'"
                + " and @VALUE='" + value + "']", xml);
    }

    protected void checkRelationships(Document xml,
                                      PredicateNode pred,
                                      ObjectNode... nodes) {
        NodeList streams =
                xml.getElementsByTagNameNS(FOXML.DATASTREAM.namespace.uri,
                                           FOXML.DATASTREAM_VERSION.localName);

        if (streams.getLength() == 0) {
            fail("No relationships found.  Serializer "
                    + m_serializer.getClass().getName());
        }

        /*
         * Get the latest RELS-EXT, assuming that the latest has the maximal
         * datastream version ID...
         */
        Element lastDS = null;
        String maxId = "";
        for (int i = 0; i < streams.getLength(); i++) {
            Element ds = (Element) streams.item(i);
            String id = ds.getAttribute("ID");
            if (id.startsWith("RELS-EXT") && id.compareTo(maxId) > 0) {
                maxId = id;
                lastDS = ds;
            }
        }

        NodeList rdf = lastDS.getElementsByTagNameNS(RDF.uri, "RDF");
        if (rdf.getLength() != 1) {
            fail("Could not locate valid RDF");
        }

        Element rdfRels = (Element) rdf.item(0);

        Set<RelationshipTuple> rels = new HashSet<RelationshipTuple>();

        try {

            StringWriter sout = new StringWriter();

            OutputFormat formatter = new OutputFormat();
            formatter.setEncoding("UTF-8"); // is the default

            XMLSerializer serializer = new XMLSerializer(sout, formatter);
            serializer.serialize(rdfRels);
            rels =
                    RDFRelationshipReader
                            .readRelationships(new ByteArrayInputStream(sout
                                    .toString().getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        /* Finally, check the relationships the hard way */
        for (ObjectNode value : nodes) {
            boolean found = false;
            for (RelationshipTuple rel : rels) {
                if (rel.predicate.equals(pred.toString())
                        && rel.object.equals(value.toString())) {
                    if ((value instanceof Literal) == rel.isLiteral)
                        found = true;
                    break;
                }
            }

            if (!found) {
                fail("Failed to find relationship " + pred + " = " + value);
            }
        }
    }
}
