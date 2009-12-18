/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities;

import fedora.server.errors.GeneralException;

/**
 * @author Bill Niebel
 */
public class ProtocolPort {

    private final String protocol;

    private final String port;

    public ProtocolPort(String protocol, String port)
            throws GeneralException {
        if (!ServerUtility.HTTP.equals(protocol)
                && !ServerUtility.HTTPS.equals(protocol)) {
            throw new GeneralException("bad protocol in ProtocolPort constructor");
        }
        if (port == null || "".equals(port)) {
            throw new GeneralException("bad port in ProtocolPort constructor");
        }
        this.protocol = protocol;
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getPort() {
        return port;
    }

    public static void main(String[] args) {
    }
}
