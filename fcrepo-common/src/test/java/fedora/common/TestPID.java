/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;

import org.junit.Test;

public class TestPID
        extends TestCase {

    /**
     * hashCode() should return the same value for lexically equivalent PIDs.
     */
    @Test
    public void testHashCodeSamePID() throws Exception {
        PID pid1 = PID.getInstance("test:somepid");
        PID pid2 = new PID("test:somepid");
        PID pid3 = PID.fromFilename("test_somepid");
        assertEquals(pid1.hashCode(), pid2.hashCode());
        assertEquals(pid2.hashCode(), pid3.hashCode());
    }
    
    /**
     * equals() should return true for lexically equivalent PIDs.
     */
    @Test
    public void testEqualsSamePID() throws Exception {
        PID pid1 = PID.getInstance("test:somepid");
        PID pid2 = new PID("test:somepid");
        PID pid3 = PID.fromFilename("test_somepid");
        assertEquals(pid1, pid2);
        assertEquals(pid2, pid3);
    }

    /**
     * equals() should return false for lexically distinct PIDs.
     */
    @Test
    public void testEqualsDifferentPID() throws Exception {
        PID pid1 = PID.getInstance("test:somepid");
        PID pid2 = new PID("test:someotherpid");
        PID pid3 = PID.fromFilename("test_yetanotherpid");
        assertFalse(pid1.equals(pid2));
        assertFalse(pid2.equals(pid1));
        assertFalse(pid2.equals(pid3));
        assertFalse(pid3.equals(pid2));
    }
    
    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestPID.class);
    }

}
