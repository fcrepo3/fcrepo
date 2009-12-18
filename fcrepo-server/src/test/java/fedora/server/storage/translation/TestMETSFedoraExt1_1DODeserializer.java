/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

import org.junit.Test;

import fedora.common.Models;

/**
 * Unit tests for METSFedoraExt1_1DODeserializer.
 *
 * @author Chris Wilper
 */
public class TestMETSFedoraExt1_1DODeserializer
        extends TestMETSFedoraExtDODeserializer {

    public TestMETSFedoraExt1_1DODeserializer() {
        // superclass sets protected fields
        // m_deserializer and m_serializer as given below
        super(new METSFedoraExt1_1DODeserializer(),
              new METSFedoraExt1_1DOSerializer());
    }

    //---
    // Tests
    //---

    @Test
    public void testDeserializeSimpleCModelObject() {
        doSimpleTest(Models.CONTENT_MODEL_3_0);
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestMETSFedoraExt1_1DODeserializer.class);
    }

}
