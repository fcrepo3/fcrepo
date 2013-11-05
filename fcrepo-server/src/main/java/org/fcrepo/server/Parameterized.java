/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server;

import java.io.File;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.fcrepo.common.Constants;
import org.fcrepo.server.config.Parameter;


/**
 * Abstract superclass of all Fedora components that can be configured by a set
 * of name-value pairs.
 *
 * @author Chris Wilper
 */
public abstract class Parameterized implements Constants {

    /** a reference to the provided params for this component */
    private final Map<String, Parameter> m_parameters = new HashMap<String, Parameter>();

    /**
     * Creates a Parameterized with no parameters.
     */
    public Parameterized() {
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

    public Parameterized(List<Parameter> parameters) {
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
        setParameters(Parameterized.getParameterList(parameters));
    }

    protected final void setParameters(List<Parameter> parameters) {
        if (parameters == null) {
            m_parameters.clear();
        }
        else {
            m_parameters.clear();
            for (Parameter p:parameters){
                m_parameters.put(p.getName(), p);
            }
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
    public String getParameter(String name, boolean asAbsolutePath) {
        if (!m_parameters.containsKey(name)) return null;

        String paramValue = m_parameters.get(name).getValue();
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
    public String getParameter(String name) {
        return getParameter(name, false);
    }

    public Parameter getParameter(String name,Class<Parameter> type) {
        return m_parameters.get(name);
    }


    protected final void setParameter(String name, String value) {
        Parameter parm = m_parameters.get(name);
        if (parm == null){
            parm = new Parameter(name);
            m_parameters.put(name, parm);
        }
        parm.setValue(value);
    }

    public Map<String, String> getParameters() {
        Map<String,String> result = new HashMap<String,String>(m_parameters.size(),1);
        for(Entry<String,Parameter> entry:m_parameters.entrySet()){
            result.put(entry.getKey(), entry.getValue().getValue());
        }
        return result;
    }

    public Collection<Parameter> getParameters(Class<Parameter> type) {
        return m_parameters.values();
    }


    /**
     * Gets an Iterator over the names of parameters for this component.
     *
     * @return Iterator The names.
     */
    public final Iterator<String> parameterNames() {
        return m_parameters.keySet().iterator();
    }

    protected static List<Parameter> getParameterList(Map<String,String> map){
        Set<String> keys = map.keySet();
        Parameter[] parms = new Parameter[keys.size()];
        int i = 0;
        for (String key:keys){
            parms[i] = new Parameter(key);
            parms[i].setValue(map.get(key));
            i++;
        }
        return Arrays.asList(parms);
    }

}
