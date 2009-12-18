/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.types;

import java.util.Date;

/**
 * @author Chris Wilper
 */
public class AuditRecord {

    public AuditRecord() {
    }

    public String id;

    public String processType;

    public String action;

    public String componentID;

    public String responsibility;

    public Date date;

    public String justification;
}
