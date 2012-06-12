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
    public Map<String, String> getHintsForAboutToBeStoredObject(
            DigitalObject obj) {

        return null;
    }

    @Override
    public Map<String, String> getHintsForAboutToBeStoredDatastream(
            DigitalObject obj, String datastreamId) {
        return null;
    }

}
