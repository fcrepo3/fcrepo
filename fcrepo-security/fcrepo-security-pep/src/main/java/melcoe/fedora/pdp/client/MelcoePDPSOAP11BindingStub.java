/**
 * MelcoePDPSOAP11BindingStub.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package melcoe.fedora.pdp.client;

public class MelcoePDPSOAP11BindingStub
        extends org.apache.axis.client.Stub
        implements melcoe.fedora.pdp.client.MelcoePDPPortType {

    private final java.util.Vector cachedSerClasses = new java.util.Vector();

    private final java.util.Vector cachedSerQNames = new java.util.Vector();

    private final java.util.Vector cachedSerFactories = new java.util.Vector();

    private final java.util.Vector cachedDeserFactories =
            new java.util.Vector();

    static org.apache.axis.description.OperationDesc[] _operations;

    static {
        _operations = new org.apache.axis.description.OperationDesc[2];
        _initOperationDesc1();
    }

    private static void _initOperationDesc1() {
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("evaluateBatch");
        param =
                new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                                                                            "requests"),
                                                              org.apache.axis.description.ParameterDesc.IN,
                                                              new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema",
                                                                                            "string"),
                                                              java.lang.String[].class,
                                                              false,
                                                              false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper
                .setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema",
                                                             "string"));
        oper.setReturnClass(java.lang.String.class);
        oper
                .setReturnQName(new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                                              "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper
                .addFault(new org.apache.axis.description.FaultDesc(new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                                                                                  "EvaluationException"),
                                                                    "melcoe.fedora.pdp.client.EvaluationExceptionType0",
                                                                    new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                                                                                  ">EvaluationException"),
                                                                    true));
        _operations[0] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("evaluate");
        param =
                new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                                                                            "request"),
                                                              org.apache.axis.description.ParameterDesc.IN,
                                                              new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema",
                                                                                            "string"),
                                                              java.lang.String.class,
                                                              false,
                                                              false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper
                .setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema",
                                                             "string"));
        oper.setReturnClass(java.lang.String.class);
        oper
                .setReturnQName(new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                                              "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper
                .addFault(new org.apache.axis.description.FaultDesc(new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                                                                                  "EvaluationException"),
                                                                    "melcoe.fedora.pdp.client.EvaluationExceptionType0",
                                                                    new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                                                                                  ">EvaluationException"),
                                                                    true));
        _operations[1] = oper;

    }

    public MelcoePDPSOAP11BindingStub()
            throws org.apache.axis.AxisFault {
        this(null);
    }

    public MelcoePDPSOAP11BindingStub(java.net.URL endpointURL,
                                      javax.xml.rpc.Service service)
            throws org.apache.axis.AxisFault {
        this(service);
        super.cachedEndpoint = endpointURL;
    }

    public MelcoePDPSOAP11BindingStub(javax.xml.rpc.Service service)
            throws org.apache.axis.AxisFault {
        if (service == null) {
            super.service = new org.apache.axis.client.Service();
        } else {
            super.service = service;
        }
        ((org.apache.axis.client.Service) super.service)
                .setTypeMappingVersion("1.2");
        java.lang.Class cls;
        javax.xml.namespace.QName qName;
        java.lang.Class beansf =
                org.apache.axis.encoding.ser.BeanSerializerFactory.class;
        java.lang.Class beandf =
                org.apache.axis.encoding.ser.BeanDeserializerFactory.class;
        qName =
                new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                              ">evaluate");
        cachedSerQNames.add(qName);
        cls = melcoe.fedora.pdp.client.Evaluate.class;
        cachedSerClasses.add(cls);
        cachedSerFactories.add(beansf);
        cachedDeserFactories.add(beandf);

        qName =
                new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                              ">evaluateResponse");
        cachedSerQNames.add(qName);
        cls = melcoe.fedora.pdp.client.EvaluateResponse.class;
        cachedSerClasses.add(cls);
        cachedSerFactories.add(beansf);
        cachedDeserFactories.add(beandf);

        qName =
                new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                              ">EvaluationException");
        cachedSerQNames.add(qName);
        cls = melcoe.fedora.pdp.client.EvaluationExceptionType0.class;
        cachedSerClasses.add(cls);
        cachedSerFactories.add(beansf);
        cachedDeserFactories.add(beandf);

        qName =
                new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                              "EvaluationException");
        cachedSerQNames.add(qName);
        cls = melcoe.fedora.pdp.client.EvaluationException.class;
        cachedSerClasses.add(cls);
        cachedSerFactories.add(beansf);
        cachedDeserFactories.add(beandf);

        qName =
                new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                              "Exception");
        cachedSerQNames.add(qName);
        cls = melcoe.fedora.pdp.client.Exception.class;
        cachedSerClasses.add(cls);
        cachedSerFactories.add(beansf);
        cachedDeserFactories.add(beandf);

        qName =
                new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                              "MelcoePDPException");
        cachedSerQNames.add(qName);
        cls = melcoe.fedora.pdp.client.MelcoePDPException.class;
        cachedSerClasses.add(cls);
        cachedSerFactories.add(beansf);
        cachedDeserFactories.add(beandf);

    }

    protected org.apache.axis.client.Call createCall()
            throws java.rmi.RemoteException {
        try {
            org.apache.axis.client.Call _call = super._createCall();
            if (super.maintainSessionSet) {
                _call.setMaintainSession(super.maintainSession);
            }
            if (super.cachedUsername != null) {
                _call.setUsername(super.cachedUsername);
            }
            if (super.cachedPassword != null) {
                _call.setPassword(super.cachedPassword);
            }
            if (super.cachedEndpoint != null) {
                _call.setTargetEndpointAddress(super.cachedEndpoint);
            }
            if (super.cachedTimeout != null) {
                _call.setTimeout(super.cachedTimeout);
            }
            if (super.cachedPortName != null) {
                _call.setPortName(super.cachedPortName);
            }
            java.util.Enumeration keys = super.cachedProperties.keys();
            while (keys.hasMoreElements()) {
                java.lang.String key = (java.lang.String) keys.nextElement();
                _call.setProperty(key, super.cachedProperties.get(key));
            }
            // All the type mapping information is registered
            // when the first call is made.
            // The type mapping information is actually registered in
            // the TypeMappingRegistry of the service, which
            // is the reason why registration is only needed for the first call.
            synchronized (this) {
                if (firstCall()) {
                    // must set encoding style before registering serializers
                    _call.setEncodingStyle(null);
                    for (int i = 0; i < cachedSerFactories.size(); ++i) {
                        java.lang.Class cls =
                                (java.lang.Class) cachedSerClasses.get(i);
                        javax.xml.namespace.QName qName =
                                (javax.xml.namespace.QName) cachedSerQNames
                                        .get(i);
                        java.lang.Object x = cachedSerFactories.get(i);
                        if (x instanceof Class) {
                            java.lang.Class sf =
                                    (java.lang.Class) cachedSerFactories.get(i);
                            java.lang.Class df =
                                    (java.lang.Class) cachedDeserFactories
                                            .get(i);
                            _call
                                    .registerTypeMapping(cls,
                                                         qName,
                                                         sf,
                                                         df,
                                                         false);
                        } else if (x instanceof javax.xml.rpc.encoding.SerializerFactory) {
                            org.apache.axis.encoding.SerializerFactory sf =
                                    (org.apache.axis.encoding.SerializerFactory) cachedSerFactories
                                            .get(i);
                            org.apache.axis.encoding.DeserializerFactory df =
                                    (org.apache.axis.encoding.DeserializerFactory) cachedDeserFactories
                                            .get(i);
                            _call
                                    .registerTypeMapping(cls,
                                                         qName,
                                                         sf,
                                                         df,
                                                         false);
                        }
                    }
                }
            }
            return _call;
        } catch (java.lang.Throwable _t) {
            throw new org.apache.axis.AxisFault("Failure trying to get the Call object",
                                                _t);
        }
    }

    public java.lang.String evaluateBatch(java.lang.String[] requests)
            throws java.rmi.RemoteException,
            melcoe.fedora.pdp.client.EvaluationExceptionType0 {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:evaluateBatch");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR,
                          Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS,
                          Boolean.FALSE);
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call
                .setOperationName(new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                                                "evaluateBatch"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp =
                    _call.invoke(new java.lang.Object[] {requests});

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (java.lang.String) _resp;
                } catch (java.lang.Exception _exception) {
                    return (java.lang.String) org.apache.axis.utils.JavaUtils
                            .convert(_resp, java.lang.String.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof melcoe.fedora.pdp.client.EvaluationExceptionType0) {
                    throw (melcoe.fedora.pdp.client.EvaluationExceptionType0) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

    public java.lang.String evaluate(java.lang.String request)
            throws java.rmi.RemoteException,
            melcoe.fedora.pdp.client.EvaluationExceptionType0 {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[1]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:evaluate");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR,
                          Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS,
                          Boolean.FALSE);
        _call
                .setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call
                .setOperationName(new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                                                "evaluate"));

        setRequestHeaders(_call);
        setAttachments(_call);
        try {
            java.lang.Object _resp =
                    _call.invoke(new java.lang.Object[] {request});

            if (_resp instanceof java.rmi.RemoteException) {
                throw (java.rmi.RemoteException) _resp;
            } else {
                extractAttachments(_call);
                try {
                    return (java.lang.String) _resp;
                } catch (java.lang.Exception _exception) {
                    return (java.lang.String) org.apache.axis.utils.JavaUtils
                            .convert(_resp, java.lang.String.class);
                }
            }
        } catch (org.apache.axis.AxisFault axisFaultException) {
            if (axisFaultException.detail != null) {
                if (axisFaultException.detail instanceof java.rmi.RemoteException) {
                    throw (java.rmi.RemoteException) axisFaultException.detail;
                }
                if (axisFaultException.detail instanceof melcoe.fedora.pdp.client.EvaluationExceptionType0) {
                    throw (melcoe.fedora.pdp.client.EvaluationExceptionType0) axisFaultException.detail;
                }
            }
            throw axisFaultException;
        }
    }

}
