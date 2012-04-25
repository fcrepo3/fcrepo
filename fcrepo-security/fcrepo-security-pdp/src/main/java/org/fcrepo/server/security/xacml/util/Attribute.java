package org.fcrepo.server.security.xacml.util;

import java.util.HashMap;
import java.util.Map;


/**
 * Encapsulates an attribute together with
 * name/value configuration items
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class Attribute {
    private final Map<String, String> options;

    public Attribute() {
        options = new HashMap<String, String>();
    }
    public Attribute(Map<String, String> options) {
        this.options = options;
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