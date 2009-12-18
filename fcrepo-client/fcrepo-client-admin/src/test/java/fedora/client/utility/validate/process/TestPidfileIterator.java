/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate.process;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author Jim Blake
 */
public class TestPidfileIterator {

    private static final String FIRST_PID = "firstPid";

    private static final String SECOND_PID = "secondPid";

    private static final String THIRD_PID = "thirdPid";

    @Test(expected = NullPointerException.class)
    public void rejectNullArgument() {
        new PidfileIterator(null);
    }

    @Test(expected = IllegalStateException.class)
    public void ioExceptionBecomesIllegalStateException() {
        new PidfileIterator(new File("/bogus/file"));
    }

    /**
     * Confirm that we iterate over the lines, ignoring comment lines and blank
     * or empty lines.
     */
    @Test
    public void simpleExerciseWithPidsBlanksAndComments() throws IOException {
        String[] lines =
                new String[] {"#comment", " #comment start with space",
                        FIRST_PID, SECOND_PID, "", THIRD_PID, " "};

        File dummyFile = null;
        try {
            dummyFile =
                    File.createTempFile("TestValidatorProcessParameter",
                                        "dummyFile");
            dummyFile.deleteOnExit();
            loadPidFile(dummyFile, lines);

            PidfileIterator iterator = new PidfileIterator(dummyFile);

            assertTrue("has first line", iterator.hasNext());
            assertEquals("first line", FIRST_PID, iterator.next());
            assertTrue("has second line", iterator.hasNext());
            assertEquals("second line", SECOND_PID, iterator.next());
            assertTrue("has third line", iterator.hasNext());
            assertEquals("third line", THIRD_PID, iterator.next());
            assertFalse("no fourth line", iterator.hasNext());
        } finally {
            if (dummyFile != null || dummyFile.exists()) {
                dummyFile.delete();
            }
        }
    }

    /**
     * Fill the file with these lines.
     */
    private void loadPidFile(File file, String[] lines) throws IOException {
        Writer writer = null;
        try {
            writer = new FileWriter(file);
            for (String line : lines) {
                writer.write(line + '\n');
            }
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println("failed to close writer on file '"
                            + file + "'");
                }
            }
        }
    }
}
