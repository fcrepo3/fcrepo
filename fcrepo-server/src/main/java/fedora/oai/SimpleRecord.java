/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai;

import java.util.Set;

/**
 * A simple implementation of Record that provides getters on the values
 * passed in the constructor.
 * 
 * @author Chris Wilper
 */
public class SimpleRecord
        implements Record {

    private final Header m_header;

    private final String m_metadata;

    private final Set m_abouts;

    public SimpleRecord(Header header, String metadata, Set abouts) {
        m_header = header;
        m_metadata = metadata;
        m_abouts = abouts;
    }

    public Header getHeader() {
        return m_header;
    }

    public String getMetadata() {
        return m_metadata;
    }

    public Set getAbouts() {
        return m_abouts;
    }

}
