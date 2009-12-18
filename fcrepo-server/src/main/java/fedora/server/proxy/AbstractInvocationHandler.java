/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.proxy;

import java.lang.reflect.InvocationHandler;

/**
 * @author Edwin Shin
 * @version $Id$
 */
public abstract class AbstractInvocationHandler
        implements InvocationHandler {

    protected Object target = null;
    
    /**
     * Sets the object upon which the method is ultimately invoked.
     * 
     * @param target
     */
    public void setTarget(Object target) {
        this.target = target;
    }
    
    public void close() {}
}
