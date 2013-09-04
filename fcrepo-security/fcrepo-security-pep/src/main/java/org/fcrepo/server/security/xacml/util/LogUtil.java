/*
 * File: LogUtil.java
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

package org.fcrepo.server.security.xacml.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtil {

    private static final Logger statlogger = LoggerFactory.getLogger(LogUtil.class);

    private static SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS");

    public static void statLog(String username,
                               String action,
                               String resourceId,
                               String dsID) {
        String tmp = null;
        StringBuilder sb = new StringBuilder();
        char separator = '\t';

        tmp = dateFormat.format(new Date());
        sb.append(tmp).append(separator);

        tmp = "".equals(username) ? "anonymous" : username;
        sb.append(tmp).append(separator);

        tmp = action == null || action.isEmpty() ? "" : action;
        sb.append(tmp).append(separator);

        tmp = resourceId == null || resourceId.isEmpty() ? "" : resourceId;
        sb.append(tmp).append(separator);

        tmp = dsID == null || dsID.isEmpty() ? "" : dsID;
        sb.append(tmp);

        statlogger.info(sb.toString());
    }
}
