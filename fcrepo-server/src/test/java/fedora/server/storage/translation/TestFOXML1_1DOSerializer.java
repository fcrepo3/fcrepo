/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

import javax.xml.transform.TransformerException;

import org.custommonkey.xmlunit.exceptions.XpathException;

import org.junit.Test;

import org.w3c.dom.Document;

import fedora.server.storage.types.DigitalObject;

import static fedora.common.Models.CONTENT_MODEL_3_0;
import static fedora.common.Models.FEDORA_OBJECT_3_0;

/**
 * Unit tests for FOXML1_1DOSerializer.
 *
 * @author Chris Wilper
 */
public class TestFOXML1_1DOSerializer
        extends TestFOXMLDOSerializer {

    public TestFOXML1_1DOSerializer() {
        // superclass sets protected field m_serializer as given below
        super(new FOXML1_1DOSerializer());
    }

    //---
    // Tests
    //---

    @Test
    public void testVersionAttribute() throws TransformerException, XpathException {
        DigitalObject obj = createTestObject(FEDORA_OBJECT_3_0);
        Document xml = doSerializeOrFail(obj);
        assertXpathExists(ROOT_PATH + "[@VERSION = '1.1']", xml);
    }

    @Test
    public void testSerializeSimpleCModelObject() {
        doSerializeAllOrFail(createTestObject(CONTENT_MODEL_3_0));
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestFOXML1_1DOSerializer.class);
    }

}
