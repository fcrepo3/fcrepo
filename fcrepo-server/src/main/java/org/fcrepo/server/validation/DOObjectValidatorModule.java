/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.validation;

import java.util.HashMap;
import java.util.Map;

import org.fcrepo.server.Context;
import org.fcrepo.server.Module;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.errors.ObjectValidityException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.DOReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * A module for validating digital objects.  Instead of validating the FOXML
 * (as per DOValidatorModule), the digital object itself is validated, wrapped
 * in a DOReader.
 * 
 * If validators have been configured (see Spring config doobjectvalidator.xml), then
 * any object creation or modification will result in the object being validated before
 * it is committed to storage.  Any validation errors will result in the modification 
 * being aborted.
 * 
 * @author stephen.bayliss
 *
 */
public class DOObjectValidatorModule extends Module implements
		DOObjectValidator {

	private static final Logger logger = LoggerFactory
			.getLogger(DOValidatorModule.class);
	
	// holds the set of validators to validate against
    private final Map<String, DOObjectValidator> m_validators =
            new HashMap<String, DOObjectValidator>();
    
    private boolean m_enabled = false;

	public DOObjectValidatorModule(Map<String, String> moduleParameters,
			Server server, String role) throws ModuleInitializationException {
		super(moduleParameters, server, role);
		
	}
    @Override
    public void postInitModule() throws ModuleInitializationException {
        try {
        } catch (Exception e) {
            throw new ModuleInitializationException(e.getMessage(),
                                                    "org.fcrepo.server.validation.DOObjectValidatorModule");
        }
    }	

    @Override
    public void validate(Context context, DOReader reader) throws ServerException {
		if (m_enabled) {
			// validate against all of the configured validators
			for (DOObjectValidator validator : m_validators.values()) {
				try {
					validator.validate(context, reader);
				} catch (ObjectValidityException e) {
					logger.error("Object validation error " + reader.GetObjectPID() + ": " + e.getMessage());
					throw e;
				}
			}
		}
	}
	
	// spring config of the validators
	public void setValidators(Map<String,? extends DOObjectValidator> validators){
		logger.info("Adding " + validators.size() + " object validators");
        m_validators.putAll(validators);
        if (m_validators.size() > 0) {
        	m_enabled = true;
        }
    }
	

}
