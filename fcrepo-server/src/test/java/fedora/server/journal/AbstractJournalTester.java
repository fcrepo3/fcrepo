/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * <p>
 * <b>Title:</b> AbstractJournalTester.java
 * </p>
 * <p>
 * <b>Description:</b> A base class that holds some useful methods for
 * Journaling test.
 * </p>
 *
 * @author jblake
 * @version $Id: AbstractJournalTester.java,v 1.3 2007/06/01 17:21:32 jblake Exp $
 */
public class AbstractJournalTester {

    /**
     * Remove all files and sub-directories in this directory, so it will be
     * pristine for the next test.
     *
     * @throws IllegalStateException
     *         if we fail to delete anything - this might cause the next test to
     *         behave incorrectly.
     */
    protected void deleteDirectoryContents(File directory) {
        File[] children = directory.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deleteDirectoryContents(child);
                }
                boolean deleted = child.delete();
                if (!deleted) {
                    throw new IllegalStateException("Failed to delete: "
                            + child);
                }
            }
        }
    }

    protected void assertFileExists(File file) {
        if (!file.exists()) {
            fail("File '" + file.getAbsolutePath() + "' does not exist.");
        }
    }

    protected void assertFileDoesNotExist(File file) {
        if (file.exists()) {
            fail("File '" + file.getAbsolutePath() + "' should not exist.");
        }
    }

    /**
     * Read the entire file into a String and see whether it is what we
     * expected.
     */
    protected void assertFileContents(String expected, File file) {
        BufferedReader reader = null;
        try {
            StringBuffer contents = new StringBuffer();
            reader = new BufferedReader(new FileReader(file));
            int howMany = 0;
            char[] buffer = new char[4096];
            while (-1 != (howMany = reader.read(buffer))) {
                contents.append(buffer, 0, howMany);
            }
            assertEquals("checking file contents for " + file,
                         expected,
                         contents.toString());
        } catch (FileNotFoundException e) {
            fail(e.toString());
        } catch (IOException e) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    fail(e.toString());
                }
            }
        }
    }

}
