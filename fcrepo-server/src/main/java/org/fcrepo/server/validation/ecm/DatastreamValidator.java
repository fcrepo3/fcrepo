package org.fcrepo.server.validation.ecm;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.RepositoryReader;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.Validation;
import org.fcrepo.server.validation.ecm.jaxb.DsCompositeModel;
import org.fcrepo.server.validation.ecm.jaxb.DsTypeModel;

import javax.xml.bind.JAXB;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Jun 26, 2010
 * Time: 12:03:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatastreamValidator {

    private SchemaValidator schemaValidator;

    private FormValidator formValidator;
    private RepositoryReader doMgr;
    private RelsIntValidator relsIntValidator;

    public DatastreamValidator(RepositoryReader doMgr) {
        this.doMgr = doMgr;
        schemaValidator = new SchemaValidator(doMgr);
        formValidator = new FormValidator();
        relsIntValidator = new RelsIntValidator(doMgr);
    }

    public void validate(Context context, DOReader currentObjectReader, Date asOfDateTime,
                         Validation validation) throws ServerException {


        // /Datastream validation stuff

        /** Get the objects content models */
        List<String> contentmodels = currentObjectReader.getContentModels();

        //For each content model, parse the DS-COMPOSITE-MODEL
        for (String contentmodel : contentmodels) {
            contentmodel = contentmodel.substring("info:fedora/".length());

            DOReader contentmodelReader = doMgr.getReader(false, context, contentmodel);
            Datastream dscompmodelDS = contentmodelReader.GetDatastream("DS-COMPOSITE-MODEL", asOfDateTime);

            if (dscompmodelDS == null) {//NO ds composite model, thats okay, continue to next content model
                continue;
            }
            DsCompositeModel dscompobject = JAXB.unmarshal(dscompmodelDS.getContentStream(), DsCompositeModel.class);
            for (DsTypeModel typeModel : dscompobject.getDsTypeModel()) {
                String DSID = typeModel.getID();
                Datastream objectDatastream = currentObjectReader.GetDatastream(DSID, asOfDateTime);

                if (objectDatastream == null) {
                    Boolean optional = typeModel.isOptional();
                    if (optional != null && optional) {
                        //optional datastream can be missing, not a problem
                    } else {
                        reportMissingDatastreamError(contentmodel, DSID, validation);
                    }
                    continue;
                }


                formValidator.checkFormAndMime(typeModel, objectDatastream, validation, contentmodelReader);
                schemaValidator
                        .validate(context, typeModel, objectDatastream, validation, contentmodelReader, asOfDateTime);
                relsIntValidator
                        .checkRelsInt(typeModel, objectDatastream, validation, contentmodelReader, asOfDateTime);
            }
        }


    }

    private void reportMissingDatastreamError(String contentmodel, String dsid, Validation validation) {
        List<String> problems = validation.getDatastreamProblems().get(dsid);
        if (problems == null) {
            problems = new ArrayList<String>();
            validation.getDatastreamProblems().put(dsid, problems);
        }

        //TODO some error code here?
        problems.add("Datastream '" + dsid + "' is required by the content model '" + contentmodel + "'");
        validation.setValid(false);
    }
}
