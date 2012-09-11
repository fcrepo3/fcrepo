package org.fcrepo.server.security.xacml.pdp.finder.policy;

import java.io.InputStream;
import java.io.OutputStream;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.Indenter;
import com.sun.xacml.MatchResult;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.ctx.Result;

import org.fcrepo.common.policy.ActionNamespace;
import org.fcrepo.common.policy.ObjectNamespace;
import org.fcrepo.common.policy.SubjectNamespace;

public class RightsMetadataPolicy
extends AbstractPolicy {
    private final String pid;
    private final Map<String,String> actionMap;
    private Map<String,Set<String>> assertions = new HashMap<String,Set<String>>();
    public RightsMetadataPolicy(String pid, Map<String,String> actionMap, InputStream in) {
        // resource-id mapped to {pid}
        // action-id mapped to /rightsMetadata/access@type
        // subject-id mapped to /rightsMetadata/access/machine/user
        // embargo is trivial if these are done
        this.pid = pid;
        this.actionMap = actionMap;

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
        String rPid = getStringAttribute(att, context);
        if (rPid.equals(this.pid)) {
            return new MatchResult(MatchResult.MATCH);
        }
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
        String rAction = getStringAttribute(action, context);
        String mAction = this.actionMap.get(rAction);
        int result = Result.DECISION_INDETERMINATE;
        if (this.assertions.containsKey(mAction)) {
            String rSubject = getStringAttribute(subject, context);
            if (this.assertions.get(mAction).contains(rSubject)) {
                result = Result.DECISION_PERMIT;
            } else {
                result = Result.DECISION_DENY;
            }
        }
        return new Result(result);
    }
    
    private String getStringAttribute(URI att, EvaluationCtx context) {
        try{
            EvaluationResult result = (context.getResourceAttribute(new URI(StringAttribute.identifier), att, null));
            return result.getAttributeValue().encode();
        } catch (URISyntaxException e) {
            return null;
        }
    }

}

