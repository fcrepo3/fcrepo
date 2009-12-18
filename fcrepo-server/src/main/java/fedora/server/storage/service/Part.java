/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.service;

import java.util.Vector;

/**
 * A data structure for holding WSDL Message part.
 * 
 * @author Sandy Payette
 */
public class Part {

    public String partName;

    public String partTypeName;

    public String partBaseTypeNamespaceURI;

    public String partBaseTypeLocalName;

    // consider...
    public Vector enumerationOfValues;

    public String defaultValue;
}
