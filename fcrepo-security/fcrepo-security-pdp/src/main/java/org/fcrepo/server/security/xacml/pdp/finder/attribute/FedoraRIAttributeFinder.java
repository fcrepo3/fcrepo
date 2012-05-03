package org.fcrepo.server.security.xacml.pdp.finder.attribute;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.fcrepo.server.security.xacml.pdp.finder.AttributeFinderException;
import org.fcrepo.server.security.xacml.util.Attribute;
import org.fcrepo.server.security.xacml.util.RelationshipResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeFactory;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.StandardAttributeFactory;
import com.sun.xacml.cond.EvaluationResult;

public class FedoraRIAttributeFinder
        extends DesignatorAttributeFinderModule {

    private static final Logger logger =
            LoggerFactory.getLogger(FedoraRIAttributeFinder.class);

    private AttributeFactory m_attributeFactory = null;

    private RelationshipResolver m_relationshipResolver = null;

    public FedoraRIAttributeFinder(RelationshipResolver relationshipResolver) {
        m_relationshipResolver =
                    relationshipResolver;

        m_attributeFactory = StandardAttributeFactory.getFactory();
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
        if (logger.isDebugEnabled()) {
            logger.debug("RIAttributeFinder: [" + attributeType.toString() + "] "
                    + attributeId + ", rid=" + resourceId);
        }

        if (resourceId == null || resourceId.equals("")) {
            return new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
        }

        if (resourceId.equals("/FedoraRepository")) {
            return new EvaluationResult(BagAttribute
                                        .createEmptyBag(attributeType));
        }

        // figure out which attribute we're looking for
        String attrName = attributeId.toString();

        // we only know about registered attributes from config file
        Map<String,Attribute> allowedAttributes = m_attributes.get(designatorType);

        if (allowedAttributes == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Does not know about designatorType: "
                        + designatorType);
            }
            return new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
        }

        Attribute attribute = allowedAttributes.get(attrName);

        if (attribute == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Does not know about attribute: " + attrName);
            }
            return new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
        }


        EvaluationResult result = null;
        try {
            result = getEvaluationResult(resourceId, attrName, attribute, attributeType);
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
                                                 String attributeID,
                                                 Attribute attribute,
                                                 URI type)
            throws AttributeFinderException {

        // split up the path of the hierarchical resource id
        String resourceParts[] = resourceID.split("/");
        Set<String> results;

        // either the last part is the pid, or the last-but one is the pid and the last is the datastream
        // if we have a pid, we query on that, if we have a datastream we query on the datastream
        String subject; // the full subject, ie pid or pid/ds
        String pid;
        if (resourceParts.length > 1) {
            if (resourceParts[resourceParts.length - 1].contains(":")) { // ends with a pid, we have pid only
                subject = resourceParts[resourceParts.length - 1];
                pid = subject;
            } else { // datastream
                pid = resourceParts[resourceParts.length - 2];
                subject = pid + "/" + resourceParts[resourceParts.length - 1];
            }
        } else {
            // eg /FedoraRepository, not a valid path to PID or PID/DS
            logger.debug("Resource ID not valid path to PID or datastream: " + resourceID);
            return new EvaluationResult(BagAttribute.createEmptyBag(type));
        }

        logger.debug("Getting attribute for resource " + subject);

        // the different types of RI attribute specification...
        // if there is no "query" option for the attribute
        String query = attribute.get("query");
        if (query == null) {
            // it's a simple relationship lookup
            // see if a relationship is specified, otherwise default to the attribute name URI
            String relationship = attribute.get("relationship");
            if (relationship == null) {
                relationship = attributeID; // default to use attribute URI as relationship if none specified
            }

            // see if we are querying based on the resource (object, datstream etc) or just on the object (pid)
            String target = attribute.get("target");
            String queryTarget;
            if (target != null && target.equals("object")) {
                queryTarget = pid;
            } else {
                queryTarget = subject;
            }

            Map<String, Set<String>> relationships;

            try {
                logger.debug("Getting attribute using relationship " + relationship);
                relationships = m_relationshipResolver.getRelationships(queryTarget, relationship);
            } catch (MelcoeXacmlException e) {
                throw new AttributeFinderException(e.getMessage(), e);
            }

            if (relationships == null || relationships.isEmpty()) {
                return new EvaluationResult(BagAttribute.createEmptyBag(type));
            }

            // there will only be results for one attribute, this will get all the values
            results = relationships.get(relationship);

        } else {
            // get the language and query output variable
            String queryLang = attribute.get("queryLang"); // language
            String variable =  attribute.get("value"); // query text
            String resource =  attribute.get("resource"); // resource marker in query
            String object = attribute.get("object"); // object/pid marker in query

            String subjectURI = "info:fedora/" + subject;
            String pidURI  = "info:fedora/" + pid;

            // replace the resource marker in the query with the subject
            if (resource != null) {
                query = query.replace(resource, subjectURI);
            }
            // and the pid/object marker
            if (object != null) {
                query = query.replace(object, pidURI);
            }

            // run it
            try {
                logger.debug("Using a " + queryLang + " query to get attribute " + attribute);
                results = m_relationshipResolver.getAttributesFromQuery(query, queryLang, variable);
            } catch (MelcoeXacmlException e) {
                throw new AttributeFinderException(e.getMessage(), e);
            }
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
}