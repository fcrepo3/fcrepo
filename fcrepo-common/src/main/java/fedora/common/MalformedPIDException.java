/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common;

/**
 * Thrown when a PID is not well-formed. <p/>
 * 
 * @author Chris Wilper
 */
public class MalformedPIDException
        extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Construct a MalformedPIDException with the given reason.
     */
    public MalformedPIDException(String why) {
        super(why);
    }

}
