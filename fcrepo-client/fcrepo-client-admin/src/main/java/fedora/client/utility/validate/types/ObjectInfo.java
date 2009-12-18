/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate.types;

import java.util.Collection;

/**
 * An abstraction of a digital object, containing only those fields and
 * attributes that are needed for validation. (As validation becomes more
 * elaborate, this interface will also.)
 * 
 * @author Jim Blake
 */
public interface ObjectInfo {

    public String getPid();

    public boolean hasRelation(String relationship);

    public Collection<RelationshipInfo> getRelations(String relationship);

    public Collection<String> getDatastreamIds();

    public DatastreamInfo getDatastreamInfo(String dsId);

    public Collection<String> getContentModels();

    public boolean hasContentModel(String contentmodelpid);

}
