package org.fcrepo.server.security.impl;

import java.util.Hashtable;

import org.fcrepo.server.Context;
import org.fcrepo.server.security.ContextRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HashtableContextRegistry implements ContextRegistry {

    private static final Logger logger = LoggerFactory.getLogger(HashtableContextRegistry.class);

    private final Hashtable<Object, Context> registry = new Hashtable<Object, Context>();
    @Override
    public void registerContext(Object key, Context value) {
        logger.debug("registering {}", key);
        registry.put(key, value);
    }

    @Override
    public void unregisterContext(Object key) {
        logger.debug("unregistering {}", key);
        registry.remove(key);
    }

    @Override
    public Context getContext(Object key) {
        return registry.get(key);
    }

}
