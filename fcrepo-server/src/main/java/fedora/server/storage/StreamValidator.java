/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage;

import java.io.InputStream;

import fedora.server.errors.ValidationException;

/**
 * @author Chris Wilper
 */
public interface StreamValidator {

    public void validate(InputStream in, String validationType)
            throws ValidationException;

}
