/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.access.defaultdisseminator;

import java.lang.reflect.Method;

import fedora.server.errors.MethodNotFoundException;
import fedora.server.errors.ServerException;
import fedora.server.storage.types.Property;

/**
 * Invokes a method on an internal service.
 * 
 * <p>This is done using Java reflection where the service is the target object 
 * of a dynamic method request.
 * 
 * @author Sandy Payette
 */
public class ServiceMethodDispatcher {

    /**
     * Invoke a method on an internal service. This is done using Java
     * reflection where the service is the target object of a dynamic method
     * request.
     * 
     * @param service_object
     *        the target object of the service request
     * @param methodName
     *        the method to invoke on the target object
     * @param userParms
     *        parameters to the method to invoke on target object
     * @return
     * @throws ServerException
     */
    public Object invokeMethod(Object service_object,
                               String methodName,
                               Property[] userParms) throws ServerException {
        Method method = null;
        if (userParms == null) {
            userParms = new Property[0];
        }
        Object[] parmValues = new Object[userParms.length];
        Class[] parmClassTypes = new Class[userParms.length];
        for (int i = 0; i < userParms.length; i++) {
            // Get parm value.  Always treat the parm value as a string.
            parmValues[i] = new String(userParms[i].value);
            parmClassTypes[i] = parmValues[i].getClass();
        }
        // Invoke method: using Java Reflection
        try {
            method =
                    service_object.getClass().getMethod(methodName,
                                                        parmClassTypes);
            return method.invoke(service_object, parmValues);
        } catch (Exception e) {
            throw new MethodNotFoundException("Error executing method: "
                    + methodName, e);
        }
    }
}
