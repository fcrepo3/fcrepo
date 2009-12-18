/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities.install;

/**
 * Signals that an option's value was not valid.
 */
public class OptionValidationException
        extends Exception {

    private static final long serialVersionUID = 1L;

    private final String _id;

    public OptionValidationException(String msg, String optionId) {
        super(msg);
        _id = optionId;
    }

    public OptionValidationException(String msg,
                                     String optionId,
                                     Throwable cause) {
        super(msg, cause);
        _id = optionId;
    }

    public String getOptionId() {
        return _id;
    }

}
