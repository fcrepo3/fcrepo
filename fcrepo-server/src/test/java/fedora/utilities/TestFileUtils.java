/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities;

import java.io.File;

import junit.framework.TestCase;

public class TestFileUtils
        extends TestCase {

    public void testDelete() throws Exception {
        File parent =
                new File(System.getProperty("tmp.io.dir"),
                         "testFileUtils.delete");
        parent.mkdirs();

        // test deleting null entry
        assertFalse(FileUtils.delete((File) null));

        // test deleting empty dir
        assertTrue(FileUtils.delete(parent));

        // test deleting populated dir
        parent.mkdirs();
        File[] children =
                {new File(parent, "dirA"), new File(parent, "dirB"),
                        new File(parent, "a"), new File(parent, "b"),
                        new File(parent, "c")};

        for (File child : children) {
            if (child.getName().startsWith("dir")) {
                child.mkdir();
                new File(child, "childOf" + child.getName()).createNewFile();
            } else {
                child.createNewFile();
            }
        }
        assertTrue(FileUtils.delete(parent));

        // test deleting file
        parent.createNewFile();
        assertTrue(FileUtils.delete(parent));
    }

    public void TODOtestCopy() throws Exception {
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestFileUtils.class);
    }

}
