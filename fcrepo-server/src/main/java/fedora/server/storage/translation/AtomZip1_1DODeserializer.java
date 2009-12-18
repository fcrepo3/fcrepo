/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.translation;

/**
 * Deserializes objects in Atom Zip 1.1 format.
 *
 * @author Edwin Shin
 * @since 3.0
 * @version $Id$
 */
public class AtomZip1_1DODeserializer
        extends AtomDODeserializer {

    public AtomZip1_1DODeserializer() {
        super(ATOM_ZIP1_1);
    }
}