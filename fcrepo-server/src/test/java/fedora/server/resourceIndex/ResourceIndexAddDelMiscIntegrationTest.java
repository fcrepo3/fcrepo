/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.resourceIndex;

import java.util.Collections;
import java.util.Set;

import org.jrdf.graph.Triple;

import org.junit.Test;

import fedora.server.storage.types.DigitalObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Miscellaneous tests of adding and deleting objects from the RI. Note: All
 * tests run at RI level 1 unless otherwise noted.
 *
 * @author Chris Wilper
 */
public class ResourceIndexAddDelMiscIntegrationTest
        extends ResourceIndexIntegrationTest {

    /**
     * Add, then delete an object with the RI at level 0. This test ensures that
     * adds and deletes at level 0 don't do anything.
     */
    @Test
    public void testAddDelObjLv0() throws Exception {
        Set<DigitalObject> objects = getTestObjects(1, 0);

        // add at level 0
        initRI(0);
        addAll(objects, true);

        assertEquals("Did not get expected triples after add",
                     Collections.EMPTY_SET,
                     getActualTriples());

        // add at level 1
        initRI(1);
        addAll(objects, true);
        Set<Triple> expected = getExpectedTriples(1, objects);

        // delete at level 0
        initRI(0);
        deleteAll(objects, true);

        assertTrue("Did not get expected triples after delete",
                   sameTriples(expected, getActualTriples(), true));
    }

    /**
     * Add, then delete several objects, each with one datastream.
     */
    @Test
    public void testAddDelMultiObjOneDS() throws Exception {
        Set<DigitalObject> objects = getTestObjects(5, 1);
        doAddDelTest(1, objects);
    }

    /**
     * Add, then delete several objects, each with several datastreams.
     */
    @Test
    public void testAddDelMultiObjMultiDS() throws Exception {
        Set<DigitalObject> objects = getTestObjects(5, 5);
        doAddDelTest(1, objects);
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(ResourceIndexAddDelMiscIntegrationTest.class);
    }

}
