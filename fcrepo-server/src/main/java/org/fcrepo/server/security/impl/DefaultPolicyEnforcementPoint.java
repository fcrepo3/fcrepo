/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.impl;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

import org.jboss.security.xacml.sunxacml.PDPConfig;
import org.jboss.security.xacml.sunxacml.ctx.ResponseCtx;
import org.jboss.security.xacml.sunxacml.ctx.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.fcrepo.server.Context;
import org.fcrepo.server.config.ModuleConfiguration;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.errors.authorization.AuthzDeniedException;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.errors.authorization.AuthzOperationalException;
import org.fcrepo.server.errors.authorization.AuthzPermittedException;
import org.fcrepo.server.security.Attribute;
import org.fcrepo.server.security.PolicyEnforcementPoint;
import org.fcrepo.server.security.RequestCtx;

/**
 * @author Bill Niebel
 */
public class DefaultPolicyEnforcementPoint 
extends AbstractPolicyEnforcementPoint
implements PolicyEnforcementPoint {

    private static final Logger logger =
            LoggerFactory.getLogger(DefaultPolicyEnforcementPoint.class);

    private static final List<Attribute> EMPTY_ENV = Collections.emptyList();

    private static final String ROLE = org.fcrepo.server.security.PolicyEnforcementPoint.class.getName();

    private static final String ENFORCE_MODE_CONFIG_KEY = "ENFORCE-MODE";

    static final String ENFORCE_MODE_ENFORCE_POLICIES = "enforce-policies";

    static final String ENFORCE_MODE_PERMIT_ALL_REQUESTS =
            "permit-all-requests";

    static final String ENFORCE_MODE_DENY_ALL_REQUESTS = "deny-all-requests";

    private String m_enforceMode = ENFORCE_MODE_ENFORCE_POLICIES;

    public DefaultPolicyEnforcementPoint(PDPConfig pdpConfig, ModuleConfiguration authzConfiguration)
            throws ModuleInitializationException {

        super(pdpConfig);

        String enforceMode = authzConfiguration.getParameter(ENFORCE_MODE_CONFIG_KEY);
        if (enforceMode != null) {
            m_enforceMode = enforceMode;
            if (ENFORCE_MODE_ENFORCE_POLICIES.equals(m_enforceMode)) {
            } else if (ENFORCE_MODE_PERMIT_ALL_REQUESTS.equals(m_enforceMode)) {
            } else if (ENFORCE_MODE_DENY_ALL_REQUESTS.equals(m_enforceMode)) {
            } else {
                throw new ModuleInitializationException("invalid enforceMode from config \"" + m_enforceMode + "\"", ROLE);
            }
        }
    }


    /* (non-Javadoc)
     * @see org.fcrepo.server.security.PolicyEnforcementPoint#enforce(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, org.fcrepo.server.Context)
     */
    @SuppressWarnings("unchecked")
    @Override
    public final void enforce(String subjectId,
                              String action,
                              String api,
                              String pid,
                              String namespace,
                              Context context) throws AuthzException {

        boolean debug = logger.isDebugEnabled();
        long enforceStartTime = debug ? System.currentTimeMillis() : 0;
        try {
            synchronized (this) {
                //wait, if pdp update is in progress
            }
            if (ENFORCE_MODE_PERMIT_ALL_REQUESTS.equals(m_enforceMode)) {
                logger.debug("permitting request because enforceMode==ENFORCE_MODE_PERMIT_ALL_REQUESTS");
            } else if (ENFORCE_MODE_DENY_ALL_REQUESTS.equals(m_enforceMode)) {
                logger.debug("denying request because enforceMode==ENFORCE_MODE_DENY_ALL_REQUESTS");
                throw new AuthzDeniedException("all requests are currently denied");
            } else if (!ENFORCE_MODE_ENFORCE_POLICIES.equals(m_enforceMode)) {
                logger.debug("denying request because enforceMode is invalid");
                throw new AuthzOperationalException("invalid enforceMode from config \"" + m_enforceMode + "\"");
            } else {
                ResponseCtx response = null;
                String contextIndex = null;
                try {
                    List<Subject> subjects = wrapSubjects(subjectId);
                    List<Attribute> actions = wrapActions(action, api, contextIndex);
                    List<Attribute> resources = wrapResources(pid, namespace);

                    RequestCtx request =
                            new BasicRequestCtx(subjects,
                                           resources,
                                           actions,
                                           EMPTY_ENV);
                    if (debug) {
                        for (Attribute tempobj: actions) {
                            logger.debug("request action has {}={}", tempobj.getId(), tempobj.getValue());
                        }
                    }
                    long st = debug ? System.currentTimeMillis() : 0;
                    try {
                        // we use a localized EvaluationCtx so that resource-id is correctly located
                        response = m_pdp.evaluate(new BasicEvaluationCtx(request, m_pdpConfig.getAttributeFinder(), context));
                    } finally {
                        if (debug) {
                          long dur = System.currentTimeMillis() - st;
                          logger.debug("Policy evaluation took {} ms.", dur);
                        }
                    }

                    logger.debug("in pep, after evaluate() called");
                } catch (Throwable t) {
                    logger.error("Error evaluating policy", t);
                    throw new AuthzOperationalException("");
                }
                logger.debug("in pep, before denyBiasedAuthz() called");
                if (!denyBiasedAuthz(response.getResults())) {
                    if (debug) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        response.encode(bos);                        
                        logger.debug(bos.toString());                        
                    }
                    throw new AuthzDeniedException("");
                }
            }
            if (context.getNoOp()) {
                throw new AuthzPermittedException("noOp");
            }
        } finally {
            if (debug) {
                long dur = System.currentTimeMillis() - enforceStartTime;
                logger.debug("Policy enforcement took {} ms.", dur);
            }
        }
    }

}
