        package org.fcrepo.server.security.impl;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.xacml.PDP;
import com.sun.xacml.PDPConfig;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Subject;
import com.sun.xacml.finder.AttributeFinder;
import com.sun.xacml.finder.AttributeFinderModule;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.ResourceFinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;

import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.authorization.AuthzOperationalException;
import org.fcrepo.server.security.PolicyEnforcementPoint;

        
public abstract class AbstractPolicyEnforcementPoint
        implements PolicyEnforcementPoint {
    private static final Logger logger = LoggerFactory.getLogger(AbstractPolicyEnforcementPoint.class);
    
    static final URI XACML_SUBJECT_ID_URI = getSafeURI(XACML_SUBJECT_ID);

    static final URI XACML_ACTION_ID_URI = getSafeURI(XACML_ACTION_ID);

    static final URI XACML_RESOURCE_ID_URI = getSafeURI(XACML_RESOURCE_ID);

    static final URI SUBJECT_ID_URI = getSafeURI(Constants.SUBJECT.LOGIN_ID.uri);

    static final URI ACTION_ID_URI = getSafeURI(Constants.ACTION.ID.uri);

    static final URI ACTION_API_URI = getSafeURI(Constants.ACTION.API.uri);

    static final URI ACTION_CONTEXT_URI = getSafeURI(Constants.ACTION.CONTEXT_ID.uri);

    static final URI RESOURCE_ID_URI = getSafeURI(Constants.OBJECT.PID.uri);

    static final URI RESOURCE_NAMESPACE_URI = getSafeURI(Constants.OBJECT.NAMESPACE.uri);
    
    protected final PDPConfig m_pdpConfig;
    protected PDP m_pdp;
    
    private static URI getSafeURI(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            logger.error("Bad URI syntax: " + uri, e);
            return null;
        } 
    }
    
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

    protected Set<Subject> wrapSubjects(String subjectLoginId) {
        logger.debug("wrapSubjectIdAsSubjects(): " + subjectLoginId);
        StringAttribute stringAttribute = new StringAttribute("");
        Attribute subjectAttribute =
                new Attribute(XACML_SUBJECT_ID_URI, null, null, stringAttribute);
        logger.debug("wrapSubjectIdAsSubjects(): subjectAttribute, id="
                + subjectAttribute.getId() + ", type="
                + subjectAttribute.getType() + ", value="
                + subjectAttribute.getValue());
        Set<Attribute> subjectAttributes = new HashSet<Attribute>();
        subjectAttributes.add(subjectAttribute);
        if (subjectLoginId != null && !"".equals(subjectLoginId)) {
            stringAttribute = new StringAttribute(subjectLoginId);
            subjectAttribute =
                    new Attribute(SUBJECT_ID_URI, null, null, stringAttribute);
            logger.debug("wrapSubjectIdAsSubjects(): subjectAttribute, id="
                    + subjectAttribute.getId() + ", type="
                    + subjectAttribute.getType() + ", value="
                    + subjectAttribute.getValue());
        }
        subjectAttributes.add(subjectAttribute);
        Subject singleSubject = new Subject(subjectAttributes);
        Set<Subject> subjects = new HashSet<Subject>();
        subjects.add(singleSubject);
        return subjects;
    }

    protected Set<Attribute> wrapActions(String actionId,
                                  String actionApi,
                                  String contextIndex) {
        Set<Attribute> actions = new HashSet<Attribute>();
        Attribute action =
                new Attribute(XACML_ACTION_ID_URI,
                              null,
                              null,
                              new StringAttribute(""));
        actions.add(action);
        action =
                new Attribute(ACTION_ID_URI,
                              null,
                              null,
                              new StringAttribute(actionId));
        actions.add(action);
        action =
                new Attribute(ACTION_API_URI,
                              null,
                              null,
                              new StringAttribute(actionApi));
        actions.add(action);
        action =
                new Attribute(ACTION_CONTEXT_URI,
                              null,
                              null,
                              new StringAttribute(contextIndex));
        actions.add(action);
        return actions;
    }

    protected Set<Attribute> wrapResources(String pid, String namespace)
            throws AuthzOperationalException {
        Set<Attribute> resources = new HashSet<Attribute>();
        Attribute attribute = null;
        attribute =
                new Attribute(XACML_RESOURCE_ID_URI,
                              null,
                              null,
                              new StringAttribute(""));
        resources.add(attribute);
        attribute =
                new Attribute(RESOURCE_ID_URI,
                              null,
                              null,
                              new StringAttribute(pid));
        resources.add(attribute);
        attribute =
                new Attribute(RESOURCE_NAMESPACE_URI,
                              null,
                              null,
                              new StringAttribute(namespace));
        resources.add(attribute);
        return resources;
    }

    protected static final boolean denyBiasedAuthz(Set set) {
        int nPermits = 0; //explicit permit returned
        int nDenies = 0; //explicit deny returned
        int nNotApplicables = 0; //no targets matched
        int nIndeterminates = 0; //for targets matched, no rules matched
        int nWrongs = 0; //none of the above, i.e., unreported failure, should not happen
        Iterator it = set.iterator();
        while (it.hasNext()) {
            Result result = (Result) it.next();
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
        logger.debug("AUTHZ:  permits=" + nPermits + " denies=" + nDenies
                + " indeterminates=" + nIndeterminates + " notApplicables="
                + nNotApplicables + " unexpecteds=" + nWrongs);
        return nPermits >= 1 && nDenies == 0 && nIndeterminates == 0
                && nWrongs == 0; // don't care about NotApplicables
    }
}

    