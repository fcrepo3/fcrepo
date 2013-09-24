/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.messaging;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.fcrepo.server.Server;
import org.fcrepo.server.proxy.AbstractInvocationHandler;
import org.fcrepo.server.proxy.ModuleConfiguredInvocationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link java.lang.reflect.InvocationHandler InvocationHandler} responsible
 * for sending notifications via {@link Messaging Messaging}.
 *
 * @author Edwin Shin
 * @version $Id$
 */
public class NotificationInvocationHandler
        extends AbstractInvocationHandler
        implements ModuleConfiguredInvocationHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(MessagingModule.class);

    private Messaging messaging;

    private final ExecutorService exec = Executors.newCachedThreadPool();

    /**
     * Note: Setting of <code>messaging</code> does not take place in this
     * constructor because the construction of the Management proxy chain (of
     * which this class is intended to be a part) takes place in
     * ManagementModule.postInit(), i.e., prior to completion of Server
     * initialization.
     */
    public NotificationInvocationHandler() {};

    /**
     * This constructor is intended for testing.
     * @param messaging
     */
    public NotificationInvocationHandler(Messaging messaging) {
        if (messaging != null) {
            this.messaging = messaging;
        }
    }
    
    public void init(Server server) {
        messaging = (MessagingModule)server.getModule("org.fcrepo.server.messaging.Messaging");
        if (messaging == null) {
            logger.warn("Unable to load MessagingModule.");
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        Object returnValue = null;

        try {
            returnValue = method.invoke(target, args);
        } catch(InvocationTargetException ite) {
            throw ite.getTargetException();
        }

        if (messaging != null && !exec.isShutdown()) {
            exec.submit(new Notifier(method, args, returnValue));
        }

        return returnValue;
    }

    @Override
    public void close() {
        exec.shutdown();
    }

    class Notifier implements Callable<Void> {
        private final Method method;
        private final Object[] args;
        private final Object returnValue;

        public Notifier(Method method, Object[] args, Object returnValue) {
            this.method = method;
            this.args = args;
            this.returnValue = returnValue;
        }

        public Void call() throws Exception {
            FedoraMethod fm = new FedoraMethod(method, args, returnValue);
            messaging.send(fm);
            return null;
        }
    }
}
