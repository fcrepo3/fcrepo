/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.batch.types;

/**
 * Launch a dialog for entering information for a new object (title, content
 * model, and possibly a specified pid), then create the object on the server
 * and launch an editor on it.
 * 
 * @author Ross Wayland
 */
public class DigitalObject {

    public String pid;

    public String label;

    public boolean force = false;

    public String state;

    public String ownerId;

    public String logMessage;

    public DigitalObject() {
    }

}
