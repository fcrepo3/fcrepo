package org.fcrepo.server.security.xacml.pep.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import com.sun.xacml.PDPConfig;
import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Subject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.authorization.AuthzDeniedException;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.errors.authorization.AuthzOperationalException;
import org.fcrepo.server.errors.authorization.AuthzPermittedException;
import org.fcrepo.server.security.ContextRegistry;
import org.fcrepo.server.security.PolicyEnforcementPoint;
import org.fcrepo.server.security.impl.AbstractPolicyEnforcementPoint;

public class FESLPolicyEnforcementPoint
extends AbstractPolicyEnforcementPoint
implements PolicyEnforcementPoint {
    
    private static final Logger logger = LoggerFactory.getLogger(FESLPolicyEnforcementPoint.class);
    
    private final ContextRegistry m_registry;
    
    public FESLPolicyEnforcementPoint(PDPConfig pdpConfig, ContextRegistry registry) {
        super(pdpConfig);

        m_registry = registry;
    }
    
    private int n = 0;

    private synchronized int next() {
        return n++;
    }

    @Override
    public void enforce(String subjectId,
                        String action,
                        String api,
                        String pid,
                        String namespace,
                        Context context) throws AuthzException {
        long enforceStartTime = System.currentTimeMillis();
        try {
            synchronized (this) {
                //wait, if pdp update is in progress
            }
                ResponseCtx response = null;
                String contextIndex = null;
                try {
                    contextIndex = Integer.toString(next());
                    logger.debug("context index set={}", contextIndex);
                    Set<Subject> subjects = wrapSubjects(subjectId);
                    Set<Attribute> actions = wrapActions(action, api, contextIndex);
                    Set<Attribute> resources = wrapResources(pid, namespace);

                    RequestCtx request =
                            new RequestCtx(subjects,
                                           resources,
                                           actions,
                                           Collections.EMPTY_SET);
                    Iterator<Attribute> tempit = actions.iterator();
                    while (tempit.hasNext()) {
                        Attribute tempobj = tempit.next();
                        logger.debug("request action has {}={}", tempobj.getId(), tempobj.getValue().toString());
                    }
                    m_registry.registerContext(contextIndex, context);
                    long st = System.currentTimeMillis();
                    try {
                        response = m_pdp.evaluate(request);
                    } finally {
                        long dur = System.currentTimeMillis() - st;
                        logger.debug("Policy evaluation took {}ms.", dur);
                    }

                    logger.debug("in pep, after evaluate() called");
                } catch (Throwable t) {
                    logger.error("Error evaluating policy", t);
                    throw new AuthzOperationalException("");
                } finally {
                    m_registry.unregisterContext(contextIndex);
                }
                logger.debug("in pep, before denyBiasedAuthz() called");
                if (!denyBiasedAuthz(response.getResults())) {
                    throw new AuthzDeniedException("");
                }
            
            if (context.getNoOp()) {
                throw new AuthzPermittedException("noOp");
            }
        } finally {
            long dur = System.currentTimeMillis() - enforceStartTime;
            logger.debug("Policy enforcement took {}ms.", dur);
        }

    }

}

