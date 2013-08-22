/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.DateAttribute;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.IntegerAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.attr.TimeAttribute;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.ctx.Status;

/**
 * @author Bill Niebel
 */
public abstract class AttributeFinderModule
        extends com.sun.xacml.finder.AttributeFinderModule {

    private static final Logger logger =
            LoggerFactory.getLogger(AttributeFinderModule.class);

    protected AttributeFinderModule() {

        URI temp;

        try {
            temp = new URI(StringAttribute.identifier);
        } catch (URISyntaxException e1) {
            temp = null;
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        STRING_ATTRIBUTE_URI = temp;
    }

    private Boolean instantiatedOk = null;

    public final void setInstantiatedOk(boolean value) {
        logger.debug("setInstantiatedOk() {}", value);
        if (instantiatedOk == null) {
            instantiatedOk = new Boolean(value);
        }
    }

    @Override
    public boolean isDesignatorSupported() {
        logger.debug("isDesignatorSupported() will return {} {}",
                iAm(), (instantiatedOk != null && instantiatedOk.booleanValue()));
        return instantiatedOk != null && instantiatedOk.booleanValue();
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
                iAm(), attributeId.toString());
        showRegisteredAttributes();

        if (hasAttribute(attributeId.toString())) {
            if (!getAttributeType(attributeId.toString()).equals(attributeType
                    .toString())) {
                logger.debug("AttributeFinder:parmsOk{} exit on attributeType incorrect for attributeId",
                        iAm());
                return false;
            }
        } else {
            if (!StringAttribute.identifier.equals(attributeType.toString())) {
                logger.debug("AttributeFinder:parmsOk{} exit on attributeType incorrect for attributeId",
                        iAm());
                return false;
            }
        }
        logger.debug("exiting parmsOk normally {}", iAm());
        return true;
    }

    protected String iAm() {
        return this.getClass().getName();
    }

    protected final Object getAttributeFromEvaluationResult(EvaluationResult attribute /*
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

        Iterator it = bag.iterator();
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

    protected final HashSet<String> attributesDenied = new HashSet<String>();

    private final Hashtable<String, URI> attributeIdUris =
            new Hashtable<String, URI>();

    private final Hashtable<String, String> attributeTypes =
            new Hashtable<String, String>();

    private final Hashtable<String, URI> attributeTypeUris =
            new Hashtable<String, URI>();

    protected final void registerAttribute(String id, String type)
            throws URISyntaxException {
        logger.debug("registering attribute {} {}", iAm(), id);
        attributeIdUris.put(id, new URI(id));
        attributeTypeUris.put(id, new URI(type));
        attributeTypes.put(id, type);
    }

    protected final URI getAttributeIdUri(String id) {
        return attributeIdUris.get(id);
    }

    protected final boolean hasAttribute(String id) {
        return attributeIdUris.containsKey(id);
    }

    private final void showRegisteredAttributes() {
        Iterator<String> it = attributeIdUris.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            logger.debug("another registered attribute  = {} {}", iAm(), key);
        }
    }

    protected final String getAttributeType(String id) {
        return attributeTypes.get(id);
    }

    protected final URI getAttributeTypeUri(String id) {
        return attributeTypeUris.get(id);
    }

    private static final Set NULLSET = new HashSet();

    private final Set<Integer> supportedDesignatorTypes =
            new HashSet<Integer>();

    protected final void registerSupportedDesignatorType(int designatorType) {
        logger.debug("registerSupportedDesignatorType() {}", iAm());
        supportedDesignatorTypes.add(designatorType);
    }

    @Override
    public Set getSupportedDesignatorTypes() {
        if (instantiatedOk != null && instantiatedOk.booleanValue()) {
            logger.debug("getSupportedDesignatorTypes() will return {} set of elements, n=",
                    iAm(), supportedDesignatorTypes.size());
            return supportedDesignatorTypes;
        }
        logger.debug("getSupportedDesignatorTypes() will return {}NULLSET", iAm());
        return NULLSET;
    }

    protected abstract boolean canHandleAdhoc();

    private final boolean willService(URI attributeId) {
        String temp = attributeId.toString();
        if (hasAttribute(temp)) {
            logger.debug("willService() {} accept this known serviced attribute {}",
                    iAm(), attributeId.toString());
            return true;
        }
        if (!canHandleAdhoc()) {
            logger.debug("willService() {} deny any adhoc attribute {}",
                    iAm(), attributeId.toString());
            return false;
        }
        if (attributesDenied.contains(temp)) {
            logger.debug("willService() {} deny this known adhoc attribute ",
                    iAm(), attributeId.toString());
            return false;
        }
        logger.debug("willService() {} allow this unknown adhoc attribute ",
                iAm(), attributeId.toString());
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
        logger.debug("attributeType=[{}], attributeId=[{}] {}", new Object[]{attributeType, attributeId, iAm()});

        if (!parmsOk(attributeType, attributeId, designatorType)) {
            logger.debug("AttributeFinder:findAttribute exit on parms not ok {}", iAm());
            if (attributeType == null) {
                try {
                    attributeType = new URI(StringAttribute.identifier);
                } catch (URISyntaxException e) {
                    //we tried
                }
            }
            return new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
        }

        if (!willService(attributeId)) {
            logger.debug("AttributeFinder:willService() {} returns false", iAm());
            return new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
        }

        if (category != null) {
            logger.debug("++++++++++ AttributeFinder:findAttribute {} category={}", iAm(), category.toString());
        }
        logger.debug("++++++++++ AttributeFinder:findAttribute {} designatorType={}", iAm(), designatorType);

        logger.debug("about to get temp {}", iAm());
        Object temp =
                getAttributeLocally(designatorType,
                                    attributeId.toASCIIString(),
                                    category,
                                    context);
        logger.debug("{} got temp={}", iAm(), temp);

        if (temp == null) {
            logger.debug("AttributeFinder:findAttribute{} exit on "
                    + "attribute value not found", iAm());
            return new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
        }

        Set<AttributeValue> set = new HashSet<AttributeValue>();
        if (temp instanceof String) {
            logger.debug("AttributeFinder:findAttribute will return a String {}", iAm());
            if (attributeType.toString().equals(StringAttribute.identifier)) {
                set.add(new StringAttribute((String) temp));
            } else if (attributeType.toString()
                    .equals(DateTimeAttribute.identifier)) {
                DateTimeAttribute tempDateTimeAttribute;
                try {
                    tempDateTimeAttribute =
                            DateTimeAttribute.getInstance((String) temp);
                    set.add(tempDateTimeAttribute);
                } catch (Throwable t) {
                }
            } else if (attributeType.toString()
                    .equals(DateAttribute.identifier)) {
                DateAttribute tempDateAttribute;
                try {
                    tempDateAttribute =
                            DateAttribute.getInstance((String) temp);
                    set.add(tempDateAttribute);
                } catch (Throwable t) {
                }
            } else if (attributeType.toString()
                    .equals(TimeAttribute.identifier)) {
                TimeAttribute tempTimeAttribute;
                try {
                    tempTimeAttribute =
                            TimeAttribute.getInstance((String) temp);
                    set.add(tempTimeAttribute);
                } catch (Throwable t) {
                }
            } else if (attributeType.toString()
                    .equals(IntegerAttribute.identifier)) {
                IntegerAttribute tempIntegerAttribute;
                try {
                    tempIntegerAttribute =
                            IntegerAttribute.getInstance((String) temp);
                    set.add(tempIntegerAttribute);
                } catch (Throwable t) {
                }
            } //xacml fixup
            //was set.add(new StringAttribute((String)temp));
        } else if (temp instanceof String[]) {
            logger.debug("AttributeFinder:findAttribute will return a String[] {}", iAm());
            for (int i = 0; i < ((String[]) temp).length; i++) {
                if (((String[]) temp)[i] == null) {
                    continue;
                }
                if (attributeType.toString().equals(StringAttribute.identifier)) {
                    set.add(new StringAttribute(((String[]) temp)[i]));
                } else if (attributeType.toString()
                        .equals(DateTimeAttribute.identifier)) {
                    logger.debug("USING AS DATETIME:{}", ((String[]) temp)[i]);
                    DateTimeAttribute tempDateTimeAttribute;
                    try {
                        tempDateTimeAttribute =
                                DateTimeAttribute
                                        .getInstance(((String[]) temp)[i]);
                        set.add(tempDateTimeAttribute);
                    } catch (Throwable t) {
                    }
                } else if (attributeType.toString()
                        .equals(DateAttribute.identifier)) {
                    logger.debug("USING AS DATE:{}", ((String[]) temp)[i]);
                    DateAttribute tempDateAttribute;
                    try {
                        tempDateAttribute =
                                DateAttribute.getInstance(((String[]) temp)[i]);
                        set.add(tempDateAttribute);
                    } catch (Throwable t) {
                    }
                } else if (attributeType.toString()
                        .equals(TimeAttribute.identifier)) {
                    logger.debug("USING AS TIME:{}", ((String[]) temp)[i]);
                    TimeAttribute tempTimeAttribute;
                    try {
                        tempTimeAttribute =
                                TimeAttribute.getInstance(((String[]) temp)[i]);
                        set.add(tempTimeAttribute);
                    } catch (Throwable t) {
                    }
                } else if (attributeType.toString()
                        .equals(IntegerAttribute.identifier)) {
                    logger.debug("USING AS INTEGER: {}", ((String[]) temp)[i]);
                    IntegerAttribute tempIntegerAttribute;
                    try {
                        tempIntegerAttribute =
                                IntegerAttribute
                                        .getInstance(((String[]) temp)[i]);
                        set.add(tempIntegerAttribute);
                    } catch (Throwable t) {
                    }
                }
            }
        }
        return new EvaluationResult(new BagAttribute(attributeType, set));
    }

    protected final URI STRING_ATTRIBUTE_URI;

    abstract protected Object getAttributeLocally(int designatorType,
                                                  String attributeId,
                                                  URI resourceCategory,
                                                  EvaluationCtx context);

}
