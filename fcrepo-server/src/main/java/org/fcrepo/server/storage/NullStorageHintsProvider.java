package org.fcrepo.server.storage;

import java.util.Map;

import org.fcrepo.server.storage.types.DigitalObject;


/**
 * The default FedoraStorageHintProvider provides null implementation
 * @author jerrypan
 *
 */
public class NullStorageHintsProvider implements FedoraStorageHintProvider {

    @Override
    public Map<Object, Object> getHintsForAboutToBeStoredObject(
            DigitalObject obj) {
        return null;
    }

    @Override
    public Map<Object, Object> getHintsForAboutToBeStoredDatastream(
            DigitalObject obj, String datastreamId) {        
        return null;
    }

}
