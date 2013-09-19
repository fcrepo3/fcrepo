/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage.types;

/**
 * @author Sandy Payette
 */
public class MethodDef {
    
    protected static final String[] EMPTY_STRING_ARRAY = new String[0];
    
    protected static final MethodParmDef[] EMPTY_PARMDEF_ARRAY =
            new MethodParmDef[0];

    public String methodName = null;

    public String methodLabel = null;

    public MethodParmDef[] methodParms = EMPTY_PARMDEF_ARRAY;

    public MethodDef() {
    }

}
