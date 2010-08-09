
package org.fcrepo.server.security.xacml;



/**
 * @author Edwin Shin
 */
public class MelcoeXacmlException
        extends Exception {

    private static final long serialVersionUID = 1L;

    public MelcoeXacmlException() {
        super();
    }

    public MelcoeXacmlException(String msg) {
        super(msg);
    }

    public MelcoeXacmlException(Throwable t) {
        super(t);
    }

    public MelcoeXacmlException(String msg, Throwable t) {
        super(msg, t);
    }
}
