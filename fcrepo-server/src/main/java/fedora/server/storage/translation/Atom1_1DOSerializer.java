/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.translation;

/**
 * Serializes objects in Atom 1.1 format.
 *
 * @author Edwin Shin
 * @since 3.0
 * @version $Id$
 */
public class Atom1_1DOSerializer
        extends AtomDOSerializer {

    public Atom1_1DOSerializer() {
        super(ATOM1_1);
    }
}
