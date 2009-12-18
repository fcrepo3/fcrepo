/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * @author Bill Niebel
 */
public class LowlevelStorageException
        extends StorageException {

    private static final long serialVersionUID = 1L;

    public LowlevelStorageException(boolean serverCaused,
                                    String bundleName,
                                    String code,
                                    String[] values,
                                    String[] details,
                                    Throwable cause) {
        super(null, code, null, null, cause);
        if (serverCaused) {
            setWasServer();
        }
    }

    public LowlevelStorageException(boolean serverCaused,
                                    String message,
                                    Throwable cause) {
        this(serverCaused, null, message, null, null, cause);
    }

    public LowlevelStorageException(boolean serverCaused, String message) {
        this(serverCaused, message, null);
    }

    @Override
    public String getMessage() {
        Throwable e = getCause();
        String temp = super.getMessage();
        if (e != null) {
            temp += "\t" + e.getMessage();
        }
        return temp;
    }

}
