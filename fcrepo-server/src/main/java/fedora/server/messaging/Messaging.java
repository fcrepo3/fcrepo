/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.messaging;

import fedora.server.errors.MessagingException;

/**
 * The Messaging subsystem interface.
 *
 * @author Edwin Shin
 * @version $Id$
 */
public interface Messaging {

    enum MessageType {
        apimUpdate, apimAccess;
    }

    /**
     * Send the <code>FedoraMessage</code> to the specified destination.
     *
     * @param destName The destination of the message.
     * @param message The message to send.
     * @throws MessagingException
     */
    public void send(String destName, FedoraMessage message) throws MessagingException;

    /**
     * Send a message representing the <code>FedoraMethod</code>.
     * The message representation and destination(s) are determined by the
     * implementing class.
     *
     * @param method The method to send.
     * @throws MessagingException
     */
    public void send(FedoraMethod method) throws MessagingException;

    /**
     * Shutdown and/or close any resources and/or connections.
     *
     * @throws MessagingException
     */
    public void close() throws MessagingException;
}
