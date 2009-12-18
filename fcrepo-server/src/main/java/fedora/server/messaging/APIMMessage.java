/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.messaging;

import java.util.Date;

/**
 * @author Edwin Shin
 * @since 3.0
 * @version $Id$
 */
public interface APIMMessage
        extends FedoraMessage {

    /**
     * @return the Base URL of the Fedora Repository that generated the message,
     *         e.g. http://localhost:8080/fedora
     */
    public String getBaseUrl();

    /**
     * @return the PID or null if not applicable for the API-M method
     */
    public String getPID();

    /**
     * @return the name of the API-M method invoked
     */
    public String getMethodName();

    /**
     * @return the Date object representing the timestamp of the method call
     */
    public Date getDate();

    // TODO: What about a getter for the API-M method arguments and return value?
}
