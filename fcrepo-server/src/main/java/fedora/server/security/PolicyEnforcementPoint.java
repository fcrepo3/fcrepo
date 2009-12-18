/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;

import com.sun.xacml.PDP;
import com.sun.xacml.PDPConfig;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Subject;
import com.sun.xacml.finder.AttributeFinder;
import com.sun.xacml.finder.PolicyFinder;

import org.apache.log4j.Logger;

import fedora.common.Constants;

import fedora.server.Context;
import fedora.server.errors.authorization.AuthzDeniedException;
import fedora.server.errors.authorization.AuthzException;
import fedora.server.errors.authorization.AuthzOperationalException;
import fedora.server.errors.authorization.AuthzPermittedException;
import fedora.server.storage.DOManager;

import fedora.utilities.Log4JRedirectFilter;

/**
 * @author Bill Niebel
 */
public class PolicyEnforcementPoint {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(PolicyEnforcementPoint.class.getName());

    public static final String SUBACTION_SEPARATOR = "//";

    public static final String SUBRESOURCE_SEPARATOR = "//";

    private static PolicyEnforcementPoint singleton = null;

    private static int count = 0;

    private String enforceMode = ENFORCE_MODE_ENFORCE_POLICIES;

    static final String ENFORCE_MODE_ENFORCE_POLICIES = "enforce-policies";

    static final String ENFORCE_MODE_PERMIT_ALL_REQUESTS =
            "permit-all-requests";

    static final String ENFORCE_MODE_DENY_ALL_REQUESTS = "deny-all-requests";

    public static final String XACML_SUBJECT_ID =
            "urn:oasis:names:tc:xacml:1.0:subject:subject-id";

    public static final String XACML_ACTION_ID =
            "urn:oasis:names:tc:xacml:1.0:action:action-id";

    public static final String XACML_RESOURCE_ID =
            "urn:oasis:names:tc:xacml:1.0:resource:resource-id";

    private final URI XACML_SUBJECT_ID_URI;

    private final URI XACML_ACTION_ID_URI;

    private final URI XACML_RESOURCE_ID_URI;

    private final URI SUBJECT_ID_URI;

    private final URI ACTION_ID_URI;

    private final URI ACTION_API_URI;

    private final URI ACTION_CONTEXT_URI;

    private final URI RESOURCE_ID_URI;

    private final URI RESOURCE_NAMESPACE_URI;

    static {
        // force com.sun.xacml logging to use Log4J
        Log4JRedirectFilter.apply("com.sun.xacml.finder.AttributeFinder");
    }

    private PolicyEnforcementPoint() {

        URI xacmlSubjectIdUri = null;
        URI xacmlActionIdUri = null;
        URI xacmlResourceIdUri = null;

        URI subjectIdUri = null;
        URI actionIdUri = null;
        URI actionApiUri = null;
        URI contextUri = null;
        URI pidUri = null;
        URI namespaceUri = null;
        try {
            xacmlSubjectIdUri = new URI(XACML_SUBJECT_ID);
            xacmlActionIdUri = new URI(XACML_ACTION_ID);
            xacmlResourceIdUri = new URI(XACML_RESOURCE_ID);
            subjectIdUri = new URI(Constants.SUBJECT.LOGIN_ID.uri);
            actionIdUri = new URI(Constants.ACTION.ID.uri);
            actionApiUri = new URI(Constants.ACTION.API.uri);
            contextUri = new URI(Constants.ACTION.CONTEXT_ID.uri);
            pidUri = new URI(Constants.OBJECT.PID.uri);
            namespaceUri = new URI(Constants.OBJECT.NAMESPACE.uri);
        } catch (URISyntaxException e) {
            LOG.fatal("Bad URI syntax", e);
        } finally {
            XACML_SUBJECT_ID_URI = xacmlSubjectIdUri;
            XACML_ACTION_ID_URI = xacmlActionIdUri;
            XACML_RESOURCE_ID_URI = xacmlResourceIdUri;
            SUBJECT_ID_URI = subjectIdUri;
            ACTION_ID_URI = actionIdUri;
            ACTION_API_URI = actionApiUri;
            ACTION_CONTEXT_URI = contextUri;
            RESOURCE_ID_URI = pidUri;
            RESOURCE_NAMESPACE_URI = namespaceUri;
        }
    }

    public static final PolicyEnforcementPoint getInstance() {
        if (singleton == null) {
            singleton = new PolicyEnforcementPoint();
        }
        count++;
        LOG.debug("***another use (" + count + ") of XACMLPep singleton");
        return singleton;
    }

