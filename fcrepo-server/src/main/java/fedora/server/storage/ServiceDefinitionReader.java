/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage;

import java.io.InputStream;

import java.util.Date;

import fedora.server.errors.ServerException;
import fedora.server.storage.types.MethodDef;

/**
 * Interface for reading Service Deployment Objects.
 * 
 * @author Sandy Payette
 */
public interface ServiceDefinitionReader
        extends DOReader {

    public MethodDef[] getAbstractMethods(Date versDateTime)
            throws ServerException;

    public InputStream getAbstractMethodsXML(Date versDateTime)
            throws ServerException;
}
