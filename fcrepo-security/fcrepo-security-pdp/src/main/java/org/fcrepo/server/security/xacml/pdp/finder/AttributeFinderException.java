/*
 * File: AttributeFinderException.java
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

package org.fcrepo.server.security.xacml.pdp.finder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class AttributeFinderException
        extends Exception {

    private static final long serialVersionUID = 1L;

    private static final Logger logger =
            LoggerFactory.getLogger(AttributeFinderException.class);

    public AttributeFinderException() {
        super();
        logger.error("No message provided");
    }

    public AttributeFinderException(String msg) {
        super(msg);
        logger.error(msg);
    }

    public AttributeFinderException(String msg, Throwable t) {
        super(msg);
        logger.error(msg, t);
    }
}
