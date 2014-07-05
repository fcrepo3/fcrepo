        package org.fcrepo.server.security.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.fcrepo.common.Constants;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.authorization.AuthzOperationalException;
import org.fcrepo.server.security.Attribute;
import org.fcrepo.server.security.PolicyEnforcementPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.security.xacml.sunxacml.PDP;
import org.jboss.security.xacml.sunxacml.PDPConfig;
import org.jboss.security.xacml.sunxacml.attr.AnyURIAttribute;
import org.jboss.security.xacml.sunxacml.attr.StringAttribute;
import org.jboss.security.xacml.sunxacml.ctx.Result;
import org.jboss.security.xacml.sunxacml.ctx.Subject;


public abstract class AbstractPolicyEnforcementPoint
        implements PolicyEnforcementPoint {
    private static final Logger logger = LoggerFactory.getLogger(AbstractPolicyEnforcementPoint.class);

    static final URI SUBJECT_ID_URI = Constants.SUBJECT.LOGIN_ID.attributeId;

    static final URI ACTION_ID_URI = Constants.ACTION.ID.attributeId;

    static final URI ACTION_API_URI = Constants.ACTION.API.attributeId;

    static final URI ACTION_CONTEXT_URI = Constants.ACTION.CONTEXT_ID.attributeId;

    static final URI RESOURCE_PID_URI = Constants.OBJECT.PID.attributeId;

    static final URI RESOURCE_NAMESPACE_URI = Constants.OBJECT.NAMESPACE.attributeId;
    
    static final StringAttribute EMPTY_ATTRIBUTE = new StringAttribute("");

    static final Attribute ACTION_ATTRIBUTE =
            new SingletonAttribute(Constants.XACML1_ACTION.ID.attributeId,
                    null, null, EMPTY_ATTRIBUTE);

    static final Attribute RESOURCE_ATTRIBUTE =
            new SingletonAttribute(Constants.XACML1_RESOURCE.ID.attributeId,
                    null, null, EMPTY_ATTRIBUTE);

    static final Attribute SUBJECT_ATTRIBUTE =
            new SingletonAttribute(Constants.XACML1_SUBJECT.ID.attributeId,
                    null, null, EMPTY_ATTRIBUTE);

    protected final PDPConfig m_pdpConfig;
    protected PDP m_pdp;

    public AbstractPolicyEnforcementPoint(PDPConfig pdpConfig) {
        m_pdpConfig = pdpConfig;
    }

    public void init() throws GeneralException {
        newPdp();
    }

    @Override
    public final void newPdp() {
        PDP pdp = new PDP(m_pdpConfig);
        synchronized (this) {
            this.m_pdp = pdp;
            //so enforce() will wait, if this pdp update is in progress
        }
    }

    /* (non-Javadoc)
     * @see org.fcrepo.server.security.PolicyEnforcementPoint#inactivate()
     */
    @Override
    public void inactivate() {
        destroy();
    }

    /* (non-Javadoc)
     * @see org.fcrepo.server.security.PolicyEnforcementPoint#destroy()
     */
    @Override
    public void destroy() {
        m_pdp = null;
    }

    protected List<Subject> wrapSubjects(String subjectLoginId) {
        logger.debug("wrapSubjectIdAsSubjects(): {}", subjectLoginId);
        StringAttribute stringAttribute = EMPTY_ATTRIBUTE;
        Attribute subjectAttribute =
                SUBJECT_ATTRIBUTE;
        logger.debug("wrapSubjectIdAsSubjects(): subjectAttribute, id={}, type={}, value={}",
                subjectAttribute.getId(), subjectAttribute.getType(), subjectAttribute.getValue());
        List<Attribute> subjectAttributes;
        if (subjectLoginId != null && !subjectLoginId.isEmpty()) {
            subjectAttributes = new ArrayList<Attribute>(2);
            subjectAttributes.add(subjectAttribute);
            stringAttribute = new StringAttribute(subjectLoginId);
            subjectAttribute =
                    new SingletonAttribute(SUBJECT_ID_URI, null, null, stringAttribute);
            logger.debug("wrapSubjectIdAsSubjects(): subjectAttribute, id={}, type={}, value={}",
                    subjectAttribute.getId(), subjectAttribute.getType(), subjectAttribute.getValue());
            subjectAttributes.add(subjectAttribute);
        } else {
            subjectAttributes = new ArrayList<Attribute>(1);
            subjectAttributes.add(subjectAttribute);
        }
        Subject singleSubject = new Subject(subjectAttributes);
        return Collections.singletonList(singleSubject);
    }
    
    protected List<Attribute> wrapActions(String actionId,
                                  String actionApi,
                                  String contextIndex) {
        List<Attribute> actions = new ArrayList<Attribute>(4);
        actions.add(ACTION_ATTRIBUTE);
        
        Attribute action =
                new SingletonAttribute(ACTION_ID_URI,
                              null,
                              null,
                              new StringAttribute(actionId));
        actions.add(action);
        action =
                new SingletonAttribute(ACTION_API_URI,
                              null,
                              null,
                              new StringAttribute(actionApi));
        actions.add(action);
        if (contextIndex != null) {
            action =
                new SingletonAttribute(ACTION_CONTEXT_URI,
                              null,
                              null,
                              new StringAttribute(contextIndex));
            actions.add(action);
        }
        return actions;
    }

    protected List<Attribute> wrapResources(String pid, String namespace)
            throws AuthzOperationalException {
        List<Attribute> resources = new ArrayList<Attribute>(3);

        Attribute attribute;
        try {
            attribute = new SingletonAttribute(Constants.XACML1_RESOURCE.ID.attributeId,
                    null, null, AnyURIAttribute.getInstance(pid));
        } catch (Exception e) {
            logger.warn("pid {} is not a valid uri; write policies against the StringAttribute {} instead.",
                    pid,
                    Constants.OBJECT.PID.uri);
            attribute = new SingletonAttribute(Constants.XACML1_RESOURCE.ID.attributeId,
                    null, null, StringAttribute.getInstance(pid));
        }
                
        resources.add(attribute);
        attribute =
                new SingletonAttribute(RESOURCE_PID_URI,
                              null,
                              null,
                              new StringAttribute(pid));
        resources.add(attribute);
        attribute =
                new SingletonAttribute(RESOURCE_NAMESPACE_URI,
                              null,
                              null,
                              new StringAttribute(namespace));
        resources.add(attribute);

        return resources;
    }

    protected static final boolean denyBiasedAuthz(Set<Result> set) {
        int nPermits = 0; //explicit permit returned
        int nDenies = 0; //explicit deny returned
        int nNotApplicables = 0; //no targets matched
        int nIndeterminates = 0; //for targets matched, no rules matched
        int nWrongs = 0; //none of the above, i.e., unreported failure, should not happen
        Iterator<Result> it = set.iterator();
        while (it.hasNext()) {
            Result result = it.next();
            int decision = result.getDecision();
            switch (decision) {
                case Result.DECISION_PERMIT:
                    nPermits++;
                    break;
                case Result.DECISION_DENY:
                    nDenies++;
                    break;
                case Result.DECISION_INDETERMINATE:
                    nIndeterminates++;
                    break;
                case Result.DECISION_NOT_APPLICABLE:
                    nNotApplicables++;
                    break;
                default:
                    nWrongs++;
                    break;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("AUTHZ:  permits=" + nPermits + " denies=" + nDenies
                + " indeterminates=" + nIndeterminates + " notApplicables="
                + nNotApplicables + " unexpecteds=" + nWrongs);
        }
        return nPermits >= 1 && nDenies == 0 && nIndeterminates == 0
                && nWrongs == 0; // don't care about NotApplicables
    }
    
}

