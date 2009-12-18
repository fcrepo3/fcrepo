/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai;

/**
 * An indicator of the level of granularity in dates a repository supports.
 * 
 * @author Chris Wilper
 */
public class DateGranularitySupport {

    /**
     * Indicates that the repository supports timestamp granularity in days.
     */
    public static final DateGranularitySupport DAYS =
            new DateGranularitySupport("YYYY-MM-DD");

    /**
     * Indicates that the repository supports timestamp granularity in seconds.
     */
    public static final DateGranularitySupport SECONDS =
            new DateGranularitySupport("YYYY-MM-DDThh:mm:ssZ");

    private final String m_stringValue;

    private DateGranularitySupport(String stringValue) {
        m_stringValue = stringValue;
    }

    @Override
    public String toString() {
        return m_stringValue;
    }

}
