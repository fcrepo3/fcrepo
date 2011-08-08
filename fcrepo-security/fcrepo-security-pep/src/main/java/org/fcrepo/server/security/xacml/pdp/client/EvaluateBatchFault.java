
package org.fcrepo.server.security.xacml.pdp.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="evaluateBatchFault" type="{http://www.w3.org/2001/XMLSchema}anyType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "evaluateBatchFault"
})
@XmlRootElement(name = "evaluateBatchFault")
public class EvaluateBatchFault {

    @XmlElement(required = true)
    protected Object evaluateBatchFault;

    /**
     * Gets the value of the evaluateBatchFault property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getEvaluateBatchFault() {
        return evaluateBatchFault;
    }

    /**
     * Sets the value of the evaluateBatchFault property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setEvaluateBatchFault(Object value) {
        this.evaluateBatchFault = value;
    }

}
