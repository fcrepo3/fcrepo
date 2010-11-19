/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.config;

import java.util.List;
import java.util.Map;

import org.fcrepo.server.Parameterized;

/**
 *
 */
public abstract class Configuration extends Parameterized {

    protected Configuration(List<Parameter> parameters) {
        super(parameters);
    }

    protected Configuration(Map<String,String> parameters) {
        super(parameters);
    }



    public void setParameterValue(String name, String value, boolean autoCreate) {
        Parameter param = getParameter(name,Parameter.class);
        if (param == null && !autoCreate) {
            return;
        }
        setParameter(name, value);
    }
}
