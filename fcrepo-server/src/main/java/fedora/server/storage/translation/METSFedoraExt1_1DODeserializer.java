/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

/**
 * Deserializes objects in METS_EXT 1.1 format.
 * 
 * @author Chris Wilper
 */
public class METSFedoraExt1_1DODeserializer
        extends METSFedoraExtDODeserializer {

    /**
     * Constructs an instance.
     */
    public METSFedoraExt1_1DODeserializer() {
        super(METS_EXT1_1);
    }
}