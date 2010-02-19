
package org.fcrepo.server.security.xacml;


import org.fcrepo.server.security.xacml.pdp.MelcoePDPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Edwin Shin
 */
public class MelcoeXacmlException
        extends Exception {

    private static final long serialVersionUID = 1L;

    private static final Logger logger =
            LoggerFactory.getLogger(MelcoePDPException.class);

    public MelcoeXacmlException() {
        super();
        logger.error("No message provided");
    }

    public MelcoeXacmlException(String msg) {
        super(msg);
        logger.error(msg);
    }

    public MelcoeXacmlException(Throwable t) {
        super(t);
        logger.error(t.getMessage());
    }

    public MelcoeXacmlException(String msg, Throwable t) {
        super(msg, t);
        logger.error(msg, t);
    }
}
