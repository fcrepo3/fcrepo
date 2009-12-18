/*
 * File: AuthHttpServletRequestWrapper.java
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

package org.fcrepo.server.jaas.auth;

import java.security.Principal;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * This class wraps a standard HttpServletRequest and adds the ability to set
 * roles and principals.
 * 
 * @author nish.naidoo@gmail.com
 */
public class AuthHttpServletRequestWrapper
        extends HttpServletRequestWrapper {

    private Principal userPrincipal = null;

    private Set<String> userRoles = null;

    public AuthHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    /**
     * Sets the userPrincipal attribute.
     * 
     * @param userPrincipal
     *        the userPrincipal to set.
     */
    public void setUserPrincipal(Principal userPrincipal) {
        this.userPrincipal = userPrincipal;
    }

    /**
     * Sets the userRoles attribute.
     * 
     * @param userRoles
     *        the userRoles to set.
     */
    public void setUserRoles(Set<String> userRoles) {
        this.userRoles = userRoles;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequestWrapper#getUserPrincipal()
     */
    @Override
    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequestWrapper#getRemoteUser()
     */
    @Override
    public String getRemoteUser() {
        if (userPrincipal == null) {
            return null;
        }

        return userPrincipal.getName();
    }

    /*
     * (non-Javadoc)
     * @see
     * javax.servlet.http.HttpServletRequestWrapper#isUserInRole(java.lang.String
     * )
     */
    @Override
    public boolean isUserInRole(String role) {
        if (userRoles == null) {
            return false;
        }

        return userRoles.contains(role);
    }
}
