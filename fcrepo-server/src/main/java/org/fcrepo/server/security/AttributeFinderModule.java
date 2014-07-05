/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.fcrepo.common.policy.XacmlName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.attr.BagAttribute;
import org.jboss.security.xacml.sunxacml.attr.DateAttribute;
import org.jboss.security.xacml.sunxacml.attr.DateTimeAttribute;
import org.jboss.security.xacml.sunxacml.attr.IntegerAttribute;
import org.jboss.security.xacml.sunxacml.attr.StringAttribute;
import org.jboss.security.xacml.sunxacml.attr.TimeAttribute;
import org.jboss.security.xacml.sunxacml.cond.EvaluationResult;
import org.jboss.security.xacml.sunxacml.ctx.Status;

/**
 * @author Bill Niebel
 */
public abstract class AttributeFinderModule
        extends org.jboss.security.xacml.sunxacml.finder.AttributeFinderModule {

    private static final Logger logger =
            LoggerFactory.getLogger(AttributeFinderModule.class);
    
    protected static final String[] EMPTY_STRING_ARRAY = new String[0];
    
    protected static final Map<URI, EvaluationResult> EMPTY_RESULTS =
            new HashMap<URI, EvaluationResult>(3);
    
    private static EvaluationResult getEmptyResult(URI attributeType) {
        EvaluationResult empty = EMPTY_RESULTS.get(attributeType);
        if (empty == null) {
            empty = new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
            EMPTY_RESULTS.put(attributeType, empty);
        }
        return empty;
    }

    protected AttributeFinderModule() {

    }

    private Boolean instantiatedOk = null;
    
    private String iAmCache = null;

    public final void setInstantiatedOk(boolean value) {
        logger.debug("setInstantiatedOk() {}", value);
        if (instantiatedOk == null) {
            instantiatedOk = Boolean.valueOf(value);
        }
    }

    @Override
    public boolean isDesignatorSupported() {
        logger.debug("isDesignatorSupported() will return {} {}",
                iAm(), (instantiatedOk == Boolean.TRUE));
        return instantiatedOk == Boolean.TRUE;
    }

    private final boolean parmsOk(URI attributeType,
                                  URI attributeId,
                                  int designatorType) {
        logger.debug("in parmsOk {}", iAm());
        if (!getSupportedDesignatorTypes()
                .contains(Integer.valueOf(designatorType))) {
            logger.debug("AttributeFinder:parmsOk{} exit on target not supported [{}]",
                    iAm(), designatorType);
            return false;
        }

        if (attributeType == null) {
            logger.debug("AttributeFinder:parmsOk{} exit on null attributeType",
                    iAm());
            return false;
        }

        if (attributeId == null) {
            logger.debug("AttributeFinder:parmsOk{} exit on null attributeId",
                    iAm());
            return false;
        }

        logger.debug("AttributeFinder:parmsOk{} looking for {}",
                iAm(), attributeId);
        showRegisteredAttributes();

        if (hasAttribute(attributeId)) {
            if (!getAttributeType(attributeId).equals(attributeType)) {
                logger.debug("AttributeFinder:parmsOk{} exit on attributeType incorrect for attributeId",
                        iAm());
                return false;
            }
        } else {
            if (!STRING_ATTRIBUTE_TYPE_URI.equals(attributeType)) {
                logger.debug("AttributeFinder:parmsOk{} exit on attributeType incorrect for attributeId",
                        iAm());
                return false;
            }
        }
        logger.debug("exiting parmsOk normally {}", iAm());
        return true;
    }

    protected String iAm() {
        if (iAmCache == null) {
            iAmCache = this.getClass().getName();
        }
        return iAmCache;
    }

    protected final Object getAttributeFromEvaluationResult(EvaluationResult attribute) {
        if (attribute.indeterminate()) {
            logger.debug("AttributeFinder:getAttributeFromEvaluationCtx{} exit on couldn't get resource attribute from xacml request indeterminate",
                    iAm());
            return null;
        }

        if (attribute.getStatus() != null
                && !Status.STATUS_OK.equals(attribute.getStatus())) {
            logger.debug("AttributeFinder:getAttributeFromEvaluationCtx{} exit on couldn't get resource attribute from xacml request bad status",
                    iAm());
            return null;
        } // (resourceAttribute.getStatus() == null) == everything is ok

        AttributeValue attributeValue = attribute.getAttributeValue();
        if (!(attributeValue instanceof BagAttribute)) {
            logger.debug("AttributeFinder:getAttributeFromEvaluationCtx{} exit on couldn't get resource attribute from xacml request no bag",
                    iAm());
            return null;
        }

        BagAttribute bag = (BagAttribute) attributeValue;
        if (1 != bag.size()) {
            logger.debug("AttributeFinder:getAttributeFromEvaluationCtx{} exit on couldn't get resource attribute from xacml request wrong bag n={}",
                    iAm(), bag.size());
            return null;
        }

        Iterator<?> it = bag.iterator();
        Object element = it.next();

        if (element == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("AttributeFinder:getAttributeFromEvaluationCtx{} exit on couldn't get resource attribute from xacml request null returned", iAm());
            }
            return null;
        }

        if (it.hasNext()) {
            if (logger.isDebugEnabled()) {
                logger.debug("AttributeFinder:getAttributeFromEvaluationCtx{} exit on couldn't get resource attribute from xacml request too many returned",
                    iAm());
                logger.debug(element.toString());
                while (it.hasNext()) {
                    logger.debug(it.next().toString());
                }
            }
            return null;
        }

        logger.debug("AttributeFinder:getAttributeFromEvaluationCtx {} returning {}",
                iAm(), element.toString());
        return element;
    }

    protected final HashSet<URI> attributesDenied = new HashSet<URI>();

    private final HashSet<URI> attributeIdUris =
            new HashSet<URI>();

    private final Hashtable<URI, URI> attributeTypes =
            new Hashtable<URI, URI>();

    @Deprecated
    protected final void registerAttribute(String id, String type)
            throws URISyntaxException {
        logger.debug("registering attribute {} {}", iAm(), id);
        URI idUri = new URI(id);
        URI typeUri = new URI(type);
        attributeIdUris.add(idUri);
        attributeTypes.put(idUri, typeUri);
    }

    protected final void registerAttribute(URI id, URI type)
            throws URISyntaxException {
        logger.debug("registering attribute {} {}", iAm(), id);
        attributeIdUris.add(id);
        attributeTypes.put(id, type);
    }
    
    protected final void registerAttribute(XacmlName attribute) {
        logger.debug("registering attribute {} {}", iAm(), attribute.uri);
        attributeIdUris.add(attribute.attributeId);
        attributeTypes.put(attribute.attributeId, attribute.datatype);
    }
    
    protected final void denyAttribute(URI id) {
        logger.debug("Denying attribute {} {}", iAm(), id);
        attributesDenied.add(id);
    }

    @Deprecated
    protected final URI getAttributeIdUri(String id) {
        URI test = URI.create(id);
        return (attributeIdUris.contains(test)) ? test : null;
    }

    @Deprecated
    protected final boolean hasAttribute(String id) {
        return attributeIdUris.contains(URI.create(id));
    }

    protected final boolean hasAttribute(URI id) {
        return attributeIdUris.contains(id);
    }

    private final void showRegisteredAttributes() {
        if (!logger.isDebugEnabled()) return;
        Iterator<URI> it = attributeIdUris.iterator();
        while (it.hasNext()) {
            String key = it.next().toString();
            logger.debug("another registered attribute  = {} {}", iAm(), key);
        }
    }

    @Deprecated
    protected final String getAttributeType(String id) {
        return getAttributeTypeUri(id).toString();
    }

    protected final URI getAttributeTypeUri(String id) {
        return getAttributeType(URI.create(id));
    }

    protected final URI getAttributeType(URI id) {
        return attributeTypes.get(id);
    }

    private final Set<Integer> supportedDesignatorTypes =
            new HashSet<Integer>();

    protected final void registerSupportedDesignatorType(int designatorType) {
        logger.debug("registerSupportedDesignatorType() {}", iAm());
        supportedDesignatorTypes.add(designatorType);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Set getSupportedDesignatorTypes() {
        if (instantiatedOk != null && instantiatedOk.booleanValue()) {
            logger.debug("getSupportedDesignatorTypes() will return {} set of elements, n={}",
                    iAm(), supportedDesignatorTypes.size());
            return supportedDesignatorTypes;
        }
        logger.debug("getSupportedDesignatorTypes() will return {}NULLSET", iAm());
        return Collections.emptySet();
    }

    protected abstract boolean canHandleAdhoc();

    private final boolean willService(URI attributeId) {
        if (hasAttribute(attributeId)) {
            logger.debug("willService() {} accept this known serviced attribute {}",
                    iAm(), attributeId);
            return true;
        }
        if (!canHandleAdhoc()) {
            logger.debug("willService() {} deny any adhoc attribute {}",
                    iAm(), attributeId);
            return false;
        }
        if (attributesDenied.contains(attributeId)) {
            logger.debug("willService() {} deny this known adhoc attribute {}",
                    iAm(), attributeId);
            return false;
        }
        logger.debug("willService() {} allow this unknown adhoc attribute {}",
                iAm(), attributeId);
        return true;
    }

    @Override
    public EvaluationResult findAttribute(URI attributeType,
                                          URI attributeId,
                                          URI issuer,
                                          URI category,
                                          EvaluationCtx context,
                                          int designatorType) {
        logger.debug("AttributeFinder:findAttribute {}", iAm());
        logger.debug("attributeType=[{}], attributeId=[{}] {}", attributeType, attributeId, iAm());

        if (!parmsOk(attributeType, attributeId, designatorType)) {
            logger.debug("AttributeFinder:findAttribute exit on parms not ok {}", iAm());
            if (attributeType == null) {
                attributeType = STRING_ATTRIBUTE_TYPE_URI;
            }
            return getEmptyResult(attributeType);
        }

        if (!willService(attributeId)) {
            logger.debug("AttributeFinder:willService() {} returns false", iAm());
            return getEmptyResult(attributeType);
        }

        if (category != null) {
            logger.debug("++++++++++ AttributeFinder:findAttribute {} category={}", iAm(), category.toString());
        }
        logger.debug("++++++++++ AttributeFinder:findAttribute {} designatorType={}", iAm(), designatorType);

        logger.debug("about to get temp {}", iAm());
        Object temp =
                getAttributeLocally(designatorType,
                                    attributeId,
                                    category,
                                    context);
        logger.debug("{} got temp={}", iAm(), temp);

        if (temp == null) {
            logger.debug("AttributeFinder:findAttribute{} exit on "
                    + "attribute value not found", iAm());
            return getEmptyResult(attributeType);
        }

        Collection<AttributeValue> set = null;
        if (temp instanceof String) {
            AttributeValue att = null;
            logger.debug("AttributeFinder:findAttribute will return a String {}", iAm());
            if (attributeType.equals(STRING_ATTRIBUTE_TYPE_URI)) {
                att = StringAttribute.getInstance((String) temp);
            } else if (attributeType.equals(DATETIME_ATTRIBUTE_TYPE_URI)) {
                try {
                    att = DateTimeAttribute.getInstance((String) temp);
                } catch (Throwable t) {
                }
            } else if (attributeType.equals(DATE_ATTRIBUTE_TYPE_URI)) {
                try {
                    att = DateAttribute.getInstance((String) temp);
                } catch (Throwable t) {
                }
            } else if (attributeType.equals(TIME_ATTRIBUTE_TYPE_URI)) {
                try {
                    att = TimeAttribute.getInstance((String) temp);
                } catch (Throwable t) {
                }
            } else if (attributeType.equals(INTEGER_ATTRIBUTE_TYPE_URI)) {
                try {
                    att = IntegerAttribute.getInstance((String) temp);
                } catch (Throwable t) {
                }
            }
            if (att != null) {
                set = Collections.singleton(att);
            }
        } else if (temp instanceof String[]) {
            String[] strings = (String[]) temp;
            // Because the number of attributes tends to be small,
            // we can minimize object creation by avoiding HashSets
            // and sizing to the size of the value list
            set = new ArrayList<AttributeValue>(strings.length);
            logger.debug("AttributeFinder:findAttribute will return a String[] {}", iAm());
            for (int i = 0; i < strings.length; i++) {
                if (strings[i] == null) {
                    continue;
                }
                AttributeValue tempAtt = null;
                if (attributeType.equals(STRING_ATTRIBUTE_TYPE_URI)) {
                    set.add(new StringAttribute(strings[i]));
                } else if (attributeType.equals(DATETIME_ATTRIBUTE_TYPE_URI)) {
                    logger.debug("USING AS DATETIME:{}", strings[i]);
                    try {
                        tempAtt =
                                DateTimeAttribute
                                        .getInstance(strings[i]);
                    } catch (Throwable t) {
                    }
                } else if (attributeType.equals(DATE_ATTRIBUTE_TYPE_URI)) {
                    logger.debug("USING AS DATE:{}", strings[i]);
                    try {
                        tempAtt =
                                DateAttribute.getInstance(strings[i]);
                    } catch (Throwable t) {
                    }
                } else if (attributeType.equals(TIME_ATTRIBUTE_TYPE_URI)) {
                    logger.debug("USING AS TIME:{}", strings[i]);
                    try {
                        tempAtt =
                                TimeAttribute.getInstance(strings[i]);
                    } catch (Throwable t) {
                    }
                } else if (attributeType.equals(INTEGER_ATTRIBUTE_TYPE_URI)) {
                    logger.debug("USING AS INTEGER: {}", strings[i]);
                    try {
                        tempAtt =
                                IntegerAttribute
                                        .getInstance(strings[i]);
                    } catch (Throwable t) {
                    }
                }
                if (tempAtt != null && !set.contains(tempAtt)) {
                    set.add(tempAtt);
                }
            }
        }
        if (set == null || set.size() == 0) {
            return getEmptyResult(attributeType);
        }
        return new EvaluationResult(new BagAttribute(attributeType, set));
    }

    protected static final URI DATE_ATTRIBUTE_TYPE_URI = URI.create(DateAttribute.identifier);

    protected static final URI DATETIME_ATTRIBUTE_TYPE_URI = URI.create(DateTimeAttribute.identifier);

    protected static final URI INTEGER_ATTRIBUTE_TYPE_URI = URI.create(IntegerAttribute.identifier);

    protected static final URI STRING_ATTRIBUTE_TYPE_URI = URI.create(StringAttribute.identifier);

    protected static final URI TIME_ATTRIBUTE_TYPE_URI = URI.create(TimeAttribute.identifier);

    abstract protected Object getAttributeLocally(int designatorType,
                                                  URI attributeId,
                                                  URI resourceCategory,
                                                  EvaluationCtx context);

}
