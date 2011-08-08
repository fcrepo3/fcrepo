
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
 *         &lt;element name="evaluateFault" type="{http://www.w3.org/2001/XMLSchema}anyType"/>
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
    "evaluateFault"
})
@XmlRootElement(name = "evaluateFault")
public class EvaluateFault {

    @XmlElement(required = true)
    protected Object evaluateFault;

    /**
     * Gets the value of the evaluateFault property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getEvaluateFault() {
        return evaluateFault;
    }

    /**
     * Sets the value of the evaluateFault property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setEvaluateFault(Object value) {
        this.evaluateFault = value;
    }

}
