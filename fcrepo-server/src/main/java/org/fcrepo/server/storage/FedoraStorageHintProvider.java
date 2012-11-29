package org.fcrepo.server.storage;

import java.util.Map;

import org.fcrepo.server.storage.types.DigitalObject;

public interface FedoraStorageHintProvider {
    Map<Object, Object> getHintsForAboutToBeStoredObject(DigitalObject obj);
    Map<Object, Object> getHintsForAboutToBeStoredDatastream(DigitalObject obj, String datastreamId);
}
