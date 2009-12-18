/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage;

// import fedora.server.ParameterizedComponent;

import java.net.URL;

import java.util.Iterator;

/**
 * @author Chris Wilper
 */
public abstract class DORegistry {

    public abstract URL add();

    public abstract URL get(String PID);

    public abstract void remove();

    public abstract Iterator ids();

}
