/*
 * File: ContextHandlerImpl.java
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

package org.fcrepo.server.security.xacml.pep;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.lang.reflect.Constructor;

import java.net.URI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;

import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.fcrepo.server.security.xacml.util.ContextUtil;

/**
 * @author nishen@melcoe.mq.edu.au
 * @see ContextHandler
 */
public class ContextHandlerImpl
        implements ContextHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(ContextHandlerImpl.class);

    private ContextUtil contextUtil = null;

    private static ContextHandler contextHandler = null;

    private PDPClient client = null;

    private EvaluationEngine evaluationEngine = null;

    private ResponseCache responseCache = null;

    /**
     * The default constructor that initialises a new ContextHandler instance.
     * This is a private constructor as this is a singleton class.
     *
     * @throws PEPException
     */
    private ContextHandlerImpl()
            throws PEPException {
        super();
        init();
    }

    /**
     * @return an instance of a ContextHandler
     * @throws PEPException
     */
    public static ContextHandler getInstance() throws PEPException {
        if (contextHandler == null) {
            try {
                contextHandler = new ContextHandlerImpl();
            } catch (Exception e) {
                logger.error("Could not initialise ContextHandler.");
                throw new PEPException("Could not initialise ContextHandler.",
                                       e);
            }
        }

        return contextHandler;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.ContextHandler#buildRequest(java.util.List,
     * java.util.Map, java.util.Map, java.util.Map)
     */
    public RequestCtx buildRequest(List<Map<URI, List<AttributeValue>>> subjects,
                                   Map<URI, AttributeValue> actions,
                                   Map<URI, AttributeValue> resources,
                                   Map<URI, AttributeValue> environment)
            throws PEPException {
        try {
            return contextUtil.buildRequest(subjects,
                                            actions,
                                            resources,
                                            environment);
        } catch (MelcoeXacmlException e) {
            throw new PEPException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.ContextHandler#evaluate(com.sun.xacml.ctx.RequestCtx)
     */
    public ResponseCtx evaluate(RequestCtx reqCtx) throws PEPException {
        return evaluationEngine.evaluate(reqCtx);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.ContextHandler#evaluate(java.lang.String)
     */
    public String evaluate(String request) throws PEPException {
        return evaluationEngine.evaluate(request);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.ContextHandler#evaluateBatch(java.lang.String[])
     */
    public String evaluateBatch(String[] requests) throws PEPException {
        return evaluationEngine.evaluate(requests);
    }

    public ResponseCache getResponseCache() {
        return responseCache;
    }

    /**
     * Reads a configuration file and configures this instance of the
     * ContextHandler. It can instantiate a client (that communicates with the
     * PEP), a relationship resolver (that communicates with the risearch REST
     * service to determine parental relationships) and a response cache (that
     * caches requests/responses for quicker evaluations).
     *
     * @throws PEPException
     */
    private void init() throws PEPException {
        try {
            // get the PEP configuration
            File configPEPFile =
                    new File(Constants.FEDORA_HOME,
                             "server/config/config-melcoe-pep.xml");
            InputStream is = new FileInputStream(configPEPFile);
            if (is == null) {
                throw new PEPException("Could not locate config file: config-melcoe-pep.xml");
            }

            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(is);
            NodeList nodes = null;

            if (logger.isDebugEnabled()) {
                logger.debug("Obtained the config file: config-melcoe-pep.xml");
            }

            String className = null;
            Constructor<?> c = null;

            Map<String, String> options = new HashMap<String, String>();

            // get the PDP Client
            nodes = doc.getElementsByTagName("pdp-client");
            if (nodes.getLength() != 1) {
                throw new PEPException("Config file needs to contain exactly 1 'pdp-client' section.");
            }

            className =
                    nodes.item(0).getAttributes().getNamedItem("class")
                            .getNodeValue();
            NodeList optionNodes = nodes.item(0).getChildNodes();
            for (int x = 0; x < optionNodes.getLength(); x++) {
                Node n = optionNodes.item(x);
                if (optionNodes.item(x).getNodeType() == Node.ELEMENT_NODE) {
                    logger.debug("Node [name]: "
                            + n.getAttributes().getNamedItem("name")
                                    .getNodeValue());
                    String key =
                            n.getAttributes().getNamedItem("name")
                                    .getNodeValue();
                    String value = n.getFirstChild().getNodeValue();
                    options.put(key, value);
                }
            }

            c =
                    Class.forName(className)
                            .getConstructor(new Class[] {Map.class});
            client = (PDPClient) c.newInstance(new Object[] {options});

            if (logger.isDebugEnabled()) {
                logger.debug("Instantiated PDPClient: " + className);
            }

            // get the Response Cache
            nodes = doc.getElementsByTagName("response-cache");
            if (nodes.getLength() != 1) {
                throw new PEPException("Config file needs to contain exactly 1 'response-cache' section.");
            }

            className =
                    nodes.item(0).getAttributes().getNamedItem("class")
                            .getNodeValue();
            if ("true".equals(nodes.item(0).getAttributes()
                    .getNamedItem("active").getNodeValue())) {
                int cacheSize = 1000; // default
                long cacheTTL = 10000; // default
                NodeList children = nodes.item(0).getChildNodes();
                for (int x = 0; x < children.getLength(); x++) {
                    if (children.item(x).getNodeType() == Node.ELEMENT_NODE) {
                        if ("cache-size".equals(children.item(x).getNodeName())) {
                            cacheSize =
                                    Integer.parseInt(children.item(x)
                                            .getFirstChild().getNodeValue());
                        }

                        if ("cache-item-ttl".equals(children.item(x)
                                .getNodeName())) {
                            cacheTTL =
                                    Long.parseLong(children.item(x)
                                            .getFirstChild().getNodeValue());
                        }
                    }
                }

                c =
                        Class.forName(className).getConstructor(new Class[] {
                                Integer.class, Long.class});
                
                // disable caching through system property or env variable
                // system property takes precedence (env variable to be deprecated)
                String noCache = System.getenv("PEP_NOCACHE");
                String noCacheProp = System.getProperty("fedora.fesl.pep_nocache");

                // if system property is set, use that
                if (noCacheProp != null && noCacheProp.toLowerCase().startsWith("t")) {
                	cacheTTL = 0;
                } else {
                	// if system property is not set ..
                	if (noCacheProp == null || noCacheProp.length() == 0) {
                		// use env variable if set
    		            if (noCache != null && noCache.toLowerCase().startsWith("t")) {
    		            	cacheTTL = 0;
    		            }
		            }
                }
                
                responseCache =
                        (ResponseCache) c.newInstance(new Object[] {
                                new Integer(cacheSize), new Long(cacheTTL)});

                logger.info("Instantiated ResponseCache with TTL " + cacheTTL + " " + className);
            }

            // Get the evaluation engine
            nodes = doc.getElementsByTagName("evaluation-engine");
            if (nodes.getLength() != 1) {
                throw new PEPException("Config file needs to contain exactly 1 'evaluation-engine' section.");
            }

            className =
                    nodes.item(0).getAttributes().getNamedItem("class")
                            .getNodeValue();
            evaluationEngine =
                    (EvaluationEngine) Class.forName(className).newInstance();
            evaluationEngine.setClient(client);
            evaluationEngine.setResponseCache(responseCache);

            if (logger.isDebugEnabled()) {
                logger.debug("Instantiated EvaluationEngine: " + className);
            }

            contextUtil = ContextUtil.getInstance();

            if (logger.isDebugEnabled()) {
                logger.debug("Instantiated ContextUtil.");
            }
        } catch (Exception e) {
            logger.error("Failed to initialse the PEP ContextHandler", e);
            throw new PEPException(e.getMessage(), e);
        }
    }
}
