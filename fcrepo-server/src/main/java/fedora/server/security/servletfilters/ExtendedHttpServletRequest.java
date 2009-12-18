/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security.servletfilters;

import java.security.Principal;

import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import fedora.server.errors.authorization.AuthzOperationalException;

/**
 * @author Bill Niebel
 */
public interface ExtendedHttpServletRequest
        extends HttpServletRequest {

    public static final String SUCCEEDED = "succeeded";

    public static final String FAILED = "failed";

    public static final ImmutableHashSet IMMUTABLE_NULL_SET =
            new ImmutableHashSet();

    public void audit();

    public void lockWrapper() throws Exception;

    public void setSponsoredUser() throws Exception;

    public void lockSponsoredUser() throws Exception;

    public void setAuthenticated(Principal userPrincipal, String authority)
            throws Exception;

    public boolean isUserSponsored();

    public boolean isAuthenticated();

    public Set getAttributeValues(String key) throws AuthzOperationalException;

    public boolean hasAttributeValues(String key)
            throws AuthzOperationalException;

    public boolean isAttributeDefined(String key)
            throws AuthzOperationalException;

    public void addAttributes(String authority, Map attributes)
            throws Exception;

    public String getUser() throws Exception;

    public String getPassword() throws Exception;

    public Map getAllAttributes() throws Exception;

    public String getAuthority();

    public String getFromHeader();

}
