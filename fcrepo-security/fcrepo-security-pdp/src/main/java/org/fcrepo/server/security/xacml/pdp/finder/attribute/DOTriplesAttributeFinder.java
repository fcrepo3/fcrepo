
package org.fcrepo.server.security.xacml.pdp.finder.attribute;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.fcrepo.common.rdf.SimpleURIReference;
import org.fcrepo.server.Context;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.security.PolicyFinderModule;
import org.fcrepo.server.security.xacml.pdp.finder.AttributeFinderException;
import org.fcrepo.server.storage.DOManager;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.types.RelationshipTuple;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.SubjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeFactory;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.StandardAttributeFactory;
import com.sun.xacml.cond.EvaluationResult;

public class DOTriplesAttributeFinder
        extends DesignatorAttributeFinderModule {

    private static final Logger logger =
            LoggerFactory.getLogger(DOTriplesAttributeFinder.class);

    private final AttributeFactory m_attributeFactory = StandardAttributeFactory.getFactory();

    private final DOManager m_doManager;

    private Context fedoraCtx;

    public DOTriplesAttributeFinder(DOManager doManager) {
        m_doManager = doManager;
    }

    public void init() throws AttributeFinderException {
        if (emptyAttributeMap()) {
            logger.warn(this.getClass().getName() + " configured with no registered attributes");
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("registering the following attributes: ");
            for (int desNum : m_attributes.keySet()) {
                for (String attrName : m_attributes.get(desNum).keySet()) {
                    logger.debug(desNum + ": " + attrName);
                }
            }
        }
        logger.info("Initialised AttributeFinder:"
                    + this.getClass().getName());
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

    @Override
    public Set<Integer> getSupportedDesignatorTypes() {
        return m_attributes.keySet();
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
        if (resourceId == null || resourceId.isEmpty()) {
            String pid = PolicyFinderModule.getPid(context);
            if (pid != null) {
                resourceId = "info:fedora/" + pid;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("RIAttributeFinder: [" + attributeType.toString() + "] "
                    + attributeId + ", rid=" + resourceId);
        }

        if (resourceId == null || resourceId.isEmpty()) {
            return new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
        }

        if (resourceId.equals("/FedoraRepository")) {
            return new EvaluationResult(BagAttribute
                                        .createEmptyBag(attributeType));
        }

        // figure out which attribute we're looking for
        String attrName = attributeId.toString();

        // we only know about registered attributes from Spring config
        if (!m_attributes.containsKey(designatorType)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Does not know about designatorType: "
                        + designatorType);
            }
            return new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
        }

        Set<String> allowedAttributes =
            m_attributes.get(designatorType).keySet();
        if (!allowedAttributes.contains(attrName)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Does not know about attribute: " + attrName);
            }
            return new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
        }

        EvaluationResult result = null;
        try {
            result = getEvaluationResult(resourceId, attrName, designatorType, attributeType);
        } catch (Exception e) {
            logger.error("Error finding attribute: " + e.getMessage(), e);
            return new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
        }

        return result;
    }

    /**
     *
     * @param resourceID - the hierarchical XACML resource ID
     * @param attribute - attribute to get - this is a URI that maps to a Fedora relationship name
     * @param type
     * @return
     * @throws AttributeFinderException
     */
    private EvaluationResult getEvaluationResult(String resourceID,
                                                 String attribute,
                                                 int designatorType,
                                                 URI type)
            throws AttributeFinderException {

        // split up the path of the hierarchical resource id
        String resourceParts[] = resourceID.split("/");
        Set<String> results = null;

        // either the last part is the pid, or the last-but one is the pid and the last is the datastream
        // if we have a pid, we query on that, if we have a datastream we query on the datastream
        String pid;
        if (resourceParts.length > 1) {
            if (resourceParts[resourceParts.length - 1].contains(":")) { // ends with a pid, we have pid only
                pid = resourceParts[resourceParts.length - 1];
            } else { // datastream
                pid = resourceParts[resourceParts.length - 2];
            }
        } else {
            // eg /FedoraRepository, not a valid path to PID or PID/DS
            logger.debug("Resource ID not valid path to PID or datastream: " + resourceID);
            return new EvaluationResult(BagAttribute.createEmptyBag(type));
        }

        logger.debug("Getting attribute for resource " + resourceID);

        try{
            SubjectNode snode = new SimpleURIReference(new URI(resourceID));
            PredicateNode pnode = new SimpleURIReference(new URI(attribute));
            DOReader reader = m_doManager.getReader(false, getContext(), pid);
            Set<RelationshipTuple> triples = reader.getRelationships(snode, pnode, null);
            results = new HashSet<String>();

            for (RelationshipTuple triple:triples){
                results.add(triple.object);
            }
        }
        catch (Exception e){
            logger.warn("Error retreiving triples in attributeFinder",e);
        }

        if (results == null || results.isEmpty()) {
            return new EvaluationResult(BagAttribute.createEmptyBag(type));
        }


        Set<AttributeValue> bagValues = new HashSet<AttributeValue>();
        logger.debug("Attribute values found: " + results.size());
        for (String s : results) {
            AttributeValue attributeValue = null;
            try {
                attributeValue = m_attributeFactory.createValue(type, s);
            } catch (Exception e) {
                logger.error("Error creating attribute: " + e.getMessage(), e);
                continue;
            }

            bagValues.add(attributeValue);

            if (logger.isDebugEnabled()) {
                logger.debug("AttributeValue found: [" + type.toASCIIString()
                             + "] " + s);
            }

        }

        BagAttribute bag = new BagAttribute(type, bagValues);
        return new EvaluationResult(bag);

    }

    private Context getContext() throws Exception {
        if (fedoraCtx != null) {
            return fedoraCtx;
        }
        fedoraCtx =
            ReadOnlyContext.getContext(null,
                                       null,
                                       null,
                                       ReadOnlyContext.DO_OP);
        return fedoraCtx;
    }

}
