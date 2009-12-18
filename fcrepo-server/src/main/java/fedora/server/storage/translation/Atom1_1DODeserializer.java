/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.translation;

/**
 * Deserializes objects in Atom 1.1 format.
 *
 * @author Edwin Shin
 * @version $Id$
 */
public class Atom1_1DODeserializer
        extends AtomDODeserializer {

    public Atom1_1DODeserializer() {
        super(ATOM1_1);
    }
}
