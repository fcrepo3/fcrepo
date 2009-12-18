/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.types;

/**
 * @author Sandy Payette
 */
public class MethodDef {

    public String methodName = null;

    public String methodLabel = null;

    public MethodParmDef[] methodParms = new MethodParmDef[0];

    public MethodDef() {
    }

}
