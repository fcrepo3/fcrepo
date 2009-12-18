/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.lowlevel.akubra;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;

import org.junit.Before;
import org.junit.Test;

import org.akubraproject.mem.MemBlobStore;

import fedora.common.FaultException;

import fedora.server.errors.LowlevelStorageException;
import fedora.server.errors.ObjectAlreadyInLowlevelStorageException;
import fedora.server.errors.ObjectNotInLowlevelStorageException;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link AkubraLowlevelStorage}
 *
 * @author Chris Wilper
 */
public class AkubraLowlevelStorageTest {

    private static final String OBJ_KEY = "test:obj";
    private static final String OBJ_CONTENT = "obj-content";
    private static final String OBJ_CONTENT2 = "obj-content2";

    private static final String DS_KEY = OBJ_KEY + "+DS+DS.0";
    private static final String DS_CONTENT = "ds-content";
    private static final String DS_CONTENT2 = "ds-content2";

    private AkubraLowlevelStorage instance;
    private AkubraLowlevelStorage safeInstance;

    @Before
    public void setUp() {
        this.instance = getInstance(false, false);
        this.safeInstance = getInstance(true, true);
    }

    /** Adding an existing datastream should fail. */
    @Test (expected=ObjectAlreadyInLowlevelStorageException.class)
    public void testAddExistingDatastream() throws LowlevelStorageException {
        instance.addDatastream(DS_KEY, toStream(DS_CONTENT));
        instance.addDatastream(DS_KEY, toStream(DS_CONTENT));
    }

    /** Adding an existing object should fail. */
    @Test (expected=ObjectAlreadyInLowlevelStorageException.class)
    public void testAddExistingObject() throws LowlevelStorageException {
        instance.addObject(OBJ_KEY, toStream(OBJ_CONTENT));
        instance.addObject(OBJ_KEY, toStream(OBJ_CONTENT));
    }

    /** Adding a new datastream should succeed. */
    @Test
    public void testAddNonExistingDatastream() throws Exception {
        instance.addDatastream(DS_KEY, toStream(DS_CONTENT));
    }

    /** Adding a new object should succeed. */
    @Test
    public void testAddNonExistingObject() throws Exception {
        instance.addObject(OBJ_KEY, toStream(OBJ_CONTENT));
    }

    /** Datastream audit should not throw an exception. */
    @Test
    public void testAuditDatastream() throws Exception {
        instance.auditDatastream();
    }

    /** Object audit should not throw an exception. */
    @Test
    public void testAuditObject() throws Exception {
        instance.auditObject();
    }

    /** Datastream rebuild should not throw an exception. */
    @Test
    public void testRebuildDatastream() throws Exception {
        instance.rebuildDatastream();
    }

    /** Object rebuild should not throw an exception. */
    @Test
    public void testRebuildObject() throws Exception {
        instance.rebuildObject();
    }

    /** Removing an existing datastream should succeed. */
    @Test
    public void testRemoveExistingDatastream() throws Exception {
        instance.addDatastream(DS_KEY, toStream(DS_CONTENT));
        instance.removeDatastream(DS_KEY);
    }

    /** Removing an existing object should succeed. */
    @Test
    public void testRemoveExistingObject() throws Exception {
        instance.addObject(OBJ_KEY, toStream(OBJ_CONTENT));
        instance.removeObject(OBJ_KEY);
    }

    /** Removing a non-existing datastream should fail. */
    @Test (expected=ObjectNotInLowlevelStorageException.class)
    public void testRemoveNonExistingDatastream() throws Exception {
        instance.removeDatastream(DS_KEY);
    }

    /** Removing a non-existing object should fail. */
    @Test (expected=ObjectNotInLowlevelStorageException.class)
    public void testRemoveNonExistingObject() throws Exception {
        instance.removeObject(OBJ_KEY);
    }

    /** Replacing an existing datastream should succeed. */
    @Test
    public void testReplaceExistingDatastream() throws Exception {
        instance.addDatastream(DS_KEY, toStream(DS_CONTENT));
        instance.replaceDatastream(DS_KEY, toStream(DS_CONTENT2));
        List<String> list = toList(instance.listDatastreams());
        assertEquals(1, list.size());
        assertEquals(DS_CONTENT2,
                     toString(instance.retrieveDatastream(DS_KEY)));
    }

    /** Replacing an existing datastream "safely" should succeed. */
    @Test
    public void testReplaceExistingDatastreamSafely() throws Exception {
        safeInstance.addDatastream(DS_KEY, toStream(DS_CONTENT));
        safeInstance.replaceDatastream(DS_KEY, toStream(DS_CONTENT2));
        List<String> list = toList(safeInstance.listDatastreams());
        assertEquals(1, list.size());
        assertEquals(DS_CONTENT2,
                     toString(safeInstance.retrieveDatastream(DS_KEY)));
    }

    /** Replacing an existing object should succeed. */
    @Test
    public void testReplaceExistingObject() throws Exception {
        instance.addObject(OBJ_KEY, toStream(OBJ_CONTENT));
        instance.replaceObject(OBJ_KEY, toStream(OBJ_CONTENT2));
        List<String> list = toList(instance.listObjects());
        assertEquals(1, list.size());
        assertEquals(OBJ_CONTENT2,
                     toString(instance.retrieveObject(OBJ_KEY)));
    }

