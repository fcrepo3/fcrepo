/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.types;

import java.util.Date;

/**
 * @author Sandy Payette
 */
@Deprecated
public class Disseminator {

    public String parentPID;

    public boolean isNew = false;

    public String dissID;

    public String dissLabel;

    public String dissVersionID;

    public String bDefID;

    public String sDepID;

    public String dsBindMapID;

    public DSBindingMap dsBindMap;

    public Date dissCreateDT;

    public String dissState;

    public boolean dissVersionable;

    public Disseminator() {
    }
}
