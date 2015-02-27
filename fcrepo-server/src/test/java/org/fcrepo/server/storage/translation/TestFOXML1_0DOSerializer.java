/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.storage.translation;


import org.fcrepo.server.storage.translation.FOXML1_0DOSerializer;

/**
 * Unit tests for FOXML1_0DOSerializer.
 *
 * @author Chris Wilper
 */
public class TestFOXML1_0DOSerializer
        extends TestFOXMLDOSerializer {

    public TestFOXML1_0DOSerializer() {
        // superclass sets protected field m_serializer as given below
        super(new FOXML1_0DOSerializer(translationUtility()));
    }

    //---
    // Tests
    //---

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestFOXML1_0DOSerializer.class);
    }

}
