/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterable implementation over a stream containing PIDs.
 *
 * Allows a stream containing a list of pids to be iterated in a for (String pid : wrapper) statement.
 *
 * Stream must contain either:
 * <ul>
 * <li>Raw list of PIDs, separated by newlines (blank lines are skipped)</li>
 * <li>XML containing &lt;pid&gt; elements.  Structure of XML is not interpreted,
 * only single lines containing this element are read.  Only one PID per line.
 * Empty pid elements (&lt;pid/&gt;) will be skipped.
 * This is compatible with basic search output</li>
 * </ul>
 * @author Stephen Bayliss
 * @version $Id$
 */
public final class PIDStreamIterableWrapper implements Iterable<String> {

    private final PIDStreamIterator m_iterator;

    public PIDStreamIterableWrapper(InputStream stream) throws IOException {
        // construct iterator here, as iterator() can't through (checked) exception
        m_iterator = new PIDStreamIterator(stream);
    }
    @Override
    public Iterator<String> iterator() {
        return m_iterator;
    }

    /**
     * Iterator over a stream containing PIDs
     *
     * Note: does not implement remove(); should not be used as a general-purpose
     * iterator and only used in context of the enclosing Iterable.
     *
     * @author Stephen Bayliss
     * @version $Id$
     */
    private class PIDStreamIterator implements Iterator<String> {

        boolean m_isXML = false;
        boolean m_started = false;
        BufferedReader m_reader;
        String m_nextPID = null;
        String m_nextLine = null;

        // text delimiters for PID element in XML file
        private static final String XML_START = "<pid>";
        private static final String XML_END = "</pid>";

        @SuppressWarnings("unused")
        private PIDStreamIterator() { }

        public PIDStreamIterator(InputStream stream) throws IOException {
            m_reader = new BufferedReader(new InputStreamReader(stream));
            // get first element ready (if there is one)
            getNext();
        }

        @Override
        public boolean hasNext() {
            return (m_nextPID != null);
        }

        @Override
        public String next(){
            // return the next element, or flag end of list
            if (m_nextPID != null) {
                String next = m_nextPID;
                // get ready for next time
                try {
                    getNext();
                } catch (IOException e) {
                    // we've already started reading from the stream by now, so this shouldn't really happen
                    throw new RuntimeException("IO Error reading PIDs from stream", e);
                }
                return next;
            } else {
                throw new NoSuchElementException("End of PIDs");
            }
        }

        @Override
        public void remove() {
            // not applicable, "collection" is not modifiable
            throw new RuntimeException("method remove() called on" + PIDStreamIterator.class.getCanonicalName());
        }

        /**
         * read from file until next pid found.  Sets m_nextPID to null if there are no more
         * @throws IOException
         */
        private void getNext() throws IOException {

            m_nextPID = null;
            while ((m_nextLine = m_reader.readLine()) != null) {
                // skip blank lines
                if (!m_nextLine.trim().isEmpty()) {
                    // first time read, see what kind of file it is
                    if (!m_started) {
                        if (m_nextLine.contains("<")) {
                            m_isXML = true;
                        } else {
                            m_isXML = false;
                        }
                        m_started = true;
                    }
                    // do we have a pid in this line?
                    String nextPID = getPID(m_nextLine);
                    if (nextPID != null) {
                        m_nextPID = nextPID;
                        break; // found one, stop reading
                    }
                }
                // continue until pid found or end of input
            }
            // either PID has been found, or end of file reached
            if (m_nextPID == null)
                m_reader.close();
        }

        /**
         * Get PID from line of text. Null if not found.
         * @param line
         * @return
         */
        private String getPID(String line) {
            if (m_isXML) {
                // xml element contents, based on textual delimiters
                int start = line.indexOf(XML_START);
                int end = line.indexOf(XML_END);
                if (start == -1 || end == -1) {
                    return null;
                } else {
                    return line.substring(start + XML_START.length(), end);
                }
            } else {
                // raw contents of line, ignore leading/trailing whitespace, ignore empty lines
                if (line.trim().isEmpty()) {
                    return null;
                } else {
                    return line.trim();
                }
            }
        }

        @Override
        protected void finalize() throws Throwable {
            // just in case...
            m_reader.close();
        }
    }

}
