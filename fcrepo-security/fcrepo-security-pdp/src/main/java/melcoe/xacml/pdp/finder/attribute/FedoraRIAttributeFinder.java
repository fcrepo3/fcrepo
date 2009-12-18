
package melcoe.xacml.pdp.finder.attribute;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import melcoe.xacml.MelcoeXacmlException;
import melcoe.xacml.pdp.finder.AttributeFinderConfigUtil;
import melcoe.xacml.pdp.finder.AttributeFinderException;
import melcoe.xacml.util.ContextUtil;
import melcoe.xacml.util.RelationshipResolver;

import org.apache.log4j.Logger;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeFactory;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.StandardAttributeFactory;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.finder.AttributeFinderModule;

public class FedoraRIAttributeFinder
        extends AttributeFinderModule {

    private static final Logger log =
            Logger.getLogger(FedoraRIAttributeFinder.class);

    private AttributeFactory attributeFactory = null;

    private RelationshipResolver relationshipResolver = null;

    private Map<Integer, Set<String>> attributes = null;

    public FedoraRIAttributeFinder() {
        try {
            attributes =
                    AttributeFinderConfigUtil.getAttributeFinderConfig(this
                            .getClass().getName());
            log
                    .info("Initialised AttributeFinder:"
                            + this.getClass().getName());

            if (log.isDebugEnabled()) {
                log.debug("registering the following attributes: ");
                for (Integer k : attributes.keySet()) {
                    for (String l : attributes.get(k)) {
                        log.debug(k + ": " + l);
                    }
                }
            }

            Map<String, String> resolverConfig =
                    AttributeFinderConfigUtil.getResolverConfig(this.getClass()
                            .getName());
            if (log.isDebugEnabled()) {
                for (String s : resolverConfig.keySet()) {
                    log.debug(s + ": " + resolverConfig.get(s));
                }
            }

            relationshipResolver =
                    ContextUtil.getInstance().getRelationshipResolver();

            attributeFactory = StandardAttributeFactory.getFactory();
        } catch (AttributeFinderException afe) {
            log.fatal("Attribute finder not initialised:"
                    + this.getClass().getName());
        }
    }

    /**
     * Returns true always because this module supports designators.
     * 
     * @return true always
     */
    @Override
    public boolean isDesignatorSupported() {
        return true;
    }

    /**
     * Returns a <code>Set</code> with a single <code>Integer</code> specifying
     * that environment attributes are supported by this module.
     * 
     * @return a <code>Set</code> with
     *         <code>AttributeDesignator.ENVIRONMENT_TARGET</code> included
     */
    @Override
    public Set<Integer> getSupportedDesignatorTypes() {
        return attributes.keySet();
    }

    /**
     * Used to get an attribute. If one of those values isn't being asked for,
     * or if the types are wrong, then an empty bag is returned.
     * 
     * @param attributeType
     *        the datatype of the attributes to find, which must be time, date,
     *        or dateTime for this module to resolve a value
     * @param attributeId
     *        the identifier of the attributes to find, which must be one of the
     *        three ENVIRONMENT_* fields for this module to resolve a value
     * @param issuer
     *        the issuer of the attributes, or null if unspecified
     * @param subjectCategory
     *        the category of the attribute or null, which ignored since this
     *        only handles non-subjects
     * @param context
     *        the representation of the request data
     * @param designatorType
     *        the type of designator, which must be ENVIRONMENT_TARGET for this
     *        module to resolve a value
     * @return the result of attribute retrieval, which will be a bag with a
     *         single attribute, an empty bag, or an error
     */
    @Override
    public EvaluationResult findAttribute(URI attributeType,
                                          URI attributeId,
                                          URI issuer,
                                          URI subjectCategory,
                                          EvaluationCtx context,
                                          int designatorType) {
        String resourceId = context.getResourceId().encode();
        if (log.isDebugEnabled()) {
            log.debug("RIAttributeFinder: [" + attributeType.toString() + "] "
                    + attributeId + ", rid=" + resourceId);
        }

        if (resourceId == null || resourceId.equals("")) {
            return new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
        }

        // figure out which attribute we're looking for
        String attrName = attributeId.toString();

        // we only know about registered attributes from config file
        if (!attributes.keySet().contains(new Integer(designatorType))) {
            if (log.isDebugEnabled()) {
                log.debug("Does not know about designatorType: "
                        + designatorType);
            }
            return new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
        }

        Set<String> allowedAttributes =
                attributes.get(new Integer(designatorType));
        if (!allowedAttributes.contains(attrName)) {
            if (log.isDebugEnabled()) {
                log.debug("Does not know about attribute: " + attrName);
            }
            return new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
        }

        EvaluationResult result = null;
        try {
            result = getEvaluationResult(resourceId, attrName, attributeType);
        } catch (Exception e) {
            log.error("Error finding attribute: " + e.getMessage(), e);
            return new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
        }

        return result;
    }

    private EvaluationResult getEvaluationResult(String pid,
                                                 String attribute,
                                                 URI type)
            throws AttributeFinderException {
        Map<String, Set<String>> relationships;
        try {
            relationships = relationshipResolver.getRelationships(pid);
        } catch (MelcoeXacmlException e) {
            throw new AttributeFinderException(e.getMessage(), e);
        }
        Set<String> results = relationships.get(attribute);
        if (results == null || results.size() == 0) {
            return new EvaluationResult(BagAttribute.createEmptyBag(type));
        }

        Set<AttributeValue> bagValues = new HashSet<AttributeValue>();
        for (String s : results) {
            AttributeValue attributeValue = null;
            try {
                attributeValue = attributeFactory.createValue(type, s);
            } catch (Exception e) {
                log.error("Error creating attribute: " + e.getMessage(), e);
                continue;
            }

            bagValues.add(attributeValue);

            if (log.isDebugEnabled()) {
                log.debug("AttributeValue found: [" + type.toASCIIString()
                        + "] " + s);
            }
        }

        BagAttribute bag = new BagAttribute(type, bagValues);

        return new EvaluationResult(bag);
    }
}
