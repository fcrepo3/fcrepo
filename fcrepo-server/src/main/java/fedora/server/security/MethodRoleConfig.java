/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security;

public class MethodRoleConfig
        extends AbstractRoleConfig {

    private final String m_role;

    public MethodRoleConfig(ServiceDeploymentRoleConfig sDepConfig, String methodName) {
        super(sDepConfig);
        m_role = sDepConfig.getRole() + "/" + methodName;
    }

    @Override
    public String getRole() {
        return m_role;
    }

}