/*
 * File: ContextUtil.java
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import java.lang.reflect.Constructor;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.sun.xacml.Indenter;
import com.sun.xacml.ParsingException;
import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Subject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.fcrepo.common.Constants;
import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Utility class that provides various methods for creating/converting contexts.
 * This class can convert requests and responses from their string
 * representations to their object representations and vice versa as well as a
 * few utility methods for getting information from the contexts. It also
 * contains methods for constructing requests.
 *
 * @author nishen@melcoe.mq.edu.au
 */

public class ContextUtilFactory {

    private static final Logger logger =
            LoggerFactory.getLogger(ContextUtilFactory.class);

    private static final URI XACML_RESOURCE_ID =
            URI.create("urn:oasis:names:tc:xacml:1.0:resource:resource-id");

    private static final Map<URI, URI> actionMap =
            new ConcurrentHashMap<URI, URI>();

    private static final Map<String, String> actionValueMap =
            new ConcurrentHashMap<String, String>();

    private final RelationshipResolver m_defaultRelationshipResolver;


    /**
     * We only read and parse the config files once.
     */
    static {
        initMappings();
    }

    public ContextUtilFactory(RelationshipResolver defaultRelationshipResolver) {
        m_defaultRelationshipResolver = defaultRelationshipResolver;
    }

    public ContextUtil getInstance() {
        return new ContextUtil(m_defaultRelationshipResolver);
    }

    public ContextUtil getInstance(RelationshipResolver relationshipResolver) {
        return new ContextUtil(relationshipResolver);
    }
    
    private static void initMappings() {
        // get the mapping information
        // get the PEP configuration
        File configPEPFile =
                new File(Constants.FEDORA_HOME,
                         "server/config/config-melcoe-pep-mapping.xml");
        InputStream is = null;
        try {
            is = new FileInputStream(configPEPFile);
        } catch (FileNotFoundException e) {
            logger.info("Mapping file, config-melcoe-pep-mapping.xml, not found.");
        }

        if (is != null) {
            logger.info("Mapping file found (config-melcoe-pep-mapping.xml). Loading maps");
            try {
                DocumentBuilderFactory factory =
                        DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = factory.newDocumentBuilder();
                Document doc = docBuilder.parse(is);
                NodeList nodes = null;

                nodes = doc.getElementsByTagName("actionAttribute");
                if (nodes != null && nodes.getLength() > 0) {

                    for (int x = 0; x < nodes.getLength(); x++) {
                        if (nodes.item(x).getNodeType() == Node.ELEMENT_NODE) {
                            String from =
                                    nodes.item(x).getAttributes()
                                            .getNamedItem("from")
                                            .getNodeValue();
                            String to =
                                    nodes.item(x).getAttributes()
                                            .getNamedItem("to").getNodeValue();
                            try {
                                URI key = new URI(from);
                                URI value = new URI(to);
                                actionMap.put(key, value);
                            } catch (URISyntaxException mue) {
                                logger.warn("Mapping contained invalid URI: ["
                                        + from + "] / [" + to + "]");
                            }
                        }
                    }
                }

                nodes = doc.getElementsByTagName("actionAttributeValue");
                if (nodes != null && nodes.getLength() > 0) {

                    for (int x = 0; x < nodes.getLength(); x++) {
                        if (nodes.item(x).getNodeType() == Node.ELEMENT_NODE) {
                            String from =
                                    nodes.item(x).getAttributes()
                                            .getNamedItem("from")
                                            .getNodeValue();
                            String to =
                                    nodes.item(x).getAttributes()
                                            .getNamedItem("to").getNodeValue();
                            actionValueMap.put(from, to);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Error occurred loading the mapping file. "
                        + "Mappings will not be used.", e);
            }
        }
    }
}
