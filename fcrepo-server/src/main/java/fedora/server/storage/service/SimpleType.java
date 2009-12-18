/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.service;

import java.util.Vector;

/**
 * A data structure for holding WSDL xsd type declarations for simple types.
 * 
 * @author Sandy Payette
 */
public class SimpleType
        extends Type {

    public Vector enumerationOfValues;
}
