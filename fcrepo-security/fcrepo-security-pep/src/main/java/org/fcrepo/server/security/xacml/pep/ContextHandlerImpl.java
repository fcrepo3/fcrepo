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
import org.fcrepo.server.security.xacml.util.RelationshipResolver;

/**
 * @author nishen@melcoe.mq.edu.au
 * @see ContextHandler
 */
public class ContextHandlerImpl
        implements ContextHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(ContextHandlerImpl.class);

    private static ContextHandler contextHandler = null;

    private ContextUtil m_contextUtil = null;

    private PDPClient m_client = null;

    private EvaluationEngine m_evaluationEngine = null;
    
    private RelationshipResolver m_relationshipResolver;

    private ResponseCache m_responseCache = null;

    /**
     * The default constructor that initialises a new ContextHandler instance.
     * This is a private constructor as this is a singleton class.
     *
     * @throws PEPException
     */
    private ContextHandlerImpl()
            throws PEPException {
        super();
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
    
    public void setEvaluationEngine(EvaluationEngine evaluationEngine) {
        m_evaluationEngine = evaluationEngine;
    }
    
    public void setRealtionshipResolver(RelationshipResolver relationshipResolver) {
        m_relationshipResolver = relationshipResolver;
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
            return m_contextUtil.buildRequest(subjects,
                                            actions,
                                            resources,
                                            environment,
                                            m_relationshipResolver);
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
        return m_evaluationEngine.evaluate(reqCtx);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.ContextHandler#evaluate(java.lang.String)
     */
    public String evaluate(String request) throws PEPException {
        return m_evaluationEngine.evaluate(request);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.ContextHandler#evaluateBatch(java.lang.String[])
     */
    public String evaluateBatch(String[] requests) throws PEPException {
        return m_evaluationEngine.evaluate(requests);
    }
    
    public void setResponseCache(ResponseCache responseCache) {
        m_responseCache = responseCache;
    }

    public ResponseCache getResponseCache() {
        return m_responseCache;
    }
    
    public void setPDPClient(PDPClient client) {
        m_client = client;
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
    public void init() throws PEPException {
        try {
            // get the PEP configuration
                // disable caching through system property or env variable
                // system property takes precedence (env variable to be deprecated)
                int cacheSize = 1000; // default
                long cacheTTL = 10000; // default

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
                m_responseCache.setTTL(cacheTTL);

            m_evaluationEngine.setClient(m_client);
            m_evaluationEngine.setResponseCache(m_responseCache);

            m_contextUtil = ContextUtil.getInstance();

            if (logger.isDebugEnabled()) {
                logger.debug("Instantiated ContextUtil.");
            }
        } catch (Exception e) {
            logger.error("Failed to initialse the PEP ContextHandler", e);
            throw new PEPException(e.getMessage(), e);
        }
    }
}