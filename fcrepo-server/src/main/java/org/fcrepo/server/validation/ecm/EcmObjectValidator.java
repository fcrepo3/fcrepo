/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.validation.ecm;

import java.util.List;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.ObjectValidityException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.rest.DefaultSerializer;
import org.fcrepo.server.storage.DOManager;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.ExternalContentManager;
import org.fcrepo.server.storage.types.Validation;
import org.fcrepo.server.validation.DOObjectValidator;

/**
 * A DOObjectValidator that performs Enhanced Content Model validation
 * for use in the DOObjectValidatorModule.
 * 
 * This is essentially a version of EcmValidator that
 * (1) conforms to DOObjectValidator
 * (2) is spring-configurable (so no DoMgr etc needed in constructor)
 * It could readily be merged into the existing EcmValidator
 * 
 * @author stephen.bayliss
 *
 */
public class EcmObjectValidator  implements DOObjectValidator{

	
	protected DOManager m_doMgr;
	protected ExternalContentManager m_ecm;
	
	protected OwlValidator m_relsExtValidator;
	protected DatastreamValidator m_datastreamValidator;

	// see spring config for constructor args
	public EcmObjectValidator(DOManager doManager, ExternalContentManager externalContentManager) {
		
		m_doMgr = doManager;
		m_ecm = externalContentManager;
		
        m_relsExtValidator = new OwlValidator(m_doMgr);
        m_datastreamValidator = new DatastreamValidator(m_doMgr);

	}
	
	@Override
	public void validate(Context context, DOReader reader)
			throws ServerException {
		DOReader currentObjectReader = reader;

		List<String> contentmodels = currentObjectReader.getContentModels();
		
		// don't validate self-referential content model objects - this would
		// effectively be validating a new (uncommitted) version of the object 
		// against the previous (committed) version, which doesn't make sense
		// (and prevents the server ingesting the initial system content model object)
		if (!contentmodels.contains("info:fedora/" + currentObjectReader.GetObjectPID())) {
		
		
			Validation validation = new Validation(reader.GetObjectPID());
			validation.setAsOfDateTime(null);
			validation.setContentModels(contentmodels);
	
			m_relsExtValidator.validate(context, null, currentObjectReader,
					validation);
	
			m_datastreamValidator.validate(context, currentObjectReader, null,
					validation, m_ecm);
	
			if (!validation.isValid()) {
				// FIXME: review msg output for exception
				DefaultSerializer serializer = new DefaultSerializer("n/a", context);
				String errors = serializer.objectValidationToXml(validation);
				throw new ObjectValidityException(errors);
	
			}
		}
	}

}
