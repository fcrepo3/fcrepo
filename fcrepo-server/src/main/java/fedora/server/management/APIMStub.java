/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.management;

import java.net.URL;

import javax.xml.rpc.Service;

import org.apache.axis.AxisFault;

/**
 * This is the auto-generated client stub, but with a new constructor that 
 * takes a user/pass combo, and creates its calls with those set as properties.
 * 
 * @author Chris Wilper
 */
public class APIMStub
        extends FedoraAPIMBindingSOAPHTTPStub {

    public APIMStub()
            throws AxisFault {
        super(null);
    }

    public APIMStub(URL endpointURL, Service service)
            throws AxisFault {
        super(service);
        super.cachedEndpoint = endpointURL;
    }

    public APIMStub(URL endpointURL,
                    Service service,
                    String username,
                    String password)
            throws AxisFault {
        super(service);
        super.cachedEndpoint = endpointURL;
        super.cachedUsername = username;
        super.cachedPassword = password;
    }

}
