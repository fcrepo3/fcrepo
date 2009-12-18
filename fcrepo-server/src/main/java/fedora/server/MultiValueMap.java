/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

public class MultiValueMap {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(MultiValueMap.class.getName());

    private boolean locked = false;

    private final Map attributes = new HashMap();

    /**
     * Creates and initializes the <code>WritableContext</code>.
     * <p>
     * </p>
     * A pre-loaded Map of name-value pairs comprising the context.
     */
    public MultiValueMap() {
    }

    public String setReturn(String name, Object value) throws Exception {
        set(name, value);
        return name;
    }

    public void set(String name, Object value) throws Exception {
        if (name == null) {
            String msg = here + ": set() has null name, value=" + value;
            LOG.debug(msg);
            throw new Exception(msg);
        }
        if (locked) {
            String msg = here + ": set() has object locked";
            LOG.debug(msg);
            throw new Exception(msg);
        }
        if (value instanceof String) {
        } else if (value instanceof String[]) {
            if (((String[]) value).length == 1) {
                value = ((String[]) value)[0];
            }
        } else if (value == null) {
            value = "";
        } else {
            String msg = here + ": set() has unhandled type";
            LOG.debug(msg);
            throw new Exception(msg);
        }
        attributes.put(name, value);
    }

    public void lock() {
        locked = true;
    }

    public Iterator names() {
        return attributes.keySet().iterator();
    }

    public int length(String name) {
        if (attributes.get(name) instanceof String) {
            return 1;
        } else if (attributes.get(name) instanceof String[]) {
            return ((String[]) attributes.get(name)).length;
        } else {
            return 0;
        }
    }

    public String getString(String name) {
        return (String) attributes.get(name);
    }

    public String[] getStringArray(String name) {
        Object value = attributes.get(name);
        if (value instanceof String) {
            return new String[] {(String) value};
        } else {
            return (String[]) value;
        }
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        Iterator it = attributes.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            buffer.append(key + "=[");
            if (attributes.get(key) instanceof String) {
                String value = (String) attributes.get(key);
                buffer.append(value);
            } else if (attributes.get(key) instanceof String[]) {
                String[] temp = (String[]) attributes.get(key);
                String comma = "";
                for (String element : temp) {
                    buffer.append(comma + element);
                    comma = ",";
                }
            }
            buffer.append("]\n");
        }
        return buffer.toString();
    }

    /**
     * Test whether this map is equal to another similar one. We can't just test
     * for equality of the underlying maps, since they may contain arrays of
     * Strings as values, and those arrays are only equal if identical.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!obj.getClass().equals(MultiValueMap.class)) {
            return false;
        }
        MultiValueMap that = (MultiValueMap) obj;

        return locked == that.locked && equalMaps(attributes, that.attributes);
    }

    private static boolean equalMaps(Map thisMap, Map thatMap) {

        /* Check for obvious differences (same number and value of keys) */
        if (!thisMap.keySet().equals(thatMap.keySet())) {
            return false;
        }

        Iterator theseKeys = thisMap.keySet().iterator();

        /* Now do a deep compare of contents.. */
        while (theseKeys.hasNext()) {
            Object key = theseKeys.next();
            if (!equalValues(thisMap.get(key), thatMap.get(key))) {
                return false;
            }
        }

        return true;
    }

    /**
     * If values are arrays, we need to check deep equality. If not arrays, just
     * test simple equality. One array and one non-array? Those aren't equal.
     */
    private static boolean equalValues(Object thisValue, Object thatValue) {
        if (thisValue instanceof Object[]) {
            if (thatValue instanceof Object[]) {
                return Arrays
                        .equals((Object[]) thisValue, (Object[]) thatValue);
            } else {
                return false;
            }
        } else {
            return thisValue.equals(thatValue);
        }
    }

    @Override
    public int hashCode() {
        return attributes.hashCode() + (locked ? 1 : 0);
    }

    protected static final String here;
    static {
        here = "MultiValueMap";
    }

}
