/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.resourceIndex;

import java.util.HashSet;
import java.util.Set;

import org.jrdf.graph.Triple;

import org.junit.Test;

import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.types.ObjectBuilder;

import static org.junit.Assert.assertTrue;

/**
 * Miscellaneous tests of modifying existing objects in the RI. Note: All tests
 * run at RI level 1 unless otherwise noted.
 *
 * @author Chris Wilper
 */
public class ResourceIndexModMiscIntegrationTest
        extends ResourceIndexIntegrationTest {

    /**
     * Modify an object's label with the RI at level 0.
     */
    @Test
    public void testModObjOnceLabelLv0() throws Exception {
        Set<DigitalObject> objects = getTestObjects(1, 0);

        // add at level 1
        initRI(1);
        addAll(objects, true);
        Set<Triple> origTriples = getExpectedTriples(1, objects);

        // mod at level 0
        DigitalObject original = (DigitalObject) objects.toArray()[0];
        DigitalObject modified = ObjectBuilder.deepCopy(original);
        modified.setLabel("new label");
        initRI(0);
        modify(original, modified, true);

        assertTrue("Did not get expected orig triples after modify at level 0",
                   sameTriples(origTriples, getActualTriples(), true));
    }

    /**
     * Modify an object's label once.
     */
    @Test
    public void testModObjOnceLabel() throws Exception {
        DigitalObject original = getTestObject("test:1", "test1");

        DigitalObject modified = ObjectBuilder.deepCopy(original);
        modified.setLabel("new label");

        doModifyTest(1, original, modified);
    }

    /**
     * Modify an object's label multiple times.
     */
    @Test
    public void testModObjMultiLabel() throws Exception {

        // prep by initting at lv 1 and adding original object
        initRI(1);
        DigitalObject previous = getTestObject("test:1", "test1");
        Set<DigitalObject> origSet = new HashSet<DigitalObject>();
        origSet.add(previous);
        addAll(origSet, true);

        // modify the label multiple times
        for (int i = 1; i <= 5; i++) {
            DigitalObject modified = ObjectBuilder.deepCopy(previous);
            modified.setLabel("new label " + i);
            doModifyTest(-1, previous, modified);
            previous = modified;
        }

    }

    /**
     * Modify an object's label multiple times while flushing the buffer many
     * times from a separate thread.
     */
    @Test
    public void testModObjMultiLabelAsyncFlush() throws Exception {

        // prep by initting at lv 1 and adding original object
        initRI(1);
        DigitalObject original = getTestObject("test:1", "test1");
        DigitalObject previous = original;
        Set<DigitalObject> origSet = new HashSet<DigitalObject>();
        origSet.add(previous);
        addAll(origSet, true);

        // hold on to the original triples so we can compare later
        Set<Triple> origTriples = getExpectedTriples(1, origSet);

        // modify the label multiple times while flushing
        startFlushing(0);
        try {
            for (int i = 0; i <= 5; i++) {
                DigitalObject modified = ObjectBuilder.deepCopy(previous);
                modified.setLabel("new label " + i);
                modify(previous, modified, false);
                previous = modified;
            }
            // last change puts obj back into original state
            modify(previous, original, false);
        } finally {
            // this stops async flushing and flushes one last time
            finishFlushing();
        }

        assertTrue("Did not get expected orig triples after multi mod of "
                           + "one object with async buffer flushing",
                   sameTriples(origTriples, getActualTriples(), true));
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(ResourceIndexModMiscIntegrationTest.class);
    }

}
