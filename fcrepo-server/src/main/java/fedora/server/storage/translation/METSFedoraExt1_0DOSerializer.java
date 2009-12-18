/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

/**
 * Serializes objects in METS Fedora Extension 1.0 format.
 * 
 * @author Chris Wilper
 */
public class METSFedoraExt1_0DOSerializer
        extends METSFedoraExtDOSerializer {

    /**
     * Constructs an instance.
     */
    public METSFedoraExt1_0DOSerializer() {
        super(METS_EXT1_0);
    }
}