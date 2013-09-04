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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
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

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.PolicySet;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.combine.PolicyCombiningAlgorithm;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.ctx.Status;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderResult;

/**
 * XACML PolicyFinder for Fedora.
 * <p>
 * This provides repository-wide policies and object-specific policies,
 * when available.
 */
public class PolicyFinderModule
        extends com.sun.xacml.finder.PolicyFinderModule {

    private static final Logger logger =
            LoggerFactory.getLogger(PolicyFinderModule.class);

    private static final List<String> ERROR_CODE_LIST = new ArrayList<String>(1);

    static {
        ERROR_CODE_LIST.add(Status.STATUS_PROCESSING_ERROR);
    }

    private static final String DEFAULT = "default";

    private static final String DEFAULT_XACML_COMBINING_ALGORITHM = "com.sun.xacml.combine.OrderedDenyOverridesPolicyAlg";

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
    
    private final String m_combiningAlgorithm;

    private final String m_serverHome;

    private final String m_repositoryPolicyDirectoryPath;

    private final String m_repositoryBackendPolicyDirectoryPath;

    private final boolean m_validateRepositoryPolicies;

    private final boolean m_validateObjectPoliciesFromDatastream;

    private final PolicyParser m_policyParser;
    
    private final PolicyLoader m_policyLoader;

    private final List<AbstractPolicy> m_repositoryPolicies;

    public PolicyFinderModule(Server server,
                              PolicyLoader policyLoader,
                              ModuleConfiguration authorizationConfig)
            throws GeneralException {
        m_serverHome = server.getHomeDir().getAbsolutePath();

        m_policyLoader = policyLoader;
        
        Map<String,String> moduleParameters = authorizationConfig.getParameters();

        m_repositoryBackendPolicyDirectoryPath = m_serverHome + File.separator
                + BACKEND_POLICIES_ACTIVE_DIRECTORY;

        if (moduleParameters.containsKey(REPOSITORY_POLICIES_DIRECTORY_KEY)) {
            m_repositoryPolicyDirectoryPath =
                    authorizationConfig.getParameter(REPOSITORY_POLICIES_DIRECTORY_KEY, true);
        } else {
            m_repositoryPolicyDirectoryPath = "";
        }

        if (moduleParameters.containsKey(COMBINING_ALGORITHM_KEY)) {
            m_combiningAlgorithm =
                    moduleParameters.get(COMBINING_ALGORITHM_KEY);
        } else {
            m_combiningAlgorithm = DEFAULT_XACML_COMBINING_ALGORITHM;
        }

        if (moduleParameters.containsKey(VALIDATE_REPOSITORY_POLICIES_KEY)) {
            m_validateRepositoryPolicies =
                    Boolean.parseBoolean(moduleParameters
                            .get(VALIDATE_REPOSITORY_POLICIES_KEY));
        } else {
            m_validateRepositoryPolicies = false;
        }
        if (moduleParameters
                .containsKey(VALIDATE_OBJECT_POLICIES_FROM_DATASTREAM_KEY)) {
            try {
                m_validateObjectPoliciesFromDatastream =
                        Boolean.parseBoolean(moduleParameters
                                .get(VALIDATE_OBJECT_POLICIES_FROM_DATASTREAM_KEY));
            } catch (Exception e) {
                throw new GeneralException("bad init parm boolean value for "
                                                        + VALIDATE_OBJECT_POLICIES_FROM_DATASTREAM_KEY,
                                                        e);
            }
        } else {
            m_validateObjectPoliciesFromDatastream = false;
        }

        // Initialize the policy parser given the POLICY_SCHEMA_PATH_KEY
        if (moduleParameters.containsKey(POLICY_SCHEMA_PATH_KEY)) {
            String schemaPath = moduleParameters.get(POLICY_SCHEMA_PATH_KEY);
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
        TransformerFactory tfactory = XmlTransformUtility.getTransformerFactory();
        try {
            Iterator<String> iterator = tempfiles.keySet().iterator();
            while (iterator.hasNext()) {
                File f =
                        new File(m_serverHome + File.separator
                                 + BACKEND_POLICIES_XSL_LOCATION); // <<stylesheet
                // location
                StreamSource ss = new StreamSource(f);
                Transformer transformer = tfactory.newTransformer(ss); // xformPath
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
            // return the transformerFactory
            XmlTransformUtility.returnTransformerFactory(tfactory);
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
        try {
            List<AbstractPolicy> policies = new ArrayList<AbstractPolicy>(m_repositoryPolicies);
            String pid = getPid(context);
            if (pid != null && !pid.isEmpty()) {
                AbstractPolicy objectPolicyFromObject = 
                        m_policyLoader.loadObjectPolicy(m_policyParser.copy(),
                                                         pid,
                                                         m_validateObjectPoliciesFromDatastream);
                if (objectPolicyFromObject != null) {
                    policies.add(objectPolicyFromObject);
                }
            }
            PolicyCombiningAlgorithm policyCombiningAlgorithm =
                    (PolicyCombiningAlgorithm) Class
                            .forName(m_combiningAlgorithm).newInstance();
            PolicySet policySet =
                    new PolicySet(EMPTY_URI,
                                  policyCombiningAlgorithm,
                                  null /*
                                   * no general target beyond those of
                                   * multiplexed individual policies
                                   */,
                                  policies);
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
                        Constants.OBJECT.PID.getURI(),
                                               null);
        Object element = getAttributeFromEvaluationResult(attribute);
        if (element == null) {
            logger.debug("PolicyFinderModule:getPid exit on "
                    + "can't get pid on request callback");
            return null;
        }

        if (!(element instanceof StringAttribute)) {
            logger.debug("PolicyFinderModule:getPid exit on "
                    + "couldn't get pid from xacml request "
                    + "non-string returned");
            return null;
        }

        return ((StringAttribute) element).getValue();
    }

    // copy of code in AttributeFinderModule; consider refactoring
    private static final Object getAttributeFromEvaluationResult(EvaluationResult attribute) {
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

        BagAttribute bag = (BagAttribute) attributeValue;
        if (1 != bag.size()) {
            return null;
        } else {
            return bag.iterator().next();
        }
    }

}
