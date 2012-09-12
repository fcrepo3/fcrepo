package org.fcrepo.server.security.xacml.pdp.finder.policy.rightsMetadata;

import java.io.InputStream;
import java.io.OutputStream;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPathException;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.Indenter;
import com.sun.xacml.MatchResult;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Status;
import com.sun.xacml.ctx.Subject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;
import org.fcrepo.common.policy.ObjectNamespace;

public class RightsMetadataPolicy
extends AbstractPolicy {
    private static final Logger LOGGER = LoggerFactory.getLogger(RightsMetadataPolicy.class);;
    private final String pid;
    private final Map<String,String> actionMap;
    private Map<String,Set<String>> assertions = new HashMap<String,Set<String>>();
    public RightsMetadataPolicy(String pid,
                                Map<String,String> actionMap,
                                InputStream is)
           throws XPathException {
        // resource-id mapped to {pid}
        // action-id mapped to /rightsMetadata/access@type
        // subject-id mapped to /rightsMetadata/access/machine/user
        // fedoraRole mapped to /rightsMetadata/access/machine/group
        // how to adopt /rightsMetadata/access/machine/world ? maybe '*' as user
        // embargo is trivial if these are done
        // does discover < read < edit < admin ?
        // create is a bit of a misnomer in rightsMD: indicates associations, not creation
        // -- given that, it requires some tree-walking
        this.pid = pid;
        this.actionMap = actionMap;
        LOGGER.info("creating rightsMetadata policy for object " + pid);
        this.assertions = new RightsMetadataDocument(is).getActionSubjectMap();
        for (String key: this.assertions.keySet()) {
            LOGGER.info("found action: " + key);
        }
    }

    @Override
    public void encode(OutputStream arg0) {
        // who cares
    }

    @Override
    public void encode(OutputStream arg0, Indenter arg1) {
        // who cares
    }

    /**
     * Given the input context sees whether or not the request matches this
     * policy. This must be called by combining algorithms before they
     * evaluate a policy. This is also used in the initial policy finding
     * operation to determine which top-level policies might apply to the
     * request.
     *
     * @param context the representation of the request
     *
     * @return the result of trying to match the policy and the request
     */
    @Override
    public MatchResult match(EvaluationCtx context) {
        // getResourceAttribute(String type, URI att, URI issuer)
        URI att = Constants.OBJECT.PID.getURI();
        String rPid = getResourceAttribute(att, context);
        if (rPid.equals(this.pid)) {
            return new MatchResult(MatchResult.MATCH);
        }
        LOGGER.debug("Request pid \"{}\" did not match rightsMetadata object pid {}", rPid, this.pid);
        return new MatchResult(MatchResult.NO_MATCH);
    }

    /**
     * Tries to evaluate the policy by calling the combining algorithm on
     * the given policies or rules. The <code>match</code> method must always
     * be called first, and must always return MATCH, before this method
     * is called.
     *
     * @param context the representation of the request
     *
     * @return the result of evaluation
     */
    @Override
    public Result evaluate(EvaluationCtx context) {
        URI action = Constants.ACTION.ID.getURI();
        URI subject = Constants.SUBJECT.LOGIN_ID.getURI();
        String rAction = getActionAttribute(action, context);
        String mAction = this.actionMap.get(rAction);
        int result = Result.DECISION_INDETERMINATE;
        if (this.assertions.containsKey(mAction)) {
            String rSubject = getSubjectAttribute(subject, context);
            if (this.assertions.get(mAction).contains(rSubject)) {
                LOGGER.debug("Permitting {} to {} for being on the list!", rSubject, rAction);
                result = Result.DECISION_PERMIT;
            } else {
                LOGGER.debug("Denying {} to {} for not being on the list!", rSubject, rAction);
                result = Result.DECISION_DENY;
            }
        } else LOGGER.debug("Could not find subjects for mapped action {} from requested action {}", mAction, rAction);
        return new Result(result);
    }
    
    private String getActionAttribute(URI att, EvaluationCtx context) {
        String result = null;
        try{
            LOGGER.debug("Requested attribute: {}", att.toString());
            EvaluationResult eval =
                    (context.getActionAttribute(new URI(StringAttribute.identifier),
                                                att,
                                                null));
            result = getAttributeFromEvaluationResult(eval);
            LOGGER.debug("Returning attribute value: {}", result);
        } catch (URISyntaxException e) {
            LOGGER.error("Unexpected URI syntax problem: " + StringAttribute.identifier,e);
        }
        return result;
    }
    private String getSubjectAttribute(URI att, EvaluationCtx context) {
        String result = null;
        try{
            LOGGER.debug("Requested attribute: {}", att.toString());
            // getSubjectAttribute(URI type, URI id, URI category)
            // unlike action or resource attributes, subjects are added to a default category by sunxacml
            EvaluationResult eval = (context.getSubjectAttribute(new URI(StringAttribute.identifier), att, Subject.DEFAULT_CATEGORY));
            result = getAttributeFromEvaluationResult(eval);
            LOGGER.debug("Returning attribute value: {}", result);
        } catch (URISyntaxException e) {
            LOGGER.error("Unexpected URI syntax problem: " + StringAttribute.identifier,e);
        }
        return result;
    }
    private String getResourceAttribute(URI att, EvaluationCtx context) {
        String result = null;
        try{
            LOGGER.debug("Requested attribute: {}", att.toString());
            EvaluationResult eval = (context.getResourceAttribute(new URI(StringAttribute.identifier), att, null));
            result = getAttributeFromEvaluationResult(eval);
            LOGGER.debug("Returning attribute value: {}", result);
        } catch (URISyntaxException e) {
            LOGGER.error("Unexpected URI syntax problem: " + StringAttribute.identifier,e);
        }
        return result;
    }

    protected String iAm() {
        return this.getClass().getName();
    }


    protected final String getAttributeFromEvaluationResult(EvaluationResult attribute /*
     * URI
     * type,
     * URI
     * id,
     * URI
     * category,
     * EvaluationCtx
     * context
     */) {
        if (attribute.indeterminate()) {
            LOGGER.debug("AttributeFinder:getAttributeFromEvaluationCtx" + iAm()
                    + " exit on "
                    + "couldn't get resource attribute from xacml request "
                    + "indeterminate");
            return null;
        }

        if (attribute.getStatus() != null
                && !Status.STATUS_OK.equals(attribute.getStatus())) {
            LOGGER.debug("AttributeFinder:getAttributeFromEvaluationCtx" + iAm()
                    + " exit on "
                    + "couldn't get resource attribute from xacml request "
                    + "bad status");
            return null;
        } // (resourceAttribute.getStatus() == null) == everything is ok

        AttributeValue attributeValue = attribute.getAttributeValue();
        if (!(attributeValue instanceof BagAttribute)) {
            LOGGER.debug("AttributeFinder:getAttributeFromEvaluationCtx" + iAm()
                    + " exit on "
                    + "couldn't get resource attribute from xacml request "
                    + "no bag");
            return null;
        }

        BagAttribute bag = (BagAttribute) attributeValue;
        if (1 != bag.size()) {
            LOGGER.debug("AttributeFinder:getAttributeFromEvaluationCtx" + iAm()
                    + " exit on "
                    + "couldn't get resource attribute from xacml request "
                    + "wrong bag n=" + bag.size());
            return null;
        }

        Iterator it = bag.iterator();
        Object element = it.next();

        if (element == null) {
            LOGGER.debug("AttributeFinder:getAttributeFromEvaluationCtx" + iAm()
                    + " exit on "
                    + "couldn't get resource attribute from xacml request "
                    + "null returned");
            return null;
        }

        if (it.hasNext()) {
            LOGGER.debug("AttributeFinder:getAttributeFromEvaluationCtx" + iAm()
                    + " exit on "
                    + "couldn't get resource attribute from xacml request "
                    + "too many returned");
            LOGGER.debug(element.toString());
            while (it.hasNext()) {
                LOGGER.debug(it.next().toString());
            }
            return null;
        }

        LOGGER.debug("AttributeFinder:getAttributeFromEvaluationCtx " + iAm()
                + " returning " + element.toString());
        return ((StringAttribute)element).getValue();
    }
}

