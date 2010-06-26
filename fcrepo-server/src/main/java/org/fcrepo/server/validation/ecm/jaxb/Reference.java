package org.fcrepo.server.validation.ecm.jaxb;

import javax.xml.bind.annotation.*;


/**
 * <p>Java class for anonymous complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *       &lt;attribute name="datastream" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "reference", namespace = "info:fedora/fedora-system:def/dsCompositeModel#")
public class Reference {

    @XmlAttribute
    @XmlSchemaType(name = "anySimpleType")
    protected String type;
    @XmlAttribute
    @XmlSchemaType(name = "anySimpleType")
    protected String datastream;

    /**
     * Gets the value of the type property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of the datastream property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getDatastream() {
        return datastream;
    }

    /**
     * Sets the value of the datastream property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDatastream(String value) {
        this.datastream = value;
    }

}
