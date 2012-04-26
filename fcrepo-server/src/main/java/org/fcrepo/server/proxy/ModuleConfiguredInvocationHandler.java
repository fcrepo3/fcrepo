package org.fcrepo.server.proxy;

import org.fcrepo.server.Server;
import org.fcrepo.server.errors.InitializationException;
        
public interface ModuleConfiguredInvocationHandler {
    public void init(Server server) throws InitializationException;
}

    