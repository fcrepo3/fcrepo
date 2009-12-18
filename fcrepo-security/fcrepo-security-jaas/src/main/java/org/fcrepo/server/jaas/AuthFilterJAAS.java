/*
 * File: AuthFilterJAAS.java
 * 
 * Copyright 2009 Muradora
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.fcrepo.server.jaas;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.fcrepo.server.jaas.auth.AuthHttpServletRequestWrapper;
import org.fcrepo.server.jaas.auth.handler.UsernamePasswordCallbackHandler;
import org.fcrepo.server.jaas.util.Base64;
import org.fcrepo.server.jaas.util.SubjectUtils;

import fedora.common.Constants;

/**
 * A Servlet Filter for protecting resources. This filter uses JAAS for
 * performing user authentication. Once a user is authenticated, a user
 * principal object that is returned from the JAAS login module is created and
 * added to the servlet request. The parameters of this filter are as follows:
 * <ul>
 * <li>
 * <p>
 * <strong>jaas.config.location</strong>
 * </p>
 * <p>
 * This specifies the location of the jaas configuration file. The default is
 * $FEDORA_HOME/server/config/jaas.conf
 * </p>
 * </li>
 * <li>
 * <p>
 * <strong>jaas.config.name</strong>
 * </p>
 * <p>
 * The name of the jaas configuration to use. The default is fedora-auth
 * </p>
 * </li>
 * </ul>
 * 
 * @author nish.naidoo@gmail.com
 */
