/**
 * Exception.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package melcoe.fedora.pdp.client;

public class Exception
        implements java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private java.lang.Object exception;

    public Exception() {
    }

    public Exception(java.lang.Object exception) {
        this.exception = exception;
    }

    /**
     * Gets the exception value for this Exception.
     * 
     * @return exception
     */
    public java.lang.Object getException() {
        return exception;
    }

    /**
     * Sets the exception value for this Exception.
     * 
     * @param exception
     */
    public void setException(java.lang.Object exception) {
        this.exception = exception;
    }

    private java.lang.Object __equalsCalc = null;

    @Override
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Exception)) {
            return false;
        }
        Exception other = (Exception) obj;
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
                true && (exception == null && other.getException() == null || exception != null
                        && exception.equals(other.getException()));
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
        if (getException() != null) {
            _hashCode += getException().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
            new org.apache.axis.description.TypeDesc(Exception.class, true);

    static {
        typeDesc
                .setXmlType(new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                                          "Exception"));
        org.apache.axis.description.ElementDesc elemField =
                new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("exception");
        elemField
                .setXmlName(new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                                          "Exception"));
        elemField
                .setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema",
                                                          "anyType"));
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
