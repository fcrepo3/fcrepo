/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.validation;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.DOReader;

/**
 * Validator for digital objects (instances of DigitalObject, wrapped in a
 * DOReader).
 * 
 * Allows validation based on the Java object rather than validating FOXML
 * 
 * @author stephen.bayliss
 * 
 */
public interface DOObjectValidator {

	/**
	 * Validate a digital object wrapped in a DOReader
	 * 
	 * @param context
	 * @param reader
	 *            - DOReader wrapping the object
	 * @throws ServerException. Throw
	 *             an ObjectValidityException if something invalid is found
	 *             (with a useful message indicating what failed) and other
	 *             types of ServerException if the validation process itself
	 *             failed
	 */
	public void validate(Context context, DOReader reader) throws ServerException;

}
