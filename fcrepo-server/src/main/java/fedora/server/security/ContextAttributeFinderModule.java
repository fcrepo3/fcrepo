/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Hashtable;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeDesignator;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.cond.EvaluationResult;

import org.apache.log4j.Logger;

import fedora.common.Constants;

import fedora.server.Context;

/**
 * @author Bill Niebel
 */
class ContextAttributeFinderModule
        extends AttributeFinderModule {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(ContextAttributeFinderModule.class.getName());

    @Override
    protected boolean canHandleAdhoc() {
        return true;
    }

    static private final ContextAttributeFinderModule singleton =
            new ContextAttributeFinderModule();

    private final Hashtable contexts = new Hashtable();

    private ContextAttributeFinderModule() {
        super();
        try {
            registerSupportedDesignatorType(AttributeDesignator.SUBJECT_TARGET);
            registerSupportedDesignatorType(AttributeDesignator.ACTION_TARGET); //<<??????
            registerSupportedDesignatorType(AttributeDesignator.RESOURCE_TARGET); //<<?????
            registerSupportedDesignatorType(AttributeDesignator.ENVIRONMENT_TARGET);

            registerAttribute(Constants.ENVIRONMENT.CURRENT_DATE_TIME.uri,
                              Constants.ENVIRONMENT.CURRENT_DATE_TIME.datatype);
            registerAttribute(Constants.ENVIRONMENT.CURRENT_DATE.uri,
                              Constants.ENVIRONMENT.CURRENT_DATE.datatype);
            registerAttribute(Constants.ENVIRONMENT.CURRENT_TIME.uri,
                              Constants.ENVIRONMENT.CURRENT_TIME.datatype);
            registerAttribute(Constants.HTTP_REQUEST.PROTOCOL.uri,
                              Constants.HTTP_REQUEST.PROTOCOL.datatype);
            registerAttribute(Constants.HTTP_REQUEST.SCHEME.uri,
                              Constants.HTTP_REQUEST.SCHEME.datatype);
            registerAttribute(Constants.HTTP_REQUEST.SECURITY.uri,
                              Constants.HTTP_REQUEST.SECURITY.datatype);
            registerAttribute(Constants.HTTP_REQUEST.AUTHTYPE.uri,
                              Constants.HTTP_REQUEST.AUTHTYPE.datatype);
            registerAttribute(Constants.HTTP_REQUEST.METHOD.uri,
                              Constants.HTTP_REQUEST.METHOD.datatype);
            registerAttribute(Constants.HTTP_REQUEST.SESSION_ENCODING.uri,
                              Constants.HTTP_REQUEST.SESSION_ENCODING.datatype);
            registerAttribute(Constants.HTTP_REQUEST.SESSION_STATUS.uri,
                              Constants.HTTP_REQUEST.SESSION_STATUS.datatype);
            registerAttribute(Constants.HTTP_REQUEST.CONTENT_LENGTH.uri,
                              Constants.HTTP_REQUEST.CONTENT_LENGTH.datatype);
            registerAttribute(Constants.HTTP_REQUEST.CONTENT_TYPE.uri,
                              Constants.HTTP_REQUEST.CONTENT_TYPE.datatype);
            registerAttribute(Constants.HTTP_REQUEST.CLIENT_FQDN.uri,
                              Constants.HTTP_REQUEST.CLIENT_FQDN.datatype);
            registerAttribute(Constants.HTTP_REQUEST.CLIENT_IP_ADDRESS.uri,
                              Constants.HTTP_REQUEST.CLIENT_IP_ADDRESS.datatype);
            registerAttribute(Constants.HTTP_REQUEST.SERVER_FQDN.uri,
                              Constants.HTTP_REQUEST.SERVER_FQDN.datatype);
            registerAttribute(Constants.HTTP_REQUEST.SERVER_IP_ADDRESS.uri,
                              Constants.HTTP_REQUEST.SERVER_IP_ADDRESS.datatype);
            registerAttribute(Constants.HTTP_REQUEST.SERVER_PORT.uri,
                              Constants.HTTP_REQUEST.SERVER_PORT.datatype);

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

    static public final ContextAttributeFinderModule getInstance() {
        return singleton;
    }

    private final String getContextId(EvaluationCtx context) {
        URI contextIdType = null;
        URI contextIdId = null;
        try {
            contextIdType = new URI(StringAttribute.identifier);
        } catch (URISyntaxException e) {
            LOG.debug("ContextAttributeFinder:getContextId" + " exit on "
                    + "couldn't make URI for contextId type");
        }
        try {
            contextIdId = new URI(Constants.ACTION.CONTEXT_ID.uri);
        } catch (URISyntaxException e) {
            LOG.debug("ContextAttributeFinder:getContextId" + " exit on "
                    + "couldn't make URI for contextId itself");
        }
        LOG.debug("ContextAttributeFinder:findAttribute"
                + " about to call getAttributeFromEvaluationCtx");

        EvaluationResult attribute =
                context.getActionAttribute(contextIdType, contextIdId, null);
        Object element = getAttributeFromEvaluationResult(attribute);
        if (element == null) {
            LOG.debug("ContextAttributeFinder:getContextId" + " exit on "
                    + "can't get contextId on request callback");
            return null;
        }

        if (!(element instanceof StringAttribute)) {
            LOG.debug("ContextAttributeFinder:getContextId" + " exit on "
                    + "couldn't get contextId from xacml request "
                    + "non-string returned");
            return null;
        }

        String contextId = ((StringAttribute) element).getValue();

        if (contextId == null) {
            LOG.debug("ContextAttributeFinder:getContextId" + " exit on "
                    + "null contextId");
            return null;
        }

        if (!validContextId(contextId)) {
            LOG.debug("ContextAttributeFinder:getContextId" + " exit on "
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
                                               String attributeId,
                                               URI resourceCategory,
                                               EvaluationCtx ctx) {
        LOG.debug("getAttributeLocally context");
        String contextId = getContextId(ctx);
        LOG.debug("contextId=" + contextId + " attributeId=" + attributeId);
        Context context = (Context) contexts.get(contextId);
        LOG.debug("got context");
        Object values = null;
        LOG.debug("designatorType" + designatorType);
        switch (designatorType) {
            case AttributeDesignator.SUBJECT_TARGET:
                if (0 > context.nSubjectValues(attributeId)) {
                    values = null;
                } else {
                    LOG.debug("getting n values for " + attributeId + "="
                            + context.nSubjectValues(attributeId));
                    switch (context.nSubjectValues(attributeId)) {
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
                                    context.getSubjectValue(attributeId);
                            break;
                        default:
                            values = context.getSubjectValues(attributeId);
                    }
                    if (values == null) {
                        LOG.debug("RETURNING NO VALUES FOR " + attributeId);
                    } else {
                        StringBuffer sb = new StringBuffer();
                        sb.append("RETURNING " + ((String[]) values).length
                                + " VALUES FOR " + attributeId + " ==");
                        for (int i = 0; i < ((String[]) values).length; i++) {
                            sb.append(" " + ((String[]) values)[i]);
                        }
                        LOG.debug(sb);
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
        if (values instanceof String) {
            LOG.debug("getAttributeLocally string value=" + (String) values);
        } else if (values instanceof String[]) {
            LOG.debug("getAttributeLocally string values=" + values);
            for (int i = 0; i < ((String[]) values).length; i++) {
                LOG.debug("another string value=" + ((String[]) values)[i]);
            }
        } else {
            LOG.debug("getAttributeLocally object value=" + values);
        }
        return values;
    }

    final void registerContext(Object key, Context value) {
        LOG.debug("registering " + key);
        contexts.put(key, value);
    }

    final void unregisterContext(Object key) {
        LOG.debug("unregistering " + key);
        contexts.remove(key);
    }

}
