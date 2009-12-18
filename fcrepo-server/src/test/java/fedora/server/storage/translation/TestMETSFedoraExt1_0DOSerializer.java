/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static fedora.common.Constants.METS;
import static fedora.common.Constants.OLD_XLINK;

/**
 * Unit tests for METSFedoraExt1_0DOSerializer.
 *
 * @author Chris Wilper
 */
@SuppressWarnings("deprecation")
public class TestMETSFedoraExt1_0DOSerializer
        extends TestMETSFedoraExtDOSerializer {

    public TestMETSFedoraExt1_0DOSerializer() {
        // superclass sets protected field m_serializer as given below
        super(new METSFedoraExt1_0DOSerializer());
    }

    //---
    // Setup/Teardown
    //---

    @Before
    @Override
    public void setUp() {
        super.setUp();
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put(METS.prefix, METS.uri);
        nsMap.put(OLD_XLINK.prefix, OLD_XLINK.uri);
        NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
        XMLUnit.setXpathNamespaceContext(ctx);
    }

    @After
    @Override
    public void tearDown() {
        XMLUnit.setXpathNamespaceContext(SimpleNamespaceContext.EMPTY_CONTEXT);
    }

    //---
    // Tests
    //---

    @Test
    public void testOldXLinkNamespace() throws TransformerException, XpathException {
        doTestXLinkNamespace();
    }

    //@Test
    /* FIXME: No cModel for pre 3.0 objects with disseminators.. deprecate/remove? */
    //public void testTwoDisseminators() throws TransformerException {
    //    DigitalObject obj = createTestObject(DigitalObject.FEDORA_OBJECT);
    //    final String dissID1 = "DISS1";
    //    Disseminator diss1 = createDisseminator(dissID1, 1);
    //    final String dissID2 = "DISS2";
    //    Disseminator diss2 = createDisseminator(dissID2, 1);
    //    obj.disseminators(dissID1).add(diss1);
    //    obj.disseminators(dissID2).add(diss2);

    //    Document xml = doSerializeOrFail(obj);
    //    assertXpathEvaluatesTo("2", "count(" + BEHAVIORSEC_PATH + ")", xml);
    //    assertXpathEvaluatesTo("2", "count(" + STRUCTMAP_PATH + ")", xml);
    //}

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestMETSFedoraExt1_0DOSerializer.class);
    }

    /* FIXME: not quite sure about mets types.. */
    //@Test
    //@Override
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
    //            + "FedoraBMechObject" + "']", xml);

    //    obj = createTestObject(DigitalObject.FEDORA_SERVICE_DEFINITION_OBJECT);
    //    xml = doSerializeOrFail(obj);
    //    assertXpathExists(ROOT_PATH + "[@TYPE='"
    //            + "FedoraBDefObject" + "']", xml);

    //}
}
