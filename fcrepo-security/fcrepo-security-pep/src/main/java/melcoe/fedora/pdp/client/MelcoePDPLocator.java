/**
 * MelcoePDPLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package melcoe.fedora.pdp.client;

public class MelcoePDPLocator
        extends org.apache.axis.client.Service
        implements melcoe.fedora.pdp.client.MelcoePDP {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public MelcoePDPLocator() {
    }

    public MelcoePDPLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public MelcoePDPLocator(java.lang.String wsdlLoc,
                            javax.xml.namespace.QName sName)
            throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for MelcoePDPSOAP11port_http
    private java.lang.String MelcoePDPSOAP11port_http_address =
            "http://localhost:8080/axis2/services/MelcoePDP";

    public java.lang.String getMelcoePDPSOAP11port_httpAddress() {
        return MelcoePDPSOAP11port_http_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String MelcoePDPSOAP11port_httpWSDDServiceName =
            "MelcoePDPSOAP11port_http";

    public java.lang.String getMelcoePDPSOAP11port_httpWSDDServiceName() {
        return MelcoePDPSOAP11port_httpWSDDServiceName;
    }

    public void setMelcoePDPSOAP11port_httpWSDDServiceName(java.lang.String name) {
        MelcoePDPSOAP11port_httpWSDDServiceName = name;
    }

    public melcoe.fedora.pdp.client.MelcoePDPPortType getMelcoePDPSOAP11port_http()
            throws javax.xml.rpc.ServiceException {
        java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(MelcoePDPSOAP11port_http_address);
        } catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getMelcoePDPSOAP11port_http(endpoint);
    }

    public melcoe.fedora.pdp.client.MelcoePDPPortType getMelcoePDPSOAP11port_http(java.net.URL portAddress)
            throws javax.xml.rpc.ServiceException {
        try {
            melcoe.fedora.pdp.client.MelcoePDPSOAP11BindingStub _stub =
                    new melcoe.fedora.pdp.client.MelcoePDPSOAP11BindingStub(portAddress,
                                                                            this);
            _stub.setPortName(getMelcoePDPSOAP11port_httpWSDDServiceName());
            return _stub;
        } catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setMelcoePDPSOAP11port_httpEndpointAddress(java.lang.String address) {
        MelcoePDPSOAP11port_http_address = address;
    }

    /**
     * For the given interface, get the stub implementation. If this service has
     * no port for the given interface, then ServiceException is thrown.
     */
    @Override
    public java.rmi.Remote getPort(Class serviceEndpointInterface)
            throws javax.xml.rpc.ServiceException {
        try {
            if (melcoe.fedora.pdp.client.MelcoePDPPortType.class
                    .isAssignableFrom(serviceEndpointInterface)) {
                melcoe.fedora.pdp.client.MelcoePDPSOAP11BindingStub _stub =
                        new melcoe.fedora.pdp.client.MelcoePDPSOAP11BindingStub(new java.net.URL(MelcoePDPSOAP11port_http_address),
                                                                                this);
                _stub.setPortName(getMelcoePDPSOAP11port_httpWSDDServiceName());
                return _stub;
            }
        } catch (java.lang.Throwable t) {
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
        java.lang.String inputPortName = portName.getLocalPart();
        if ("MelcoePDPSOAP11port_http".equals(inputPortName)) {
            return getMelcoePDPSOAP11port_http();
        } else {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    @Override
    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                             "MelcoePDP");
    }

    private java.util.HashSet ports = null;

    @Override
    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports
                    .add(new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                                       "MelcoePDPSOAP11port_http"));
        }
        return ports.iterator();
    }

    /**
     * Set the endpoint address for the specified port name.
     */
    public void setEndpointAddress(java.lang.String portName,
                                   java.lang.String address)
            throws javax.xml.rpc.ServiceException {

        if ("MelcoePDPSOAP11port_http".equals(portName)) {
            setMelcoePDPSOAP11port_httpEndpointAddress(address);
        } else { // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port"
                    + portName);
        }
    }

    /**
     * Set the endpoint address for the specified port name.
     */
    public void setEndpointAddress(javax.xml.namespace.QName portName,
                                   java.lang.String address)
            throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
