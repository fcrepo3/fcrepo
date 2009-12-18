/*
 * File: TestXacmlRequest.java
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

package melcoe.xacml.test;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import melcoe.xacml.util.ContextUtil;

import org.apache.log4j.Logger;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.RequestCtx;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class TestXacmlRequest {

    private static final Logger log = Logger.getLogger(TestXacmlRequest.class);

    private static ContextHandler contextHandler = null;

    private static ContextUtil contextUtil = null;

    public static void main(String[] args) throws Exception {
        contextHandler = ContextHandler.getInstance();
        contextUtil = ContextUtil.getInstance();
        StringBuilder request = new StringBuilder();
        if (args.length > 0) {
            File reqFile = new File(args[0]);
            if (log.isDebugEnabled()) {
                log.debug("Using request file: " + reqFile.getAbsolutePath());
            }

            Scanner scanner = new Scanner(new FileInputStream(reqFile));
            while (scanner.hasNextLine()) {
                request.append(scanner.nextLine());
            }

            testRequest(request.toString());
        } else {
            testRequest(null);
        }
    }

    public static void testRequest(String request) throws Exception {
        String reqs = null;

        if (request == null) {
            // RequestCtx req = makeRequest("public", "read",
            // "demo:SmileyBeerGlass");
            RequestCtx req =
                    makeRequest("administrator",
                                "urn:fedora:names:fedora:2.1:action:id-ingestObject",
                                "/coll:accg806/coll:acst305");
            reqs = contextUtil.makeRequestCtx(req);
        } else {
            reqs = request;
        }

        System.out.println(reqs);

        long a = System.nanoTime();
        String response = contextHandler.evaluate(reqs);
        long b = System.nanoTime();

        System.out.println("Total Time (ns): " + (b - a));
        System.out.println(response);

        a = System.nanoTime();
        response = contextHandler.evaluate(reqs);
        b = System.nanoTime();

        System.out.println("Total Time (ns): " + (b - a));
        System.out.println(response);

        a = System.nanoTime();
        response = contextHandler.evaluate(reqs);
        b = System.nanoTime();

        System.out.println("Total Time (ns): " + (b - a));
        System.out.println(response);
    }

    public static List<Map<URI, List<AttributeValue>>> getSubjects(String subject) {
        // setup the id and value for the requesting subject
        List<Map<URI, List<AttributeValue>>> subjects =
                new ArrayList<Map<URI, List<AttributeValue>>>();

        if (subject == null || subject.equals("")) {
            return subjects;
        }

        Map<URI, List<AttributeValue>> subAttr =
                new HashMap<URI, List<AttributeValue>>();
        try {
            List<AttributeValue> attrList = new ArrayList<AttributeValue>();
            attrList.add(new StringAttribute(subject));
            subAttr.put(new URI("urn:fedora:names:fedora:2.1:subject:loginId"),
                        attrList);

            attrList = new ArrayList<AttributeValue>();
            for (int x = 0; x < 10; x++) {
                attrList.add(new StringAttribute("role" + x));
            }
            subAttr.put(new URI("urn:fedora:names:fedora:2.1:subject:role"),
                        attrList);
        } catch (URISyntaxException use) {
            System.out.println(use.getMessage());
        }
        subjects.add(subAttr);

        subAttr = new HashMap<URI, List<AttributeValue>>();
        try {
            List<AttributeValue> attrList = new ArrayList<AttributeValue>();
            attrList.add(new StringAttribute(subject));
            subAttr
                    .put(new URI("urn:oasis:names:tc:xacml:1.0:subject:subject-id"),
                         attrList);
        } catch (URISyntaxException use) {
            System.out.println(use.getMessage());
        }
        subjects.add(subAttr);

        return subjects;
    }

    public static Map<URI, AttributeValue> getEnvironment() {
        return new HashMap<URI, AttributeValue>();
    }

    public static RequestCtx makeRequest(String user, String action, String pid) {
        RequestCtx req = null;

        Map<URI, AttributeValue> resAttr = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> actionAttr =
                new HashMap<URI, AttributeValue>();

        try {
            if (pid != null && !pid.equals("")) {
                resAttr
                        .put(new URI("urn:fedora:names:fedora:2.1:resource:object:pid"),
                             new StringAttribute(pid));
                resAttr
                        .put(new URI("urn:oasis:names:tc:xacml:1.0:resource:resource-id"),
                             new AnyURIAttribute(new URI(pid)));
            }

            if (action != null && !action.equals("")) {
                actionAttr
                        .put(new URI("urn:fedora:names:fedora:2.1:action:id"),
                             new StringAttribute(action));
                actionAttr
                        .put(new URI("urn:oasis:names:tc:xacml:1.0:action:action-id"),
                             new StringAttribute(action));
            }
        } catch (URISyntaxException use) {
            System.out.println(use.getMessage());
        }

        try {
            req =
                    contextUtil.buildRequest(getSubjects(user),
                                             actionAttr,
                                             resAttr,
                                             getEnvironment());
        } catch (Exception e) {
            System.out.print(e.getMessage());
        }

        return req;
    }
}
