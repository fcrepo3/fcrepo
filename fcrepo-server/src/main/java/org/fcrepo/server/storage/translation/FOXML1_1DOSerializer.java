/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.storage.translation;

/**
 * Serializes objects in FOXML 1.1 format.
 * 
 * @author Chris Wilper
 */
public class FOXML1_1DOSerializer
        extends FOXMLDOSerializer {

    /**
     * Constructs an instance.
     */
    public FOXML1_1DOSerializer() {
        this(null);
    }
    public FOXML1_1DOSerializer(DOTranslationUtility translator) {
        super(FOXML1_1, translator);
    }
}