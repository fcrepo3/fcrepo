/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.StreamIOException;
import fedora.server.storage.types.DigitalObject;

/**
 * A test implementation of DOSerializer that only writes format\npid.
 *
 * @author Chris Wilper
 */
public class MockDOSerializer
        implements DOSerializer {

    private final String m_format;

    public MockDOSerializer() {
        m_format = new String();
    }

    public MockDOSerializer(String format) {
        m_format = format;
    }

    public DOSerializer getInstance() {
        return new MockDOSerializer(m_format);
    }

    public void serialize(DigitalObject obj,
                          OutputStream out,
                          String encoding,
                          int transContext) throws ObjectIntegrityException,
            StreamIOException, UnsupportedEncodingException {
        PrintWriter writer =
                new PrintWriter(new OutputStreamWriter(out, encoding));
        try {
            writer.println(m_format);
            writer.print(obj.getPid());
        } finally {
            writer.close();
        }
    }

}
