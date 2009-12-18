/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

/**
 * Deserializes objects in FOXML 1.0 format.
 * 
 * @author Chris Wilper
 */
public class FOXML1_0DODeserializer
        extends FOXMLDODeserializer {

    /**
     * Constructs an instance.
     */
    public FOXML1_0DODeserializer() {
        super(FOXML1_0);
    }
}