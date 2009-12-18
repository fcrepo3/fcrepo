/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import junit.framework.TestCase;

import fedora.server.journal.helpers.DecodingBase64OutputStream;
import fedora.server.journal.helpers.EncodingBase64InputStream;

/**
 * Confirm that we can properly encode and decode any data stream that comes
 * our way.
 * 
 * @author Jim Blake
 */
public class JournalBase64Test
        extends TestCase {

    private static final String WHITE_SPACE_CHARACTERS = " \t\n\r";

    private static final String BASE64_ENCODING_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    public JournalBase64Test(String name) {
        super(name);
    }

    /**
     * Generate a temporary file with all possible bytes in it. Encode it,
     * decode it, test for correctness.
     */
    public void testOnArbitraryBytes() throws IOException {

        // create the source file and fill it.
        File source = createTempFile();
        OutputStream os = new FileOutputStream(source);
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            os.write(i);
        }
        os.close();

        // process the file and test.
        encodeAndDecodeWithAssertions(source);
    }

    /**
     * Generate a temporary file with all sorts of Unicode characters in it,
     * encoded to UTF-8. Encode it, decode it, test for correctness.
     */
    public void testOnUTF8Characters() throws IOException {
        // create the source file and fill it.
        File source = createTempFile();
        Writer writer =
                new OutputStreamWriter(new FileOutputStream(source), "UTF-8");
        for (int i = 0, charValue = 0; i < 256; i++) {
            writer.write(charValue);
            charValue += 131;
        }
        writer.close();

        // process the file and test.
        encodeAndDecodeWithAssertions(source);
    }

    /**
     * Generate a temporary file with a troublesome section in it - this is text
     * that is known to have caused problems with the previous encode/decode
     * scheme. Encode it, decode it, test for correctness.
     */
    public void testOnKnownProblemFile() throws IOException {
        byte[] problemData =
                new byte[] {0x73, 0x00, 0x6F, 0x00, 0x75, 0x00, 0x72, 0x00,
                        0x63, 0x00, 0x65, 0x00, 0x73, 0x00, 0x20, 0x00, 0x61,
                        0x00, 0x6E, 0x00, 0x64, 0x00, 0x20, 0x00, 0x6C, 0x00,
                        0x65, 0x00, 0x61, 0x00, 0x72, 0x00, 0x6E, 0x00, 0x69,
                        0x00, 0x6E, 0x00, 0x67, 0x00, 0x20, 0x00, 0x61, 0x00,
                        0x63, 0x00, 0x74, 0x00, 0x69, 0x00, 0x76, 0x00, 0x69,
                        0x00, 0x74, 0x00, 0x69, 0x00, 0x65, 0x00, 0x73, 0x00,
                        0x20, 0x00, 0x74, 0x00, 0x68, 0x00, 0x61, 0x00, 0x74,
                        0x00, 0x20, 0x00, 0x72, 0x00, 0x65, 0x00, 0x66, 0x00,
                        0x6C, 0x00, 0x65, 0x00, 0x63, 0x00, 0x74, 0x00, 0x20,
                        0x00, 0x74, 0x00, 0x68, 0x00, 0x65, 0x00, 0x20, 0x00,
                        0x6D, 0x00, 0x75, 0x00, 0x73, 0x00, 0x65, 0x00, 0x75,
                        0x00, 0x6D, 0x00, (byte) 0xE2, 0x00, (byte) 0xAC, 0x20,
                        0x22, 0x21, 0x73, 0x00, 0x20, 0x00, 0x66, 0x00, 0x6F,
                        0x00, 0x75, 0x00, 0x6E, 0x00, 0x64, 0x00, 0x61, 0x00,
                        0x74, 0x00, 0x69, 0x00, 0x6F, 0x00, 0x6E, 0x00, 0x20,
                        0x00, 0x6F, 0x00, 0x66, 0x00, 0x20, 0x00, 0x70, 0x00,
                        0x6C, 0x00, 0x61, 0x00, 0x79, 0x00, 0x66, 0x00, 0x75,
                        0x00, 0x6C, 0x00, 0x20, 0x00, 0x65, 0x00, 0x78, 0x00,
                        0x68, 0x00, 0x69, 0x00, 0x62, 0x00, 0x69, 0x00, 0x74,
                        0x00, 0x2D, 0x00, 0x62, 0x00, 0x61, 0x00, 0x73, 0x00,
                        0x65, 0x00, 0x64, 0x00, 0x20, 0x00, 0x69, 0x00, 0x6E,
                        0x00, 0x71, 0x00, 0x75, 0x00, 0x69, 0x00, 0x72, 0x00,
                        0x79, 0x00, 0x20, 0x00};

        // create the source file and fill it.
        File source = createTempFile();
        OutputStream os = new FileOutputStream(source);
        os.write(problemData);
        os.close();

        // process the file and test.
        encodeAndDecodeWithAssertions(source);
    }

    /**
     * Create a temp file that will be deleted when the test is complete.
     */
    private File createTempFile() throws IOException {
        File file = File.createTempFile("test", null);
        System.out.println(file.getPath());
        file.deleteOnExit();
        return file;
    }

    /**
     * Encode the source file to a String, then decode back to a file. Check
     * that the String is valid Base64, and that the target file has the same
     * contents as the source file.
     */
    private void encodeAndDecodeWithAssertions(File source)
            throws FileNotFoundException, IOException {
        // encode the file to a String and test that it is valid Base64
        String encodedString = readClearFileCreateEncodedString(source);
        assertStringContainsValidBase64(encodedString);

        // read the encoded String and encode it to the target file.
        File target = createTempFile();
        writeClearFileFromEncodedString(target, encodedString);

        // test the files.
        assertFileContentsAreEqual(source, target);
    }

    /**
     * Read a file of arbitrary bytes and produce a base-64 encoded String.
     */
    private String readClearFileCreateEncodedString(File source)
            throws IOException {
        // use a small buffer size, not a multiple, so we will do multiple
        // reads.
        EncodingBase64InputStream encoder =
                new EncodingBase64InputStream(new FileInputStream(source), 61);
        StringBuffer encoded = new StringBuffer();
        String chunk;
        while (null != (chunk = encoder.read(35))) {
            encoded.append(chunk);
        }
        encoder.close();

        String encodedString = encoded.toString();
        return encodedString;
    }

    /**
     * Produce a file of arbitrary bytes from a base-64 encoded String. We need
     * to process the String in chunks to simulate the Text events we will get
     * from the XmlReader.
     */
    private void writeClearFileFromEncodedString(File target,
                                                 String encodedString)
            throws FileNotFoundException, IOException {
        int maxChunkSize = 61;
        int length = encodedString.length();

        DecodingBase64OutputStream decoder =
                new DecodingBase64OutputStream(new FileOutputStream(target));

        int start = 0;
        int remainder;
        while (0 < (remainder = length - start)) {
            int chunkSize = Math.min(remainder, maxChunkSize);
            String chunk = encodedString.substring(start, start + chunkSize);
            decoder.write(chunk);
            start += chunkSize;
        }
        decoder.close();
    }

    /**
     * Base64 data should consist only of the 64 pemissible characters and the
     * equals sign, in groups of 4 characters. Equals should appear only at the
     * end, and no more than 2 are allowed. The spec allows any other characters
     * to appear, and they should be ignored, but we'll be a little more strict
     * and only allow white space characters.
     */
    private void assertStringContainsValidBase64(String string) {
        int howManyEqualsSigns = 0;
        int howManyEncodingCharacters = 0;
        for (int i = 0; i < string.length(); i++) {
            char thisChar = string.charAt(i);
            if (thisChar == '=') {
                howManyEqualsSigns++;
            } else if (BASE64_ENCODING_CHARACTERS.indexOf(thisChar) != -1) {
                howManyEncodingCharacters++;
                if (howManyEqualsSigns > 0) {
                    fail("Encoding character at " + i
                            + " follows an equals sign.");
                }
            } else if (WHITE_SPACE_CHARACTERS.indexOf(thisChar) == -1) {
                fail("Character '" + thisChar + "' at " + i
                        + " is not an encoding character, "
                        + "an equals sign, or white space.");
            }
        }
        if (howManyEqualsSigns > 2) {
            fail("Too many equals signs: " + howManyEqualsSigns);
        }
        int totalCharacters = howManyEqualsSigns + howManyEncodingCharacters;
        if (0 != totalCharacters % 4) {
            fail("Number of encoding characters plus equals signs "
                    + "must be a multiple of 4, not " + totalCharacters);
        }
    }

    /**
     * Read the files byte by byte and compare.
     */
    private void assertFileContentsAreEqual(File source, File target)
            throws IOException {
        InputStream sourceStream =
                new BufferedInputStream(new FileInputStream(source));
        InputStream targetStream =
                new BufferedInputStream(new FileInputStream(target));

        for (int i = 0; true; i++) {
            int sourceByte = sourceStream.read();
            int targetByte = targetStream.read();
            if (sourceByte == -1) {
                if (targetByte == -1) {
                    break;
                } else {
                    fail("source file is shorter than target file.");
                }
            } else if (targetByte == -1) {
                fail("target file is shorter than source file.");
            } else if (sourceByte != targetByte) {
                fail("files don't match at byte " + i + ": source="
                        + sourceByte + ", target=" + targetByte);
            }
        }
    }
}
