/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.types;

import java.util.Date;

/**
 * Data structure to contain all method definitions for a digital object.
 * 
 * @author Ross Wayland
 */
public class ObjectMethodsDef {

    public String PID = null;

    public String sDefPID = null;

    public String methodName = null;

    public MethodParmDef[] methodParmDefs = null;

    public Date asOfDate = null;
}
