/*
 * File: LdapModule.java
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

package org.fcrepo.server.jaas.auth.module;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.log4j.Logger;
import org.fcrepo.server.jaas.auth.UserPrincipal;
import org.fcrepo.server.jaas.util.Base64;

public class LdapModule
        implements LoginModule {

    private static final Logger log = Logger.getLogger(LdapModule.class);

    private Subject subject = null;

    private CallbackHandler handler = null;

    // private Map<String, ?> sharedState = null;
    private Map<String, ?> options = null;

    private String username = null;

    private UserPrincipal principal = null;

    private Map<String, Set<String>> attributes = null;

    private boolean debug = false;

    private boolean successLogin = false;

    public void initialize(Subject subject,
                           CallbackHandler handler,
                           Map<String, ?> sharedState,
                           Map<String, ?> options) {
        this.subject = subject;
        this.handler = handler;
        // this.sharedState = sharedState;
        this.options = options;

        String debugOption = (String) this.options.get("debug");
        if (debugOption != null && "true".equalsIgnoreCase(debugOption)) {
            debug = true;
        }

        attributes = new HashMap<String, Set<String>>();

        if (debug) {
            log.debug("login module initialised: " + this.getClass().getName());
        }
    }

    public boolean login() throws LoginException {
        if (debug) {
            log.debug(this.getClass().getName() + " login called.");
        }

        // The only 2 callback types that are supported.
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("username");
        callbacks[1] = new PasswordCallback("password", false);

        String password = null;
        try {
            // sets the username and password from the callback handler
            handler.handle(callbacks);
            username = ((NameCallback) callbacks[0]).getName();
            char[] passwordCharArray =
                    ((PasswordCallback) callbacks[1]).getPassword();
            password = new String(passwordCharArray);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new LoginException("IOException occured: " + ioe.getMessage());
        } catch (UnsupportedCallbackException ucbe) {
            ucbe.printStackTrace();
            throw new LoginException("UnsupportedCallbackException encountered: "
                    + ucbe.getMessage());
        }

        successLogin = authenticate(username, password);

        return successLogin;
    }

    public boolean commit() throws LoginException {
        if (!successLogin) {
            return false;
        }

        try {
            subject.getPrincipals().add(principal);
            subject.getPublicCredentials().add(attributes);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    public boolean abort() throws LoginException {
        try {
            clear();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    public boolean logout() throws LoginException {
        try {
            clear();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    private void clear() {
        subject.getPrincipals().clear();
        subject.getPublicCredentials().clear();
        subject.getPrivateCredentials().clear();
        principal = null;
        username = null;
    }

    private boolean authenticate(String username, String password) {
        try {
            // required attributes
            String hostUrl = getOption("host.url", true);
            String authType = getOption("auth.type", true);
            String bindMode = getOption("bind.mode", true);

            // retrieve the attributes to fetch from ldap
            String[] attrList = null;
            String attrsFetch = getOption("attrs.fetch", false);
            if (attrsFetch != null && !"".equals(attrsFetch)) {
                attrList = attrsFetch.split(" *, *");
            } else if (attrList == null || attrList.length == 0) {
                attrList = new String[] {"cn", "sn", "mail", "displayName"};
            }

            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put(Context.INITIAL_CONTEXT_FACTORY,
                    "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.SECURITY_AUTHENTICATION, authType);
            env.put(Context.PROVIDER_URL, hostUrl);

            if ("bind".equals(bindMode)) {
                if (debug) {
                    log.debug("authenticating with mode: " + bindMode);
                }

                return bind(username, password, env, attrList);
            } else if ("bind-search-compare".equals(bindMode)) {
                if (debug) {
                    log.debug("authenticating with mode: " + bindMode);
                }

                return bindSearchX(username, password, env, attrList, false);
            } else if ("bind-search-bind".equals(bindMode)) {
                if (debug) {
                    log.debug("authenticating with mode: " + bindMode);
                }

                return bindSearchX(username, password, env, attrList, true);
            }
        } catch (NamingException ne) {
            log.error(ne.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return false;
    }

    private boolean bind(String username,
                         String password,
                         Hashtable<String, String> env,
                         String[] attrList) throws Exception {
        String bindFilter = getOption("bind.filter", true);
        String dn = MessageFormat.format(bindFilter, username);
        if (debug) {
            log.debug("authenticating user: " + dn);
        }

        env.put(Context.SECURITY_PRINCIPAL, dn);
        env.put(Context.SECURITY_CREDENTIALS, password);

        DirContext ctx = new InitialDirContext(env);
        // we've successfully bound at this point. Auth is good.
        // we instantiate the principal.
        Attributes attributes = ctx.getAttributes(dn, attrList);

        makePrincipal(username, attributes);

        return true;
    }

    private boolean bindSearchX(String username,
                                String password,
                                Hashtable<String, String> env,
                                String[] attrList,
                                boolean bind) throws Exception {
        String bindUser = getOption("bind.user", true);
        String bindPass = getOption("bind.pass", true);
        String searchBase = getOption("search.base", true);
        String searchFilter = getOption("search.filter", true);

        env.put(Context.SECURITY_PRINCIPAL, bindUser);
        env.put(Context.SECURITY_CREDENTIALS, bindPass);

        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(env);
        } catch (NamingException ne) {
            log.error("Failed to bind as bindUser: " + bindUser);
            throw ne;
        }

        // ensure we have the userPassword attribute at a minimum
        String[] attributeList = null;
        if (attrList == null) {
            attributeList = new String[] {"userPassword"};
        } else if (!Arrays.asList(attrList).contains("userPassword")) {
            attributeList = new String[attrList.length + 1];
            for (int x = 0; x < attrList.length; x++) {
                attributeList[x] = attrList[x];
            }
            attributeList[attrList.length] = "userPassword";
        }

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setReturningAttributes(attributeList);
        sc.setDerefLinkFlag(true);
        sc.setReturningObjFlag(false);
        sc.setTimeLimit(5000);

        String filter = MessageFormat.format(searchFilter, username);
        NamingEnumeration<SearchResult> results =
                ctx.search(searchBase, filter, sc);
        if (!results.hasMore()) {
            log.warn("no valid user found.");
            return false;
        }

        SearchResult result = results.next();
        if (debug) {
            log.debug("authenticating user: " + result.getNameInNamespace());
        }

        if (bind) {
            // setup user context for binding
            Hashtable<String, String> userEnv = new Hashtable<String, String>();
            userEnv.put(Context.INITIAL_CONTEXT_FACTORY,
                        "com.sun.jndi.ldap.LdapCtxFactory");
            userEnv.put(Context.SECURITY_AUTHENTICATION, getOption("auth.type",
                                                                   true));
            userEnv.put(Context.PROVIDER_URL, getOption("host.url", true));
            userEnv
                    .put(Context.SECURITY_PRINCIPAL, result
                            .getNameInNamespace());
            userEnv.put(Context.SECURITY_CREDENTIALS, password);

            try {
                new InitialDirContext(userEnv);
            } catch (NamingException ne) {
                log.error("failed to authenticate user: "
                        + result.getNameInNamespace());
                throw ne;
            }
        } else {
            // get userPassword attribute
            Attribute up = result.getAttributes().get("userPassword");
            if (up == null) {
                log.error("unable to read userPassword attribute for: "
                        + result.getNameInNamespace());
                return false;
            }

            byte[] userPasswordBytes = (byte[]) up.get();
            String userPassword = new String(userPasswordBytes);

            // compare passwords - also handles encodings
            if (!passwordsMatch(password, userPassword)) {
                return false;
            }
        }

        Attributes attributes = result.getAttributes();
        makePrincipal(username, attributes);

        return true;
    }

    private void makePrincipal(String username, Attributes ldapAttributes)
            throws NamingException {
        principal = new UserPrincipal(username);
        NamingEnumeration<? extends Attribute> attributeList =
                ldapAttributes.getAll();
        while (attributeList.hasMore()) {
            Attribute attribute = attributeList.next();
            NamingEnumeration<?> values = attribute.getAll();
            while (values.hasMore()) {
                Object value = values.next();
                if (value instanceof String) {
                    Set<String> aValues = attributes.get(attribute.getID());
                    if (aValues == null) {
                        aValues = new HashSet<String>();
                        attributes.put(attribute.getID(), aValues);
                    }
                    aValues.add((String) value);

                    if (debug) {
                        log.debug("added to principal: " + attribute.getID()
                                + "/" + value);
                    }
                }
            }
        }
    }

    /**
     * Method to compare two passwords. The method attempts to encode the user
     * password based on the ldap password encoding extracted from the storage
     * format (e.g. {SHA}g0bbl3d3g00ka12@#19/=).
     * 
     * @param userPassword
     *        the password that the user entered
     * @param ldapPassword
     *        the password from the ldap directory
     * @return true if userPassword equals ldapPassword with respect to encoding
     */
    private static boolean passwordsMatch(String userPassword,
                                          String ldapPassword) {
        final String LDAP_PASSWORD_REGEX = "\\{(.+)\\}(.+)";
        Pattern p = Pattern.compile(LDAP_PASSWORD_REGEX);
        Matcher m = p.matcher(ldapPassword);

        boolean match = false;
        if (m.find() && m.groupCount() == 2) {
            // if password is encoded in the LDAP, encode the password before
            // compare
            String encoding = m.group(1);
            String password = m.group(2);
            if (log.isDebugEnabled()) {
                log.debug("Encoding: " + encoding + ", Password: " + password);
            }

            MessageDigest digest = null;
            try {
                digest = MessageDigest.getInstance(encoding.toUpperCase());
            } catch (NoSuchAlgorithmException e) {
                log.error("Unsupported Algorithm used: " + encoding);
                log.error(e.getMessage());
                return false;
            }

            byte[] resultBytes = digest.digest(userPassword.getBytes());
            byte[] result = Base64.encodeBytesToBytes(resultBytes);

            String pwd = new String(password);
            String ldp = new String(result);
            match = pwd.equals(ldp);
        } else {
            // if passwords are not encoded, just do raw compare
            match = userPassword.equals(ldapPassword);
        }

        return match;
    }

    private String getOption(String key, boolean required) throws Exception {
        String value = (String) options.get(key);
        if (required && (value == null || "".equals(value))) {
            throw new Exception("Missing required option in JAAS Config file: "
                    + key);
        }

        return value;
    }
}
