/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage;

import java.io.InputStream;

import java.util.Date;

import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.types.DeploymentDSBindSpec;
import org.fcrepo.server.storage.types.MethodDef;
import org.fcrepo.server.storage.types.MethodDefOperationBind;
import org.fcrepo.server.storage.types.MethodParmDef;


/**
 * Interface for reading Service Deployment Objects.
 * 
 * @author Sandy Payette
 */
public interface ServiceDeploymentReader
        extends DOReader {

    public MethodDef[] getServiceMethods(Date versDateTime)
            throws ServerException;

    public MethodDefOperationBind[] getServiceMethodBindings(Date versDateTime)
            throws ServerException;

    public InputStream getServiceMethodsXML(Date versDateTime)
            throws ServerException;

    public DeploymentDSBindSpec getServiceDSInputSpec(Date versDateTime)
            throws ServerException;

    public MethodParmDef[] getServiceMethodParms(String methodName,
                                                 Date versDateTime)
            throws ServerException;
}
