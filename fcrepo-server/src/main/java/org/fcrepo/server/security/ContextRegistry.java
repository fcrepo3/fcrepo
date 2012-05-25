package org.fcrepo.server.security;

import org.fcrepo.server.Context;

public interface ContextRegistry {
    public void registerContext(Object key, Context value);

    public void unregisterContext(Object key);

    public Context getContext(Object key);
}
