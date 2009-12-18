/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai;

import java.util.Set;

/**
 * A simple implementation of SetInfo that provides getters on the values
 * passed in the constructor.
 * 
 * @author Chris Wilper
 */
public class SimpleSetInfo
        implements SetInfo {

    private final String m_name;

    private final String m_spec;

    private final Set m_descriptions;

    public SimpleSetInfo(String name, String spec, Set descriptions) {
        m_name = name;
        m_spec = spec;
        m_descriptions = descriptions;
    }

    public String getName() {
        return m_name;
    }

    public String getSpec() {
        return m_spec;
    }

    public Set getDescriptions() {
        return m_descriptions;
    }

}
