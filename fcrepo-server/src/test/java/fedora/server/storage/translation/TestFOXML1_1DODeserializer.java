/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

import org.junit.Test;

import static fedora.common.Models.CONTENT_MODEL_3_0;

/**
 * Unit tests for FOXML1_1DODeserializer.
 *
 * @author Chris Wilper
 */
public class TestFOXML1_1DODeserializer
        extends TestFOXMLDODeserializer {

    public TestFOXML1_1DODeserializer() {
        // superclass sets protected fields
        // m_deserializer and m_serializer as given below
        super(new FOXML1_1DODeserializer(), new FOXML1_1DOSerializer());
    }

    //---
    // Tests
    //---

    @Test
    public void testDeserializeSimpleCModelObject() {
        doSimpleTest(CONTENT_MODEL_3_0);
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestFOXML1_1DODeserializer.class);
    }

}
