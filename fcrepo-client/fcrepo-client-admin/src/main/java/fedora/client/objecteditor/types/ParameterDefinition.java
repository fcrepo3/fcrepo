/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.objecteditor.types;

import java.util.List;

/**
 * Defines a single parameter for a method.
 */
public class ParameterDefinition {

    private final String m_name;

    private final String m_label;

    private final boolean m_isRequired;

    private final String m_defaultValue;

    private final List m_validValues;

    /**
     * Initialize a parameter definition with all values. The label,
     * defaultValue, and validValues may each be null or empty.
     */
    public ParameterDefinition(String name,
                               String label,
                               boolean isRequired,
                               String defaultValue,
                               List validValues) {
        m_name = name;
        m_label = label;
        m_isRequired = isRequired;
        m_defaultValue = defaultValue;
        m_validValues = validValues;
    }

    public String getName() {
        return m_name;
    }

    public String getLabel() {
        return m_label;
    }

    public boolean isRequired() {
        return m_isRequired;
    }

    public String getDefaultValue() {
        return m_defaultValue;
    }

    public List validValues() {
        return m_validValues;
    }
}
