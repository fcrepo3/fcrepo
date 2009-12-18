/*
 * File: DemoLoginModule.java
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

public class DemoLoginModule
        implements LoginModule {

    private static final Logger log = Logger.getLogger(DemoLoginModule.class);

    private Subject subject = null;

    private CallbackHandler handler = null;

    private Map<String, ?> sharedState = null;

    private Map<String, ?> options = null;

    private String username = null;

    private Map<String, Set<String>> attributes = null;

    private boolean debug = false;

    private boolean successLogin = false;

    public void initialize(Subject subject,
                           CallbackHandler handler,
                           Map<String, ?> sharedState,
                           Map<String, ?> options) {
        this.subject = subject;
        this.handler = handler;
        this.sharedState = sharedState;
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
            log.debug("DemoLoginModule login called.");
            for (String key : sharedState.keySet()) {
                String value = sharedState.get(key).toString();
                log.debug(key + ": " + value);
            }
        }

        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("username");
        callbacks[1] = new PasswordCallback("password", false);

        try {
            handler.handle(callbacks);
            username = ((NameCallback) callbacks[0]).getName();
            char[] passwordCharArray =
                    ((PasswordCallback) callbacks[1]).getPassword();
            String password = new String(passwordCharArray);

            successLogin = username.equals(password);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new LoginException("IOException occured: " + ioe.getMessage());
        } catch (UnsupportedCallbackException ucbe) {
            ucbe.printStackTrace();
            throw new LoginException("UnsupportedCallbackException encountered: "
                    + ucbe.getMessage());
        }

        return successLogin;
    }

    public boolean commit() throws LoginException {
        if (!successLogin) {
            return false;
        }

        try {
            UserPrincipal p = new UserPrincipal(username);
            Set<String> roles = attributes.get("role");
            if (roles == null) {
                roles = new HashSet<String>();
                attributes.put("role", roles);
            }

            roles.add("test1");
            roles.add("test2");
            roles.add("test3");

            subject.getPrincipals().add(p);
            subject.getPublicCredentials().add(attributes);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    public boolean abort() throws LoginException {
        try {
            subject.getPrincipals().clear();
            subject.getPublicCredentials().clear();
            subject.getPrivateCredentials().clear();
            username = null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    public boolean logout() throws LoginException {
        try {
            subject.getPrincipals().clear();
            subject.getPublicCredentials().clear();
            subject.getPrivateCredentials().clear();
            username = null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }

        return true;
    }
}
