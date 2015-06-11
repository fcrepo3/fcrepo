package org.fcrepo.server.validation.ecm.jaxb;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for anonymous complex type.
 * </p>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * </p>
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{info:fedora/fedora-system:def/dsCompositeModel#}form" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element ref="{info:fedora/fedora-system:def/dsCompositeModel#}extension" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="ID" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *       &lt;attribute name="optional" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "form",
        "extension"
})
@XmlRootElement(name = "dsTypeModel", namespace = "info:fedora/fedora-system:def/dsCompositeModel#")
public class DsTypeModel {

    @XmlElement(namespace = "info:fedora/fedora-system:def/dsCompositeModel#")
    protected List<Form> form;
    @XmlElement(namespace = "info:fedora/fedora-system:def/dsCompositeModel#")
    protected List<Extension> extension;
    @XmlAttribute(name = "ID", required = true)
    @XmlSchemaType(name = "anySimpleType")
    protected String id;
    @XmlAttribute
    protected Boolean optional;

    /**
     * Gets the value of the form property.
     * <br>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the form property.
     * <br>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getForm().add(newItem);
     * </pre>
     * <br>
     * Objects of the following type(s) are allowed in the list
     * {@link Form }
     */
    public List<Form> getForm() {
        if (form == null) {
            form = new ArrayList<Form>();
        }
        return this.form;
    }

    /**
     * Gets the value of the extension property.
     * <br>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the extension property.
     * <br>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getExtension().add(newItem);
     * </pre>
     * <br>
     * Objects of the following type(s) are allowed in the list
     * {@link Extension }
     */
    public List<Extension> getExtension() {
        if (extension == null) {
            extension = new ArrayList<Extension>();
        }
        return this.extension;
    }

    /**
     * Gets the value of the id property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getID() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setID(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the optional property.
     *
     * @return possible object is
     *         {@link Boolean }
     */
    public Boolean isOptional() {
        return optional;
    }

    /**
     * Sets the value of the optional property.
     *
     * @param value allowed object is
     *              {@link Boolean }
     */
    public void setOptional(Boolean value) {
        this.optional = value;
    }

}
