/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.types;

/**
 * @author Sandy Payette
 */
@Deprecated
@SuppressWarnings("deprecation")
public class DSBindingMap {

    public String dsBindMapID = null;

    public String dsBindMechanismPID = null;

    public String dsBindMapLabel = null;

    public String state = null;

    public DSBinding[] dsBindings = new DSBinding[0];

    public DSBindingMap() {
    }
}
