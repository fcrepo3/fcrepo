/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

import org.junit.Test;

/**
 * Unit tests for FOXML1_0DODeserializer.
 *
 * @author Chris Wilper
 */
public class TestFOXML1_0DODeserializer
        extends TestFOXMLDODeserializer {

    public TestFOXML1_0DODeserializer() {
        // superclass sets protected fields
        // m_deserializer and m_serializer as given below
        super(new FOXML1_0DODeserializer(), new FOXML1_0DOSerializer());
    }

    //---
    // Tests
    //---

    @Test
    public void testTwoDisseminators() {
        doTestTwoDisseminators();
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestFOXML1_0DODeserializer.class);
    }

}
