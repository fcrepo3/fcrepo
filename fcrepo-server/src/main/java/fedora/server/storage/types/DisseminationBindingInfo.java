/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.types;

import java.util.Date;

/**
 * Data struture for holding information necessary to complete a 
 * dissemination request.
 * 
 * @author Ross Wayland
 */
public class DisseminationBindingInfo {
    
    public String DSBindKey = null;

    public MethodParmDef[] methodParms = null;

    public String dsLocation = null;

    public String dsControlGroupType = null;

    public String dsID = null;

    public String dsVersionID = null;

    public String AddressLocation = null;

    public String OperationLocation = null;

    public String ProtocolType = null;

    public String dsState = null;
    
    public Date dsCreateDT = null;
}
