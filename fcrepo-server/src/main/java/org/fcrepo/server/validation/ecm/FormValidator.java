package org.fcrepo.server.validation.ecm;

import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.Validation;
import org.fcrepo.server.validation.ecm.jaxb.DsTypeModel;
import org.fcrepo.server.validation.ecm.jaxb.Form;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Jun 26, 2010
 * Time: 11:54:29 AM
 * To change this template use File | Settings | File Templates.
 */
public class FormValidator {


    void checkFormAndMime(DsTypeModel typeModel, Datastream objectDatastream, Validation validation,
                          DOReader contentmodelReader) throws ServerException {
        List<Form> forms = typeModel.getForm();
        boolean valid = false;
        if (forms.size() == 0){
            valid = true;
        }
        for (Form form : forms) {
            boolean uriMatch = true;
            if (form.getFORMATURI() != null) {
                if (!form.getFORMATURI().equals(objectDatastream.DSFormatURI)) {
                    uriMatch = false;
                }
            }
            if (!uriMatch) {
                continue;
            }
            boolean mimeMatch = true;
            if (form.getMIME() != null) {
                if (!form.getMIME().equals(objectDatastream.DSMIME)) {

                    mimeMatch = false;
                }
            }
            if (mimeMatch && uriMatch) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            String dsid = objectDatastream.DatastreamID;
            List<String> problems = validation.getDatastreamProblems().get(dsid);
            if (problems == null) {
                problems = new ArrayList<String>();
                validation.getDatastreamProblems().put(dsid, problems);
            }
            //TODO some error code here?
            String contentmodel = contentmodelReader.GetObjectPID();
            problems.add(Errors.invalidFormatURIorMimeType(dsid,contentmodel));
            validation.setValid(false);
        }
    }
}
