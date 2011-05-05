/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Hashtable;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeDesignator;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.cond.EvaluationResult;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bill Niebel
 */
class TestAttributeFinderModule
        extends AttributeFinderModule {

    private static final Logger logger =
            LoggerFactory.getLogger(TestAttributeFinderModule.class);
    public static final String ATTRIBUTE_ID = Constants.ENVIRONMENT.uri + ":springConfigured";
    public static final String ATTRIBUTE_VALUE = "demo:5";
    @Override
    protected boolean canHandleAdhoc() {
        return true;
    }

    static private final TestAttributeFinderModule singleton =
            new TestAttributeFinderModule();

    private TestAttributeFinderModule() {
        super();
        try {
            registerSupportedDesignatorType(AttributeDesignator.ENVIRONMENT_TARGET);

            registerAttribute(ATTRIBUTE_ID, StringAttribute.identifier);

            attributesDenied.add(PolicyEnforcementPoint.XACML_SUBJECT_ID);
            attributesDenied.add(PolicyEnforcementPoint.XACML_ACTION_ID);
            attributesDenied.add(PolicyEnforcementPoint.XACML_RESOURCE_ID);

            attributesDenied.add(Constants.ACTION.CONTEXT_ID.uri);
            attributesDenied.add(Constants.SUBJECT.LOGIN_ID.uri);
            attributesDenied.add(Constants.ACTION.ID.uri);
            attributesDenied.add(Constants.ACTION.API.uri);

            setInstantiatedOk(true);
        } catch (URISyntaxException e1) {
            setInstantiatedOk(false);
        }
    }

    static public final TestAttributeFinderModule getInstance() {
        return singleton;
    }

    @Override
    protected final Object getAttributeLocally(int designatorType,
                                               String attributeId,
                                               URI resourceCategory,
                                               EvaluationCtx ctx) {
        logger.debug("getAttributeLocally test");
        logger.debug("TestAttributeFinderModule attributeId=" + attributeId);
        Object values = null;
        logger.debug("designatorType" + designatorType);
        if (designatorType == AttributeDesignator.ENVIRONMENT_TARGET) {
            if (ATTRIBUTE_ID.equals(attributeId)) {
                values = ATTRIBUTE_VALUE;
            } else {
                values = null;
            }
        } else {
            values = null;
        }
        if (values instanceof String) {
            logger.debug("getAttributeLocally string value=" + (String) values);
        } else {
            logger.debug("getAttributeLocally object value=" + values);
        }
        return values;
    }

}
