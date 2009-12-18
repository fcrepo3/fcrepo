/**
 * EvaluateResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package melcoe.fedora.pdp.client;

public class EvaluateResponse
        implements java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private java.lang.String _return;

    public EvaluateResponse() {
    }

    public EvaluateResponse(java.lang.String _return) {
        this._return = _return;
    }

    /**
     * Gets the _return value for this EvaluateResponse.
     * 
     * @return _return
     */
    public java.lang.String get_return() {
        return _return;
    }

    /**
     * Sets the _return value for this EvaluateResponse.
     * 
     * @param _return
     */
    public void set_return(java.lang.String _return) {
        this._return = _return;
    }

    private java.lang.Object __equalsCalc = null;

    @Override
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof EvaluateResponse)) {
            return false;
        }
        EvaluateResponse other = (EvaluateResponse) obj;
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (__equalsCalc != null) {
            return __equalsCalc == obj;
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals =
                true && (_return == null && other.get_return() == null || _return != null
                        && _return.equals(other.get_return()));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;

    @Override
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (get_return() != null) {
            _hashCode += get_return().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
            new org.apache.axis.description.TypeDesc(EvaluateResponse.class,
                                                     true);

    static {
        typeDesc
                .setXmlType(new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                                          ">evaluateResponse"));
        org.apache.axis.description.ElementDesc elemField =
                new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("_return");
        elemField
                .setXmlName(new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                                          "return"));
        elemField
                .setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema",
                                                          "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(java.lang.String mechType,
                                                                    java.lang.Class _javaType,
                                                                    javax.xml.namespace.QName _xmlType) {
        return new org.apache.axis.encoding.ser.BeanSerializer(_javaType,
                                                               _xmlType,
                                                               typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(java.lang.String mechType,
                                                                        java.lang.Class _javaType,
                                                                        javax.xml.namespace.QName _xmlType) {
        return new org.apache.axis.encoding.ser.BeanDeserializer(_javaType,
                                                                 _xmlType,
                                                                 typeDesc);
    }

}
