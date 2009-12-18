/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security.servletfilters.xmluserfile;

import java.io.Serializable;

public class Role
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private String rolename;

    public Role() {
    }

    public String getRolename() {
        return rolename;
    }

    public void setRolename(String rolename) {
        this.rolename = rolename;
    }
}
