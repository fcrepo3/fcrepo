package org.fcrepo.server.validation.ecm;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.ObjectValidityException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.rest.DefaultSerializer;
import org.fcrepo.server.storage.DOManager;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.ExternalContentManager;
import org.fcrepo.server.storage.RepositoryReader;
import org.fcrepo.server.storage.types.Validation;
import org.fcrepo.server.validation.DOObjectValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Jun 24, 2010
 * Time: 10:01:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class EcmValidator implements DOObjectValidator {
    private RepositoryReader doMgr;
    private ExternalContentManager m_exExternalContentManager;


    private OwlValidator relsExtValidator;

    private static final Logger logger =
            LoggerFactory.getLogger(EcmValidator.class);

    private DatastreamValidator datastreamValidator;

    public EcmValidator(RepositoryReader doMgr, ExternalContentManager m_exExternalContentManager) {

        this.doMgr = doMgr;
        this.m_exExternalContentManager = m_exExternalContentManager;
        relsExtValidator = new OwlValidator(doMgr);

        datastreamValidator = new DatastreamValidator(doMgr);
    }

    public Validation validate(Context context, String pid, Date asOfDateTime)
            throws ServerException {
        if (asOfDateTime == null) asOfDateTime = new Date();
        //TODO if the object and stuff exist
        DOReader currentObjectReader = doMgr.getReader(false, context, pid);

        List<String> contentmodels = currentObjectReader.getContentModels();

        return doValidate(context, currentObjectReader, asOfDateTime, contentmodels);
        
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
		String pid = currentObjectReader.GetObjectPID();
		String objectUri = "info:fedora/" + pid;
		if (!contentmodels.contains(objectUri)) {
		
		Validation validation = doValidate(context, currentObjectReader, new Date(), contentmodels);
	
			if (!validation.isValid()) {
				throw new ObjectValidityException("ECM validation failure", validation);
	
			}
		}
		
	}
    
    
    protected  Validation doValidate(Context context, DOReader reader, Date asOfDateTime, List<String> contentModels) throws ServerException {

    	String pid = reader.GetObjectPID();
        Validation validation = new Validation(pid);
        validation.setAsOfDateTime(asOfDateTime);
        validation.setContentModels(contentModels);
        Date createDate = reader.getCreateDate();
        if (createDate.after(asOfDateTime)) {
            reportNonExistenceProblem(validation, pid, createDate, asOfDateTime);
            return validation;
        }

        relsExtValidator.validate(context, asOfDateTime, reader, validation);

        datastreamValidator.validate(context, reader, asOfDateTime, validation, m_exExternalContentManager);
    	
        return validation;
    }
    
    private static void reportNonExistenceProblem(Validation validation,
                                                  String pid, Date createDate,
                                                  Date asOfDateTime) {
        List<String> problems = validation.getObjectProblems();
        if (problems == null) {
            problems = new ArrayList<String>();
            validation.setObjectProblems(problems);
        }
        problems.add(Errors.doesNotExistAsOfDateTime(pid,
                                                     createDate, asOfDateTime));
        validation.setValid(false);
        
    }

}

