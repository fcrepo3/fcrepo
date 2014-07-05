
package org.fcrepo.server.security.xacml.pdp.finder.attribute;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.fcrepo.server.security.xacml.pdp.finder.AttributeFinderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;
import org.fcrepo.common.policy.xacml1.XACML1SubjectCategoryNamespace;

import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.attr.AttributeFactory;
import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.attr.BagAttribute;
import org.jboss.security.xacml.sunxacml.attr.StandardAttributeFactory;
import org.jboss.security.xacml.sunxacml.cond.EvaluationResult;

public class LDAPAttributeFinder
        extends DesignatorAttributeFinderModule {

    private static final Logger logger =
            LoggerFactory.getLogger(LDAPAttributeFinder.class);
    
    private static final URI STRING_DATATYPE = URI.create("http://www.w3.org/2001/XMLSchema#string");


    private final AttributeFactory attributeFactory = StandardAttributeFactory.getFactory();

    private Hashtable<String, String> dirEnv = null;

    private Map<String, String> m_options = null;

    private InitialDirContext ctx = null;

    public LDAPAttributeFinder(Map<String, String> options) {
        try {

            m_options =
                    options;
            dirEnv = new Hashtable<String, String>(options);

            ctx = new InitialDirContext(dirEnv);
        } catch (Exception e) {
            logger.error("Attribute finder not initialised:"
                    + this.getClass().getName(), e);
        }
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
        String user = null;
        URI userId = Constants.SUBJECT.LOGIN_ID.getURI();
        URI category = XACML1SubjectCategoryNamespace.getInstance().ACCESS_SUBJECT.getURI();

        EvaluationResult userER =
                context.getSubjectAttribute(STRING_DATATYPE, userId, category);
        if (userER == null) {
            return new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
        }

        AttributeValue userAV = userER.getAttributeValue();
        if (userAV == null) {
            return new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
        }

        user = userAV.encode();
        if (logger.isDebugEnabled()) {
            logger.debug("LDAPAttributeFinder: Getting info for " + user);
        }

        // figure out which attribute we're looking for
        String attrName = attributeId.toString();

        // we only know about registered attributes from config file
        if (m_attributes.get(designatorType) == null) {
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
            result = getEvaluationResult(user, attrName, attributeType);
        } catch (Exception e) {
            logger.error("Error finding attribute: " + e.getMessage(), e);
            return new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
        }

        return result;
    }

    private EvaluationResult getEvaluationResult(String user,
                                                 String attribute,
                                                 URI type) {
        String[] attrsReturned = new String[] {attribute};
        String base = m_options.get("searchbase");
        String filter = "(" + m_options.get("id-attribute") + "=" + user + ")";

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setDerefLinkFlag(true);
        searchControls.setReturningObjFlag(true);
        searchControls.setReturningAttributes(attrsReturned);

        Set<AttributeValue> bagValues = new HashSet<AttributeValue>();
        try {
            NamingEnumeration<SearchResult> ne = ctx.search(base, filter, searchControls);

            while (ne.hasMore()) {
                SearchResult result = (SearchResult) ne.next();
                Attributes attrs = result.getAttributes();
                NamingEnumeration<? extends Attribute> neas = attrs.getAll();

                while (neas.hasMoreElements()) {
                    Attribute attr = (Attribute) neas.nextElement();
                    if (!attr.getID().equals(attribute)) {
                        continue;
                    }

                    NamingEnumeration<?> nea = attr.getAll();
                    while (nea.hasMoreElements()) {
                        String value = (String) nea.nextElement();
                        if (logger.isDebugEnabled()) {
                            logger.debug(attr.getID() + ": " + value);
                        }

                        AttributeValue attributeValue = null;
                        try {
                            attributeValue =
                                    attributeFactory.createValue(type, value);
                        } catch (Exception e) {
                            logger.error("Error creating attribute: "
                                    + e.getMessage(), e);
                            continue;
                        }

                        bagValues.add(attributeValue);

                        if (logger.isDebugEnabled()) {
                            logger.debug("AttributeValue found: ["
                                    + type.toASCIIString() + "] " + value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting evaluation result", e);
            return new EvaluationResult(BagAttribute.createEmptyBag(type));
        }

        BagAttribute bag = new BagAttribute(type, bagValues);

        return new EvaluationResult(bag);
    }

    public static void main(String[] args) throws Exception {
        HashMap<String,String> test = new HashMap<String, String>();
        LDAPAttributeFinder finder = new LDAPAttributeFinder(test);
        URI type = new URI("http://www.w3.org/2001/XMLSchema#string");

        EvaluationResult result =
                finder.getEvaluationResult("nishen",
                                           "eduPersonEntitlement",
                                           type);
        BagAttribute bag = (BagAttribute) result.getAttributeValue();

        Iterator<?> i = bag.iterator();
        while (i.hasNext()) {
            AttributeValue a = (AttributeValue) i.next();
            logger.info("value: " + a.encode());
        }
    }
}
