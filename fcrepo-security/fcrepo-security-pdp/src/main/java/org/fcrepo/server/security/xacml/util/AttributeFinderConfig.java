package org.fcrepo.server.security.xacml.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.fcrepo.server.security.xacml.pdp.finder.AttributeFinderException;

import com.sun.xacml.attr.AttributeDesignator;




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
        return designators.get(Integer.valueOf(designator));
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
            designators.put(Integer.valueOf(target), des);
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

}