/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security.servletfilters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*
 * import java.security.Principal; import java.util.Map; import java.util.Set;
 * import java.util.HashSet; import java.util.Hashtable;
 */

/**
 * @author Bill Niebel
 */
public class Principal
        implements java.security.Principal {

    private final Log log = LogFactory.getLog(Principal.class);

    private final String name;

    public Principal(String name) {
        //this.authority = null;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        //need to re-implement this        
        return "Principal[" + getName() + "]";
    }

    @Override
    public int hashCode() {
        //need to implement this        
        return 1;
    }

    @Override
    public boolean equals(Object another) {
        //need to implement this        
        return false;
    }

    public String[] getRoles() {
        return new String[0];
    }

}
