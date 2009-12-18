/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.config;

import java.util.HashMap;
import java.util.List;

/**
 *
 */
public abstract class Configuration {

    private final List<Parameter> m_parameters;

    protected Configuration(List<Parameter> parameters) {
        m_parameters = parameters;
    }

    public List<Parameter> getParameters() {
        return m_parameters;
    }

    public Parameter getParameter(String name) {
        for (int i = 0; i < m_parameters.size(); i++) {
            Parameter param = m_parameters.get(i);
            if (param.getName().equals(name)) {
                return param;
            }
        }
        return null;
    }

    public void setParameterValue(String name, String value, boolean autoCreate) {
        Parameter param = getParameter(name);
        if (param == null) {
            m_parameters.add(new Parameter(name,
                                           value,
                                           false,
                                           null,
                                           new HashMap()));
        } else {
            param.setValue(value);
        }
    }
}
