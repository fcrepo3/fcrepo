/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.objecteditor.types;

/**
 * A single rule of a service deployment's datastream binding specification. For
 * a certain binding key (the "key" of the rule), a rule specifies a range
 * indicating the number of datastreams to be bound, whether order is important,
 * and the types of datastreams that may be bound.
 */
public class DatastreamBindingRule {

    private final String m_key;

    private final String m_inputLabel;

    private final String m_inputInstruction;

    private final int m_max;

    private final boolean m_orderMatters;

    private String[] m_types;

    private boolean m_acceptsAll = false;

    /**
     * Initialize a DatastreamBindingRule with all values. If maximum is -1,
     * that means there is no maximum. If the array of types is null or empty,
     * getTypes() will return a single-valued array with the "any type" pattern.
     */
    public DatastreamBindingRule(String key,
                                 String inputLabel,
                                 String inputInstruction,
                                 int min,
                                 int max,
                                 boolean orderMatters,
                                 String[] types) {
        m_key = key;
        m_inputLabel = inputLabel;
        m_inputInstruction = inputInstruction;
        m_max = max;
        m_orderMatters = orderMatters;
        m_types = types;
        if (m_types == null || m_types.length == 0) {
            m_types = new String[] {"*/*"};
        }
        for (String element : m_types) {
            if (element.equals("*/*") || element.equals("*")) {
                m_acceptsAll = true;
            }
        }
    }

    /**
     * Does this rule allow the given type?
     */
    public boolean accepts(String type) {
        if (m_acceptsAll) {
            return true;
        }
        String[] parts = type.split("/");
        if (parts.length != 2) {
            return false;
        }
        for (String element : m_types) {
            if (element.equals(parts[0] + "/*") || element.equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the key of the rule.
     */
    public String getKey() {
        return m_key;
    }

    /**
     * Get the input label. This may be null.
     */
    public String getInputLabel() {
        return m_inputLabel;
    }

    /**
     * Get the input instruction. This may be null.
     */
    public String getInputInstruction() {
        return m_inputInstruction;
    }

    /**
     * Get the maximum number of datastreams that can be bound. -1 means there
     * is no maximum.
     */
    public int getMax() {
        return m_max;
    }

    /**
     * Get the minimum number of datastreams that can be bound.
     */
    public int getMin() {
        return m_max;
    }

    /**
     * Does order matter?
     */
    public boolean orderMatters() {
        return m_orderMatters;
    }

    /**
     * Get the type list. This will always have at least one element.
     */
    public String[] getTypes() {
        return m_types;
    }

}