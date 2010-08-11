package org.fcrepo.server.validation.ecm;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.RepositoryReader;
import org.fcrepo.server.storage.types.Validation;

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

    private static final XPath xpathCompiler =
            XPathFactory.newInstance().newXPath();

    private OwlValidator relsExtValidator;


    private DatastreamValidator datastreamValidator;

    public EcmValidator(RepositoryReader doMgr) {

        this.doMgr = doMgr;
        relsExtValidator = new OwlValidator(doMgr);

        datastreamValidator = new DatastreamValidator(doMgr);
    }

    public Validation validate(Context context, String pid, Date asOfDateTime)
            throws ServerException {

        //TODO if the object and stuff exist
        DOReader currentObjectReader = doMgr.getReader(false, context, pid);

        List<String> contentmodels = currentObjectReader.getContentModels();
        Validation validation = new Validation(pid);
        validation.setContentModels(contentmodels);

        relsExtValidator.validate(context, asOfDateTime, currentObjectReader, validation);

        datastreamValidator.validate(context, currentObjectReader, asOfDateTime, validation);

        return validation;
    }

}

