/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.access;

import java.util.Date;
import java.util.Set;

/**
 * Data structure to contain a profile of a digital object that includes
 * both stored information about the object and dynamic information about
 * the object.
 *
 * @author Sandy Payette
 */
public class ObjectProfile {

    public String PID = null;

    public String objectLabel = null;

    public String objectOwnerId = null;

    public Set<String> objectModels = null;

    public Date objectCreateDate = null;

    public Date objectLastModDate = null;

    public String dissIndexViewURL = null;

    public String itemIndexViewURL = null;

    public String objectState = null;
}
