/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import fedora.common.Constants;

/**
 * Abstract superclass of all Fedora components that can be configured by a set
 * of name-value pairs.
 * 
 * @author Chris Wilper
 */
public abstract class Parameterized implements Constants {

    /** a reference to the provided params for this component */
    private Map<String, String> m_parameters;

    /**
     * Creates a Parameterized with no parameters.
     */
    public Parameterized() {
        setParameters(null);
    }

    /**
     * Creates a Parameterized with name-value pairs from the supplied Map.
     * 
     * @param parameters
     *        The map from which to derive the name-value pairs.
     */
    public Parameterized(Map<String, String> parameters) {
        setParameters(parameters);
    }

    /**
     * Sets the parameters with name-value pairs from the supplied Map. This is
     * protected because it is intended to only be called by subclasses where
     * super(Map m) is not possible to call at the start of the constructor.
     * Server.java:Server(URL) is an example of this.
     * 
     * @param parameters
     *        The map from which to derive the name-value pairs.
     */
    protected final void setParameters(Map<String, String> parameters) {
        m_parameters = parameters;
        if (m_parameters == null) {
            m_parameters = new HashMap<String, String>();
        }
    }

    /**
     * Gets the value of a named configuration parameter. Same as
     * getParameter(String name) but prepends the location of FEDORA_HOME if
     * asAbsolutePath is true and the parameter location does not already
     * specify an absolute pathname.
     * 
     * @param name
     *        The parameter name.
     * @param asAbsolutePath
     *        Whether to return the parameter value as an absolute path relative
     *        to FEDORA_HOME.
     * @return The value, null if undefined.
     */
    public final String getParameter(String name, boolean asAbsolutePath) {
        String paramValue = m_parameters.get(name);
        if (asAbsolutePath && paramValue != null) {
            File f = new File(paramValue);
            if (!f.isAbsolute()) {
                paramValue = FEDORA_HOME + File.separator + paramValue;
            }
        }
        return paramValue;
    }

    /**
     * Gets the value of a named configuration parameter.
     * 
     * @param name
     *        The parameter name.
     * @return String The value, null if undefined.
     */
    public final String getParameter(String name) {
        return getParameter(name, false);
    }

    protected final void setParameter(String name, String value) {
        m_parameters.put(name, value);
    }

    public Map<String, String> getParameters() {
        return m_parameters;
    }

    /**
     * Gets an Iterator over the names of parameters for this component.
     * 
     * @return Iterator The names.
     */
    public final Iterator<String> parameterNames() {
        return m_parameters.keySet().iterator();
    }

}
