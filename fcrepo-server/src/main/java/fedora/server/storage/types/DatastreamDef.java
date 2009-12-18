/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.types;

/**
 * @author Ross Wayland
 */
public class DatastreamDef {

    public final String dsID;

    public final String dsLabel;

    public final String dsMIME;

    public DatastreamDef(String dsID, String dsLabel, String dsMIME) {
        this.dsID = dsID;
        this.dsLabel = dsLabel;
        this.dsMIME = dsMIME;
    }

    @Override
    public String toString() {
        return "DatastreamDef[dsID=" + dsID + ", dsLabel=" + dsLabel
                + ", dsMIME=" + dsMIME + "]";
    }

}
