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


    // specifies the XACML policy targest
    // the attribute map uses these in the format
    // lowerCase(target) + "Attributes"
    // and the names are used in the generation of the xpath
    // eg target + "s" for the target element, target for each individual element beneath that, etc
    private static String[] targets =
            new String[] {"Subject", "Resource", "Action", "Environment"};


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

        Map<String, String> variables = new HashMap<String, String>();

        for (String t : targets) {
            int count = 0;
            for (AttributeBean bean : attributeMap.get(t.toLowerCase()
                                                       + "Attributes")) {
                if (bean.getId().equals(XACML_RESOURCE_ID)) {
                    variables.put("XacmlResourceId", bean.getId());

                    if (log.isDebugEnabled()) {
                        log.debug("XacmlResourceId = '" + bean.getId()
                                  + "'");
                    }


                    int c = 0;
                    for (String value : bean.getValues()) {
                        variables.put("XacmlResourceIdValue"
                                          + c, value);
                        if (log.isDebugEnabled()) {
                            log.debug("XacmlResourceIdValue"
                                      + c + " = '" + value + "'");
                        }


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
    protected static String getXpath(Map<String, Set<AttributeBean>> attributeMap) {

        int sections = 0;
        StringBuilder sb = new StringBuilder();

        // FIXME:
        // this from the original dbxml implementation
        // "r" is the count of resource values where the attribute ID is xacml resource-id
        // not clear why this is actually needed but the query generator tests for a zero value
        int resourceValueCount = 0;
        for (AttributeBean b : attributeMap.get("resourceAttributes")) {
            if (b.getId().equals(XACML_RESOURCE_ID)) {
                resourceValueCount = resourceValueCount + b.getValues().size();
            }

        }



        // FIXME: will not work with policy sets
        sb.append("/p:Policy/p:Target[");
        for (String t : targets) {
            if (attributeMap.get(t.toLowerCase() + "Attributes").size() == 0) {
                continue;
            }

            if (sections > 0) {
                sb.append(" and ");
            }

            sections++;

            // Target:Start
            sb.append("(");


            // TargetNotSpecified:Start
            // selects policies which do not specify resources/subjects etc
            sb.append("(");
            // true if no resouces targets found in policy
            sb.append("not(p:" + t+ "s)");
            sb.append(" or ");
            // matches the legacy 1.0 "AnyResource etc - should not be required
            sb.append("p:" + t + "s/p:Any" + t);

            sb.append(")");  // TargetNotSpecified:End

            int count = 0;

            // test for a policy attribute id not specified in the request
            // (as the request doesn't (yet) know about the attribute, there should be a match; eg custom RI attribute)
            sb.append(" or (");
            sb.append("p:" + t + "s" + "/p:" + t + "/p:" + t + "Match/p:" + t + "AttributeDesignator/@AttributeId[" );
            // do each attribute ID
            boolean firstBean = true;
            for (AttributeBean bean : attributeMap.get(t.toLowerCase() + "Attributes")) {

                if (!firstBean) {
                    sb.append(" and ");
                }

                // special case for XACML_RESOURCE_ID (but see FIXME note below...)
                if (bean.getId().equals(XACML_RESOURCE_ID) && resourceValueCount > 0) {
                    // tests that policy attribute id is not the request attribute id
                    sb.append(". != $XacmlResourceId");
                    firstBean = false;
                } else {
                    // same for non XACML_RESOURCE_ID
                    sb.append("$" + t + "Id" + count);
                    firstBean = false;
                }
            }

            sb.append("]");
            sb.append(")");

            // Do each target attribute id
            for (AttributeBean bean : attributeMap.get(t.toLowerCase()
                    + "Attributes")) {
                sb.append(" or ");
                sb.append("(");

                // FIXME: r = 0 seems to cater for a case where there is/are resource attributeBeans, but
                // no attribute values are specified - could this actually happen?

                if (bean.getId().equals(XACML_RESOURCE_ID) && resourceValueCount > 0) {
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
            sb.append(")"); // Target:End
        }
        sb.append("]");

        return sb.toString();
    }



}
