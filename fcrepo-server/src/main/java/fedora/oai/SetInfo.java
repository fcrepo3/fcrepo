/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai;

import java.util.Set;

/**
 * Describes a set in the repository.
 * 
 * @author Chris Wilper
 * @see <a
 *      href="http://www.openarchives.org/OAI/openarchivesprotocol.html#ListSets">
 *      http://www.openarchives.org/OAI/openarchivesprotocol.html#ListSets</a>
 */
public interface SetInfo {

    /**
     * Get the name of the set.
     */
    public abstract String getName();

    /**
     * Get the setSpec of the set.
     */
    public abstract String getSpec();

    /**
     * Get the descriptions of the set.
     */
    public abstract Set getDescriptions();

}
