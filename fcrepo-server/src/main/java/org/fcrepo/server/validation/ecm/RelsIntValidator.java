package org.fcrepo.server.validation.ecm;

import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.RepositoryReader;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.Validation;
import org.fcrepo.server.validation.ecm.jaxb.DsTypeModel;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Jun 26, 2010
 * Time: 11:54:53 AM
 * To change this template use File | Settings | File Templates.
 */
public class RelsIntValidator {
    private RepositoryReader doMgr;

    public RelsIntValidator(RepositoryReader doMgr) {
        this.doMgr = doMgr;
    }

    void checkRelsInt(DsTypeModel typeModel, Datastream objectDatastream, Validation validation,
                      DOReader contentmodelReader, Date asOfDateTime) {
        //TODO
    }
}
