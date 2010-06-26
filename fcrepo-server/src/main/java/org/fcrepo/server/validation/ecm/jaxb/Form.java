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
 *       &lt;attribute name="FORMAT_URI" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="MIME" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "form", namespace = "info:fedora/fedora-system:def/dsCompositeModel#")
public class Form {

    @XmlAttribute(name = "FORMAT_URI")
    @XmlSchemaType(name = "anyURI")
    protected String formaturi;
    @XmlAttribute(name = "MIME")
    @XmlSchemaType(name = "anySimpleType")
    protected String mime;

    /**
     * Gets the value of the formaturi property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getFORMATURI() {
        return formaturi;
    }

    /**
     * Sets the value of the formaturi property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setFORMATURI(String value) {
        this.formaturi = value;
    }

    /**
     * Gets the value of the mime property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getMIME() {
        return mime;
    }

    /**
     * Sets the value of the mime property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setMIME(String value) {
        this.mime = value;
    }

}
