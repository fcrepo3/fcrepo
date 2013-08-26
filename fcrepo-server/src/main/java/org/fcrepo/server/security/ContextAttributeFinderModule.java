/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security;

import java.net.URI;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeDesignator;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.cond.EvaluationResult;

/**
 * @author Bill Niebel
 */
class ContextAttributeFinderModule
        extends AttributeFinderModule {

    private static final Logger logger =
            LoggerFactory.getLogger(ContextAttributeFinderModule.class);

    @Override
    protected boolean canHandleAdhoc() {
        return true;
    }

    private final ContextRegistry m_contexts;

    private ContextAttributeFinderModule(ContextRegistry contexts) {
        super();
        m_contexts = contexts;

        registerSupportedDesignatorType(AttributeDesignator.SUBJECT_TARGET);
        registerSupportedDesignatorType(AttributeDesignator.ACTION_TARGET); //<<??????
        registerSupportedDesignatorType(AttributeDesignator.RESOURCE_TARGET); //<<?????
        registerSupportedDesignatorType(AttributeDesignator.ENVIRONMENT_TARGET);

        registerAttribute(Constants.ENVIRONMENT.CURRENT_DATE_TIME);
        registerAttribute(Constants.ENVIRONMENT.CURRENT_DATE);
        registerAttribute(Constants.ENVIRONMENT.CURRENT_TIME);
        registerAttribute(Constants.HTTP_REQUEST.PROTOCOL);
        registerAttribute(Constants.HTTP_REQUEST.SCHEME);
        registerAttribute(Constants.HTTP_REQUEST.SECURITY);
        registerAttribute(Constants.HTTP_REQUEST.AUTHTYPE);
        registerAttribute(Constants.HTTP_REQUEST.METHOD);
        registerAttribute(Constants.HTTP_REQUEST.SESSION_ENCODING);
        registerAttribute(Constants.HTTP_REQUEST.SESSION_STATUS);
        registerAttribute(Constants.HTTP_REQUEST.CONTENT_LENGTH);
        registerAttribute(Constants.HTTP_REQUEST.CONTENT_TYPE);
        registerAttribute(Constants.HTTP_REQUEST.CLIENT_FQDN);
        registerAttribute(Constants.HTTP_REQUEST.CLIENT_IP_ADDRESS);
        registerAttribute(Constants.HTTP_REQUEST.SERVER_FQDN);
        registerAttribute(Constants.HTTP_REQUEST.SERVER_IP_ADDRESS);
        registerAttribute(Constants.HTTP_REQUEST.SERVER_PORT);

        denyAttribute(Constants.XACML1_SUBJECT.ID.attributeId);
        denyAttribute(Constants.XACML1_ACTION.ID.attributeId);
        denyAttribute(Constants.XACML1_RESOURCE.ID.attributeId);

        denyAttribute(Constants.ACTION.CONTEXT_ID.attributeId);
        denyAttribute(Constants.SUBJECT.LOGIN_ID.attributeId);
        denyAttribute(Constants.ACTION.ID.attributeId);
        denyAttribute(Constants.ACTION.API.attributeId);

        setInstantiatedOk(true);
    }

    private final String getContextId(EvaluationCtx context) {
        final URI contextIdType = STRING_ATTRIBUTE_TYPE_URI;
        final URI contextIdId = Constants.ACTION.CONTEXT_ID.getURI();

        logger.debug("ContextAttributeFinder:findAttribute"
                + " about to call getAttributeFromEvaluationCtx");

        EvaluationResult attribute =
                context.getActionAttribute(contextIdType, contextIdId, null);
        Object element = getAttributeFromEvaluationResult(attribute);
        if (element == null) {
            logger.debug("ContextAttributeFinder:getContextId" + " exit on "
                    + "can't get contextId on request callback");
            return null;
        }

        if (!(element instanceof StringAttribute)) {
            logger.debug("ContextAttributeFinder:getContextId" + " exit on "
                    + "couldn't get contextId from xacml request "
                    + "non-string returned");
            return null;
        }

        String contextId = ((StringAttribute) element).getValue();

        if (contextId == null) {
            logger.debug("ContextAttributeFinder:getContextId" + " exit on "
                    + "null contextId");
            return null;
        }

        if (!validContextId(contextId)) {
            logger.debug("ContextAttributeFinder:getContextId" + " exit on "
                    + "invalid context-id");
            return null;
        }

        return contextId;
    }

    private final boolean validContextId(String contextId) {
        if (contextId == null) {
            return false;
        }
        if ("".equals(contextId)) {
            return false;
        }
        if (" ".equals(contextId)) {
            return false;
        }
        return true;
    }

    @Override
    protected final Object getAttributeLocally(int designatorType,
                                               URI attributeId,
                                               URI resourceCategory,
                                               EvaluationCtx ctx) {
        logger.debug("getAttributeLocally context");
        String contextId = getContextId(ctx);
        logger.debug("contextId={} attributeID={}", contextId, attributeId);
        if (contextId == null || contextId.isEmpty()) {
            return null;
        }
        Context context = m_contexts.getContext(contextId);
        logger.debug("got context");
        Object values = null;
        logger.debug("designatorType{}", designatorType);
        switch (designatorType) {
            case AttributeDesignator.SUBJECT_TARGET:
                String attributeName = attributeId.toString();
                if (0 > context.nSubjectValues(attributeName)) {
                    values = null;
                } else {
                    if (logger.isDebugEnabled()) {
                    logger.debug("getting n values for {}={}", attributeId,
                            context.nSubjectValues(attributeName));
                    }
                    switch (context.nSubjectValues(attributeName)) {
                        case 0:
                            values = null;
                            /*
                             * values = new String[1]; ((String[])values)[0] =
                             * Authorization.UNDEFINED;
                             */
                            break;
                        case 1:
                            values = new String[1];
                            ((String[]) values)[0] =
                                    context.getSubjectValue(attributeName);
                            break;
                        default:
                            values = context.getSubjectValues(attributeName);
                    }
                    if (logger.isDebugEnabled()) {
                        if (values == null) {
                            logger.debug("RETURNING NO VALUES FOR " + attributeName);
                        } else {
                            StringBuffer sb = new StringBuffer();
                            sb.append("RETURNING " + ((String[]) values).length
                                    + " VALUES FOR " + attributeName + " ==");
                            for (int i = 0; i < ((String[]) values).length; i++) {
                                sb.append(" " + ((String[]) values)[i]);
                            }
                            logger.debug(sb.toString());
                        }
                    }
                }
                break;
            case AttributeDesignator.ACTION_TARGET:
                if (0 > context.nActionValues(attributeId)) {
                    values = null;
                } else {
                    switch (context.nActionValues(attributeId)) {
                        case 0:
                            values = null;
                            /*
                             * values = new String[1]; ((String[])values)[0] =
                             * Authorization.UNDEFINED;
                             */
                            break;
                        case 1:
                            values = new String[1];
                            ((String[]) values)[0] =
                                    context.getActionValue(attributeId);
                            break;
                        default:
                            values = context.getActionValues(attributeId);
                    }
                }
                break;
            case AttributeDesignator.RESOURCE_TARGET:
                if (0 > context.nResourceValues(attributeId)) {
                    values = null;
                } else {
                    switch (context.nResourceValues(attributeId)) {
                        case 0:
                            values = null;
                            /*
                             * values = new String[1]; ((String[])values)[0] =
                             * Authorization.UNDEFINED;
                             */
                            break;
                        case 1:
                            values = new String[1];
                            ((String[]) values)[0] =
                                    context.getResourceValue(attributeId);
                            break;
                        default:
                            values = context.getResourceValues(attributeId);
                    }
                }
                break;
            case AttributeDesignator.ENVIRONMENT_TARGET:
                if (0 > context.nEnvironmentValues(attributeId)) {
                    values = null;
                } else {
                    switch (context.nEnvironmentValues(attributeId)) {
                        case 0:
                            values = null;
                            /*
                             * values = new String[1]; ((String[])values)[0] =
                             * Authorization.UNDEFINED;
                             */
                            break;
                        case 1:
                            values = new String[1];
                            ((String[]) values)[0] =
                                    context.getEnvironmentValue(attributeId);
                            break;
                        default:
                            values = context.getEnvironmentValues(attributeId);
                    }
                }
                break;
            default:
        }
        logger.debug("local context attribute {}", attributeId);
        if (values instanceof String) {
            logger.debug("getAttributeLocally string value={}", (String) values);
        } else if (values instanceof String[]) {
            logger.debug("getAttributeLocally string values={}", values);
            for (int i = 0; i < ((String[]) values).length; i++) {
                logger.debug("another string value={}", ((String[]) values)[i]);
            }
        } else {
            logger.debug("getAttributeLocally object value={}", values);
        }
        return values;
    }

}
