/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.access.defaultdisseminator;

import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.types.MethodDef;


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
