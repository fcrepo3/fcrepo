/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fedora.common.Constants;

/**
 * A simple immutable implementation of {@link ObjectInfo}.
 * 
 * @author Jim Blake
 */
public class BasicObjectInfo
        implements ObjectInfo {

    private final String pid;

    private final Set<RelationshipInfo> relations;

    private final Map<String, DatastreamInfo> datastreamMap;

    /** Create a "stub" object with no relations, no datastreams. */
    public BasicObjectInfo(String pid) {
        this(pid,
             new HashSet<RelationshipInfo>(),
             new HashSet<DatastreamInfo>());
    }

    /** Create a full object. */
    public BasicObjectInfo(String pid,
                           Collection<RelationshipInfo> relations,
                           Collection<DatastreamInfo> datastreams) {
        if (pid == null) {
            throw new NullPointerException("'pid' may not be null");
        }
        if (relations == null) {
            throw new NullPointerException("'relations' may not be null");
        }
        if (datastreams == null) {
            throw new NullPointerException("'datastreams' may not be null");
        }

        this.pid = pid;

        this.relations =
                Collections
                        .unmodifiableSet(new HashSet<RelationshipInfo>(relations));

        Map<String, DatastreamInfo> datastreamMap =
                new HashMap<String, DatastreamInfo>();
        for (DatastreamInfo dsInfo : datastreams) {
            datastreamMap.put(dsInfo.getId(), dsInfo);
        }
        this.datastreamMap = Collections.unmodifiableMap(datastreamMap);
    }

    public String getPid() {
        return pid;
    }

    public boolean hasRelation(String relationship) {
        if (relationship == null) {
            throw new NullPointerException("'relationship' may not be null.");
        }
        for (RelationshipInfo relation : relations) {
            if (relationship.equals(relation.getPredicate())) {
                return true;
            }
        }
        return false;
    }

    public Collection<RelationshipInfo> getRelations(String relationship) {
        List<RelationshipInfo> result = new ArrayList<RelationshipInfo>();
        for (RelationshipInfo relation : relations) {
            if (relationship.equals(relation.getPredicate())) {
                result.add(relation);
            }
        }
        return result;
    }

    public Collection<String> getDatastreamIds() {
        return new HashSet<String>(datastreamMap.keySet());
    }

    public DatastreamInfo getDatastreamInfo(String dsId) {
        return datastreamMap.get(dsId);
    }

    public Collection<String> getContentModels() {
        //TODO: This is one of the methods to change for inheritance
        Collection<RelationshipInfo> cms = getRelations(Constants.MODEL.HAS_MODEL.uri);
        List<String> result = new ArrayList<String>();
        for (RelationshipInfo cm:cms){
            String uri = cm.getObject();
            result.add(uri);
        }
        return result;
    }

    public boolean hasContentModel(String contentmodelpid) {
        //TODO: This is one of the methods to change for inheritance
        Collection<RelationshipInfo> cms = getRelations(Constants.MODEL.HAS_MODEL.uri);
        for (RelationshipInfo cm:cms){
            if (cm.getObjectPid().equals(contentmodelpid)){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BasicObjectInfo that = (BasicObjectInfo) obj;
        return equivalent(pid, that.pid)
                && equivalent(datastreamMap, that.datastreamMap)
                && equivalent(relations, that.relations);
    }

    private boolean equivalent(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    @Override
    public int hashCode() {
        return hashIt(pid) ^ hashIt(datastreamMap) ^ hashIt(relations);
    }

    private int hashIt(Object o) {
        return o == null ? 0 : o.hashCode();
    }

    @Override
    public String toString() {
        return "BasicObjectInfo[pid='" + pid + "', relations=" + relations
                + "', datastreamMap=" + datastreamMap + "]";
    }

}