    /** Replacing an existing object "safely" should succeed. */
    @Test
    public void testReplaceExistingObjectSafely() throws Exception {
        safeInstance.addObject(OBJ_KEY, toStream(OBJ_CONTENT));
        safeInstance.replaceObject(OBJ_KEY, toStream(OBJ_CONTENT2));
        List<String> list = toList(safeInstance.listObjects());
        assertEquals(1, list.size());
        assertEquals(OBJ_CONTENT2,
                     toString(safeInstance.retrieveObject(OBJ_KEY)));
    }

    /** Replacing a non-existing datastream should fail. */
    @Test (expected=ObjectNotInLowlevelStorageException.class)
    public void testReplaceNonExistingDatastream() throws Exception {
        instance.replaceDatastream(DS_KEY, toStream(DS_CONTENT));
    }

    /** Replacing a non-existing datastream "safely" should fail. */
    @Test (expected=ObjectNotInLowlevelStorageException.class)
    public void testReplaceNonExistingDatastreamSafely() throws Exception {
        safeInstance.replaceDatastream(DS_KEY, toStream(DS_CONTENT));
    }

    /** Replacing a non-existing object should fail. */
    @Test (expected=ObjectNotInLowlevelStorageException.class)
    public void testReplaceNonExistingObject() throws Exception {
        instance.replaceObject(OBJ_KEY, toStream(OBJ_CONTENT));
    }

    /** Replacing a non-existing object "safely" should fail. */
    @Test (expected=ObjectNotInLowlevelStorageException.class)
    public void testReplaceNonExistingObjectSafely() throws Exception {
        safeInstance.replaceObject(OBJ_KEY, toStream(OBJ_CONTENT));
    }

    /** Retrieving an existing datastream should succeed. */
    @Test
    public void testRetrieveExistingDatastream() throws Exception {
        instance.addDatastream(DS_KEY, toStream(DS_CONTENT));
        assertEquals(DS_CONTENT,
                     toString(instance.retrieveDatastream(DS_KEY)));
    }

    /** Retrieving an existing object should succeed. */
    @Test
    public void testRetrieveExistingObject() throws Exception {
        instance.addObject(OBJ_KEY, toStream(OBJ_CONTENT));
        assertEquals(OBJ_CONTENT,
                     toString(instance.retrieveObject(OBJ_KEY)));
    }

    /** Retrieving a non-existing datastream should fail. */
    @Test (expected=ObjectNotInLowlevelStorageException.class)
    public void testRetrieveNonExistingDatastream() throws Exception {
        instance.retrieveDatastream(DS_KEY);
    }

    /** Retrieving a non-existing object should fail. */
    @Test (expected=ObjectNotInLowlevelStorageException.class)
    public void testRetrieveNonExistingObject() throws Exception {
        instance.retrieveDatastream(OBJ_KEY);
    }

    /**
     * List of datastreams should start at 0, and change to reflect
     * reflect what's added and removed.
     */
    @Test
    public void testListDatastreams() throws Exception {
        List<String> list;
        list = toList(instance.listDatastreams());
        assertEquals(0, list.size());
        instance.addDatastream(DS_KEY, toStream(DS_CONTENT));
        list = toList(instance.listDatastreams());
        assertEquals(1, list.size());
        assertEquals(DS_KEY, list.get(0));
        instance.removeDatastream(DS_KEY);
        list = toList(instance.listDatastreams());
        assertEquals(0, list.size());
    }

    /**
     * List of objects should start at 0, and change to reflect
     * reflect what's added and removed.
     */
    @Test
    public void testListObjects() throws Exception {
        List<String> list;
        list = toList(instance.listObjects());
        assertEquals(0, list.size());
        instance.addObject(OBJ_KEY, toStream(OBJ_CONTENT));
        list = toList(instance.listObjects());
        assertEquals(1, list.size());
        assertEquals(OBJ_KEY, list.get(0));
        instance.removeObject(OBJ_KEY);
        list = toList(instance.listObjects());
        assertEquals(0, list.size());
    }

    private static AkubraLowlevelStorage getInstance(
            boolean forceSafeObjectOverwrites,
            boolean forceSafeDatastreamOverwrites) {
        return new AkubraLowlevelStorage(new MemBlobStore(),
                                         new MemBlobStore(),
                                         forceSafeObjectOverwrites,
                                         forceSafeDatastreamOverwrites);
    }

    private static List<String> toList(Iterator<String> iter) {
        List<String> list = new ArrayList<String>();
        while (iter.hasNext()) {
            list.add(iter.next());
        }
        return list;
    }

    private static InputStream toStream(String string) {
        try {
            return new ByteArrayInputStream(string.getBytes("UTF-8"));
        } catch (IOException wontHappen) {
            throw new FaultException(wontHappen);
        }
    }

    private static String toString(InputStream stream) {
        try {
            return new BufferedReader(new InputStreamReader(stream)).readLine();
        } catch (IOException wontHappen) {
            throw new FaultException(wontHappen);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(AkubraLowlevelStorageTest.class);
    }

}
