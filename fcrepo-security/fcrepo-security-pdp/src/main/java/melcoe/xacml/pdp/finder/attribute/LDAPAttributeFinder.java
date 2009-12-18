
package melcoe.xacml.pdp.finder.attribute;

import java.net.URI;
import java.net.URISyntaxException;
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

import melcoe.xacml.pdp.finder.AttributeFinderConfigUtil;

import org.apache.log4j.Logger;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeFactory;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.StandardAttributeFactory;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.finder.AttributeFinderModule;

public class LDAPAttributeFinder
        extends AttributeFinderModule {

    private static final Logger log =
            Logger.getLogger(LDAPAttributeFinder.class);

    private AttributeFactory attributeFactory = null;

    private Map<Integer, Set<String>> attributes = null;

    private Hashtable<String, String> dirEnv = null;

    private Map<String, String> options = null;

    private InitialDirContext ctx = null;

    public LDAPAttributeFinder() {
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

            options =
                    AttributeFinderConfigUtil.getOptionMap(this.getClass()
                            .getName());
            dirEnv = new Hashtable<String, String>(options);
            attributeFactory = StandardAttributeFactory.getFactory();

            ctx = new InitialDirContext(dirEnv);
        } catch (Exception e) {
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
        String user = null;
        try {
            URI userId = new URI("urn:fedora:names:fedora:2.1:subject:loginId");
            URI userType = new URI("http://www.w3.org/2001/XMLSchema#string");
            URI category =
                    new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");

            EvaluationResult userER =
                    context.getSubjectAttribute(userType, userId, category);
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
            if (log.isDebugEnabled()) {
                log.debug("LDAPAttributeFinder: Getting info for " + user);
            }
        } catch (URISyntaxException use) {
            log.error(use.getMessage());
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
            result = getEvaluationResult(user, attrName, attributeType);
        } catch (Exception e) {
            log.error("Error finding attribute: " + e.getMessage(), e);
            return new EvaluationResult(BagAttribute
                    .createEmptyBag(attributeType));
        }

        return result;
    }

    private EvaluationResult getEvaluationResult(String user,
                                                 String attribute,
                                                 URI type) {
        String[] attrsReturned = new String[] {attribute};
        String base = options.get("searchbase");
        String filter = "(" + options.get("id-attribute") + "=" + user + ")";

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setDerefLinkFlag(true);
        searchControls.setReturningObjFlag(true);
        searchControls.setReturningAttributes(attrsReturned);

        Set<AttributeValue> bagValues = new HashSet<AttributeValue>();
        try {
            NamingEnumeration ne = ctx.search(base, filter, searchControls);

            while (ne.hasMore()) {
                SearchResult result = (SearchResult) ne.next();
                Attributes attrs = result.getAttributes();
                NamingEnumeration neas = attrs.getAll();

                while (neas.hasMoreElements()) {
                    Attribute attr = (Attribute) neas.nextElement();
                    if (!attr.getID().equals(attribute)) {
                        continue;
                    }

                    NamingEnumeration nea = attr.getAll();
                    while (nea.hasMoreElements()) {
                        String value = (String) nea.nextElement();
                        if (log.isDebugEnabled()) {
                            log.debug(attr.getID() + ": " + value);
                        }

                        AttributeValue attributeValue = null;
                        try {
                            attributeValue =
                                    attributeFactory.createValue(type, value);
                        } catch (Exception e) {
                            log.error("Error creating attribute: "
                                    + e.getMessage(), e);
                            continue;
                        }

                        bagValues.add(attributeValue);

                        if (log.isDebugEnabled()) {
                            log.debug("AttributeValue found: ["
                                    + type.toASCIIString() + "] " + value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e);
            return new EvaluationResult(BagAttribute.createEmptyBag(type));
        }

        BagAttribute bag = new BagAttribute(type, bagValues);

        return new EvaluationResult(bag);
    }

    public static void main(String[] args) throws Exception {
        LDAPAttributeFinder finder = new LDAPAttributeFinder();
        URI type = new URI("http://www.w3.org/2001/XMLSchema#string");

        EvaluationResult result =
                finder.getEvaluationResult("nishen",
                                           "eduPersonEntitlement",
                                           type);
        BagAttribute bag = (BagAttribute) result.getAttributeValue();

        Iterator i = bag.iterator();
        while (i.hasNext()) {
            AttributeValue a = (AttributeValue) i.next();
            log.info("value: " + a.encode());
        }
    }
}
