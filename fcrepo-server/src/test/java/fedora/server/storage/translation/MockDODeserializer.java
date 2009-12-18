/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.StreamIOException;
import fedora.server.storage.types.DigitalObject;

/**
 * A mock implementation of DODeserializer that reads format\n\pid, and sets the
 * object's label and pid to those read values, respectively.
 *
 * @author Chris Wilper
 */
public class MockDODeserializer
        implements DODeserializer {

    private final String m_format;

    public MockDODeserializer() {
        m_format = new String();
    }

    public MockDODeserializer(String format) {
        m_format = format;
    }

    public DODeserializer getInstance() {
        return new MockDODeserializer(m_format);
    }

    public void deserialize(InputStream in,
                            DigitalObject obj,
                            String encoding,
                            int transContext) throws ObjectIntegrityException,
            StreamIOException, UnsupportedEncodingException {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(in, encoding));
        try {
            obj.setLabel(reader.readLine());
            obj.setPid(reader.readLine());
        } catch (IOException e) {
            throw new StreamIOException("Error reading stream", e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                throw new StreamIOException("Error closing reader", e);
            }
        }
    }

}
