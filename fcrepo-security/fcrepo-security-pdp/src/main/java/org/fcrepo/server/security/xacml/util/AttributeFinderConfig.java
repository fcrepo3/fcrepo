package org.fcrepo.server.security.xacml.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sun.xacml.attr.AttributeDesignator;

import org.fcrepo.server.security.xacml.pdp.finder.AttributeFinderException;




/**
 * Configuration for an attribute finder.
 *
 * Encapsulates a set of designators (corresponding to XACML policy targets), each
 * of which encapsulates a set of XACML attribute IDs and configurations
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class AttributeFinderConfig {

    private final Map<Integer, Designator> designators;

    public AttributeFinderConfig() {
        designators = new HashMap<Integer, Designator>();
    }
    /**
     * Get a Designator based on the Sun XACML designator ID
     * @param designator
     * @return
     */
    public Designator get(int designator) {
        return designators.get(new Integer(designator));
    }
    /**
     * Get a Designator based on the XACML target name (in lower case)
     * @param designatorName
     * @return
     */
    public Designator get(String designatorName) {
        return designators.get(getTarget(designatorName));
    }
    /**
     * Gets the designator IDs that have been configured
     * @return
     */
    public Set<Integer> getDesignatorIds() {
        return designators.keySet();
    }

    /**
     * Add or update a Designator.  The added/updated designator will have no attributes.
     * Returns the added/updated designator
     *
     * @param designatorName
     * @return
     * @throws AttributeFinderException
     */
    public Designator put(String designatorName) throws AttributeFinderException {
        int target = getTarget(designatorName);
        if (target != -1) {
            Designator des = designators.get(target);
            if (des == null) {
                des = new Designator();
            }
            designators.put(new Integer(target), des);
            return des;
        } else {
            throw new AttributeFinderException("Invalid attribute designator name " + designatorName);
        }
    }

    /**
     * Converts a XACML target name (in lower case) to the Sun XACML designator identifier
     * @param targetName
     * @return
     */
    private static Integer getTarget(String targetName) {
        if (targetName.equals("resource")) {
            return AttributeDesignator.RESOURCE_TARGET;
        } else if (targetName.equals("subject")) {
            return AttributeDesignator.SUBJECT_TARGET;
        } else if (targetName.equals("environment")) {
            return AttributeDesignator.ENVIRONMENT_TARGET;
        } else if (targetName.equals("action")) {
            return AttributeDesignator.ACTION_TARGET;
        } else {
            return -1;
        }
    }

    /**
     * Encapsulates a set of attributes for this designator (ie for XACML target corresponding to a target designator)
     *
     * @author Stephen Bayliss
     * @version $Id$
     */
    public class Designator {
        private final Map<String, Attribute> attributes;

        protected Designator() {
            attributes = new HashMap<String, Attribute>();
        }

        /**
         * Gets an attribute by name
         * @param attributeName
         * @return
         */
        public Attribute get(String attributeName) {
            return attributes.get(attributeName);
        }
        /**
         * Get all attribute names for this designator (XACML target)
         * @return
         */
        public Set<String> getAttributeNames() {
            return attributes.keySet();
        }
        /**
         * Add/update an attribute for this target.  Note that the attribute will have
         * no configuration (empty configuration is created).  Returns the attribued added/updated
         * @param attributeName
         * @return
         */
        public Attribute put(String attributeName) {
            Attribute attr = attributes.get(attributeName);
            if (attr == null) {
                attr = new Attribute();
            }
            attributes.put(attributeName, attr);
            return attr;
        }

        /**
         * Encapsulates an attribute together with
         * name/value configuration items
         *
         * @author Stephen Bayliss
         * @version $Id$
         */
        public class Attribute {
            private final Map<String, String> options;

            protected Attribute() {
                options = new HashMap<String, String>();
            }
            /**
             * Get named config item for this attribute
             * @param optionName
             * @return
             */
            public String get(String optionName) {
                return options.get(optionName);
            }
            /**
             * Add or update a config item for this attribute
             * @param optionName
             * @param optionValue
             * @return
             */
            public String put(String optionName, String optionValue) {
                options.put(optionName, optionValue);
                return optionValue;
            }
        }
    }

}
