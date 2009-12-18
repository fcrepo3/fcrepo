/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate;

import fedora.client.utility.validate.types.ContentModelInfo;
import fedora.client.utility.validate.types.ObjectInfo;

/**
 * Provides an abstract wrapper around the repository of digital objects.
 * 
 * @author Jim Blake
 */
public interface ObjectSource {

    /**
     * Get the object that has this PID, or <code>null</code> if there is no
     * such object.
     */
    ObjectInfo getValidationObject(String pid) throws ObjectSourceException;

    /**
     * Get the object that has this PID (or <code>null</code>) and confirm
     * that it is a valid content model.
     */
    ContentModelInfo getContentModelInfo(String pid)
            throws ObjectSourceException, InvalidContentModelException;
}
