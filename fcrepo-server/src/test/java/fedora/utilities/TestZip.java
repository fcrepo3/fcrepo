/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;

import junit.framework.TestCase;

public class TestZip
        extends TestCase {

    private final String TMP_DIR = System.getProperty("java.io.tmpdir");

    private final String ZIP_FILE = TMP_DIR + File.separator + "test.zip";

    private final String TEST_DIR = TMP_DIR + File.separator + "test";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        FileUtils.delete(TEST_DIR);
        File testDir = new File(TEST_DIR);
        File foo = new File(testDir, "foo");
        File bar = new File(testDir, "bar");
        File baz = new File(bar, "baz");
        File footxt = new File(foo, "foo.txt");
        File bartxt = new File(bar, "bar.txt");

        foo.mkdirs();
        baz.mkdirs();
        FileWriter fw = new FileWriter(footxt);
        fw.write("foo");
        fw.flush();
        fw.close();

        FileWriter bw = new FileWriter(bartxt);
        bw.write("bar");
        bw.flush();
        bw.close();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        //Zip.deleteDirectory(TEST_DIR);
    }

    public void testZip() throws Exception {
        File dir = new File(TMP_DIR + File.separator + "test");
        //Zip.zip(ZIP_FILE, TMP_DIR + File.separator + "test");
        Zip.zip(new File(ZIP_FILE), dir.listFiles());
    }

    public void testUnzip() throws Exception {
        FileInputStream fis = new FileInputStream(ZIP_FILE);
        Zip.unzip(fis, TEST_DIR);

        FileReader fr =
                new FileReader(TEST_DIR + File.separator + "foo"
                        + File.separator + "foo.txt");
        BufferedReader buff = new BufferedReader(fr);
        boolean eof = false;
        while (!eof) {
            String line = buff.readLine();
            if (line == null) {
                eof = true;
            } else {
                assertEquals("foo", line);
            }
        }
    }
}
