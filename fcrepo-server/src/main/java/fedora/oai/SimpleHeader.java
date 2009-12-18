/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai;

import java.util.Date;
import java.util.Set;

/**
 * A simple implementation of Header that provides getters on the values 
 * passed in the constructor.
 * 
 * @author Chris Wilper
 */
public class SimpleHeader
        implements Header {

    private final String m_identifier;

    private final Date m_datestamp;

    private final Set m_setSpecs;

    private final boolean m_isAvailable;

    public SimpleHeader(String identifier,
                        Date datestamp,
                        Set setSpecs,
                        boolean isAvailable) {
        m_identifier = identifier;
        m_datestamp = datestamp;
        m_setSpecs = setSpecs;
        m_isAvailable = isAvailable;
    }

    public String getIdentifier() {
        return m_identifier;
    }

    public Date getDatestamp() {
        return m_datestamp;
    }

    public Set getSetSpecs() {
        return m_setSpecs;
    }

    public boolean isAvailable() {
        return m_isAvailable;
    }

}
