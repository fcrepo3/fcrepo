/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security;

/**
 * Configuration for a given backend service role.
 * 
 * @author Chris Wilper
 */
public interface BERoleConfig {

    /**
     * Get the name of the role this configuration applies to.
     */
    public String getRole();

    /**
     * Get the list of IP addresses that are allowed to make back-end callbacks
     * to Fedora using this role. For SDep/MethodRoleConfig, null means the
     * effective value is inherited. For DefaultRoleConfig, null means no
     * restriction.
     */
    public String[] getIPList();

    public String[] getEffectiveIPList();

    /**
     * Set the list of IP addresses that are allowed to make back-end callbacks
     * to Fedora using this role. For SDep/MethodRoleConfig, null means the
     * effective value is inherited. For DefaultRoleConfig, null means no
     * restriction.
     */
    public void setIPList(String[] ips);

    /**
     * Get whether backend callbacks for this role require basic auth. For
     * SDep/MethodRoleConfig, null means the effective value is inherited. For
     * DefaultRoleConfig, null means the effective value is false.
     */
    public Boolean getCallbackBasicAuth();

    public Boolean getEffectiveCallbackBasicAuth();

    /**
     * Set whether backend callbacks for this role require basic auth. For
     * SDep/MethodRoleConfig, null means the effective value is inherited. For
     * DefaultRoleConfig, null means the effective value is false.
     */
    public void setCallbackBasicAuth(Boolean value);

    /**
     * Get whether backend callbacks for this role require SSL. For
     * SDep/MethodRoleConfig, null means the effective value is inherited. For
     * DefaultRoleConfig, null means the effective value is false.
     */
    public Boolean getCallbackSSL();

    public Boolean getEffectiveCallbackSSL();

    /**
     * Set whether backend callbacks for this role require SSL. For
     * SDep/MethodRoleConfig, null means the effective value is inherited. For
     * DefaultRoleConfig, null means the effective value is false.
     */
    public void setCallbackSSL(Boolean value);

    /**
     * Get whether backend calls for this role will use basic auth. For
     * SDep/MethodRoleConfig, null means the effective value is inherited. For
     * DefaultRoleConfig, null means the effective value is false.
     */
    public Boolean getCallBasicAuth();

    public Boolean getEffectiveCallBasicAuth();

    /**
     * Set whether backend calls for this role will use basic auth. For
     * SDep/MethodRoleConfig, null means the effective value is inherited. For
     * DefaultRoleConfig, null means the effective value is false.
     */
    public void setCallBasicAuth(Boolean value);

    /**
     * Get whether backend calls for this role will SSL. For
     * SDep/MethodRoleConfig, null means the effective value is inherited. For
     * DefaultRoleConfig, null means the effective value is false.
     */
    public Boolean getCallSSL();

    public Boolean getEffectiveCallSSL();

    /**
     * Set whether backend calls for this role will SSL. For
     * SDep/MethodRoleConfig, null means the effective value is inherited. For
     * DefaultRoleConfig, null means the effective value is false.
     */
    public void setCallSSL(Boolean value);

    /**
     * Get the basicauth username for backend calls for this role. For
     * SDep/MethodRoleConfig, null means the effective value is inherited. For
     * DefaultRoleConfig, null means unspecified.
     */
    public String getCallUsername();

    public String getEffectiveCallUsername();

    /**
     * Set the basicauth username for backend calls for this role. For
     * SDep/MethodRoleConfig, null means the effective value is inherited. For
     * DefaultRoleConfig, null means unspecified.
     */
    public void setCallUsername(String user);

    /**
     * Get the basicauth password for backend calls for this role. For
     * SDep/MethodRoleConfig, null means the effective value is inherited. For
     * DefaultRoleConfig, null means unspecified.
     */
    public String getCallPassword();

    public String getEffectiveCallPassword();

    /**
     * Set the basicauth password for backend calls for this role. For
     * SDep/MethodRoleConfig, null means the effective value is inherited. For
     * DefaultRoleConfig, null means unspecified.
     */
    public void setCallPassword(String pass);

}