/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.objecteditor;

/**
 * Interface for things that listen to validity changes.
 */
public interface ValidityListener {

    public void setValid(boolean isValid);

}