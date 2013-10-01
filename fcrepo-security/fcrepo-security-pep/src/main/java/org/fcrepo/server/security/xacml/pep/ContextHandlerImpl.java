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

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.fcrepo.server.security.xacml.util.ContextUtil;
import org.fcrepo.server.security.xacml.util.RelationshipResolver;

import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;

/**
 * @author nishen@melcoe.mq.edu.au
 * @see ContextHandler
 */
public class ContextHandlerImpl
        implements ContextHandler {

    private ContextUtil m_contextUtil = null;

    private EvaluationEngine m_evaluationEngine = null;

    private RelationshipResolver m_relationshipResolver;

    /**
     * The default constructor that initialises a new ContextHandler instance.
     * This is a private constructor as this is a singleton class.
     *
     * @throws PEPException
     */
    public ContextHandlerImpl()
            throws PEPException {
        super();
    }

    public void setContextUtil(ContextUtil contextUtil) {
        m_contextUtil = contextUtil;
    }

    public void setEvaluationEngine(EvaluationEngine evaluationEngine) {
        m_evaluationEngine = evaluationEngine;
    }

    public void setRelationshipResolver(RelationshipResolver relationshipResolver) {
        m_relationshipResolver = relationshipResolver;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.ContextHandler#buildRequest(java.util.List,
     * java.util.Map, java.util.Map, java.util.Map)
     */
    @Override
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
    @Override
    public ResponseCtx evaluate(RequestCtx reqCtx) throws PEPException {
        return m_evaluationEngine.evaluate(reqCtx);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.ContextHandler#evaluate(java.lang.String)
     */
    @Override
    public String evaluate(String request) throws PEPException {
        return m_evaluationEngine.evaluate(request);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.ContextHandler#evaluateBatch(java.lang.String[])
     */
    @Override
    public String evaluateBatch(String[] requests) throws PEPException {
        return m_evaluationEngine.evaluate(requests);
    }

}