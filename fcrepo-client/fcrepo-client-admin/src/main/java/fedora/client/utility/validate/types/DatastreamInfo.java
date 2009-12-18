/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate.types;

/**
 * Information required to validate an object's datastream against a content
 * model.
 * 
 * @author Jim Blake
 */
public class DatastreamInfo {

    private final String id;

    private final String mimeType;

    private final String formatUri;

    public DatastreamInfo(String id, String mimeType, String formatUri) {
        this.id = id;
        this.mimeType = mimeType;
        this.formatUri = formatUri;
    }

    public String getId() {
        return id;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFormatUri() {
        return formatUri;
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

        DatastreamInfo that = (DatastreamInfo) obj;
        return equivalent(id, that.id) && equivalent(mimeType, that.mimeType)
                && equivalent(formatUri, that.formatUri);
    }

    private boolean equivalent(Object obj1, Object obj2) {
        return obj1 == null ? obj2 == null : obj1.equals(obj2);
    }

    @Override
    public int hashCode() {
        return hashIt(id) ^ hashIt(mimeType) ^ hashIt(formatUri);
    }

    private int hashIt(Object obj) {
        return obj == null ? 0 : obj.hashCode();
    }

    @Override
    public String toString() {
        return "DatastreamInfo[id='" + id + "', mimeType='" + mimeType
                + "', formatUri='" + formatUri + "']";
    }

}
