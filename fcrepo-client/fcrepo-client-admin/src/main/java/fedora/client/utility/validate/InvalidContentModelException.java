/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate;

/**
 * Indicates that the content model object we are looking at is not valid --
 * e.g., missing the dataset, invalid format, etc.
 * 
 * @author Jim Blake
 */
public class InvalidContentModelException
        extends Exception {

    /** It's serializable, so give it a version ID. */
    private static final long serialVersionUID = 1L;

    private final String contentModelPid;

    public InvalidContentModelException(String contentModelPid,
                                        String message,
                                        Throwable cause) {
        super(message, cause);
        this.contentModelPid = contentModelPid;
    }

    public InvalidContentModelException(String contentModelPid, String message) {
        super(message);
        this.contentModelPid = contentModelPid;
    }

    public String getContentModelPid() {
        return contentModelPid;
    }
}
