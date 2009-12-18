/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate.process;

/**
 * Indicates bad or missing arguments to the {@link ValidatorProcess}.
 * 
 * @author Jim Blake
 */
public class ValidatorProcessUsageException
        extends RuntimeException {

    /** It's serializable, so give it a version ID. */
    private static final long serialVersionUID = 1L;

    public ValidatorProcessUsageException() {
        super(ValidatorProcessParameters.USAGE);
    }

    public ValidatorProcessUsageException(String message, Throwable cause) {
        super(message + "\n" + ValidatorProcessParameters.USAGE, cause);
    }

    public ValidatorProcessUsageException(String message) {
        super(message + "\n" + ValidatorProcessParameters.USAGE);
    }

    public ValidatorProcessUsageException(Throwable cause) {
        super(ValidatorProcessParameters.USAGE, cause);
    }

}
