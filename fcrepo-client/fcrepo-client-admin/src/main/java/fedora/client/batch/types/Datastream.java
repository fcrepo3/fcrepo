/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.batch.types;

/**
 * @author Sandy Payette
 */
public class Datastream {

    public String dsID;

    public String dsVersionID;

    public String dsLabel;

    public String dsMIME;

    public String asOfDate;

    public String endDate;

    public String dsControlGrp;

    public String dsInfoType;

    public String dsState;

    public String dsLocation;

    public String objectPID;

    public boolean versionable = true;

    public String formatURI;

    public byte[] xmlContent;

    public boolean force = false;

    public String[] altIDs = new String[0];

    public String logMessage;

    public String checksumType;

    public String checksum;

}
