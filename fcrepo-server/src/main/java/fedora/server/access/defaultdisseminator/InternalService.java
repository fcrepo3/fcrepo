/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.access.defaultdisseminator;

import fedora.server.errors.ServerException;
import fedora.server.storage.types.MethodDef;

/**
 * Abstract class that should be extended by every internal service class.
 * 
 * This defines the methods that all internal services must implement.
 * 
 * @author Sandy Payette
 */
public abstract class InternalService {

    /**
     * A method to reflect the method definitions implemented by the
     * internal service.
     */
    public static MethodDef[] reflectMethods() throws ServerException {
        return null;
    }

}
