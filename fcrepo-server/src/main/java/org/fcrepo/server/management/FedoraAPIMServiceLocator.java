/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.management;

import org.fcrepo.common.Constants;

/**
 * This file was originally auto-generated from the API-M WSDL by the Apache
 * Axis WSDL2Java emitter. The generated file was then modified so that it has a
 * constructor that takes username and password, so that the service stub class
 * can have username and passord. The following methods were modified:
 * getFedoraAPIMPortSOAPHTTP - custom stub (org.fcrepo.server.management.FedoraAPIM)
 *
 * @author Chris Wilper
 */
public class FedoraAPIMServiceLocator
        extends org.apache.axis.client.Service
        implements Constants, org.fcrepo.server.management.FedoraAPIMService {

    private static final long serialVersionUID = 1L;

    // Use to get a proxy class for FedoraAPIMPortSOAPHTTP and FedoraAPIMPortSOAPHTTPS (secure)
    private final java.lang.String FedoraAPIMPortSOAPHTTP_address =
            "http://localhost:0/fedora/services/management"; //port replaced in external code

    private final java.lang.String FedoraAPIMPortSOAPHTTPS_address =
            "https://localhost:0/fedora/services/management"; //port replaced in external code

    private String username = null;

    private String password = null;

    private int socketTimeoutMilliseconds = 120000; // two minute default

    public FedoraAPIMServiceLocator(String user, String pass) {
        username = user;
        password = pass;
    }

    public FedoraAPIMServiceLocator(String user,
                                    String pass,
                                    int socketTimeoutSeconds) {
        username = user;
        password = pass;
        socketTimeoutMilliseconds = socketTimeoutSeconds * 1000;
    }

    public java.lang.String getFedoraAPIMPortSOAPHTTPAddress() {
        return FedoraAPIMPortSOAPHTTP_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String FedoraAPIMPortSOAPHTTPWSDDServiceName =
            "FedoraAPIMPortSOAPHTTP";

    public java.lang.String getFedoraAPIMPortSOAPHTTPWSDDServiceName() {
        return FedoraAPIMPortSOAPHTTPWSDDServiceName;
    }

    public void setFedoraAPIMPortSOAPHTTPWSDDServiceName(java.lang.String name) {
        FedoraAPIMPortSOAPHTTPWSDDServiceName = name;
    }

    public org.fcrepo.server.management.FedoraAPIM getFedoraAPIMPortSOAPHTTP()
            throws javax.xml.rpc.ServiceException {
        java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(FedoraAPIMPortSOAPHTTP_address);
        } catch (java.net.MalformedURLException e) {
            return null; // unlikely as URL was validated in WSDL2Java
        }
        return getFedoraAPIMPortSOAPHTTP(endpoint);
    }

    public org.fcrepo.server.management.FedoraAPIM getFedoraAPIMPortSOAPHTTP(java.net.URL portAddress)
            throws javax.xml.rpc.ServiceException {
        try {
            org.fcrepo.server.management.APIMStub _stub =
                    new org.fcrepo.server.management.APIMStub(portAddress,
                                                          this,
                                                          username,
                                                          password);
            _stub.setPortName(getFedoraAPIMPortSOAPHTTPWSDDServiceName());
            _stub.setTimeout(socketTimeoutMilliseconds);
            return _stub;
        } catch (org.apache.axis.AxisFault e) {
            return null; // ???
        }
    }

    //SDP - HTTPS

    public java.lang.String getFedoraAPIMPortSOAPHTTPSAddress() {
        return FedoraAPIMPortSOAPHTTPS_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String FedoraAPIMPortSOAPHTTPSWSDDServiceName =
            "FedoraAPIMPortSOAPHTTPS";

    public java.lang.String getFedoraAPIMPortSOAPHTTPSWSDDServiceName() {
        return FedoraAPIMPortSOAPHTTPSWSDDServiceName;
    }

    public void setFedoraAPIMPortSOAPHTTPSWSDDServiceName(java.lang.String name) {
        FedoraAPIMPortSOAPHTTPSWSDDServiceName = name;
    }

    public org.fcrepo.server.management.FedoraAPIM getFedoraAPIMPortSOAPHTTPS()
            throws javax.xml.rpc.ServiceException {
        java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(FedoraAPIMPortSOAPHTTPS_address);
        } catch (java.net.MalformedURLException e) {
            return null; // unlikely as URL was validated in WSDL2Java
        }
        return getFedoraAPIMPortSOAPHTTPS(endpoint);
    }

    public org.fcrepo.server.management.FedoraAPIM getFedoraAPIMPortSOAPHTTPS(java.net.URL portAddress)
            throws javax.xml.rpc.ServiceException {
        try {
            org.fcrepo.server.management.APIMStub _stub =
                    new org.fcrepo.server.management.APIMStub(portAddress,
                                                          this,
                                                          username,
                                                          password);
            _stub.setPortName(getFedoraAPIMPortSOAPHTTPSWSDDServiceName());
            _stub.setTimeout(socketTimeoutMilliseconds);
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
            if (org.fcrepo.server.management.FedoraAPIM.class
                    .isAssignableFrom(serviceEndpointInterface)) {
                org.fcrepo.server.management.APIMStub _stub =
                        new org.fcrepo.server.management.APIMStub(new java.net.URL(FedoraAPIMPortSOAPHTTP_address),
                                                              this,
                                                              username,
                                                              password);
                _stub.setPortName(getFedoraAPIMPortSOAPHTTPWSDDServiceName());
                _stub.setTimeout(socketTimeoutMilliseconds);
                return _stub;
            }
            //SDP - HTTPS (added second port for https)
            if (org.fcrepo.server.management.FedoraAPIM.class
                    .isAssignableFrom(serviceEndpointInterface)) {
                org.fcrepo.server.management.APIMStub _stub =
                        new org.fcrepo.server.management.APIMStub(new java.net.URL(FedoraAPIMPortSOAPHTTPS_address),
                                                              this,
                                                              username,
                                                              password);
                _stub.setPortName(getFedoraAPIMPortSOAPHTTPSWSDDServiceName());
                _stub.setTimeout(socketTimeoutMilliseconds);
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

        //SDP - HTTPS
        //commented out old code in lieu of newly generated code for two ports.
        //java.rmi.Remote _stub = getPort(serviceEndpointInterface);
        //((org.apache.axis.client.Stub) _stub).setPortName(portName);
        //return _stub;

        //
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        String inputPortName = portName.getLocalPart();
        if ("FedoraAPIMPortSOAPHTTPS".equals(inputPortName)) {
            return getFedoraAPIMPortSOAPHTTPS();
        } else if ("FedoraAPIMPortSOAPHTTP".equals(inputPortName)) {
            return getFedoraAPIMPortSOAPHTTP();
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
        return new javax.xml.namespace.QName(API.uri, "Fedora-API-M-Service");
    }

    private java.util.HashSet ports = null;

    @Override
    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("FedoraAPIMPortSOAPHTTP"));
            ports.add(new javax.xml.namespace.QName("FedoraAPIMPortSOAPHTTPS"));
        }
        return ports.iterator();
    }

}
