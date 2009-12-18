/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.HashSet;
import java.util.Iterator;

/**
 * @author Chris Wilper
 */
public class BasisDataStream
        extends DataStream {

    private final HashSet<InlineDataStream> m_descriptiveStreams =
            new HashSet<InlineDataStream>();

    private boolean m_internallyStored = true;

    private String m_location;

    public BasisDataStream(File tempDir, String id) {
        super(tempDir, id);
    }

    @Override
    public final int getType() {
        return DataStream.BASIS;
    }

    public void addDescriptiveStream(InlineDataStream inlineStream) {
        m_dirty = true;
        m_descriptiveStreams.add(inlineStream);
    }

    public void removeDescriptiveStream(InlineDataStream inlineStream) {
        m_dirty = true;
        m_descriptiveStreams.remove(inlineStream);
    }

    public Iterator descriptiveStreams() {
        return m_descriptiveStreams.iterator();
    }

    public boolean isInternallyStored() {
        return m_internallyStored;
    }

    public void setLocation(String location) {
        m_location = location;
        m_internallyStored = false;
        clearData();
    }

    public String getLocation() {
        return m_location;
    }

    @Override
    public void setData(InputStream in) throws IOException {
        super.setData(in);
        m_location = null;
        m_internallyStored = true;
    }

}
