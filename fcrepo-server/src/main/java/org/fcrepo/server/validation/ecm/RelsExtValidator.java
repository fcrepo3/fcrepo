package org.fcrepo.server.validation.ecm;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.LowlevelStorageException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.RepositoryReader;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.Validation;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Jun 26, 2010
 * Time: 11:55:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class RelsExtValidator {
    private RepositoryReader doMgr;

    public RelsExtValidator(RepositoryReader doMgr) {
        //To change body of created methods use File | Settings | File Templates.
        this.doMgr = doMgr;
    }


    public void validate(Context context, Date asOfDateTime, DOReader currentObjectReader,
                         Validation validation) throws ServerException {


        List<String> contentmodels = currentObjectReader.getContentModels();

        Map<String, InputStream> ontologies = new HashMap<String, InputStream>();
        for (String contentmodel : contentmodels) {
            contentmodel = contentmodel.substring("info:fedora/".length());
            DOReader contentmodelReader;
            try {
                contentmodelReader = doMgr.getReader(false, context, contentmodel);
            } catch (LowlevelStorageException e) {//content model could not be found
                continue;
            }

            if (asOfDateTime != null) { //disregard content models created after the asOfDateTime
                if (!contentmodelReader.getCreateDate().before(asOfDateTime)) {
                    continue;
                }
            }

            Datastream ontologyDS = contentmodelReader.GetDatastream("ONTOLOGY", asOfDateTime);

            if (ontologyDS == null) {//No ontology in the content model, continue
                continue;
            }
            InputStream ontologyStream = ontologyDS.getContentStream();
            ontologies.put(contentmodel, ontologyStream);

        }
        OntologyValidator ontologyValidator = new OntologyValidator(doMgr, context);
        Datastream relsextDS = currentObjectReader.GetDatastream("RELS-EXT", asOfDateTime);
        if (relsextDS != null) {
            InputStream relsExtStream = relsextDS.getContentStream();
            ontologyValidator.validate(relsExtStream, ontologies, validation, currentObjectReader.GetObjectPID());
        }


    }
}
