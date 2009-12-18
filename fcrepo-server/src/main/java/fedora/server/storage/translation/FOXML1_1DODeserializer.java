/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

/**
 * Deserializes objects in FOXML 1.1 format.
 * 
 * @author Chris Wilper
 */
public class FOXML1_1DODeserializer
        extends FOXMLDODeserializer {

    /**
     * Constructs an instance.
     */
    public FOXML1_1DODeserializer() {
        super(FOXML1_1);
    }
}