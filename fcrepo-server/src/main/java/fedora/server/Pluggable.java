/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server;

import java.util.Map;

/**
 * Abstract superclass of all Fedora components that can be configured by a 
 * set of name-value pairs.
 * 
 * @author Chris Wilper
 */
public abstract class Pluggable
        extends Parameterized {

    /** an empty array of strings */
    private final static String[] EMPTY_STRING_ARRAY = new String[] {};

    /**
     * Creates a Pluggable with no parameters.
     */
    public Pluggable() {
    }

    /**
     * Creates a Pluggable with name-value pairs from the supplied Map.
     * 
     * @param parameters
     *        The map from which to derive the name-value pairs.
     */
    public Pluggable(Map<String, String> parameters) {
        setParameters(parameters);
    }

    /**
     * Gets the names of required parameters for this component.
     * 
     * @return String[] The required parameter names.
     */
    public String[] getRequiredParameters() {
        return EMPTY_STRING_ARRAY;
    }

    /**
     * Gets the names of optional parameters for this component.
     * 
     * @return String[] The required parameter names.
     */
    public String[] getOptionalParameters() {
        return EMPTY_STRING_ARRAY;
    }

    /**
     * Gets a short explanation of how to use a named parameter.
     * 
     * @param name
     *        The name of the parameter.
     * @return String The explanation, null if no help is available or the
     *         parameter is unknown.
     */
    public String getParameterHelp(String name) {
        return null;
    }

    /**
     * Gets an explanation of how this component is to be configured via
     * parameters. This should not include the information available via
     * getParameterHelp, but is more intended as an overall explanation or an
     * explanation of those parameters whose names might be dynamic.
     */
    public String getHelp() {
        return "";
    }

    /**
     * Gets the names of the roles that are required by this
     * <code>Pluggable</code>.
     * <p>
     * </p>
     * By default, no roles need to be fulfilled.
     * 
     * @return The roles.
     */
    public String[] getRequiredModuleRoles() {
        return EMPTY_STRING_ARRAY;
    }

}
