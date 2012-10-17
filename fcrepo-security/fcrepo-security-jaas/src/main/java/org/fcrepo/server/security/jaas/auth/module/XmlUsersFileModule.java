/*
 * File: XmlUsersFileModule.java
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

package org.fcrepo.server.security.jaas.auth.module;

import java.io.File;
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

import org.fcrepo.common.Constants;
import org.fcrepo.server.security.jaas.auth.UserPrincipal;
import org.fcrepo.server.security.jaas.util.DataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XmlUsersFileModule
        implements LoginModule {

    private static final Logger logger =
            LoggerFactory.getLogger(XmlUsersFileModule.class);

    private static final File userFile = getUsersFile();

    // must be marked volatile for double-checked locking in caching method to work
    private static volatile long userFileParsed = Long.MIN_VALUE;

    private static volatile long userFileBytesLoaded = 0L;

    private static volatile Document userDoc = null;

    private Subject subject = null;

    private CallbackHandler handler = null;

    // private Map<String, ?> sharedState = null;
    private Map<String, ?> options = null;

    private String username = null;

    private UserPrincipal principal = null;

    private Map<String, Set<String>> attributes = null;

    private boolean debug = false;

    private boolean successLogin = false;

    @Override
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
            logger.debug("login module initialised: " + this.getClass().getName());
        }
    }

    @Override
    public boolean login() throws LoginException {
        if (debug) {
            logger.debug(this.getClass().getName() + " login called.");
        }

        if (Constants.FEDORA_HOME == null || "".equals(Constants.FEDORA_HOME.trim())) {
            logger.error("FEDORA_HOME constant is not set");
            return false;
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

    @Override
    public boolean commit() throws LoginException {
        if (!successLogin) {
            return false;
        }

        try {
            subject.getPrincipals().add(principal);
            subject.getPublicCredentials().add(attributes);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        try {
            clear();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        try {
            clear();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
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
        if (!userFile.exists()) {
            logger.error("XmlUsersFile not found: {}", userFile.getAbsolutePath());
            return false;
        }

        try {
            Document userDoc = getUserDocument();
            // go through each user
            NodeList userList = userDoc.getElementsByTagName("user");
            for (int x = 0; x < userList.getLength(); x++) {
                Element user = (Element) userList.item(x);
                String a_username = user.getAttribute("name");
                String a_password = user.getAttribute("password");
                if (!a_username.equals(username)
                        || !a_password.equals(password)) {
                    continue;
                }

                principal = new UserPrincipal(username);

                // for a matched user, go through each attribute
                NodeList attributeList = user.getElementsByTagName("attribute");
                for (int y = 0; y < attributeList.getLength(); y++) {
                    Element attribute = (Element) attributeList.item(y);
                    String name = attribute.getAttribute("name");

                    // go through each value
                    NodeList valueList =
                            attribute.getElementsByTagName("value");
                    for (int z = 0; z < valueList.getLength(); z++) {
                        Element value = (Element) valueList.item(z);
                        String v = value.getFirstChild().getNodeValue();

                        Set<String> values = attributes.get(name);
                        if (values == null) {
                            values = new HashSet<String>();
                            attributes.put(name, values);
                        }
                        values.add(v);
                    }
                }

                return true;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return false;
    }

    private static File getUsersFile() {
        if (Constants.FEDORA_HOME == null || "".equals(Constants.FEDORA_HOME)) {
            logger.error("FEDORA_HOME constant is not set");
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("using FEDORA_HOME: " + Constants.FEDORA_HOME);
            }
        }

        return new File(Constants.FEDORA_HOME + "/server/config/fedora-users.xml");
    }

    /**
     * Quick and dirty caching.
     * @return DOM of a parsed user file
     * @throws Exception
     */
    private static Document getUserDocument() throws Exception {
        if (userDoc == null || userFileParsed != userFile.lastModified() || userFileBytesLoaded != userFile.length()) {
            synchronized(XmlUsersFileModule.class) {
                if (userDoc == null || userFileParsed != userFile.lastModified() || userFileBytesLoaded != userFile.length()) {
                    userDoc = DataUtils.getDocumentFromFile(userFile);
                    userFileParsed = userFile.lastModified();
                    userFileBytesLoaded = userFile.length();
                }
            }
        }
        return userDoc;
    }
}