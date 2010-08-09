/*
 * File: MelcoePDPException.java
 *
 * Copyright 2007 Macquarie E-Learning Centre Of Excellence
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.server.security.xacml.pdp;


import org.fcrepo.server.security.xacml.MelcoeXacmlException;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class MelcoePDPException
        extends MelcoeXacmlException {

    private static final long serialVersionUID = 1L;


    public MelcoePDPException() {
        super();
    }

    public MelcoePDPException(String msg) {
        super(msg);
    }

    public MelcoePDPException(String msg, Throwable t) {
        super(msg);
    }
}
