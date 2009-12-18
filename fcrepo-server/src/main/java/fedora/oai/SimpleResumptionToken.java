/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai;

import java.util.Date;

/**
 * A simple implementation of ResumptionToken that provides getters on the 
 * values passed in the constructor.
 * 
 * @author Chris Wilper
 */
public class SimpleResumptionToken
        implements ResumptionToken {

    private final String m_value;

    private final Date m_expirationDate;

    private final long m_completeListSize;

    private final long m_cursor;

    public SimpleResumptionToken(String value,
                                 Date expirationDate,
                                 long completeListSize,
                                 long cursor) {
        m_value = value;
        m_expirationDate = expirationDate;
        m_completeListSize = completeListSize;
        m_cursor = cursor;
    }

    public String getValue() {
        return m_value;
    }

    public Date getExpirationDate() {
        return m_expirationDate;
    }

    public long getCompleteListSize() {
        return m_completeListSize;
    }

    public long getCursor() {
        return m_cursor;
    }

}
