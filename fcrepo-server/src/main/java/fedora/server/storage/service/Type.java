/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.service;

/**
 * A data structure for holding WSDL xsd type declarations.
 * 
 * @author Sandy Payette
 */
public class Type {

    public String typeName;

    public String baseTypeName;

    public String baseTypeNamespaceURI;

    public String baseTypeLocalName;
}
