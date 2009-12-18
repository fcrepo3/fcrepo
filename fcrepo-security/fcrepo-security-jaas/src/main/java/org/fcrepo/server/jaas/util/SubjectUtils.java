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

package org.fcrepo.server.jaas.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.log4j.Logger;

public class SubjectUtils {

    private static final Logger log = Logger.getLogger(SubjectUtils.class);

    private static final String CLASS_OBJECT = "java.util.HashMap";

    private static final String KEY_OBJECT = "java.lang.String";

    private static final String VALUE_OBJECT = "java.util.HashSet";

    @SuppressWarnings("unchecked")
    public static Map<String, Set<String>> getAttributes(Subject subject) {
        Map<String, Set<String>> attributes = null;

        if (subject.getPublicCredentials() == null) {
            return new HashMap<String, Set<String>>();
        }

        Iterator<?> i = subject.getPublicCredentials().iterator();
        while (attributes == null && i.hasNext()) {
            Map<String, Set<String>> tmp = null;
            Object o = i.next();

            if (log.isDebugEnabled()) {
                log.debug("checking for attributes (class name): "
                        + o.getClass().getName());
            }

            if (!o.getClass().getName().equals(CLASS_OBJECT)) {
                continue;
            }

            tmp = (Map) o;
            Object tObject = null;
            Iterator<?> t = null;

            t = tmp.keySet().iterator();
            if (!t.hasNext()) {
                continue;
            }

            tObject = t.next();

            if (log.isDebugEnabled()) {
                log.debug("checking for attributes (key object name): "
                        + tObject.getClass().getName());
            }

            if (!tObject.getClass().getName().equals(KEY_OBJECT)) {
                continue;
            }

            t = tmp.values().iterator();
            if (!t.hasNext()) {
                continue;
            }

            tObject = t.next();

            if (log.isDebugEnabled()) {
                log.debug("checking for attributes (value object name): "
                        + tObject.getClass().getName());
            }

            if (!tObject.getClass().getName().equals(VALUE_OBJECT)) {
                continue;
            }

            attributes = (Map) o;
        }

        if (attributes == null) {
            return new HashMap<String, Set<String>>();
        }

        return attributes;
    }
}
