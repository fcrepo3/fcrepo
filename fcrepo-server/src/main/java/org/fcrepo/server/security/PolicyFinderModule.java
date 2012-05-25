/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.fcrepo.common.Constants;
import org.fcrepo.common.FaultException;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.ObjectNotInLowlevelStorageException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.errors.ValidationException;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.RepositoryReader;
import org.fcrepo.server.storage.types.Datastream;
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

    private static final String XACML_DIST_BASE = "fedora-internal-use";

    private static final String DEFAULT_REPOSITORY_POLICIES_DIRECTORY =
            XACML_DIST_BASE
            + "/fedora-internal-use-repository-policies-approximating-2.0";

    private static final String BE_SECURITY_XML_LOCATION =
            "config/beSecurity.xml";

    private static final String BACKEND_POLICIES_XSL_LOCATION =
            XACML_DIST_BASE + "/build-backend-policy.xsl";

    private final String m_combiningAlgorithm;

    private final String m_serverHome;

    private final String m_repositoryPolicyDirectoryPath;

    private final String m_repositoryBackendPolicyDirectoryPath;

    private final RepositoryReader m_repoReader;

    private final boolean m_validateRepositoryPolicies;

    private final boolean m_validateObjectPoliciesFromDatastream;

    private final PolicyParser m_policyParser;

    private final List<AbstractPolicy> m_repositoryPolicies;

    public PolicyFinderModule(String serverHome,
                              String combiningAlgorithm,
                              String repositoryPolicyDirectoryPath,
                              String repositoryBackendPolicyDirectoryPath,
                              String repositoryPolicyGuiToolDirectoryPath,
                              RepositoryReader repoReader,
                              boolean validateRepositoryPolicies,
                              boolean validateObjectPoliciesFromDatastream,
                              PolicyParser policyParser)
            throws GeneralException {
        m_serverHome = serverHome;
        m_repositoryPolicyDirectoryPath = repositoryPolicyDirectoryPath;
        m_repositoryBackendPolicyDirectoryPath = repositoryBackendPolicyDirectoryPath;
        m_combiningAlgorithm = combiningAlgorithm;
        m_repoReader = repoReader;
        m_validateRepositoryPolicies = validateRepositoryPolicies;
        m_validateObjectPoliciesFromDatastream = validateObjectPoliciesFromDatastream;
        m_policyParser = policyParser;

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
            m_repositoryPolicies.addAll(
                    loadPolicies(m_policyParser,
                                 m_validateRepositoryPolicies,
                                 new File(m_repositoryPolicyDirectoryPath)));
            m_repositoryPolicies.addAll(
                    loadPolicies(m_policyParser,
                                 m_validateRepositoryPolicies,
                                 new File(m_repositoryBackendPolicyDirectoryPath)));
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
        Hashtable tempfiles = backendPolicies.generateBackendPolicies();
        TransformerFactory tfactory = XmlTransformUtility.getTransformerFactory();
        try {
            Iterator iterator = tempfiles.keySet().iterator();
            while (iterator.hasNext()) {
                File f =
                        new File(m_serverHome + File.separator
                                 + BACKEND_POLICIES_XSL_LOCATION); // <<stylesheet
                // location
                StreamSource ss = new StreamSource(f);
                Transformer transformer = tfactory.newTransformer(ss); // xformPath
                String key = (String) iterator.next();
                File infile = new File((String) tempfiles.get(key));
                FileInputStream fis = new FileInputStream(infile);
                FileOutputStream fos =
                        new FileOutputStream(m_repositoryBackendPolicyDirectoryPath
                                             + File.separator + key);
                transformer.transform(new StreamSource(fis),
                                      new StreamResult(fos));
            }
        } finally {
            // we're done with temp files now, so delete them
            Iterator iter = tempfiles.keySet().iterator();
            while (iter.hasNext()) {
                File tempFile = new File((String) tempfiles.get(iter.next()));
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
            if (pid != null && !"".equals(pid)) {
                AbstractPolicy objectPolicyFromObject = loadObjectPolicy(pid);
                if (objectPolicyFromObject != null) {
                    policies.add(objectPolicyFromObject);
                }
            }
            PolicyCombiningAlgorithm policyCombiningAlgorithm =
                    (PolicyCombiningAlgorithm) Class
                            .forName(m_combiningAlgorithm).newInstance();
            PolicySet policySet =
                    new PolicySet(new URI(""),
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

    // if the object exists and has a POLICY datastream, parse and return it
    private AbstractPolicy loadObjectPolicy(String pid) throws ServerException {
        try {
            DOReader reader = m_repoReader.getReader(Server.USE_DEFINITIVE_STORE,
                                                     ReadOnlyContext.EMPTY,
                                                     pid);
            Datastream ds = reader.GetDatastream("POLICY", null);
            if (ds != null) {
                logger.debug("Using POLICY for " + pid);
                return m_policyParser //TODO performance hole. Each copy() performs schema parsing, which is expensive
                        .copy().parse(ds.getContentStream(),
                                      m_validateObjectPoliciesFromDatastream);
            } else {
                return null;
            }
        } catch (ObjectNotInLowlevelStorageException e) {
            return null;
        }
    }

    // get the pid from the context, or null if unable
    public static String getPid(EvaluationCtx context) {
        URI resourceIdType = null;
        URI resourceIdId = null;
        try {
            resourceIdType = new URI(StringAttribute.identifier);
            resourceIdId = new URI(Constants.OBJECT.PID.uri);
        } catch (URISyntaxException e) {
            throw new FaultException("Bad URI syntax", e);
        }
        EvaluationResult attribute
                = context.getResourceAttribute(resourceIdType,
                                               resourceIdId,
                                               null);
        Object element = getAttributeFromEvaluationResult(attribute);
        if (element == null) {
            logger.debug("PolicyFinderModule:getPid exit on "
                    + "can't get contextId on request callback");
            return null;
        }

        if (!(element instanceof StringAttribute)) {
            logger.debug("PolicyFinderModule:getPid exit on "
                    + "couldn't get contextId from xacml request "
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

    // load and parse all policies (*.xml) from a given directory, recursively
    private static List<AbstractPolicy> loadPolicies(PolicyParser parser,
                                                     boolean validate,
                                                     File dir)
            throws IOException, ValidationException {
        List<AbstractPolicy> policies = new ArrayList<AbstractPolicy>();
        for (File file: dir.listFiles()) {
            if (file.isDirectory()) {
                policies.addAll(loadPolicies(parser, validate, file));
            } else {
                if (file.getName().endsWith(".xml")) {
                    logger.info("Loading policy: " + file.getPath());
                    InputStream policyStream = new FileInputStream(file);
                    policies.add(parser.parse(policyStream, validate));
                }
            }
        }
        return policies;
    }

}
