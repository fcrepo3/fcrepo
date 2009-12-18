/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The model of a datastream as it exists inside the editor.
 * 
 * This class has getters and setters for the fields and bytes of a datastream
 * while it is being edited.
 * 
 * @author Chris Wilper
 */
public abstract class DataStream {

    /** Empty stream */
    public final static ByteArrayInputStream EMPTY =
            new ByteArrayInputStream(new byte[0]);

    /** Identifier for INLINE datastreams */
    public final static int INLINE = 0;

    /** Identifier for BASIS datastreams */
    public final static int BASIS = 1;

    /** The file where the bytes are temporarily stored during editing */
    private File m_dataFile;

    /** The mime type of the datastream */
    private String m_mimeType;

    /** The identified for the datastream */
    private final String m_id;

    /** The size of the datastream, in bytes */
    private long m_size;

    /** Whether this datastream is dirty */
    protected boolean m_dirty = true;

    /**
     * Constructs a datastream with a given temporary directory to write itself
     * to, and an identifier.
     */
    public DataStream(File tempDir, String id) {
        m_id = id;
        File m_dataFile = new File(tempDir, id);
        clearData();
    }

    /**
     * Returns INLINE or BASIS.
     */
    public abstract int getType();

    /**
     * Gets the id of the datastream inside the object.
     */
    public String getId() {
        return m_id;
    }

    /**
     * Gets the mime type.
     */
    public String getMimeType() {
        return m_mimeType;
    }

    /**
     * Sets the mime type.
     */
    public void setMimeType(String mimeType) {
        m_dirty = true;
        m_mimeType = mimeType;
    }

    /**
     * Gets the size, in bytes.
     */
    public long getSize() {
        return m_size;
    }

    /**
     * Gets an <code>InputStream</code> to the local copy of the datastream.
     */
    public InputStream getData() throws IOException {
        if (m_size == 0) {
            return EMPTY;
        }
        return new FileInputStream(m_dataFile);
    }

    /**
     * Reads the bytes from the given <code>InputStream</code> as the data for
     * this digital object. When finished, the <code>InputStream</code> is
     * closed.
     */
    public void setData(InputStream in) throws IOException {
        m_dirty = true;
        FileOutputStream out = new FileOutputStream(m_dataFile);
        byte[] buf = new byte[4096];
        int i = 0;
        m_size = 0;
        while ((i = in.read(buf)) != -1) {
            m_size += i;
            out.write(buf, 0, i);
        }
        in.close();
        out.close();
    }

    public boolean isDirty() {
        return m_dirty;
    }

    public void setClean() {
        m_dirty = false;
    }

    public void clearData() {
        m_size = 0;
        m_dirty = true;
        m_dataFile.delete();
    }

}
