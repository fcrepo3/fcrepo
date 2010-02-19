/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.servletfilters;

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

import org.fcrepo.server.errors.authorization.AuthzOperationalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Bill Niebel
 */
public class ExtendedHttpServletRequestWrapper
        extends HttpServletRequestWrapper
        implements ExtendedHttpServletRequest {

    private static final Logger logger =
            LoggerFactory.getLogger(ExtendedHttpServletRequestWrapper.class);

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
        logger.debug(method + " , isSponsoredUserRequested()=="
                + isSponsoredUserRequested());
        if (isSponsoredUserRequested()) {
            sponsoredUser = getFromHeader();
            logger.debug(method + " , sponsoredUser==" + sponsoredUser);
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

    @Override
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

    @Override
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
        if (logger.isDebugEnabled()) {
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
                logger.debug(sb.toString());
            }
        }
    }

    public final void auditInnerSet(Set set) {
        if (logger.isDebugEnabled()) {
            for (Iterator it = set.iterator(); it.hasNext();) {
                Object value = it.next();
                if (value instanceof String) {
                    logger.debug((String) value);
                } else {
                    logger.debug("UNKNOWN");
                }
            }
        }
    }

    public final void auditOuterMap(Map map, String desc) {
        if (logger.isDebugEnabled()) {
            logger.debug("");
            logger.debug("auditing " + desc);
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
                    logger.debug(authority + " maps to . . .");
                    auditInnerMap((Map) inner);
                } else if (inner instanceof Set) {
                    logger.debug(authority + " maps to . . .");
                    auditInnerSet((Set) inner);
                } else {
                    logger.debug(authority + " maps to an unknown object=="
                            + map.getClass().getName());
                }
            }
        }
    }

    public void audit() {
        if (logger.isDebugEnabled()) {
            logger.debug("\n===AUDIT===");
            logger.debug("auditing wrapped request");
            auditOuterMap(authenticatedAttributes, "authenticatedAttributes");
            auditOuterMap(sponsoredAttributes, "sponsoredAttributes");
            logger.debug("===AUDIT===\n");
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
        logger.debug("mapping " + key + " => " + value + " in " + map);
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
            logger.error(exceptionMsg + ", header==" + header);
            throw new Exception(exceptionMsg);
        }
        logger.debug(msg + SUCCEEDED);

        String authschemeUsernamepassword[] = header.split("\\s+");

        msg = here + "header split";
        if (authschemeUsernamepassword.length != 2) {
            String exceptionMsg = msg + FAILED;
            logger.error(exceptionMsg + ", header==" + header);
            throw new Exception(exceptionMsg);
        }
        logger.debug(msg + SUCCEEDED);

        msg = here + "auth scheme";
        String authscheme = authschemeUsernamepassword[0];
        if (authscheme == null && !BASIC.equalsIgnoreCase(authscheme)) {
            String exceptionMsg = msg + FAILED;
            logger.error(exceptionMsg + ", authscheme==" + authscheme);
            throw new Exception(exceptionMsg);
        }
        logger.debug(msg + SUCCEEDED);

        msg = here + "digest non-null";
        String usernamepassword = authschemeUsernamepassword[1];
        if (usernamepassword == null || "".equals(usernamepassword)) {
            String exceptionMsg = msg + FAILED;
            logger.error(exceptionMsg + ", usernamepassword==" + usernamepassword);
            throw new Exception(exceptionMsg);
        }
        logger.debug(msg + SUCCEEDED + ", usernamepassword==" + usernamepassword);

        byte[] encoded = usernamepassword.getBytes();
        msg = here + "digest base64-encoded";
        if (!Base64.isArrayByteBase64(encoded)) {
            String exceptionMsg = msg + FAILED;
            logger.error(exceptionMsg + ", encoded==" + encoded);
            throw new Exception(exceptionMsg);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(msg + SUCCEEDED + ", encoded==" + encoded);
        }

        byte[] decodedAsByteArray = Base64.decodeBase64(encoded);
        logger.debug(here + "got decoded bytes" + SUCCEEDED
                + ", decodedAsByteArray==" + decodedAsByteArray);

        String decoded = new String(decodedAsByteArray); //decodedAsByteArray.toString();
        logger.debug(here + "got decoded string" + SUCCEEDED + ", decoded=="
                + decoded);

        msg = here + "digest decoded";
        if (decoded == null || "".equals(decoded)) {
            String exceptionMsg = msg + FAILED;
            logger.error(exceptionMsg + ", digest decoded==" + decoded);
            throw new Exception(exceptionMsg);
        }
        logger.debug(msg + SUCCEEDED);

        String DELIMITER = ":";
        if (decoded == null) {
            logger.error("decoded user/password is null . . . returning 0-length strings");
            usernamePassword = new String[2];
            usernamePassword[0] = "";
            usernamePassword[1] = "";
        } else if (decoded.indexOf(DELIMITER) < 0) {
            String exceptionMsg = "decoded user/password lacks delimiter";
            logger.error(exceptionMsg + " . . . throwing exception");
            throw new Exception(exceptionMsg);
        } else if (decoded.startsWith(DELIMITER)) {
            logger.error("decoded user/password is lacks user . . . returning 0-length strings");
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
            logger.error(exceptionMsg + ", digest decoded==" + decoded);
            throw new Exception(exceptionMsg);
        }
        logger.debug(msg + SUCCEEDED);

        return usernamePassword;
    }

    public static final String AUTHORIZATION = "Authorization";

    public final String getAuthorizationHeader() {
        logger.debug("getAuthorizationHeader()");
        logger.debug("getting this headers");
        for (Enumeration enu = getHeaderNames(); enu.hasMoreElements();) {
            String name = (String) enu.nextElement();
            logger.debug("another headername==" + name);
            String value = getHeader(name);
            logger.debug("another headervalue==" + value);
        }
        logger.debug("getting super headers");
        for (Enumeration enu = super.getHeaderNames(); enu.hasMoreElements();) {
            String name = (String) enu.nextElement();
            logger.debug("another headername==" + name);
            String value = super.getHeader(name);
            logger.debug("another headervalue==" + value);
        }
        return getHeader(AUTHORIZATION);
    }

    public static final String FROM = "From";

    public final String getFromHeader() {
        return getHeader(FROM);
    }

    public final String getUser() throws Exception {
        if (username == null) {
            logger.debug("username==null, so will grok now");
            String authorizationHeader = getAuthorizationHeader();
            logger.debug("authorizationHeader==" + authorizationHeader);
            if (authorizationHeader != null && !"".equals(authorizationHeader)) {
                logger.debug("authorizationHeader is intact");
                String[] usernamePassword =
                        parseUsernamePassword(authorizationHeader);
                logger.debug("usernamePassword[] length=="
                        + usernamePassword.length);
                username = usernamePassword[0];
                logger.debug("username (usernamePassword[0])==" + username);
                if (super.getRemoteUser() == null) {
                    logger.debug("had none before");
                } else if (super.getRemoteUser() == username
                        || super.getRemoteUser().equals(username)) {
                    logger.debug("got same now");
                } else {
                    throw new Exception("somebody got it wrong");
                }
            }
        }
        logger.debug("return user==" + username);
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
        logger.debug("return password==" + password);
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
    @Override
    @Deprecated
    public String getRealPath(String path) {
        return super.getRealPath(path);
    }

    /**
     * @deprecated As of Version 2.1 of the Java Servlet API, use
     *             {@link #isRequestedSessionIdFromURL()}.
     */
    @Override
    @Deprecated
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }

    @Override
    public boolean isSecure() {
        logger.debug("super.isSecure()==" + super.isSecure());
        logger.debug("this.getLocalPort()==" + getLocalPort());
        logger.debug("this.getProtocol()==" + getProtocol());
        logger.debug("this.getServerPort()==" + getServerPort());
        logger.debug("this.getRequestURI()==" + getRequestURI());
        return super.isSecure();
    }

}
