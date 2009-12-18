/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security.servletfilters;

import java.security.Principal;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.codec.binary.Base64;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fedora.server.errors.authorization.AuthzOperationalException;

/**
 * @author Bill Niebel
 */
public class ExtendedHttpServletRequestWrapper
        extends HttpServletRequestWrapper
        implements ExtendedHttpServletRequest {

    private static Log log =
            LogFactory.getLog(ExtendedHttpServletRequestWrapper.class);

    private String username = null;

    private String password = null;

    private String authority;

    private Principal userPrincipal;

    private boolean wrapperWriteLocked = false;

    public final void lockWrapper() throws Exception {
        lockSponsoredUser();
        wrapperWriteLocked = true;
    }

    private String sponsoredUser = null; // null == not yet set; "" == no sponsored user; other values valid

    private void setSponsoredUser(String sponsoredUser) throws Exception {
        if (this.sponsoredUser == null) {
            this.sponsoredUser = sponsoredUser;
        }
    }

    public void setSponsoredUser() throws Exception {
        String method = "setSponsoredUser";
        String sponsoredUser = "";
        log.debug(method + " , isSponsoredUserRequested()=="
                + isSponsoredUserRequested());
        if (isSponsoredUserRequested()) {
            sponsoredUser = getFromHeader();
            log.debug(method + " , sponsoredUser==" + sponsoredUser);
        }
        setSponsoredUser(sponsoredUser);
    }

    public void lockSponsoredUser() throws Exception {
        setSponsoredUser("");
    }

    public void setAuthenticated(Principal userPrincipal, String authority)
            throws Exception {
        if (wrapperWriteLocked) {
            throw new Exception();
        }
        if (isAuthenticated()) {
            throw new Exception();
        }
        this.userPrincipal = userPrincipal;
        this.authority = authority;
    }

    public Principal getUserPrincipal() {
        //this order reinforces that container-supplied userPrincipal should not be overridden in setUserPrincipal()
        Principal userPrincipal = super.getUserPrincipal();
        if (userPrincipal == null) {
            userPrincipal = this.userPrincipal;
        }
        return userPrincipal;
    }

    public final boolean isUserSponsored() {
        return !(sponsoredUser == null || "".equals(sponsoredUser));
    }

    protected boolean isSponsoredUserRequested() {
        String sponsoredUser = getFromHeader();
        boolean isSponsoredUserRequested =
                !(sponsoredUser == null || "".equals(sponsoredUser));
        return isSponsoredUserRequested;
    }

    public final boolean isAuthenticated() {
        return getUserPrincipal() != null;
    }

    public String getRemoteUser() {
        String remoteUser = null;
        if (isUserSponsored()) {
            remoteUser = sponsoredUser;
        } else {
            remoteUser = super.getRemoteUser();
            if (remoteUser == null && userPrincipal != null) {
                remoteUser = userPrincipal.getName();
            }
        }
        return remoteUser;
    }

    private final Map authenticatedAttributes = new Hashtable();

    private final Map sponsoredAttributes = new Hashtable();

    public final void auditInnerMap(Map map) {
        if (log.isDebugEnabled()) {
            for (Iterator it = map.keySet().iterator(); it.hasNext();) {
                String key = (String) it.next();
                Object value = map.get(key);
                StringBuffer sb = new StringBuffer(key + "==");
                String comma = "";
                if (value instanceof String) {
                    sb.append(value);
                } else if (value instanceof String[]) {
                    sb.append("[");
                    for (int i = 0; i < ((String[]) value).length; i++) {
                        Object o = ((String[]) value)[i];
                        if (o instanceof String) {
                            sb.append(comma + o);
                            comma = ",";
                        } else {
                            sb.append(comma + "UNKNOWN");
                            comma = ",";
                        }
                    }
                    sb.append("]");
                } else if (value instanceof Set) {
                    sb.append("{");
                    for (Iterator it2 = ((Set) value).iterator(); it2.hasNext();) {
                        Object o = it2.next();
                        if (o instanceof String) {
                            sb.append(comma + o);
                            comma = ",";
                        } else {
                            sb.append(comma + "UNKNOWN");
                            comma = ",";
                        }
                    }
                    sb.append("}");
                } else {
                    sb.append("UNKNOWN");
                }
                log.debug(sb.toString());
            }
        }
    }

    public final void auditInnerSet(Set set) {
        if (log.isDebugEnabled()) {
            for (Iterator it = set.iterator(); it.hasNext();) {
                Object value = it.next();
                if (value instanceof String) {
                    log.debug(value);
                } else {
                    log.debug("UNKNOWN");
                }
            }
        }
    }

    public final void auditOuterMap(Map map, String desc) {
        if (log.isDebugEnabled()) {
            log.debug("");
            log.debug("auditing " + desc);
            for (Iterator it = map.keySet().iterator(); it.hasNext();) {
                Object key = it.next();
                Object inner = map.get(key);
                String authority = "";
                if (key instanceof String) {
                    authority = (String) key;
                } else {
                    authority = "<authority not a string>";
                }
                if (inner instanceof Map) {
                    log.debug(authority + " maps to . . .");
                    auditInnerMap((Map) inner);
                } else if (inner instanceof Set) {
                    log.debug(authority + " maps to . . .");
                    auditInnerSet((Set) inner);
                } else {
                    log.debug(authority + " maps to an unknown object=="
                            + map.getClass().getName());
                }
            }
        }
    }

    public void audit() {
        if (log.isDebugEnabled()) {
            log.debug("\n===AUDIT===");
            log.debug("auditing wrapped request");
            auditOuterMap(authenticatedAttributes, "authenticatedAttributes");
            auditOuterMap(sponsoredAttributes, "sponsoredAttributes");
            log.debug("===AUDIT===\n");
        }
    }

    public boolean getAttributeDefined(String key)
            throws AuthzOperationalException {
        boolean defined = false;
        Map map = null;
        if (isUserSponsored()) {
            map = sponsoredAttributes;
        } else {
            map = authenticatedAttributes;
        }
        for (Iterator iterator = map.values().iterator(); iterator.hasNext();) {
            Map attributesFromOneAuthority = (Map) iterator.next();
            if (attributesFromOneAuthority.containsKey(key)) {
                defined = true;
                break;
            }
        }
        return defined;
    }

    public Set getAttributeValues(String key) throws AuthzOperationalException {
        Set accumulatedValues4Key = null;
        Map map = null;
        if (isUserSponsored()) {
            map = sponsoredAttributes;
        } else {
            map = authenticatedAttributes;
        }
        for (Iterator iterator = map.values().iterator(); iterator.hasNext();) {
            Map attributesFromOneAuthority = (Map) iterator.next();
            if (attributesFromOneAuthority.containsKey(key)) {
                Set someValues4Key = (Set) attributesFromOneAuthority.get(key);
                if (someValues4Key != null && !someValues4Key.isEmpty()) {
                    if (accumulatedValues4Key == null) {
                        accumulatedValues4Key = new HashSet();
                    }
                    accumulatedValues4Key.addAll(someValues4Key);
                }
            }
        }
        if (accumulatedValues4Key == null) {
            accumulatedValues4Key = IMMUTABLE_NULL_SET;
        }
        return accumulatedValues4Key;
    }

    public boolean hasAttributeValues(String key)
            throws AuthzOperationalException {
        Set temp = getAttributeValues(key);
        return !temp.isEmpty();
    }

    public boolean isAttributeDefined(String key)
            throws AuthzOperationalException {
        boolean isAttributeDefined;
        isAttributeDefined = getAttributeDefined(key);
        return isAttributeDefined;
    }

    private void putIntoMap(Map map, String key, Object value) throws Exception {
        if (wrapperWriteLocked) {
            throw new Exception();
        }
        if (!isAuthenticated()) {
            throw new Exception("can't collect user roles/attributes/groups until after authentication");
        }
        if (map == null || key == null || value == null) {
            throw new Exception("null parm, map==" + map + ", key==" + key
                    + ", value==" + value);
        }
        if (map.containsKey(key)) {
            throw new Exception("map already contains key==" + key);
        }
        log.debug("mapping " + key + " => " + value + " in " + map);
        map.put(key, value);
    }

    private void putMapIntoMap(Map map, String key, Object value)
            throws Exception {
        if (!(value instanceof Map)) {
            throw new Exception("input parm must be a map");
        }
        putIntoMap(map, key, value);
    }

    public void addAttributes(String authority, Map attributes)
            throws Exception {
        if (isUserSponsored()) {
            // after user is sponsored, only sponsored-user roles/attributes/groups are collected
            putMapIntoMap(sponsoredAttributes, authority, attributes);
        } else {
            // before user is sponsored, only authenticated-user roles/attributes/groups are collected
            putMapIntoMap(authenticatedAttributes, authority, attributes);
        }
    }

    private Map getAllAttributes(Map attributeGroup) {
        Map all = new Hashtable();
        for (Iterator it = attributeGroup.values().iterator(); it.hasNext();) {
            Map m = (Map) it.next();
            all.putAll(m);
        }
        return all;
    }

    public Map getAllAttributes() throws AuthzOperationalException {
        Map all = null;
        if (isUserSponsored()) {
            all = getAllAttributes(sponsoredAttributes);
        } else {
            all = getAllAttributes(authenticatedAttributes);
        }
        return all;
    }

    public static final String BASIC = "Basic";

    private final String[] parseUsernamePassword(String header)
            throws Exception {
        String here = "parseUsernamePassword():";
        String[] usernamePassword = null;

        String msg = here + "header intact";
        if (header == null || "".equals(header)) {
            String exceptionMsg = msg + FAILED;
            log.fatal(exceptionMsg + ", header==" + header);
            throw new Exception(exceptionMsg);
        }
        log.debug(msg + SUCCEEDED);

        String authschemeUsernamepassword[] = header.split("\\s+");

        msg = here + "header split";
        if (authschemeUsernamepassword.length != 2) {
            String exceptionMsg = msg + FAILED;
            log.fatal(exceptionMsg + ", header==" + header);
            throw new Exception(exceptionMsg);
        }
        log.debug(msg + SUCCEEDED);

        msg = here + "auth scheme";
        String authscheme = authschemeUsernamepassword[0];
        if (authscheme == null && !BASIC.equalsIgnoreCase(authscheme)) {
            String exceptionMsg = msg + FAILED;
            log.fatal(exceptionMsg + ", authscheme==" + authscheme);
            throw new Exception(exceptionMsg);
        }
        log.debug(msg + SUCCEEDED);

        msg = here + "digest non-null";
        String usernamepassword = authschemeUsernamepassword[1];
        if (usernamepassword == null || "".equals(usernamepassword)) {
            String exceptionMsg = msg + FAILED;
            log.fatal(exceptionMsg + ", usernamepassword==" + usernamepassword);
            throw new Exception(exceptionMsg);
        }
        log.debug(msg + SUCCEEDED + ", usernamepassword==" + usernamepassword);

        byte[] encoded = usernamepassword.getBytes();
        msg = here + "digest base64-encoded";
        if (!Base64.isArrayByteBase64(encoded)) {
            String exceptionMsg = msg + FAILED;
            log.fatal(exceptionMsg + ", encoded==" + encoded);
            throw new Exception(exceptionMsg);
        }
        if (log.isDebugEnabled()) {
            log.debug(msg + SUCCEEDED + ", encoded==" + encoded);
        }

        byte[] decodedAsByteArray = Base64.decodeBase64(encoded);
        log.debug(here + "got decoded bytes" + SUCCEEDED
                + ", decodedAsByteArray==" + decodedAsByteArray);

        String decoded = new String(decodedAsByteArray); //decodedAsByteArray.toString();
        log.debug(here + "got decoded string" + SUCCEEDED + ", decoded=="
                + decoded);

        msg = here + "digest decoded";
        if (decoded == null || "".equals(decoded)) {
            String exceptionMsg = msg + FAILED;
            log.fatal(exceptionMsg + ", digest decoded==" + decoded);
            throw new Exception(exceptionMsg);
        }
        log.debug(msg + SUCCEEDED);

        String DELIMITER = ":";
        if (decoded == null) {
            log
                    .error("decoded user/password is null . . . returning 0-length strings");
            usernamePassword = new String[2];
            usernamePassword[0] = "";
            usernamePassword[1] = "";
        } else if (decoded.indexOf(DELIMITER) < 0) {
            String exceptionMsg = "decoded user/password lacks delimiter";
            log.fatal(exceptionMsg + " . . . throwing exception");
            throw new Exception(exceptionMsg);
        } else if (decoded.startsWith(DELIMITER)) {
            log
                    .error("decoded user/password is lacks user . . . returning 0-length strings");
            usernamePassword = new String[2];
            usernamePassword[0] = "";
            usernamePassword[1] = "";
        } else if (decoded.endsWith(DELIMITER)) { // no password, e.g., user == "guest"
            usernamePassword = new String[2];
            usernamePassword[0] = decoded.substring(0, decoded.length() - 1);
            usernamePassword[1] = "";
        } else { // usual, expected case
            usernamePassword = decoded.split(DELIMITER);
        }

        msg = here + "user/password split";
        if (usernamePassword.length != 2) {
            String exceptionMsg = msg + FAILED;
            log.fatal(exceptionMsg + ", digest decoded==" + decoded);
            throw new Exception(exceptionMsg);
        }
        log.debug(msg + SUCCEEDED);

        return usernamePassword;
    }

    public static final String AUTHORIZATION = "Authorization";

    public final String getAuthorizationHeader() {
        log.debug("getAuthorizationHeader()");
        log.debug("getting this headers");
        for (Enumeration enu = getHeaderNames(); enu.hasMoreElements();) {
            String name = (String) enu.nextElement();
            log.debug("another headername==" + name);
            String value = getHeader(name);
            log.debug("another headervalue==" + value);
        }
        log.debug("getting super headers");
        for (Enumeration enu = super.getHeaderNames(); enu.hasMoreElements();) {
            String name = (String) enu.nextElement();
            log.debug("another headername==" + name);
            String value = super.getHeader(name);
            log.debug("another headervalue==" + value);
        }
        return getHeader(AUTHORIZATION);
    }

    public static final String FROM = "From";

    public final String getFromHeader() {
        return getHeader(FROM);
    }

    public final String getUser() throws Exception {
        if (username == null) {
            log.debug("username==null, so will grok now");
            String authorizationHeader = getAuthorizationHeader();
            log.debug("authorizationHeader==" + authorizationHeader);
            if (authorizationHeader != null && !"".equals(authorizationHeader)) {
                log.debug("authorizationHeader is intact");
                String[] usernamePassword =
                        parseUsernamePassword(authorizationHeader);
                log.debug("usernamePassword[] length=="
                        + usernamePassword.length);
                username = usernamePassword[0];
                log.debug("username (usernamePassword[0])==" + username);
                if (super.getRemoteUser() == null) {
                    log.debug("had none before");
                } else if (super.getRemoteUser() == username
                        || super.getRemoteUser().equals(username)) {
                    log.debug("got same now");
                } else {
                    throw new Exception("somebody got it wrong");
                }
            }
        }
        log.debug("return user==" + username);
        return username;
    }

    public final String getPassword() throws Exception {
        if (password == null) {
            String authorizationHeader = getAuthorizationHeader();
            if (authorizationHeader != null && !"".equals(authorizationHeader)) {
                String[] usernamePassword =
                        parseUsernamePassword(authorizationHeader);
                password = usernamePassword[1];
            }
        }
        log.debug("return password==" + password);
        return password;
    }

    public final String getAuthority() {
        return authority;
    }

    public ExtendedHttpServletRequestWrapper(HttpServletRequest wrappedRequest)
            throws Exception {
        super(wrappedRequest);
    }

    /**
     * @deprecated As of Version 2.1 of the Java Servlet API, use
     *             {@link ServletContext#getRealPath(java.lang.String)}.
     */
    @Deprecated
    public String getRealPath(String path) {
        return super.getRealPath(path);
    }

    /**
     * @deprecated As of Version 2.1 of the Java Servlet API, use
     *             {@link #isRequestedSessionIdFromURL()}.
     */
    @Deprecated
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }

    public boolean isSecure() {
        log.debug("super.isSecure()==" + super.isSecure());
        log.debug("this.getLocalPort()==" + getLocalPort());
        log.debug("this.getProtocol()==" + getProtocol());
        log.debug("this.getServerPort()==" + getServerPort());
        log.debug("this.getRequestURI()==" + getRequestURI());
        return super.isSecure();
    }

}
