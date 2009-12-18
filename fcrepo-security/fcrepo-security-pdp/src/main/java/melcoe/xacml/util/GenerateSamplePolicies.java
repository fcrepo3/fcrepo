/*
 * File: GenerateSamplePolicies.java
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

package melcoe.xacml.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class GenerateSamplePolicies {

    private static final String POLICY_HOME = "C:/Code/policies";

    private static final String XACML20_CONTEXT_NS =
            "urn:oasis:names:tc:xacml:2.0:context:schema:os";

    private static final String XACML20_POLICY_NS =
            "urn:oasis:names:tc:xacml:2.0:policy:schema:os";

    private static final String XACML20_CONTEXT_LOC =
            "http://docs.oasis-open.org/xacml/2.0/access_control-xacml-2.0-context-schema-os.xsd";

    private static final String XACML20_POLICY_LOC =
            "http://docs.oasis-open.org/xacml/2.0/access_control-xacml-2.0-policy-schema-os.xsd";

    private static final String XSI_NS =
            "http://www.w3.org/2001/XMLSchema-instance";

    private static final String RULE_COMB_ALG_ID =
            "urn:oasis:names:tc:xacml:1.0:rule-combining-algorithm:permit-overrides";

    private static final String POLICY_ID_PREFIX =
            "au:edu:mq:melcoe:ramp:fedora:xacml:2.0:policy:";

    private static Validator validator = null;

    private static List<String> nameList = null;

    private static List<String> resourceList = null;

    private static Map<String, List<String>> actionList = null;

    public static void main(String[] args) throws Exception {
        nameList = loadStrings(POLICY_HOME + "/names.txt");
        resourceList = loadStrings(POLICY_HOME + "/resources.txt");
        actionList = loadActions(POLICY_HOME + "/actions.txt");

        SchemaFactory schemaFactory =
                SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        Schema schema = schemaFactory.newSchema(new URL(XACML20_POLICY_LOC));
        validator = schema.newValidator();

        System.out.println("Starting clock!");
        long time1 = System.currentTimeMillis();
        for (int x = 20; x <= 10000; x++) {
            int level = 0;
            if (x > 5500) {
                level = 1;
            }
            if (x > 8000) {
                level = 2;
            }
            if (x > 9000) {
                level = 3;
            }
            if (x > 9750) {
                level = 4;
            }

            String filename =
                    POLICY_HOME + "/policies/policy-"
                            + prePad(5, String.valueOf(x), '0') + ".xml";
            byte[] bytes = generatePolicy(level, x);
            File f = new File(filename);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bytes);
            fos.close();
            System.out.println("Created policy: " + filename);
        }
        long time2 = System.currentTimeMillis();
        System.out.println("Stopping clock!");
        System.out.println("Time taken: " + (time2 - time1));
    }

    public static List<String> loadStrings(String filename) {
        List<String> strings = new ArrayList<String>();
        File file = new File(filename);
        Scanner scanner = null;

        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException fe) {
            System.err
                    .println("Could not location the file: " + file.getName());
            return null;
        }

        while (scanner.hasNextLine()) {
            String item = scanner.nextLine().trim();
            if (!strings.contains(item)) {
                strings.add(item);
            }
        }

        System.out.println("Loaded " + strings.size() + " values.");

        return strings;
    }

    public static Map<String, List<String>> loadActions(String filename) {
        Map<String, List<String>> actions = new HashMap<String, List<String>>();
        File file = new File(filename);
        Scanner scanner = null;

        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException fe) {
            System.err
                    .println("Could not location the file: " + file.getName());
            return null;
        }

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            String[] result = line.split("--");
            if (result.length == 2) {
                List<String> action = actions.get(result[0]);
                if (action == null) {
                    action = new ArrayList<String>();
                    actions.put(result[0], action);
                }

                if (!action.contains(result[1])) {
                    action.add(result[1]);
                }
            }
        }

        System.out.println("Loaded action values.");

        return actions;
    }

    public static byte[] generatePolicy(int level, int id) throws Exception {
        // Create instance of DocumentBuilderFactory
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // Get the DocumentBuilder
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        // Create blank DOM Document
        Document doc = docBuilder.newDocument();

        Element root = doc.createElement("Policy");
        doc.appendChild(root);
        root.setAttribute("xmlns", XACML20_POLICY_NS);
        root.setAttribute("PolicyId", POLICY_ID_PREFIX + String.valueOf(id));
        root.setAttribute("RuleCombiningAlgId", RULE_COMB_ALG_ID);
        root.setAttribute("xmlns:xacl-context", XACML20_CONTEXT_NS);
        root.setAttribute("xmlns:xsi", XSI_NS);
        root.setAttributeNS(XSI_NS, "xsi:schemaLocation", XACML20_POLICY_NS
                + " " + XACML20_POLICY_LOC + " " + XACML20_CONTEXT_NS + " "
                + XACML20_CONTEXT_LOC);

        Element desc = doc.createElement("Description");
        root.appendChild(desc);
        desc
                .appendChild(doc
                        .createTextNode("This is one of many sample policies that has been auto-generated."));

        Element target = doc.createElement("Target");
        root.appendChild(target);
        target.appendChild(generateSubjects(doc));
        target.appendChild(generateResources(doc));
        target.appendChild(generateActions(level, doc));

        Element rule = doc.createElement("Rule");
        root.appendChild(rule);
        rule.setAttribute("Effect", "Permit");
        rule
                .setAttribute("RuleId",
                              "au:edu:mq:melcoe:ramp:fedora:xacml:2.0:rule:generic-permit");

        TransformerFactory xFactory = TransformerFactory.newInstance();
        xFactory.setAttribute("indent-number", new Integer(2));
        Transformer aTransformer = xFactory.newTransformer();
        aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
        aTransformer
                .setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
                                   "4");

        Source src = new DOMSource(doc);

        validator.validate(src);

        ByteArrayOutputStream bDoc = new ByteArrayOutputStream();

        Result dest = new StreamResult(new OutputStreamWriter(bDoc));
        aTransformer.transform(src, dest);

        return bDoc.toByteArray();
    }

    public static Element generateSubjects(Document doc) {
        int max = (int) Math.round(Math.random() * 10);
        if (max == 0) {
            max = 1;
        }
        List<String> mySubjects =
                getStrings(max, GenerateSamplePolicies.nameList);

        Element subjects = doc.createElement("Subjects");

        for (int x = 0; x < max; x++) {
            Element subject = doc.createElement("Subject");

            Element subjectMatch = doc.createElement("SubjectMatch");
            subjectMatch
                    .setAttribute("MatchId",
                                  "urn:oasis:names:tc:xacml:1.0:function:string-equal");
            subject.appendChild(subjectMatch);

            Element attributeValue = doc.createElement("AttributeValue");
            attributeValue
                    .setAttribute("DataType",
                                  "http://www.w3.org/2001/XMLSchema#string");
            attributeValue.appendChild(doc.createTextNode(mySubjects.get(x)));
            subjectMatch.appendChild(attributeValue);

            Element subjectAttributeDesignator =
                    doc.createElement("SubjectAttributeDesignator");
            subjectAttributeDesignator
                    .setAttribute("AttributeId",
                                  "urn:oasis:names:tc:xacml:1.0:subject:subject-id");
            subjectAttributeDesignator
                    .setAttribute("DataType",
                                  "http://www.w3.org/2001/XMLSchema#string");
            subjectMatch.appendChild(subjectAttributeDesignator);

            subjects.appendChild(subject);
        }

        return subjects;
    }

    public static Element generateResources(Document doc) {
        int max = (int) Math.round(Math.random() * 10);
        if (max == 0) {
            max = 1;
        }
        List<String> myResources =
                getStrings(max, GenerateSamplePolicies.resourceList);

        Element resources = doc.createElement("Resources");

        for (int x = 0; x < max; x++) {
            Element resource = doc.createElement("Resource");

            Element resourceMatch = doc.createElement("ResourceMatch");
            resourceMatch
                    .setAttribute("MatchId",
                                  "urn:oasis:names:tc:xacml:1.0:function:anyURI-equal");
            resource.appendChild(resourceMatch);

            Element attributeValue = doc.createElement("AttributeValue");
            attributeValue
                    .setAttribute("DataType",
                                  "http://www.w3.org/2001/XMLSchema#anyURI");
            attributeValue.appendChild(doc.createTextNode(myResources.get(x)));
            resourceMatch.appendChild(attributeValue);

            Element resourceAttributeDesignator =
                    doc.createElement("ResourceAttributeDesignator");
            resourceAttributeDesignator
                    .setAttribute("AttributeId",
                                  "urn:oasis:names:tc:xacml:1.0:resource:resource-id");
            resourceAttributeDesignator
                    .setAttribute("DataType",
                                  "http://www.w3.org/2001/XMLSchema#anyURI");
            resourceMatch.appendChild(resourceAttributeDesignator);

            resources.appendChild(resource);
        }

        return resources;
    }

    public static Element generateActions(int level, Document doc) {
        Element actionElement = doc.createElement("Actions");

        // Each gets the previous in order: 0 read, 1 update, 2 create, 3
        // delete, 4 admin
        String[] myActions =
                new String[] {"read", "update", "create", "delete", "admin"};

        for (int x = 0; x <= level; x++) {
            List<String> actions =
                    GenerateSamplePolicies.actionList.get(myActions[x]);
            for (String a : actions) {
                Element action = doc.createElement("Action");

                Element actionMatch = doc.createElement("ActionMatch");
                actionMatch
                        .setAttribute("MatchId",
                                      "urn:oasis:names:tc:xacml:1.0:function:string-equal");
                action.appendChild(actionMatch);

                Element attributeValue = doc.createElement("AttributeValue");
                attributeValue
                        .setAttribute("DataType",
                                      "http://www.w3.org/2001/XMLSchema#string");
                attributeValue.appendChild(doc.createTextNode(a));
                actionMatch.appendChild(attributeValue);

                Element actionAttributeDesignator =
                        doc.createElement("ActionAttributeDesignator");
                actionAttributeDesignator
                        .setAttribute("AttributeId",
                                      "urn:fedora:names:fedora:2.1:action:id");
                actionAttributeDesignator
                        .setAttribute("DataType",
                                      "http://www.w3.org/2001/XMLSchema#string");
                actionMatch.appendChild(actionAttributeDesignator);

                actionElement.appendChild(action);
            }
        }

        return actionElement;
    }

    public static List<String> getStrings(int max, List<String> strings) {
        List<String> myStrings = new ArrayList<String>();
        int x = 0;

        while (x < max) {
            int r = (int) Math.round(Math.random() * (strings.size() - 1));
            if (!myStrings.contains(strings.get(r))) {
                myStrings.add(strings.get(r));
                x++;
            }
        }

        return myStrings;
    }

    private static String prePad(int length, String str, char c) {
        StringBuffer sb = new StringBuffer();
        for (int x = 0; x < length - str.length(); x++) {
            sb.append(c);
        }

        return new String(sb.toString() + str);
    }
}
