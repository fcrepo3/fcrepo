/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.types;

/**
 * A data structure for holding properties as name/value pairs.
 * 
 * @author Ross Wayland
 */
public class Property {

    public String name;

    public String value;

    public Property() {
    }

    public Property(String propertyName, String propertyValue) {
        name = propertyName;
        value = propertyValue;
    }
}
