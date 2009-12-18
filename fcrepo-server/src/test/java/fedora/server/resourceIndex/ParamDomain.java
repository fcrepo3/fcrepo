/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.resourceIndex;

import java.util.TreeSet;

/**
 * A sorted set of domain values for a parameter. As per the
 * <code>SortedSet</code> contract, iterators over the values in this
 * collection will provide the elements in ascending order.
 * 
 * @author Chris Wilper
 */
public class ParamDomain
        extends TreeSet<String> {

    private static final long serialVersionUID = 1L;

    /**
     * The parameter whose domain is being described.
     */
    private final String _parameterName;

    /**
     * Whether specifying a value is required.
     */
    private final boolean _isRequired;

    /**
     * Construct an empty <code>ParamDomain</code>.
     * 
     * @param parameterName
     *        the parameter whose domain is being described.
     * @param isRequired
     *        whether specifying a value is required.
     */
    public ParamDomain(String parameterName, boolean isRequired) {
        _parameterName = parameterName;
        _isRequired = isRequired;
    }

    /**
     * Construct a <code>ParamDomain</code> with values from the given array.
     * 
     * @param parameterName
     *        the parameter whose domain is being described.
     * @param isRequired
     *        whether specifying a value is required.
     * @param values
     *        the domain values.
     */
    public ParamDomain(String parameterName,
                       boolean isRequired,
                       String[] domainValues) {
        _parameterName = parameterName;
        _isRequired = isRequired;
        for (String element : domainValues) {
            add(element);
        }
    }

    /**
     * Get the name of the parameter whose domain is being described.
     */
    public String getParameterName() {
        return _parameterName;
    }

    /**
     * Tell whether specifying a value is required.
     */
    public boolean isRequired() {
        return _isRequired;
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            ParamDomain p = (ParamDomain) obj;
            return _parameterName.equals(p.getParameterName())
                    && _isRequired == p.isRequired();
        } else {
            return false;
        }
    }

}
