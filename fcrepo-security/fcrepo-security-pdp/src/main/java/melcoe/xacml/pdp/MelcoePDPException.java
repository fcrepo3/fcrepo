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

package melcoe.xacml.pdp;

import melcoe.xacml.MelcoeXacmlException;

import org.apache.log4j.Logger;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class MelcoePDPException
        extends MelcoeXacmlException {

    private static final long serialVersionUID = 1L;

    private static final Logger log =
            Logger.getLogger(MelcoePDPException.class.getName());

    public MelcoePDPException() {
        super();
        log.error("No message provided");
    }

    public MelcoePDPException(String msg) {
        super(msg);
        log.error(msg);
    }

    public MelcoePDPException(String msg, Throwable t) {
        super(msg);
        log.error(msg, t);
    }
}
