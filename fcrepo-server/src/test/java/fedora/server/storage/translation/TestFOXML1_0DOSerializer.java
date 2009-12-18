/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

import static fedora.common.Constants.FOXML;

/**
 * Unit tests for FOXML1_0DOSerializer.
 *
 * @author Chris Wilper
 */
@SuppressWarnings("deprecation")
public class TestFOXML1_0DOSerializer
        extends TestFOXMLDOSerializer {

    private static final String DISSEMINATOR_PATH =
            ROOT_PATH + "/" + FOXML.DISSEMINATOR.qName;

    public TestFOXML1_0DOSerializer() {
        // superclass sets protected field m_serializer as given below
        super(new FOXML1_0DOSerializer());
    }

    //---
    // Tests
    //---

    /*
     * FIXME: No content model for pre 3.0 objects, so let's not do this for now
     * unless we decide we are really supporting pre-3.0 semantics..
     */
    //public void testTwoDisseminators() throws TransformerException {
    //    DigitalObject obj = createTestObject(DigitalObject.FEDORA_OBJECT);
    //    final String dissID1 = "DISS1";
    //    Disseminator diss1 = createDisseminator(dissID1, 1);
    //    final String dissID2 = "DISS2";
    //    Disseminator diss2 = createDisseminator(dissID2, 1);
    //    obj.disseminators(dissID1).add(diss1);
    //    obj.disseminators(dissID2).add(diss2);

    //    Document xml = doSerializeOrFail(obj);
    //    assertXpathEvaluatesTo("2", "count(" + DISSEMINATOR_PATH + ")", xml);
    //}

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestFOXML1_0DOSerializer.class);
    }

}
