package org.fcrepo.server.security.xacml.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


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
     * @return Attribute
     */
    public Attribute get(String attributeName) {
        return attributes.get(attributeName);
    }
    /**
     * Get all attribute names for this designator (XACML target)
     * @return Set&lt;String&gt;
     */
    public Set<String> getAttributeNames() {
        return attributes.keySet();
    }
    /**
     * Add/update an attribute for this target.  Note that the attribute will have
     * no configuration (empty configuration is created).  Returns the attribued added/updated
     * @param attributeName
     * @return Attribute
     */
    public Attribute put(String attributeName) {
        Attribute attr = attributes.get(attributeName);
        if (attr == null) {
            attr = new Attribute();
        }
        attributes.put(attributeName, attr);
        return attr;
    }

}