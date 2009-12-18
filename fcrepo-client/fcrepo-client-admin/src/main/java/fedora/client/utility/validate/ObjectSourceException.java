/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate;

/**
 * Indicates a problem fetching objects from the {@link ObjectSource}.
 * 
 * @author Jim Blake
 */
public class ObjectSourceException
        extends Exception {

    /** It's serializable, so give it a version ID. */
    private static final long serialVersionUID = 1L;

    public ObjectSourceException() {
        super();
    }

    public ObjectSourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ObjectSourceException(String message) {
        super(message);
    }

    public ObjectSourceException(Throwable cause) {
        super(cause);
    }

}
