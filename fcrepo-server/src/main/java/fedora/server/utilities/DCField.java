/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities;

/**
 *
 * @author Edwin Shin
 * @since 3.0.1
 * @version $Id$
 */
public class DCField {
    private final String value;
    private final String lang;

    public DCField(String value) {
        this(value, null);
    }

    public DCField(String value, String lang) {
        this.value = value;
        this.lang = lang;
    }

    public String getValue() {
        return value;
    }

    public String getLang() {
        return lang;
    }
}
