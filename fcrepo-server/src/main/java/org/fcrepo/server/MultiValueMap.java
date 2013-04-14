/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiValueMap {

    private static final Logger logger =
            LoggerFactory.getLogger(MultiValueMap.class);

    private static final String [] EMPTY = new String[0];

    private boolean locked = false;

    private final Map<String,String[]> attributes = new HashMap<String,String[]>();

    /**
     * Creates and initializes the <code>WritableContext</code>.
     * <p>
     * </p>
     * A pre-loaded Map of name-value pairs comprising the context.
     */
    public MultiValueMap() {
    }

    public String setReturn(String name, String value)
            throws IllegalArgumentException, IllegalStateException {
        set(name, value);
        return name;
    }

    public void set(String name, String value)
            throws IllegalArgumentException, IllegalStateException {
        audit(name, value);
        if (value != null) {
            String [] temp = attributes.get(name);
            if (temp == null || temp.length != 1) {
                attributes.put(name, new String[]{value});
            } else temp[0] = value;
        } else {
            attributes.put(name, EMPTY);
        }
    }

    public String setReturn(String name, String[] value)
            throws IllegalArgumentException, IllegalStateException {
        set(name, value);
        return name;
    }

    public void set(String name, String[] value)
            throws IllegalArgumentException, IllegalStateException {
        audit(name, value);
        if (value != null) {
            attributes.put(name, value);
        } else {
            attributes.put(name, new String[0]);
        }
    }

    public void lock() {
        locked = true;
    }

    public Iterator<String> names() {
        return attributes.keySet().iterator();
    }

    public int length(String name) {
        if (attributes.get(name) != null) {
            return attributes.get(name).length;
        } else {
            return 0;
        }
    }

    /**
     * Returns the first (or only) value for an attribute
     * @param name
     * @return first available value
     */
    public String getString(String name) {
        return attributes.get(name)[0];
    }

    public String[] getStringArray(String name) {
        return attributes.get(name);
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        Iterator<String> it = attributes.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            buffer.append(key + "=[");
            if (attributes.get(key) != null) {
                String[] temp = attributes.get(key);
                boolean second = false;
                for (String element : temp) {
                    if (second) buffer.append(',');
                    buffer.append(element);
                    second |= true;
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
        if (!(obj instanceof MultiValueMap)) {
            return false;
        }
        MultiValueMap that = (MultiValueMap) obj;

        return locked == that.locked && equalMaps(attributes, that.attributes);
    }

    private static boolean equalMaps(Map<String,String[]> thisMap, Map<String,String[]> thatMap) {

        /* Check for obvious differences (same number and value of keys) */
        if (!thisMap.keySet().equals(thatMap.keySet())) {
            return false;
        }

        Iterator<String> theseKeys = thisMap.keySet().iterator();

        /* Now do a deep compare of contents.. */
        while (theseKeys.hasNext()) {
            Object key = theseKeys.next();
            if (!Arrays.equals(thisMap.get(key), thatMap.get(key))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return attributes.hashCode() + (locked ? 1 : 0);
    }

    protected static final String here;
    static {
        here = "MultiValueMap";
    }

    private void audit(String key, Object value)
            throws IllegalArgumentException, IllegalStateException {
        if (key == null) {
            String msg = "{}: set() has null name, value={}";
            logger.debug(msg, here, value);
            throw new IllegalArgumentException(msg);
        }
        if (locked) {
            String msg = "{}: set() has object locked";
            logger.debug(msg, here);
            throw new IllegalStateException(msg);
        }
    }

}
