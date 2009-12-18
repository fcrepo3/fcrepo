/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.access;

import fedora.common.Constants;

/**
 * This file was originally auto-generated from the API-A WSDL by the Apache
 * Axis WSDL2Java emitter. The generated file was then modified so that it has a
 * constructor that takes username and password, so that the service stub class
 * can have username and passord. The following methods were modified:
 * getFedoraAPIAPortSOAPHTTP - custom stub (fedora.server.access.FedoraAPIA)
 * 
 * @author Chris Wilper
 */
public class FedoraAPIAServiceLocator
        extends org.apache.axis.client.Service
        implements Constants, fedora.server.access.FedoraAPIAService {

    private static final long serialVersionUID = 1L;

    // Use to get a proxy class for FedoraAPIAPortSOAPHTTP and FedoraAPIAPortSOAPHTTPS (secure)
    private final java.lang.String FedoraAPIAPortSOAPHTTP_address =
            "http://localhost:0/fedora/services/access"; //port replaced in external code

    private final java.lang.String FedoraAPIAPortSOAPHTTPS_address =
            "https://localhost:0/fedora/services/access"; //port replaced in external code

    private String username = null;

    private String password = null;

    private int socketTimeoutMilliseconds = 120000; // two minute default

    public FedoraAPIAServiceLocator(String user, String pass) {
        username = user;
        password = pass;
    }

    public FedoraAPIAServiceLocator(String user,
                                    String pass,
                                    int socketTimeoutSeconds) {
        username = user;
        password = pass;
        socketTimeoutMilliseconds = socketTimeoutSeconds * 1000;
    }

    public FedoraAPIAServiceLocator() { // for AccessConsole
        username = "nobody";
        password = "nobody";
    }

    public java.lang.String getFedoraAPIAPortSOAPHTTPAddress() {
        return FedoraAPIAPortSOAPHTTP_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String FedoraAPIAPortSOAPHTTPWSDDServiceName =
            "FedoraAPIAPortSOAPHTTP";

    public java.lang.String getFedoraAPIAPortSOAPHTTPWSDDServiceName() {
        return FedoraAPIAPortSOAPHTTPWSDDServiceName;
    }

    public void setFedoraAPIAPortSOAPHTTPWSDDServiceName(java.lang.String name) {
        FedoraAPIAPortSOAPHTTPWSDDServiceName = name;
    }

    public fedora.server.access.FedoraAPIA getFedoraAPIAPortSOAPHTTP()
            throws javax.xml.rpc.ServiceException {
        java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(FedoraAPIAPortSOAPHTTP_address);
        } catch (java.net.MalformedURLException e) {
            return null; // unlikely as URL was validated in WSDL2Java
        }
        return getFedoraAPIAPortSOAPHTTP(endpoint);
    }

    public fedora.server.access.FedoraAPIA getFedoraAPIAPortSOAPHTTP(java.net.URL portAddress)
            throws javax.xml.rpc.ServiceException {
        try {
            fedora.server.access.APIAStub _stub =
                    new fedora.server.access.APIAStub(portAddress,
                                                      this,
                                                      username,
                                                      password);
            _stub.setPortName(getFedoraAPIAPortSOAPHTTPWSDDServiceName());
            _stub.setTimeout(socketTimeoutMilliseconds);
            return _stub;
        } catch (org.apache.axis.AxisFault e) {
            return null; // ???
        }
    }

    //SDP - HTTPS    
    public java.lang.String getFedoraAPIAPortSOAPHTTPSAddress() {
        return FedoraAPIAPortSOAPHTTPS_address;
    }

    //SDP - HTTPS
    private java.lang.String FedoraAPIAPortSOAPHTTPSWSDDServiceName =
            "FedoraAPIAPortSOAPHTTPS";

    //SDP - HTTPS
    public java.lang.String getFedoraAPIAPortSOAPHTTPSWSDDServiceName() {
        return FedoraAPIAPortSOAPHTTPSWSDDServiceName;
    }

    //SDP - HTTPS
    public void setFedoraAPIAPortSOAPHTTPSWSDDServiceName(java.lang.String name) {
        FedoraAPIAPortSOAPHTTPSWSDDServiceName = name;
    }

    //SDP - HTTPS    
    public fedora.server.access.FedoraAPIA getFedoraAPIAPortSOAPHTTPS()
            throws javax.xml.rpc.ServiceException {
        java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(FedoraAPIAPortSOAPHTTPS_address);
        } catch (java.net.MalformedURLException e) {
            return null; // unlikely as URL was validated in WSDL2Java
        }
        return getFedoraAPIAPortSOAPHTTPS(endpoint);
    }

    //SDP - HTTPS     
    public fedora.server.access.FedoraAPIA getFedoraAPIAPortSOAPHTTPS(java.net.URL portAddress)
            throws javax.xml.rpc.ServiceException {
        try {
            fedora.server.access.APIAStub _stub =
                    new fedora.server.access.APIAStub(portAddress,
                                                      this,
                                                      username,
                                                      password);
            _stub.setPortName(getFedoraAPIAPortSOAPHTTPSWSDDServiceName());
            _stub.setTimeout(socketTimeoutMilliseconds);
            // _stub._setProperty("httpclient.authentication.preemptive","true");
            return _stub;
        } catch (org.apache.axis.AxisFault e) {
            return null; // ???
        }
    }

    /**
     * For the given interface, get the stub implementation. If this service has
     * no port for the given interface, then ServiceException is thrown.
     */
    @Override
    public java.rmi.Remote getPort(Class serviceEndpointInterface)
            throws javax.xml.rpc.ServiceException {
        try {
            if (fedora.server.access.FedoraAPIA.class
                    .isAssignableFrom(serviceEndpointInterface)) {
                fedora.server.access.APIAStub _stub =
                        new fedora.server.access.APIAStub(new java.net.URL(FedoraAPIAPortSOAPHTTP_address),
                                                          this,
                                                          username,
                                                          password);
                _stub.setPortName(getFedoraAPIAPortSOAPHTTPWSDDServiceName());
                _stub.setTimeout(socketTimeoutMilliseconds);
                // _stub._setProperty("httpclient.authentication.preemptive","true");                
                return _stub;
            }
            //SDP - HTTPS (added second port for https)
            if (fedora.server.access.FedoraAPIA.class
                    .isAssignableFrom(serviceEndpointInterface)) {
                fedora.server.access.APIAStub _stub =
                        new fedora.server.access.APIAStub(new java.net.URL(FedoraAPIAPortSOAPHTTPS_address),
                                                          this,
                                                          username,
                                                          password);
                _stub.setPortName(getFedoraAPIAPortSOAPHTTPSWSDDServiceName());
                _stub.setTimeout(socketTimeoutMilliseconds);
                // _stub._setProperty("httpclient.authentication.preemptive","true");                
                return _stub;
            }
        } catch (Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  "
                + (serviceEndpointInterface == null ? "null"
                        : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation. If this service has
     * no port for the given interface, then ServiceException is thrown.
     */
    @Override
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName,
                                   Class serviceEndpointInterface)
            throws javax.xml.rpc.ServiceException {

        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        String inputPortName = portName.getLocalPart();
        if ("FedoraAPIAPortSOAPHTTP".equals(inputPortName)) {
            return getFedoraAPIAPortSOAPHTTP();
        } else if ("FedoraAPIAPortSOAPHTTPS".equals(inputPortName)) {
            return getFedoraAPIAPortSOAPHTTPS();
        } else {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            ((org.apache.axis.client.Stub) _stub)
                    .setTimeout(socketTimeoutMilliseconds);
            return _stub;
        }
    }

    @Override
    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName(API.uri, "Fedora-API-A-Service");
    }

    private java.util.HashSet ports = null;

    @Override
    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("FedoraAPIAPortSOAPHTTP"));
            ports.add(new javax.xml.namespace.QName("FedoraAPIAPortSOAPHTTPS"));
        }
        return ports.iterator();
    }

}
