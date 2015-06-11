/*
 * File: SubjectUtils.java
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

package org.fcrepo.server.security.jaas.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubjectUtils {

    private static final Logger logger =
            LoggerFactory.getLogger(SubjectUtils.class);

    /**
     * Get the attribute map of String keys to Set&lt;String&gt; values
     * This method will not return a null
     * @param subject
     * @return Map&lt;String, Set&lt;String&gt;&gt;
     */
    
    @SuppressWarnings("unchecked")
    public static Map<String, Set<String>> getAttributes(Subject subject) {
        Map<String, Set<String>> attributes = null;

        if (subject.getPublicCredentials() == null) {
            return new HashMap<String, Set<String>>();
        }

        @SuppressWarnings("rawtypes")
        Iterator<HashMap> credentialObjects = subject.getPublicCredentials(HashMap.class).iterator();
        while (attributes == null && credentialObjects.hasNext()) {
            HashMap<?,?> credentialObject = credentialObjects.next();

            if (logger.isDebugEnabled()) {
                logger.debug("checking for attributes (class name): "
                        + credentialObject.getClass().getName());
            }

            Object key = null;
            Iterator<?> keys = null;

            keys = credentialObject.keySet().iterator();
            if (!keys.hasNext()) {
                continue;
            }

            key = keys.next();

            if (logger.isDebugEnabled()) {
                logger.debug("checking for attributes (key object name): "
                        + key.getClass().getName());
            }

            if (!(key instanceof String)) {
                continue;
            }

            keys = credentialObject.values().iterator();
            if (!keys.hasNext()) {
                continue;
            }

            key = keys.next();

            if (logger.isDebugEnabled()) {
                logger.debug("checking for attributes (value object name): "
                        + key.getClass().getName());
            }

            if (!(key instanceof HashSet)) {
                continue;
            }

            attributes = (Map<String, Set<String>>) credentialObject;
        }

        if (attributes == null) {
            attributes = new HashMap<String, Set<String>>();
            subject.getPublicCredentials().add(attributes);
        }

        return attributes;
    }
}
