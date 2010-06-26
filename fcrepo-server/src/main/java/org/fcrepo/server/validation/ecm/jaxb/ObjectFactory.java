package org.fcrepo.server.validation.ecm.jaxb;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the org.fcrepo.server.validation.ecm.jaxb package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.fcrepo.server.validation.ecm.jaxb
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link DsTypeModel }
     */
    public DsTypeModel createDsTypeModel() {
        return new DsTypeModel();
    }

    /**
     * Create an instance of {@link Reference }
     */
    public Reference createReference() {
        return new Reference();
    }

    /**
     * Create an instance of {@link Form }
     */
    public Form createForm() {
        return new Form();
    }

    /**
     * Create an instance of {@link DsCompositeModel }
     */
    public DsCompositeModel createDsCompositeModel() {
        return new DsCompositeModel();
    }

    /**
     * Create an instance of {@link Extension }
     */
    public Extension createExtension() {
        return new Extension();
    }

}
