/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>
 * Iterates through a {@link File} of PIDs. Each PID appears on its own line.
 * Blank lines are ignored. White space at beginning and end of a PID is
 * trimmed. Lines whose first non-blank character is an octothorpe ('#') are
 * comments, and are ignored also.
 * </p>
 * <p>
 * Note that checked exceptions are not permitted on {@link Iterator#next() } or
 * {@link Iterator#hasNext() } methods, so if we get an {@link IOException} on
 * one of these methods, we'll wrap it in an {@link IllegalStateException}.
 * </p>
 * 
 * @author Jim Blake
 */
public class PidfileIterator
        implements Iterator<String> {

    private final BufferedReader reader;

    /**
     * If true, indicates that the end of the file has been reached, and that
     * there are no more PIDs.
     */
    private boolean eof;

    /**
     * <p>
     * If this is non-null, it holds the trimmed line of text that is the next
     * available PID.
     * </p>
     * <p>
     * If null, call {@link #fillNextLine()} before deciding that there are no
     * more.
     * </p>
     */
    private String nextLine;

    public PidfileIterator(File pidfile) {
        if (pidfile == null) {
            throw new NullPointerException("pidfile may not be null.");
        }
        try {
            reader = new BufferedReader(new FileReader(pidfile));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        fillNextLine();
    }

    /**
     * Scan through the file until we get an EOF or a good line.
     */
    private void fillNextLine() {
        try {
            while (nextLine == null && !eof) {
                String line = reader.readLine();
                if (line == null) {
                    eof = true;
                    reader.close();
                } else if (isBlank(line)) {
                    continue;
                } else if (isComment(line)) {
                    continue;
                } else {
                    nextLine = line;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * A comment is a line whose first non-blank character is a hash ('#').
     */
    private boolean isComment(String line) {
        String trimmed = line.trim();
        if (trimmed.length() == 0) {
            return false;
        }
        return trimmed.charAt(0) == '#';
    }

    /**
     * A blank line is either empty or contains only white-space characters.
     */
    private boolean isBlank(String line) {
        return line.trim().length() == 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        fillNextLine();
        return !eof;
    }

    /**
     * {@inheritDoc}
     */
    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        String result = nextLine;
        nextLine = null;
        fillNextLine();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
