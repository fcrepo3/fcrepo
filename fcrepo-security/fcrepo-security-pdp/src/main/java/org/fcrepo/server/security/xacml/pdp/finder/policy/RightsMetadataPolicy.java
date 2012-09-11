package org.fcrepo.server.security.xacml.pdp.finder.policy;

import java.io.InputStream;
import java.io.OutputStream;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.Indenter;
import com.sun.xacml.MatchResult;
import com.sun.xacml.ctx.Result;

public class RightsMetadataPolicy
extends AbstractPolicy {
    public RightsMetadataPolicy(InputStream in) {

    }

    @Override
    public void encode(OutputStream arg0) {

    }

    @Override
    public void encode(OutputStream arg0, Indenter arg1) {

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
        return null;
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
        return null;
    }

}

