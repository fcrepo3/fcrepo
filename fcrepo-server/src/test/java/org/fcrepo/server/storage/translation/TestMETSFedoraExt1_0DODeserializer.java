/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.storage.translation;

import org.junit.Test;

import org.fcrepo.server.storage.translation.METSFedoraExt1_0DODeserializer;
import org.fcrepo.server.storage.translation.METSFedoraExt1_0DOSerializer;

/**
 * Unit tests for METSFedoraExt1_0DODeserializer.
 *
 * @author Chris Wilper
 */
public class TestMETSFedoraExt1_0DODeserializer
        extends TestMETSFedoraExtDODeserializer {

    public TestMETSFedoraExt1_0DODeserializer() {
        // superclass sets protected fields
        // m_deserializer and m_serializer as given below
        super(new METSFedoraExt1_0DODeserializer(translationUtility()),
              new METSFedoraExt1_0DOSerializer(translationUtility()));
    }

    //---
    // Tests
    //---

    @Test
    /* FIXME: no longer disseminators, so shold probably remove test */
    //public void testTwoDisseminators() {
    //    doTestTwoDisseminators();
    //}
    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestMETSFedoraExt1_0DODeserializer.class);
    }
}
