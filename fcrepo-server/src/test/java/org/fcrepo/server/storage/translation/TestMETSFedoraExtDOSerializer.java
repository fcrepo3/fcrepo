/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.storage.translation;

import static org.fcrepo.common.Constants.METS;
import static org.fcrepo.common.Constants.XLINK;
import static org.fcrepo.common.Models.FEDORA_OBJECT_3_0;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.DatastreamManagedContent;
import org.fcrepo.server.storage.types.DatastreamReferencedContent;
import org.fcrepo.server.storage.types.DatastreamXMLMetadata;
import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.utilities.TestBase64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;



/**
 * Common unit tests for METSFedoraExt serializers.
 *
 * @author Chris Wilper
 */
public abstract class TestMETSFedoraExtDOSerializer
        extends TestXMLDOSerializer {

    protected static final String ROOT_PATH = "/" + METS.METS.qName;

    protected static final String AMDSEC_PATH =
            ROOT_PATH + "/" + METS.AMD_SEC.qName;
    
    protected static final String SERIALIZED_DS_CONTENT =
        "<" + METS.prefix + ":FContent> \n              " + TestBase64.FOO_STRING_ENCODED + "\n</" + METS.prefix + ":FContent>\n";

    TestMETSFedoraExtDOSerializer(DOSerializer serializer) {
        super(serializer);
    }

    //---
    // Setup/Teardown
    //---

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put(METS.prefix, METS.uri);
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
    public void testOBJIDAttribute() throws TransformerException, XpathException {
        DigitalObject obj = createTestObject(FEDORA_OBJECT_3_0);
        Document xml = doSerializeOrFail(obj);
        assertXpathExists(ROOT_PATH + "[@OBJID='" + TEST_PID + "']", xml);
    }

    //@Test
    /* FIXME: not sure how this one is supposed to work in METS... */
    //public void testCommonFedoraObjectTypes() throws TransformerException {
    //    DigitalObject obj;
    //    Document xml;
    //    obj = createTestObject(DigitalObject.FEDORA_OBJECT);
    //    xml = doSerializeOrFail(obj);
    //    assertXpathExists(ROOT_PATH + "[@TYPE='" + MODEL.DATA_OBJECT.localName
    //            + "']", xml);
    //    obj = createTestObject(DigitalObject.FEDORA_SERVICE_DEPLOYMENT_OBJECT);
    //    xml = doSerializeOrFail(obj);
    //    assertXpathExists(ROOT_PATH + "[@TYPE='"
    //            + MODEL.SERVICE_DEPLOYMENT_OBJECT.localName + "']", xml);
    //    obj = createTestObject(DigitalObject.FEDORA_SERVICE_DEFINITION_OBJECT);
    //    xml = doSerializeOrFail(obj);
    //    assertXpathExists(ROOT_PATH + "[@TYPE='"
    //            + MODEL.SERVICE_DEFINITION_OBJECT.localName + "']", xml);
    //}
    @Test
    public void testNoDatastreams() throws TransformerException, XpathException {
        DigitalObject obj = createTestObject(FEDORA_OBJECT_3_0);
        Document xml = doSerializeOrFail(obj);

        /* rels-ext */
        assertXpathEvaluatesTo("1", "count(" + AMDSEC_PATH + ")", xml);
    }

    @Test
    public void testTwoInlineDatastreams() throws TransformerException, XpathException, XPathExpressionException {
        DigitalObject obj = createTestObject(FEDORA_OBJECT_3_0);

        final String dsID1 = "DS1";
        DatastreamXMLMetadata ds1 = createXDatastream(dsID1);

        final String dsID2 = "DS2";
        DatastreamXMLMetadata ds2 = createXDatastream(dsID2);

        obj.addDatastreamVersion(ds1, true);
        obj.addDatastreamVersion(ds2, true);
        Document xml = doSerializeOrFail(obj);
        
        assertXpathEvaluatesTo("3", "count(" + AMDSEC_PATH + ")", xml);
    }
    @Test
    public void testTwoDataStreamsVersion() throws TransformerException, XpathException, XPathExpressionException {
        DigitalObject obj = createTestObject(FEDORA_OBJECT_3_0);

        final String dsID1 = "DS1";
        final String dsID2 = "DS2";
        // hugely randomly generated test data 
        DatastreamManagedContent ds1 = createMDatastream(dsID1, "aölksdiudshfljdsfnalj mdscmjlfjaö nsaölkjfsölkjfsöldkjfaöslfjasödflaöl".getBytes());
        DatastreamManagedContent ds2 = createMDatastream(dsID2, "älkfddöslfjsölkfjäaoiam,yjöoicncäaskcäaäöl kf,jvdhfkjh".getBytes());


        obj.addDatastreamVersion(ds1, true);
        obj.addDatastreamVersion(ds2, true);
        Document xml = doSerializeOrFail(obj);

        // was unable to do this with assertXpathsNotEqual() method
        // therefore do the assertions by xpath manually
        XPath xp = XPathFactory.newInstance().newXPath();
        xp.setNamespaceContext(new javax.xml.namespace.NamespaceContext() {
			@Override
			public Iterator<String> getPrefixes(String namespaceURI) {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getPrefix(String namespaceURI) {
				return "METS";
			}
			@Override
			public String getNamespaceURI(String prefix) {
				return "http://www.loc.gov/METS/";
			}
		});
        XPathExpression expr = xp.compile("//METS:fileGrp[@ID='DS1']/METS:file");
        NodeList list = (NodeList) expr.evaluate(xml, XPathConstants.NODESET);
        String checksum1 = list.item(0).getAttributes().getNamedItem("CHECKSUM").toString();
        expr = xp.compile("//METS:fileGrp[@ID='DS2']/METS:file");
        list = (NodeList) expr.evaluate(xml, XPathConstants.NODESET);
        String checkSum2 = list.item(0).getAttributes().getNamedItem("CHECKSUM").toString();
        assertFalse(checksum1.equals(checkSum2));
    }
    @Test
    public void testDatastreamContentSerialization()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Datastream dsc = createMDatastream("DS1", TestBase64.FOO_BYTES);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(bos);
        Method testMethod = METSFedoraExtDOSerializer.class.getDeclaredMethod("serializeDatastreamContent", Datastream.class, PrintWriter.class);
        boolean accessible = testMethod.isAccessible();
        if ( !accessible ) testMethod.setAccessible(true);
        try{
            testMethod.invoke(this.m_serializer, dsc, pw);
        }
        finally {
            if ( !accessible ) testMethod.setAccessible( accessible );
        }
        pw.flush();
        String actual = bos.toString();
        assertEquals(SERIALIZED_DS_CONTENT, actual);
    }

    //---
    // Instance Helpers
    //---

    protected void doTestXLinkNamespace() throws TransformerException, XpathException {
        DigitalObject obj = createTestObject(FEDORA_OBJECT_3_0);
        final String url = "http://example.org/DS1";
        DatastreamReferencedContent ds = createRDatastream("DS1", url);
        obj.addDatastreamVersion(ds, true);
        Document xml = doSerializeOrFail(obj);
        String xpath =
                ROOT_PATH + "/" + METS.FILE_SEC.qName + "/"
                        + METS.FILE_GRP.qName + "[@ID='DATASTREAMS']" + "/"
                        + METS.FILE_GRP.qName + "[@ID='DS1']" + "/"
                        + METS.FILE.qName + "/" + METS.FLOCAT.qName + "[@"
                        + XLINK.HREF.qName + "='" + url + "']";
        assertXpathExists(xpath, xml);
    }

}