    /**
     * xacml pdp
     */
    private PDP pdp = null;

    /**
     * available during init(); keep as logging hook
     */
    private ServletContext servletContext = null;

    private ContextAttributeFinderModule contextAttributeFinder;

    public final void newPdp() throws Exception {
        AttributeFinder attrFinder = new AttributeFinder();
        List<AttributeFinderModule> attrModules =
                new ArrayList<AttributeFinderModule>();

        ResourceAttributeFinderModule resourceAttributeFinder =
                ResourceAttributeFinderModule.getInstance();
        resourceAttributeFinder.setServletContext(servletContext);
        resourceAttributeFinder.setDOManager(manager);
        resourceAttributeFinder.setOwnerIdSeparator(ownerIdSeparator);
        attrModules.add(resourceAttributeFinder);
        try {
            LOG.debug("about to set contextAttributeFinder in original");
            contextAttributeFinder = ContextAttributeFinderModule.getInstance();
        } catch (Throwable t) {
            enforceMode = ENFORCE_MODE_DENY_ALL_REQUESTS;
            LOG.error("Error in newPdp", t);
            if (t instanceof Exception) {
                throw (Exception) t;
            }
            throw new Exception("wrapped", t);
        }

        LOG.debug("just set contextAttributeFinder=" + contextAttributeFinder);
        contextAttributeFinder.setServletContext(servletContext);
        attrModules.add(contextAttributeFinder);

        attrFinder.setModules(attrModules);
        LOG.debug("before building policy finder");

        PolicyFinder policyFinder = new PolicyFinder();

        Set<PolicyFinderModule> policyModules =
                new HashSet<PolicyFinderModule>();
        PolicyFinderModule combinedPolicyModule = null;
        combinedPolicyModule =
                new PolicyFinderModule(combiningAlgorithm,
                                       globalPolicyConfig,
                                       globalBackendPolicyConfig,
                                       globalPolicyGuiToolConfig,
                                       manager,
                                       validateRepositoryPolicies,
                                       validateObjectPoliciesFromDatastream,
                                       policyParser);
        LOG.debug("after constucting fedora policy finder module");
        LOG
                .debug("before adding fedora policy finder module to policy finder hashset");
        policyModules.add(combinedPolicyModule);
        LOG
                .debug("after adding fedora policy finder module to policy finder hashset");
        LOG.debug("o before setting policy finder hashset into policy finder");
        policyFinder.setModules(policyModules);
        LOG.debug("o after setting policy finder hashset into policy finder");

        PDP pdp = new PDP(new PDPConfig(attrFinder, policyFinder, null));
        synchronized (this) {
            this.pdp = pdp;
            //so enforce() will wait, if this pdp update is in progress
        }
    }

    String combiningAlgorithm = null;

    String globalPolicyConfig = null;

    String globalBackendPolicyConfig = null;

    String globalPolicyGuiToolConfig = null;

    DOManager manager = null;

    boolean validateRepositoryPolicies = false;

    boolean validateObjectPoliciesFromDatastream = false;

    PolicyParser policyParser;

    String ownerIdSeparator = ",";

    public void initPep(String enforceMode,
                        String combiningAlgorithm,
                        String globalPolicyConfig,
                        String globalBackendPolicyConfig,
                        String globalPolicyGuiToolConfig,
                        DOManager manager,
                        boolean validateRepositoryPolicies,
                        boolean validateObjectPoliciesFromDatastream,
                        PolicyParser policyParser,
                        String ownerIdSeparator) throws Exception {
        LOG.debug("in initPep()");
        destroy();
        this.policyParser = policyParser;
        this.enforceMode = enforceMode;
        if (ENFORCE_MODE_ENFORCE_POLICIES.equals(enforceMode)) {
        } else if (ENFORCE_MODE_PERMIT_ALL_REQUESTS.equals(enforceMode)) {
        } else if (ENFORCE_MODE_DENY_ALL_REQUESTS.equals(enforceMode)) {
        } else {
            throw new AuthzOperationalException("invalid enforceMode from config");
        }
        this.combiningAlgorithm = combiningAlgorithm;
        this.globalPolicyConfig = globalPolicyConfig;
        this.globalBackendPolicyConfig = globalBackendPolicyConfig;
        this.globalPolicyGuiToolConfig = globalPolicyGuiToolConfig;
        this.manager = manager;
        this.validateRepositoryPolicies = validateRepositoryPolicies;
        this.validateObjectPoliciesFromDatastream =
                validateObjectPoliciesFromDatastream;
        this.ownerIdSeparator = ownerIdSeparator;
        newPdp();
    }

