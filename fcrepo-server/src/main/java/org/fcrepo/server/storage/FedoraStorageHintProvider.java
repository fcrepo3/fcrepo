package org.fcrepo.server.storage;

import java.util.Map;

import org.fcrepo.server.storage.types.DigitalObject;

public interface FedoraStorageHintProvider {
    Map<String, String> getHintsForAboutToBeStoredObject(DigitalObject obj);
    Map<String, String> getHintsForAboutToBeStoredDatastream(DigitalObject obj, String datastreamId);
}
