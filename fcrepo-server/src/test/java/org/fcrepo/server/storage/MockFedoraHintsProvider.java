package org.fcrepo.server.storage;

import java.util.HashMap;
import java.util.Map;

import org.fcrepo.server.storage.types.DigitalObject;

public class MockFedoraHintsProvider implements FedoraStorageHintProvider{

    @Override
    public Map<String, String> getHintsForAboutToBeStoredObject(
            DigitalObject obj) {
        
        Map<String, String> hints = new HashMap<String, String>();
        hints.put("object_one", "1");
        hints.put("object_two", "2");
        return hints;
    }

    @Override
    public Map<String, String> getHintsForAboutToBeStoredDatastream(
            DigitalObject obj, String datastreamId) {
        Map<String, String> hints = new HashMap<String, String>();
        hints.put("ds_one", "1");
        hints.put("ds_two", "2");
        return hints;
    }

}
