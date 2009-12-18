/*
 * File: UsernamePasswordCallbackHandler.java
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

package org.fcrepo.server.jaas.auth.handler;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * A basic callback handler that supports name and password callbacks.
 * 
 * @author nish.naidoo@gmail.com
 */
public class UsernamePasswordCallbackHandler
        implements CallbackHandler {

    private final String username;

    private final String password;

    public UsernamePasswordCallbackHandler(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /*
     * (non-Javadoc)
     * @see
     * javax.security.auth.callback.CallbackHandler#handle(javax.security.auth
     * .callback.Callback[])
     */
    public void handle(Callback[] callbacks) throws IOException,
            UnsupportedCallbackException {
        for (Callback c : callbacks) {
            if (c instanceof NameCallback) {
                ((NameCallback) c).setName(username);
            } else if (c instanceof PasswordCallback) {
                ((PasswordCallback) c).setPassword(password.toCharArray());
            } else {
                throw new UnsupportedCallbackException(c);
            }
        }
    }
}
