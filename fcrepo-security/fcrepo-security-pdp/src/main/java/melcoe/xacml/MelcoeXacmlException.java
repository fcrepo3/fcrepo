
package melcoe.xacml;

import melcoe.xacml.pdp.MelcoePDPException;

import org.apache.log4j.Logger;

/**
 * @author Edwin Shin
 */
public class MelcoeXacmlException
        extends Exception {

    private static final long serialVersionUID = 1L;

    private static final Logger log =
            Logger.getLogger(MelcoePDPException.class.getName());

    public MelcoeXacmlException() {
        super();
        log.error("No message provided");
    }

    public MelcoeXacmlException(String msg) {
        super(msg);
        log.error(msg);
    }

    public MelcoeXacmlException(Throwable t) {
        super(t);
        log.error(t.getMessage());
    }

    public MelcoeXacmlException(String msg, Throwable t) {
        super(msg, t);
        log.error(msg, t);
    }
}
