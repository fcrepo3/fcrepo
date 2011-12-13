package org.fcrepo.server.validation.ecm;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.ExternalContentManager;
import org.fcrepo.server.storage.RepositoryReader;
import org.fcrepo.server.storage.types.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Jun 24, 2010
 * Time: 10:01:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class EcmValidator {
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

        //TODO if the object and stuff exist
        DOReader currentObjectReader = doMgr.getReader(false, context, pid);

        List<String> contentmodels = currentObjectReader.getContentModels();
        Validation validation = new Validation(pid);
        validation.setAsOfDateTime(asOfDateTime);
        validation.setContentModels(contentmodels);

        relsExtValidator.validate(context, asOfDateTime, currentObjectReader, validation);

        datastreamValidator.validate(context, currentObjectReader, asOfDateTime, validation, m_exExternalContentManager);

        return validation;
    }

}

