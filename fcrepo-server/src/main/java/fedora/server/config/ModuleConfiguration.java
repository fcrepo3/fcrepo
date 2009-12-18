/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.config;

import java.util.List;

/**
 *
 */
public class ModuleConfiguration
        extends Configuration {

    private final String m_roleName;

    private String m_className;

    private final String m_comment;

    public ModuleConfiguration(List<Parameter> parameters,
                               String roleName,
                               String className,
                               String comment) {
        super(parameters);
        m_roleName = roleName;
        m_className = className;
        m_comment = comment;
    }

    public String getRole() {
        return m_roleName;
    }

    public void setClassName(String className) {
        m_className = className;
    }

    public String getClassName() {
        return m_className;
    }

    public String getComment() {
        return m_comment;
    }

}
