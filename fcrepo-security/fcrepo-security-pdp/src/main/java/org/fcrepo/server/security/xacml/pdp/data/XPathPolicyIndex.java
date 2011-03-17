package org.fcrepo.server.security.xacml.pdp.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.server.security.xacml.util.AttributeBean;


/**
 * Abstract class for XPath-based policy index
 *
 * Provides helper functions for generating xpath queries, decomposing
 * request details etc
 *
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public abstract class XPathPolicyIndex
        extends PolicyIndexBase
        implements PolicyIndex {


    private static final Logger log =
        LoggerFactory.getLogger(XPathPolicyIndex.class.getName());


    protected XPathPolicyIndex() throws PolicyIndexException {
        super();
    }


    /**
     * Get XPath variables to use against an xpath query
     * @param attributeMap
     * @return a map of variable name / variable values
     */
    // FIXME: public for some external testing. -> protected
    public static Map<String, String> getXpathVariables(Map<String, Set<AttributeBean>> attributeMap) {
        // Set all the bind variables in the query context
        String[] types =
            new String[] {"Subject", "Resource", "Action",
        "Environment"};

        Map<String, String> variables = new HashMap<String, String>();

        for (String t : types) {
            int count = 0;
            for (AttributeBean bean : attributeMap.get(t.toLowerCase()
                                                       + "Attributes")) {
                if (bean.getId().equals(XACML_RESOURCE_ID)) {
                    variables.put("XacmlResourceId", bean.getId());

                    int c = 0;
                    for (String value : bean.getValues()) {
                        variables.put("XacmlResourceIdValue"
                                          + c, value);


                        c++;
                    }
                } else {
                    variables.put(t + "Id" + count,
                                             bean.getId());

                    if (log.isDebugEnabled()) {
                        log.debug(t + "Id" + count + " = '" + bean.getId()
                                  + "'");
                    }

                    int valueCount = 0;
                    for (String value : bean.getValues()) {
                        variables.put(t + "Id" + count
                                                 + "-Value"
                                                 + valueCount,
                                                 value);
                        if (log.isDebugEnabled()) {
                            log.debug(t + "Id" + count + "-Value"
                                      + valueCount + " = '" + value + "'");
                        }

                        valueCount++;
                    }

                    count++;
                }
            }
        }
        return variables;


    }

    /**
     * Creates an XPath query from the attributes
     * @param attributeMap attributes from request
     * @param r number of resource-id values
     * @return
     */
    // FIXME: public for some external testing
    public static String getXpath(Map<String, Set<AttributeBean>> attributeMap, int r) {
        // The query contains these 4 sections.
        String[] types =
                new String[] {"Subject", "Resource", "Action", "Environment"};

        int sections = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("/p:Policy/p:Target[");
        for (String t : types) {
            if (attributeMap.get(t.toLowerCase() + "Attributes").size() == 0) {
                continue;
            }

            if (sections > 0) {
                sb.append(" and ");
            }

            sections++;

            // start of Target
            sb.append("(");


            // selects policies which do not specify resources/subjects etc
            // old DBXML version - relies on dbxml metadata
            // sb.append("(exists(dbxml:metadata('m:any" + t + "')))");

            // instead...
            sb.append("(");
            // true if no resouces targets found in policy
            sb.append("not(//p:Policy/p:Target/p:" + t+ ")");
            sb.append(" or ");
            // matches the legacy 1.0 "AnyResource etc - should not be required
            sb.append("//p:Policy/p:Target/p:Resources/p:Any" + t);

            sb.append(")");


            // count(//p:Policy/p:Target[count(p:Resources) = 0)
            // not(//p:Policy/p:Target/p:Resources)

            // or
            // //p:Policy/p:Target/p:Resources/p:AnyResource


            // sb.append("((not('p:" + t + "s'))");

            int count = 0;
            for (AttributeBean bean : attributeMap.get(t.toLowerCase()
                    + "Attributes")) {
                sb.append(" or ");
                sb.append("(");

                // FIXME: r > 0 seems to cater for a case where there is/are resource attributeBeans, but
                // no attribute values are specified - could this actually happen?
                // TODO: remove dependency on r, or calculate here?
                if (bean.getId().equals(XACML_RESOURCE_ID) && r > 0) {
                    sb.append("p:" + t + "s/p:" + t + "/p:" + t + "Match/");
                    sb.append("p:" + t + "AttributeDesignator/@AttributeId = ");
                    sb.append("$XacmlResourceId");
                    sb.append(" and ");

                    /*
                     * sb.append("p:" + t + "s/p:" + t + "/p:" + t + "Match/");
                     * sb.append("p:" + t + "AttributeDesignator/@DataType = ");
                     * sb.append("$XacmlResourceType"); sb.append(" and ");
                     */

                    sb.append("(");
                    for (int i = 0; i < bean.getValues().size(); i++) {
                        if (i > 0) {
                            sb.append(" or ");
                        }

                        sb.append("p:" + t + "s/p:" + t + "/p:" + t + "Match/");
                        sb.append("p:AttributeValue = ");
                        sb.append("$XacmlResourceIdValue" + i);
                    }
                    sb.append(")");
                } else {
                    sb.append("p:" + t + "s/p:" + t + "/p:" + t + "Match/");
                    sb.append("p:" + t + "AttributeDesignator/@AttributeId = ");
                    sb.append("$" + t + "Id" + count);
                    sb.append(" and ");
                    sb.append("(");
                    /*
                     * sb.append("p:" + t + "s/p:" + t + "/p:" + t + "Match/");
                     * sb.append("p:" + t + "AttributeDesignator/@DataType = ");
                     * sb.append("$" + t + "Type" + count); sb.append(" and ");
                     */

                    for (int valueCount = 0; valueCount < bean.getValues()
                            .size(); valueCount++) {
                        if (valueCount > 0) {
                            sb.append(" or ");
                        }

                        sb.append("p:" + t + "s/p:" + t + "/p:" + t + "Match/");
                        sb.append("p:AttributeValue = ");
                        sb.append("$" + t + "Id" + count + "-Value"
                                + valueCount);
                    }
                    sb.append(")");

                    count++;
                }
                sb.append(")");
            }
            sb.append(")"); // end of Target
        }
        sb.append("]");

        return sb.toString();
    }



}