    public void inactivate() {
        destroy();
    }

    public void destroy() {
        servletContext = null;
        pdp = null;
    }

    private final Set wrapSubjects(String subjectLoginId) {
        LOG.debug("wrapSubjectIdAsSubjects(): " + subjectLoginId);
        StringAttribute stringAttribute = new StringAttribute("");
        Attribute subjectAttribute =
                new Attribute(XACML_SUBJECT_ID_URI, null, null, stringAttribute);
        LOG.debug("wrapSubjectIdAsSubjects(): subjectAttribute, id="
                + subjectAttribute.getId() + ", type="
                + subjectAttribute.getType() + ", value="
                + subjectAttribute.getValue());
        Set<Attribute> subjectAttributes = new HashSet<Attribute>();
        subjectAttributes.add(subjectAttribute);
        if (subjectLoginId != null && !"".equals(subjectLoginId)) {
            stringAttribute = new StringAttribute(subjectLoginId);
            subjectAttribute =
                    new Attribute(SUBJECT_ID_URI, null, null, stringAttribute);
            LOG.debug("wrapSubjectIdAsSubjects(): subjectAttribute, id="
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

    private final Set wrapActions(String actionId,
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

    private final Set wrapResources(String pid, String namespace)
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

    private int n = 0;

    private synchronized int next() {
        return n++;
    }

    private final Set NULL_SET = new HashSet();

    public final void enforce(String subjectId,
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
            if (ENFORCE_MODE_PERMIT_ALL_REQUESTS.equals(enforceMode)) {
                LOG
                        .debug("permitting request because enforceMode==ENFORCE_MODE_PERMIT_ALL_REQUESTS");
            } else if (ENFORCE_MODE_DENY_ALL_REQUESTS.equals(enforceMode)) {
                LOG
                        .debug("denying request because enforceMode==ENFORCE_MODE_DENY_ALL_REQUESTS");
                throw new AuthzDeniedException("all requests are currently denied");
            } else if (!ENFORCE_MODE_ENFORCE_POLICIES.equals(enforceMode)) {
                LOG.debug("denying request because enforceMode is invalid");
                throw new AuthzOperationalException("invalid enforceMode from config");
            } else {
                ResponseCtx response = null;
                String contextIndex = null;
                try {
                    contextIndex = (new Integer(next())).toString();
                    LOG.debug("context index set=" + contextIndex);
                    Set subjects = wrapSubjects(subjectId);
                    Set actions = wrapActions(action, api, contextIndex);
                    Set resources = wrapResources(pid, namespace);

                    RequestCtx request =
                            new RequestCtx(subjects,
                                           resources,
                                           actions,
                                           NULL_SET);
                    Set tempset = request.getAction();
                    Iterator tempit = tempset.iterator();
                    while (tempit.hasNext()) {
                        Attribute tempobj = (Attribute) tempit.next();
                        LOG.debug("request action has " + tempobj.getId() + "="
                                + tempobj.getValue().toString());
                    }
                    LOG.debug("about to ref contextAttributeFinder="
                            + contextAttributeFinder);
                    contextAttributeFinder.registerContext(contextIndex,
                                                           context);

                    long st = System.currentTimeMillis();
                    try {
                        response = pdp.evaluate(request);
                    } finally {
                        long dur = System.currentTimeMillis() - st;
                        LOG.debug("Policy evaluation took " + dur + "ms.");
                    }

                    LOG.debug("in pep, after evaluate() called");
                } catch (Throwable t) {
                    LOG.error("Error evaluating policy", t);
                    throw new AuthzOperationalException("");
                } finally {
                    contextAttributeFinder.unregisterContext(contextIndex);
                }
                LOG.debug("in pep, before denyBiasedAuthz() called");
                if (!denyBiasedAuthz(response.getResults())) {
                    throw new AuthzDeniedException("");
                }
            }
            if (context.getNoOp()) {
                throw new AuthzPermittedException("noOp");
            }
        } finally {
            long dur = System.currentTimeMillis() - enforceStartTime;
            LOG.debug("Policy enforcement took " + dur + "ms.");
        }
    }

    private static final boolean denyBiasedAuthz(Set set) {
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
        LOG.debug("AUTHZ:  permits=" + nPermits + " denies=" + nDenies
                + " indeterminates=" + nIndeterminates + " notApplicables="
                + nNotApplicables + " unexpecteds=" + nWrongs);
        return nPermits >= 1 && nDenies == 0 && nIndeterminates == 0
                && nWrongs == 0; // don't care about NotApplicables
    }

}
