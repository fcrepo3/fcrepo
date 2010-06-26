package org.fcrepo.server.validation.ecm.jaxb;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for anonymous complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{info:fedora/fedora-system:def/dsCompositeModel#}dsTypeModel" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "dsTypeModel"
})
@XmlRootElement(name = "dsCompositeModel", namespace = "info:fedora/fedora-system:def/dsCompositeModel#")
public class DsCompositeModel {

    @XmlElement(namespace = "info:fedora/fedora-system:def/dsCompositeModel#")
    protected List<DsTypeModel> dsTypeModel;

    /**
     * Gets the value of the dsTypeModel property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dsTypeModel property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDsTypeModel().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link DsTypeModel }
     */
    public List<DsTypeModel> getDsTypeModel() {
        if (dsTypeModel == null) {
            dsTypeModel = new ArrayList<DsTypeModel>();
        }
        return this.dsTypeModel;
    }

}
