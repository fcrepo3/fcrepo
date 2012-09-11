package org.fcrepo.server.security.xacml.pdp.finder.policy;

import java.io.InputStream;
import java.io.OutputStream;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPathException;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.Indenter;
import com.sun.xacml.MatchResult;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.ctx.Result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.policy.ActionNamespace;
import org.fcrepo.common.policy.ObjectNamespace;
import org.fcrepo.common.policy.SubjectNamespace;

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
        // embargo is trivial if these are done
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
        URI att = null;
        try {
            att = new URI(ObjectNamespace.getInstance().PID.uri);
        } catch (Throwable t) { } //shaddup
        String rPid = getResourceAttribute(att, context);
        if (rPid.equals(this.pid)) {
            LOGGER.info("Request pid match rightsMetadata object pid " + this.pid);
            return new MatchResult(MatchResult.MATCH);
        }
        LOGGER.info("Request pid \"" + rPid +  "\" did not match rightsMetadata object pid " + this.pid);
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
        URI action = null;
        URI subject = null;
        try {
            action = new URI(ActionNamespace.getInstance().ID.uri);
            subject = new URI(SubjectNamespace.getInstance().LOGIN_ID.uri);
        } catch (Throwable t) {};
        String rAction = getActionAttribute(action, context);
        String mAction = this.actionMap.get(rAction);
        int result = Result.DECISION_INDETERMINATE;
        if (this.assertions.containsKey(mAction)) {
            String rSubject = getSubjectAttribute(subject, context);
            if (this.assertions.get(mAction).contains(rSubject)) {
                LOGGER.info("Permitting " + rSubject + " to " + rAction + " for being on the list!");
                result = Result.DECISION_PERMIT;
            } else {
                LOGGER.info("Denying " + rSubject + " to " + rAction + " for not being on the list!");
                result = Result.DECISION_DENY;
            }
        } else LOGGER.info("Could not find subjects for mapped action " + mAction + " from requested action " + rAction);
        return new Result(result);
    }
    
    private String getActionAttribute(URI att, EvaluationCtx context) {
        String result = null;
        try{
            LOGGER.info("Requested attribute: " + att.toString());
            EvaluationResult eval = (context.getActionAttribute(new URI(StringAttribute.identifier), att, null));
            AttributeValue attVal = eval.getAttributeValue();
            if (attVal.isBag()) {
                List children = attVal.getChildren();
                if (children.size() > 0) {
                    AttributeValue c1 = (AttributeValue)attVal.getChildren().get(0);
                    result = c1.encode();
                }
            } else result = attVal.encode();
            LOGGER.info("Returning attribute value: " + result);
        } catch (URISyntaxException e) {
            LOGGER.error("Unexpected URI syntax problem: " + StringAttribute.identifier,e);
        }
        return result;
    }
    private String getSubjectAttribute(URI att, EvaluationCtx context) {
        String result = null;
        try{
            LOGGER.info("Requested attribute: " + att.toString());
            EvaluationResult eval = (context.getSubjectAttribute(new URI(StringAttribute.identifier), att, null));
            AttributeValue attVal = eval.getAttributeValue();
            if (attVal.isBag()) {
                List children = attVal.getChildren();
                if (children.size() > 0) {
                    AttributeValue c1 = (AttributeValue)attVal.getChildren().get(0);
                    result = c1.encode();
                }
            } else result = attVal.encode();
            LOGGER.info("Returning attribute value: " + result);
        } catch (URISyntaxException e) {
            LOGGER.error("Unexpected URI syntax problem: " + StringAttribute.identifier,e);
        }
        return result;
    }
    private String getResourceAttribute(URI att, EvaluationCtx context) {
        String result = null;
        try{
            LOGGER.info("Requested attribute: " + att.toString());
            EvaluationResult eval = (context.getResourceAttribute(new URI(StringAttribute.identifier), att, null));
            AttributeValue attVal = eval.getAttributeValue();
            if (attVal.isBag()) {
                List children = attVal.getChildren();
                if (children.size() > 0) {
                    AttributeValue c1 = (AttributeValue)attVal.getChildren().get(0);
                    result = c1.encode();
                }
            } else result = attVal.encode();
            LOGGER.info("Returning attribute value: " + result);
        } catch (URISyntaxException e) {
            LOGGER.error("Unexpected URI syntax problem: " + StringAttribute.identifier,e);
        }
        return result;
    }
}

