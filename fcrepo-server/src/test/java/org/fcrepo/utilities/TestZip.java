/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utilities;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.zip.ZipFile;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestZip {

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    private File TMP_DIR;

    private File SRC_DIR;

    private File ZIP_FILE;

    @Before
    public void setUp() throws Exception {
        TMP_DIR = folder.newFolder("TestZip");
        SRC_DIR = new File(TMP_DIR, "src");
        ZIP_FILE = new File(TMP_DIR, "test.zip");

        File foo = new File(SRC_DIR, "foo");
        File bar = new File(SRC_DIR, "bar");
        File baz = new File(bar, "baz");
        File footxt = new File(foo, "foo.txt");
        File bartxt = new File(bar, "bar.txt");

        foo.mkdirs();
        baz.mkdirs();
        FileWriter fw = new FileWriter(footxt);
        fw.write("foo");
        fw.close();

        FileWriter bw = new FileWriter(bartxt);
        bw.write("bar");
        bw.close();
    }

    @Test
    public void testZip() throws Exception {
        Zip.zip(ZIP_FILE, SRC_DIR.listFiles());
        ZipFile zf = new ZipFile(ZIP_FILE);
        try {
            assertEquals(5, zf.size());
        } finally {
            zf.close();
        }
    }

    @Test
    public void testUnzip() throws Exception {
        FileInputStream fis = new FileInputStream(ZIP_FILE);
        Zip.unzip(fis, TMP_DIR);

        FileReader fr =
                new FileReader(TMP_DIR + File.separator + "foo"
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
        buff.close();
    }
}
