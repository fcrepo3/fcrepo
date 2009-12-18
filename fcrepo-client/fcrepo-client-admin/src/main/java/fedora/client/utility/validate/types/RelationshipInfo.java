/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate.types;

/**
 * Information needed to validate an object's relationships against its content
 * model.
 * 
 * @author Jim Blake
 */
public class RelationshipInfo {

    private final String predicate;

    private final String object;

    public RelationshipInfo(String predicate, String object) {
        this.predicate = predicate;
        this.object = object;
    }

    public String getPredicate() {
        return predicate;
    }

    public String getObject() {
        return object;
    }

    public String getObjectPid() {
        if (object != null && object.startsWith("info:fedora/")) {
            return object.substring(12);
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!this.getClass().equals(obj.getClass())) {
            return false;
        }

        RelationshipInfo that = (RelationshipInfo) obj;
        return equivalent(predicate, that.predicate)
                && equivalent(object, that.object);
    }

    @Override
    public int hashCode() {
        return hashIt(predicate) ^ hashIt(object);
    }

    @Override
    public String toString() {
        return "RelationshipInfo[predicate='" + predicate + "', object='"
                + object + "']";
    }

    private boolean equivalent(Object obj1, Object obj2) {
        return obj1 == null ? obj2 == null : obj1.equals(obj2);
    }

    private int hashIt(Object obj) {
        return obj == null ? 0 : obj.hashCode();
    }

}
