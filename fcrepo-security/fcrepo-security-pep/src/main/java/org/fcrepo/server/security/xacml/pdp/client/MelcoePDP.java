/**
 * MelcoePDP.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.fcrepo.server.security.xacml.pdp.client;

public interface MelcoePDP
        extends javax.xml.rpc.Service {

    public java.lang.String getMelcoePDPSOAP11port_httpAddress();

    public org.fcrepo.server.security.xacml.pdp.client.MelcoePDPPortType getMelcoePDPSOAP11port_http()
            throws javax.xml.rpc.ServiceException;

    public org.fcrepo.server.security.xacml.pdp.client.MelcoePDPPortType getMelcoePDPSOAP11port_http(java.net.URL portAddress)
            throws javax.xml.rpc.ServiceException;
}
