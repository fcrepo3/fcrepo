package org.fcrepo.server.storage;

import java.util.HashMap;
import java.util.Map;

import org.fcrepo.server.storage.types.DigitalObject;

public class MockFedoraHintsProvider implements FedoraStorageHintProvider{

    @Override
    public Map<Object, Object> getHintsForAboutToBeStoredObject(
            DigitalObject obj) {
        
        Map<Object, Object> hints = new HashMap<Object, Object>();
        hints.put("object_one", "1");
        hints.put("object_two", "2");
        return hints;
    }

    @Override
    public Map<Object, Object> getHintsForAboutToBeStoredDatastream(
            DigitalObject obj, String datastreamId) {
        Map<Object, Object> hints = new HashMap<Object, Object>();
        hints.put("ds_one", "1");
        hints.put("ds_two", "2");
        return hints;
    }

}
