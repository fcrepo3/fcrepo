/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.helpers;

import java.io.IOException;
import java.io.OutputStream;

import java.util.regex.Pattern;

import fedora.utilities.Base64;

/**
 * Wraps an OutputStream with a Base64 decoder, so when you "write" to the
 * stream, you write Strings of Base64-encoded characters, but the OutputStream
 * receives decoded bytes.
 * <p>
 * Base64 encoding is defined in Internet RFC 3548, found at
 * http://tools.ietf.org/html/rfc3548 (among other places).
 *
 * @author Jim Blake
 */
public class DecodingBase64OutputStream {

    private final Pattern pattern = Pattern.compile("[^A-Za-z0-9+/=]*");

    private final OutputStream stream;

    private String residual = "";

    private boolean open = true;

    /**
     * @param stream
     *        the destination for the decoded bytes.
     */
    public DecodingBase64OutputStream(OutputStream stream) {
        this.stream = stream;
    }

    /**
     * Add Base64-encoded characters to be decoded. This is not a trivial
     * operation for two reasons: any characters that are not valid for
     * Base64-encoding must be ignored, and we can only decode groups of 4
     * characters. So, when data is received, we remove any invalid characters
     * and then strip off any trailing characters that don't fit in the
     * 4-character groups. Those trailing characters will be prefixed to the
     * next set of data, and hopefully we will have none left over when the
     * writer is closed.
     *
     * @throws IllegalStateException
     *         if called after close().
     * @throws IOException
     *         from the inner OutputStream.
     */
    public void write(String data) throws IOException {
        if (!open) {
            throw new IllegalStateException("Stream has already been closed.");
        }

        String buffer = pattern.matcher(residual + data).replaceAll("");
        int usableLength = buffer.length() - buffer.length() % 4;
        stream.write(Base64.decode((buffer.substring(0, usableLength))));
        residual = buffer.substring(usableLength);
    }

    /**
     * Close the writer. If there are any residual characters at this point, the
     * data stream was not a valid Base64 encoding.
     *
     * @throws IOException
     *         from the inner OutputStream.
     */
    public void close() throws IOException {
        if (open) {
            if (residual.length() > 0) {
                throw new IOException("Base64 error - data is not properly"
                        + "padded to 4-character groups.");
            }
            stream.close();
            open = false;
        }
    }

}
