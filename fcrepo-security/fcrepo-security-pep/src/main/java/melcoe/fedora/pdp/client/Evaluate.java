/**
 * Evaluate.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package melcoe.fedora.pdp.client;

public class Evaluate
        implements java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private java.lang.String request;

    public Evaluate() {
    }

    public Evaluate(java.lang.String request) {
        this.request = request;
    }

    /**
     * Gets the request value for this Evaluate.
     * 
     * @return request
     */
    public java.lang.String getRequest() {
        return request;
    }

    /**
     * Sets the request value for this Evaluate.
     * 
     * @param request
     */
    public void setRequest(java.lang.String request) {
        this.request = request;
    }

    private java.lang.Object __equalsCalc = null;

    @Override
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Evaluate)) {
            return false;
        }
        Evaluate other = (Evaluate) obj;
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
                true && (request == null && other.getRequest() == null || request != null
                        && request.equals(other.getRequest()));
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
        if (getRequest() != null) {
            _hashCode += getRequest().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
            new org.apache.axis.description.TypeDesc(Evaluate.class, true);

    static {
        typeDesc
                .setXmlType(new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                                          ">evaluate"));
        org.apache.axis.description.ElementDesc elemField =
                new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("request");
        elemField
                .setXmlName(new javax.xml.namespace.QName("http://pdp.xacml.melcoe/xsd",
                                                          "request"));
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
