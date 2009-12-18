/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.proxy;

import java.lang.reflect.Proxy;

/**
 * @author Edwin Shin
 * @version $Id$
 */
public class ProxyFactory {
    public static Object getProxy(Object target, Object[] invocationHandlers)
            throws Exception {
        if (invocationHandlers != null && invocationHandlers.length > 0) {
            Object proxy = target;
            for (int i = 0; i < invocationHandlers.length; i++) {
                proxy =
                        getProxy(target,
                                 (AbstractInvocationHandler) invocationHandlers[i],
                                 proxy);
            }
            return proxy;
        } else {
            return target;
        }
    }

    private static Object getProxy(Object target,
                                   AbstractInvocationHandler invocationHandler,
                                   Object proxy) throws Exception {
        if (invocationHandler == null) {
            return proxy;
        }
        invocationHandler.setTarget(proxy);

        return Proxy.newProxyInstance(target.getClass().getClassLoader(),
                                      target.getClass().getInterfaces(),
                                      invocationHandler);
    }
}