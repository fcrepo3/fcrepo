/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.service;

import java.util.Hashtable;

/**
 * @author Sandy Payette
 */
public class Mmap {

    public String mmapName = null;

    public MmapMethodDef[] mmapMethods = new MmapMethodDef[0];

    public Hashtable wsdlOperationToMethodDef;

}
