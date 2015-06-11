/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.storage;

import java.io.InputStream;

import java.util.Date;

import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.ServiceDeploymentReader;
import org.fcrepo.server.storage.types.DeploymentDSBindSpec;
import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.server.storage.types.MethodDef;
import org.fcrepo.server.storage.types.MethodDefOperationBind;
import org.fcrepo.server.storage.types.MethodParmDef;


/**
 * A partial implementation of {@link ServiceDeploymentReader} for use in unit tests. Add
 * more mocking to this class as needed, or override methods in sub-classes.
 *
 * @author Jim Blake
 */
public class MockServiceDeploymentReader
        extends MockDOReader
        implements ServiceDeploymentReader {

    // ----------------------------------------------------------------------
    // Mocking infrastructure
    // ----------------------------------------------------------------------

    public MockServiceDeploymentReader(DigitalObject theObject) {
        super(theObject);
    }

    // ----------------------------------------------------------------------
    // Mocked methods
    // ----------------------------------------------------------------------

    // ----------------------------------------------------------------------
    // Un-implemented methods
    // ----------------------------------------------------------------------

    public DeploymentDSBindSpec getServiceDSInputSpec(Date versDateTime)
            throws ServerException {
        throw new RuntimeException("MockBmechReader.getServiceDSInputSpec not implemented");
    }

    public MethodDefOperationBind[] getServiceMethodBindings(Date versDateTime)
            throws ServerException {
        throw new RuntimeException("MockBmechReader.getServiceMethodBindings not implemented");
    }

    public MethodParmDef[] getServiceMethodParms(String methodName,
                                                 Date versDateTime)
            throws ServerException {
        throw new RuntimeException("MockBmechReader.getServiceMethodParms not implemented");
    }

    public MethodDef[] getServiceMethods(Date versDateTime)
            throws ServerException {
        throw new RuntimeException("MockBmechReader.getServiceMethods not implemented");
    }

    public InputStream getServiceMethodsXML(Date versDateTime)
            throws ServerException {
        throw new RuntimeException("MockBmechReader.getServiceMethodsXML not implemented");
    }


}
