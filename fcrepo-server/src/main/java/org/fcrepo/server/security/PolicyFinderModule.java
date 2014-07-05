/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Server;
import org.fcrepo.server.config.ModuleConfiguration;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.validation.ValidationUtility;
import org.fcrepo.utilities.FileUtils;
import org.fcrepo.utilities.XmlTransformUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jboss.security.xacml.sunxacml.AbstractPolicy;
import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.PolicySet;
import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.attr.BagAttribute;
import org.jboss.security.xacml.sunxacml.attr.StringAttribute;
import org.jboss.security.xacml.sunxacml.combine.OrderedDenyOverridesPolicyAlg;
import org.jboss.security.xacml.sunxacml.combine.PolicyCombiningAlgorithm;
import org.jboss.security.xacml.sunxacml.cond.EvaluationResult;
import org.jboss.security.xacml.sunxacml.ctx.Status;
import org.jboss.security.xacml.sunxacml.finder.PolicyFinder;
import org.jboss.security.xacml.sunxacml.finder.PolicyFinderResult;

/**
 * XACML PolicyFinder for Fedora.
 * <p>
 * This provides repository-wide policies and object-specific policies,
 * when available.
 */
public class PolicyFinderModule
        extends org.jboss.security.xacml.sunxacml.finder.PolicyFinderModule {

    private static final Logger logger =
            LoggerFactory.getLogger(PolicyFinderModule.class);

    private static final List<String> ERROR_CODE_LIST = new ArrayList<String>(1);

    static {
        ERROR_CODE_LIST.add(Status.STATUS_PROCESSING_ERROR);
    }

    private static final String DEFAULT = "default";

    private static final String DEFAULT_XACML_COMBINING_ALGORITHM = "org.jboss.security.xacml.sunxacml.combine.OrderedDenyOverridesPolicyAlg";

    private static final String XACML_DIST_BASE = "fedora-internal-use";

    private static final String DEFAULT_REPOSITORY_POLICIES_DIRECTORY =
            XACML_DIST_BASE
            + "/fedora-internal-use-repository-policies-approximating-2.0";

    private static final String BACKEND_POLICIES_ACTIVE_DIRECTORY =
            XACML_DIST_BASE + "/fedora-internal-use-backend-service-policies";

    private static final String BE_SECURITY_XML_LOCATION =
            "config/beSecurity.xml";

    private static final String BACKEND_POLICIES_XSL_LOCATION =
            XACML_DIST_BASE + "/build-backend-policy.xsl";

    private static final String COMBINING_ALGORITHM_KEY = "XACML-COMBINING-ALGORITHM";

    private static final String REPOSITORY_POLICIES_DIRECTORY_KEY =
            "REPOSITORY-POLICIES-DIRECTORY";

    private static final String POLICY_SCHEMA_PATH_KEY = "POLICY-SCHEMA-PATH";

    private static final String VALIDATE_REPOSITORY_POLICIES_KEY =
            "VALIDATE-REPOSITORY-POLICIES";

    private static final String VALIDATE_OBJECT_POLICIES_FROM_DATASTREAM_KEY =
            "VALIDATE-OBJECT-POLICIES-FROM-DATASTREAM";

    private static final URI STRING_ATTRIBUTE = URI.create(StringAttribute.identifier);
    
    private static final URI EMPTY_URI = URI.create("");

    @SuppressWarnings("unchecked")
    private static final PolicySet EMPTY_SET = toPolicySet(Collections.EMPTY_LIST, new OrderedDenyOverridesPolicyAlg());
    
    private final PolicyCombiningAlgorithm m_combiningAlgorithm;

    private final String m_serverHome;

    private final String m_repositoryPolicyDirectoryPath;

    private final String m_repositoryBackendPolicyDirectoryPath;

    private final boolean m_validateRepositoryPolicies;

    private final boolean m_validateObjectPoliciesFromDatastream;

    private final PolicyParser m_policyParser;
    
    private final PolicyLoader m_policyLoader;

    private final List<AbstractPolicy> m_repositoryPolicies;

    private PolicySet m_repositoryPolicySet = EMPTY_SET;

    public PolicyFinderModule(Server server,
                              PolicyLoader policyLoader,
                              ModuleConfiguration authorizationConfig)
            throws GeneralException {
        m_serverHome = server.getHomeDir().getAbsolutePath();

        m_policyLoader = policyLoader;

        m_repositoryBackendPolicyDirectoryPath = m_serverHome + File.separator
                + BACKEND_POLICIES_ACTIVE_DIRECTORY;

        String repositoryPolicyDirectoryPath =
                authorizationConfig.getParameter(REPOSITORY_POLICIES_DIRECTORY_KEY, true);
        if (repositoryPolicyDirectoryPath == null) repositoryPolicyDirectoryPath = "";
        m_repositoryPolicyDirectoryPath = repositoryPolicyDirectoryPath;

        String combAlgClass = authorizationConfig.getParameter(COMBINING_ALGORITHM_KEY);
        if (combAlgClass == null) combAlgClass = DEFAULT_XACML_COMBINING_ALGORITHM;
                    
        try {
            m_combiningAlgorithm =
                    (PolicyCombiningAlgorithm) Class
                            .forName(combAlgClass).newInstance();
        } catch (Exception e) {
            throw new GeneralException(e.getMessage(), e);
        }

        String validatePolicies = authorizationConfig.getParameter(VALIDATE_REPOSITORY_POLICIES_KEY);
        try {
            m_validateRepositoryPolicies = (validatePolicies != null) ? Boolean.parseBoolean(validatePolicies) : false;
        } catch (Exception e) {
            throw new GeneralException("bad init parm boolean value for "
                                                    + VALIDATE_REPOSITORY_POLICIES_KEY, e);
        }

        validatePolicies = authorizationConfig.getParameter(VALIDATE_OBJECT_POLICIES_FROM_DATASTREAM_KEY);
        try {
            m_validateObjectPoliciesFromDatastream = (validatePolicies != null) ? Boolean.parseBoolean(validatePolicies) : false;
        } catch (Exception e) {
            throw new GeneralException("bad init parm boolean value for "
                                                    + VALIDATE_OBJECT_POLICIES_FROM_DATASTREAM_KEY, e);
        }

        // Initialize the policy parser given the POLICY_SCHEMA_PATH_KEY
        String schemaPath = authorizationConfig.getParameter(POLICY_SCHEMA_PATH_KEY);
        if (schemaPath != null) {
            File schema;
            if (schemaPath.startsWith(File.separator)){ // absolute
                schema = new File(schemaPath);
            } else {
                schema = new File(new File(m_serverHome), schemaPath);
            }
            try {
                FileInputStream in = new FileInputStream(schema);
                m_policyParser = new PolicyParser(in);
                ValidationUtility.setPolicyParser(m_policyParser);
            } catch (Exception e) {
                throw new GeneralException("Error loading policy"
                                                        + " schema: " + schema.getAbsolutePath(), e);
            }
        } else {
            throw new GeneralException("Policy schema path not"
                                                    + " specified.  Must be given as " + POLICY_SCHEMA_PATH_KEY);
        }

        m_repositoryPolicies = new ArrayList<AbstractPolicy>();
    }

    /**
     * Does nothing at init time.
     */
    @Override
    public void init(PolicyFinder finder) {
        try {
            logger.info("Loading repository policies...");
            setupActivePolicyDirectories();
            m_repositoryPolicies.clear();
            Map<String,AbstractPolicy> repositoryPolicies =
                    m_policyLoader.loadPolicies(m_policyParser,
                    m_validateRepositoryPolicies,
                    new File(m_repositoryBackendPolicyDirectoryPath));
            repositoryPolicies.putAll(
                    m_policyLoader.loadPolicies(m_policyParser,
                                 m_validateRepositoryPolicies,
                                 new File(m_repositoryPolicyDirectoryPath)));
            m_repositoryPolicies.addAll(repositoryPolicies.values());
            m_repositoryPolicySet = toPolicySet(m_repositoryPolicies, m_combiningAlgorithm);
        } catch (Throwable t) {
            logger.error("Error loading repository policies: " + t.toString(), t);
        }
    }

    private final void generateBackendPolicies() throws Exception {
        logger.info("Generating backend policies...");
        FileUtils.deleteContents(new File(m_repositoryBackendPolicyDirectoryPath));
        BackendPolicies backendPolicies =
                new BackendPolicies(m_serverHome + File.separator
                                    + BE_SECURITY_XML_LOCATION);
        Hashtable<String, String> tempfiles = backendPolicies.generateBackendPolicies();
        try {
            Iterator<String> iterator = tempfiles.keySet().iterator();
            Transformer transformer = null;
            while (iterator.hasNext()) {
                if (transformer == null) {
                    File f =
                            new File(m_serverHome + File.separator
                                    + BACKEND_POLICIES_XSL_LOCATION); // <<stylesheet
                    // location
                    StreamSource ss = new StreamSource(f);
                    transformer = XmlTransformUtility.getTransformer(ss); // xformPath
                } else {
                    transformer.reset();
                }
                String key = iterator.next();
                File infile = new File(tempfiles.get(key));
                FileInputStream fis = new FileInputStream(infile);
                FileOutputStream fos =
                        new FileOutputStream(m_repositoryBackendPolicyDirectoryPath
                                             + File.separator + key);
                transformer.transform(new StreamSource(fis),
                                      new StreamResult(fos));
            }
        } finally {
            // we're done with temp files now, so delete them
            Iterator<String> iter = tempfiles.keySet().iterator();
            while (iter.hasNext()) {
                File tempFile = new File(tempfiles.get(iter.next()));
                tempFile.delete();
            }
        }
    }

    private void setupActivePolicyDirectories() throws Exception {
        File repoPolicyDir = new File(m_repositoryPolicyDirectoryPath + File.separator + DEFAULT);
        if (!repoPolicyDir.exists()){
            repoPolicyDir.mkdirs();
            File source = new File(m_serverHome + File.separator + DEFAULT_REPOSITORY_POLICIES_DIRECTORY);
            FileUtils.copy(source, repoPolicyDir);
        }
        generateBackendPolicies();
    }


    /**
     * Always returns true, indicating that this impl supports finding policies
     * based on a request.
     */
    @Override
    public boolean isRequestSupported() {
        return true;
    }

    /**
     * Gets a deny-biased policy set that includes all repository-wide and
     * object-specific policies.
     */
    @Override
    public PolicyFinderResult findPolicy(EvaluationCtx context) {
        PolicyFinderResult policyFinderResult = null;
        PolicySet policySet = m_repositoryPolicySet;
        try {
            String pid = getPid(context);
            if (pid != null && !pid.isEmpty()) {
                AbstractPolicy objectPolicyFromObject = 
                        m_policyLoader.loadObjectPolicy(m_policyParser.copy(),
                                                         pid,
                                                         m_validateObjectPoliciesFromDatastream);
                if (objectPolicyFromObject != null) {
                    List<AbstractPolicy> policies = new ArrayList<AbstractPolicy>(m_repositoryPolicies);
                    policies.add(objectPolicyFromObject);
                    policySet = toPolicySet(policies, m_combiningAlgorithm);
                }
            }
            policyFinderResult = new PolicyFinderResult(policySet);
        } catch (Exception e) {
            logger.warn("PolicyFinderModule seriously failed to evaluate a policy ", e);
            policyFinderResult =
                    new PolicyFinderResult(new Status(ERROR_CODE_LIST, e
                            .getMessage()));
        }
        return policyFinderResult;
    }

    // get the pid from the context, or null if unable
    public static String getPid(EvaluationCtx context) {
        EvaluationResult attribute
                = context.getResourceAttribute(STRING_ATTRIBUTE,
                        Constants.OBJECT.PID.attributeId,
                                               null);
        BagAttribute element = getAttributeFromEvaluationResult(attribute);
        if (element == null) {
            logger.debug("PolicyFinderModule:getPid exit on can't get pid on request callback");
            return null;
        }

        if (!(element.getType().equals(STRING_ATTRIBUTE))) {
            logger.debug("PolicyFinderModule:getPid exit on couldn't get pid from xacml request non-string returned");
            return null;
        }

        return (element.size() == 1) ? (String) element.getValue() : null;
    }

    // copy of code in AttributeFinderModule; consider refactoring
    private static final BagAttribute getAttributeFromEvaluationResult(EvaluationResult attribute) {
        if (attribute.indeterminate()) {
            return null;
        }

        if (attribute.getStatus() != null
                && !Status.STATUS_OK.equals(attribute.getStatus())) {
            return null;
        }

        AttributeValue attributeValue = attribute.getAttributeValue();
        if (!(attributeValue instanceof BagAttribute)) {
            return null;
        }

        return (BagAttribute) attributeValue;
    }

    private static PolicySet toPolicySet(List<AbstractPolicy> policies, PolicyCombiningAlgorithm alg) {
        return new PolicySet(EMPTY_URI,
                              alg,
                              null /*
                               * no general target beyond those of
                               * multiplexed individual policies
                               */,
                              policies);
    }
}