public class AuthFilterJAAS
        implements Filter {

    private static Logger log = Logger.getLogger(AuthFilterJAAS.class);

    private static final String SESSION_SUBJECT_KEY =
            "javax.security.auth.subject";

    private static final String JAAS_CONFIG_KEY =
            "java.security.auth.login.config";

    private static final String JAAS_CONFIG_DEFAULT = "fedora-auth";

    private static final String ROLE_KEY = "role";

    private static final String FEDORA_ROLE_KEY = "fedoraRole";

    private static final String FEDORA_ATTRIBUTES_KEY =
            "FEDORA_AUX_SUBJECT_ATTRIBUTES";

    private String jaasConfigName = null;

    private FilterConfig filterConfig = null;

    private Set<String> userClassNames = null;

    private Set<String> roleClassNames = null;

    private Set<String> roleAttributeNames = null;

    private Set<String> excludedUris = null;

    public void init(FilterConfig filterConfig) throws ServletException {
        // get FEDORA_HOME. This being set is mandatory.
        String fedoraHome = Constants.FEDORA_HOME;
        if (fedoraHome == null || "".equals(fedoraHome)) {
            String msg = "FEDORA_HOME environment variable not set";
            throw new ServletException(msg);
        }

        this.filterConfig = filterConfig;
        if (this.filterConfig == null) {
            log.info("No configuration for: " + this.getClass().getName());
        }

        log.info("using FEDORA_HOME: " + fedoraHome);

        // Get the jaas.conf file to use and the config to use from the
        // jaas.conf file. This defaults to $FEDORA_HOME/server/config/jaas.conf
        // and 'fedora-auth' for the configuration.
        String jaasConfigLocation = fedoraHome + "/server/config/jaas.conf";
        jaasConfigName = JAAS_CONFIG_DEFAULT;

        String tmp = null;

        tmp = filterConfig.getInitParameter("jaas.config.location");
        if (tmp != null && !"".equals(tmp)) {
            jaasConfigLocation = tmp;
            if (log.isDebugEnabled()) {
                log.debug("using location from init file: "
                        + jaasConfigLocation);
            }
        }

        tmp = filterConfig.getInitParameter("jaas.config.name");
        if (tmp != null && !"".equals(tmp)) {
            jaasConfigName = tmp;
            if (log.isDebugEnabled()) {
                log.debug("using name from init file: " + jaasConfigName);
            }
        }

        tmp = filterConfig.getInitParameter("excludeUris");
        excludedUris = new HashSet<String>();
        if (tmp != null) {
            String[] names = tmp.split(" *, *");
            if (names != null && names.length > 0) {
                for (String n : names) {
                    excludedUris.add(n);
                }
            }
        }

        tmp = filterConfig.getInitParameter("userClassNames");
        userClassNames = new HashSet<String>();
        if (tmp != null) {
            String[] names = tmp.split(" *, *");
            if (names != null && names.length > 0) {
                for (String n : names) {
                    userClassNames.add(n);
                }
            }
        }

        tmp = filterConfig.getInitParameter("roleClassNames");
        roleClassNames = new HashSet<String>();
        if (tmp != null) {
            String[] names = tmp.split(" *, *");
            if (names != null && names.length > 0) {
                for (String n : names) {
                    roleClassNames.add(n);
                }
            }
        }

        tmp = filterConfig.getInitParameter("roleAttributeNames");
        roleAttributeNames = new HashSet<String>();
        roleAttributeNames.add(ROLE_KEY);
        roleAttributeNames.add(FEDORA_ROLE_KEY);
        if (tmp != null) {
            String[] names = tmp.split(" *, *");
            if (names != null && names.length > 0) {
                for (String n : names) {
                    roleAttributeNames.add(n);
                }
            }
        }

        File jaasConfig = new File(jaasConfigLocation);
        if (!jaasConfig.exists()) {
            String msg =
                    "JAAS config file not at: " + jaasConfig.getAbsolutePath();
            log.error(msg);
            throw new ServletException(msg);
        }

        System.setProperty(JAAS_CONFIG_KEY, jaasConfig.getAbsolutePath());

        log.info("initialised servlet filter: " + this.getClass().getName());
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     * javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // if the servlet is in our excluded list, just continue the
        // chain and return after processing the chain.
        if (excludedUris != null && excludedUris.size() > 0) {
            if (excludedUris.contains(req.getServletPath())) {
                if (log.isDebugEnabled()) {
                    log.debug("skipping authentication on servlet: "
                            + req.getServletPath());
                }

                chain.doFilter(request, response);

                return;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("incoming filter: " + this.getClass().getName());
            log.debug("session-id: " + req.getSession().getId());
        }

        Subject subject = authenticate(req);

        if (subject == null) {
            loginForm(res);
            return;
        }

        // obtain the user principal from the subject and add to servlet.
        Principal userPrincipal = getUserPrincipal(subject);

        // obtain the user roles from the subject and add to servlet.
        Set<String> userRoles = getUserRoles(subject);

        // wrap the request in one that has the ability to store role
        // and principal information and store this information.
        AuthHttpServletRequestWrapper authRequest =
                new AuthHttpServletRequestWrapper(req);
        authRequest.setUserPrincipal(userPrincipal);
        authRequest.setUserRoles(userRoles);

        // add the roles that were obtained to the Subject.
        addRolesToSubject(subject, userRoles);

        // add the roles that were obtained to the Fedora attribute location.
        populateFedoraRoles(subject, userRoles, authRequest);

        chain.doFilter(authRequest, response);

        if (log.isDebugEnabled()) {
            log.debug("outgoing filter: " + this.getClass().getName());
        }
    }

    public void destroy() {
        log.info("destroying servlet filter: " + this.getClass().getName());
        filterConfig = null;
    }

    /**
     * Sends a 401 error to the browser. This forces a login box to be displayed
     * allowing the user to login.
     * 
     * @param response
     *        the response to set the headers and status
     */
    private void loginForm(HttpServletResponse response) throws IOException {
        response.reset();
        response.addHeader("WWW-Authenticate",
                           "Basic realm=\"!!Fedora Repository Server\"");
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		OutputStream out = response.getOutputStream();
		out.write("Fedora: 401 ".getBytes());
		out.flush();
		out.close();
	}

    /**
     * Performs the authentication. Once a Subject is obtained, it is stored in
     * the users session. Subsequent requests check for the existence of this
     * object before performing the authentication again.
     * 
     * @param req
     *        the servlet request.
     * @return a user principal that was extracted from the login context.
     */
    private Subject authenticate(HttpServletRequest req) {
        String authorization = req.getHeader("authorization");
        if (authorization == null || "".equals(authorization.trim())) {
            return null;
        }

        // subject from session instead of re-authenticating
        // can't change username/password for this session.
        Subject subject =
                (Subject) req.getSession().getAttribute(authorization);
        if (subject != null) {
            return subject;
        }

        String auth = null;
        try {
            byte[] data = Base64.decode(authorization.substring(6));
            auth = new String(data);
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }

        String username = auth.substring(0, auth.indexOf(':'));
        String password = auth.substring(auth.indexOf(':') + 1);

        if (log.isDebugEnabled()) {
            log.debug("auth username: " + username);
        }

        LoginContext loginContext = null;
        try {
            CallbackHandler handler =
                    new UsernamePasswordCallbackHandler(username, password);
            loginContext = new LoginContext(jaasConfigName, handler);
            loginContext.login();
        } catch (LoginException le) {
            log.error(le.getMessage());
            return null;
        }

        // successfully logged in
        subject = loginContext.getSubject();

        // object accessable by a fixed key for usage
        req.getSession().setAttribute(SESSION_SUBJECT_KEY, subject);

        // object accessable only by base64 encoded username:password that was
        // initially used - prevents some dodgy stuff
        req.getSession().setAttribute(authorization, subject);

        return subject;
    }

    /**
     * Given a subject, obtain the userPrincipal from it. The user principal is
     * defined by a Principal class that can be defined in the web.xml file. If
     * this is undefined, the first principal found is assumed to be the
     * userPrincipal.
     * 
     * @param subject
     *        the subject returned from authentication.
     * @return the userPrincipal associated with the given subject.
     */
    private Principal getUserPrincipal(Subject subject) {
        Principal userPrincipal = null;

        Set<Principal> principals = subject.getPrincipals();

        // try and get userPrincipal based on userClassNames
        if (userClassNames != null && userClassNames.size() > 0) {
            for (Principal p : principals) {
                if (userPrincipal == null
                        && userClassNames.contains(p.getClass().getName())) {
                    userPrincipal = p;
                }
            }
        }

        // no userPrincipal found using userClassNames, just grab first principal
        if (userPrincipal == null) {
            Iterator<Principal> i = principals.iterator();
            // should always have 1 at least and 1st should be user principal
            if (i.hasNext()) {
                userPrincipal = i.next();
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("found userPrincipal ["
                    + userPrincipal.getClass().getName() + "]: "
                    + userPrincipal.getName());
        }

        return userPrincipal;
    }

    /**
     * Obtains the roles for the user based on the class names and attribute
     * names provided in the web.xml file.
     * 
     * @param subject
     *        the subject returned from authentication.
     * @return a set of strings that represent the users roles.
     */
    private Set<String> getUserRoles(Subject subject) {
        Set<String> userRoles = new HashSet<String>();

        // get roles from specified classes
        Set<Principal> principals = subject.getPrincipals();
        if (roleClassNames != null && roleClassNames.size() > 0) {
            for (Principal p : principals) {
                if (roleClassNames.contains(p.getClass().getName())) {
                    userRoles.add(p.getName());
                }
            }
        }

        // get roles from specified attributes
        Map<String, Set<String>> attributes =
                SubjectUtils.getAttributes(subject);
        if (attributes != null) {
            for (String key : attributes.keySet()) {
                if (roleAttributeNames.contains(key)) {
                    userRoles.addAll(attributes.get(key));
                }
            }
        }

        if (log.isDebugEnabled()) {
            for (String r : userRoles) {
                log.debug("found role: " + r);
            }
        }

        return userRoles;
    }

    /**
     * Adds roles to the Subject object.
     * 
     * @param subject
     *        the subject that was returned from authentication.
     * @param userRoles
     *        the set of user roles that were found.
     */
    private void addRolesToSubject(Subject subject, Set<String> userRoles) {
        if (userRoles == null) {
            userRoles = new HashSet<String>();
        }

        Map<String, Set<String>> attributes =
                SubjectUtils.getAttributes(subject);

        Set<String> roles = attributes.get(ROLE_KEY);
        if (roles == null) {
            roles = new HashSet<String>();
            attributes.put(ROLE_KEY, roles);
        }

        for (String role : userRoles) {
            roles.add(role);
            if (log.isDebugEnabled()) {
                log.debug("added role: " + role);
            }
        }
    }

    /**
     * Add roles to where Fedora expects them -
     * FEDORA_AUX_SUBJECT_ATTRIBUTES.fedoraRole.
     * 
     * @param subject
     *        the aubject from authentication.
     * @param userRoles
     *        the set of user roles.
     * @param request
     *        the request in which to place the roles for Fedora.
     */
    private void populateFedoraRoles(Subject subject,
                                     Set<String> userRoles,
                                     HttpServletRequest request) {
        Map<String, Set<String>> attributes =
                SubjectUtils.getAttributes(subject);
        if (attributes == null) {
            attributes = new HashMap<String, Set<String>>();
        }

        // get the fedoraRole attribute or create it.
        Set<String> roles = attributes.get(FEDORA_ROLE_KEY);
        if (roles == null) {
            roles = new HashSet<String>();
            attributes.put(FEDORA_ROLE_KEY, roles);
        }

        roles.addAll(userRoles);

        request.setAttribute(FEDORA_ATTRIBUTES_KEY, attributes);
    }
}
