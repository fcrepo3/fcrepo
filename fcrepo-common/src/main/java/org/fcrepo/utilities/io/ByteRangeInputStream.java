/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utilities.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapping an InputStream to limit it to a sub-range of bytes,
 * but allow it to be used as a response entity
 * @author armintor@gmail.com
 *
 */
public class ByteRangeInputStream extends InputStream {

    private static final Pattern RANGE_HEADER = Pattern.compile("^bytes\\s*=\\s*(\\d*)\\s*(-\\s*(\\d*))?\\s*$");

    public final long offset;

    public final long length;

    public final String contentRange;

    private final InputStream src;
    
    private long read = 0;

    /**
     * 
     * @param src the source input stream
     * @param limit the maximum size of the stream
     * @param rangeHeader the value of a rfc2616 HTTP Range request header, given in the form "bytes=[num][-num]"
     * @throws IndexOutOfBoundsException when there is no satisfiable range in the header value (HTTP 416)
     * @throws IOException
     */
    public ByteRangeInputStream(InputStream src, long limit, String rangeHeader)
        throws IOException, IndexOutOfBoundsException {
        Matcher m = RANGE_HEADER.matcher(rangeHeader);
        if (!m.find()) {
            throw new IOException("Bad range spec: " + rangeHeader);
        }
        String g1 = m.group(1), g3 = m.group(3);
        if (g1 == null && g3 == null) {
            throw new IOException("Bad range spec values: " + rangeHeader);
        }

        long endByte;
        if (g3 == null || g3.isEmpty()) {
            endByte = limit;
        } else {
            endByte = Long.parseLong(g3);
        }

        long offset = (g1 != null && !g1.isEmpty()) ? Long.parseLong(g1) : (0L - endByte);
        long length = 0;
        if (offset < 0) {
            length = Math.min(limit, Math.abs(offset));
            offset = limit - length;
        } else {
            length = Math.min(limit - offset, endByte + 1 - offset);
        }
        if (offset >= limit || offset < 0) {
            throw new IndexOutOfBoundsException("Bad range spec start position: " + rangeHeader);
        }
        this.length = Math.min(length, limit);
        if (length < 0) {
            throw new IndexOutOfBoundsException("Bad range spec end position: " + rangeHeader);
        }
        this.offset = offset;
        this.src = src;
        long skipped = 0;
        while ((skipped += src.skip(offset - skipped)) < offset) {
            src.skip(offset - skipped);
        }
        // describe the inclusive range of byte positions of this segment
        contentRange = "bytes " +  Long.toString(this.offset) + "-" + Long.toString(this.offset + this.length - 1)
                + "/" + Long.toString(limit);
    }

    @Override
    public int read() throws IOException {
        java.io.FileInputStream.class.getCanonicalName();
        java.io.ByteArrayInputStream.class.getCanonicalName();
        if ((this.read+1) < this.length) {
            this.read++;
            return src.read();
        } else {
            src.close();
            return -1;
        }
    }

    @Override
    public int read(byte[] buf) throws IOException {
        return read(buf, 0 , buf.length);
    }

    @Override
    public int read(byte[] buf, int offset, int length) throws IOException {
        long rem = this.length - this.read;
        if (rem < 1) return -1;
        int toRead = (int)Math.min(rem, length);
        int read = src.read(buf, offset, toRead);
        this.read += read;
        if (this.read >= this.length) {
            src.close();
        }
        return read;
    }

    @Override
    public int available() throws IOException {
        long rem = this.length - this.read;
        if (rem > Integer.MAX_VALUE) {
            return src.available();
        }
        return Math.min(src.available(), (int)(rem));
    }

    @Override
    public void close() throws IOException {
        this.src.close();
    }
    @Override
    public long skip(long skip) throws IOException {
        long skipped = this.src.skip(Math.min(skip, this.length - this.read));
        this.read += skipped;
        return skipped;
    }
}
