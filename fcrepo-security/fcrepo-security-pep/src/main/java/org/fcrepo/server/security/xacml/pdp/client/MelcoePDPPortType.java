/**
 * MelcoePDPPortType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.fcrepo.server.security.xacml.pdp.client;

public interface MelcoePDPPortType
        extends java.rmi.Remote {

    public java.lang.String evaluateBatch(java.lang.String[] requests)
            throws java.rmi.RemoteException,
            org.fcrepo.server.security.xacml.pdp.client.EvaluationExceptionType0;

    public java.lang.String evaluate(java.lang.String request)
            throws java.rmi.RemoteException,
            org.fcrepo.server.security.xacml.pdp.client.EvaluationExceptionType0;
}
